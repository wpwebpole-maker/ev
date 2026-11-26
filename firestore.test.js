const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const CUSTOMER_A = "customer_a_uid";
const CUSTOMER_B = "customer_b_uid";
const ADMIN_UID = "super_admin_uid";
const DELEGATED_ADMIN_UID = "delegated_admin_uid";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

const pastTime = (offsetMs = 5000) => new Date(Date.now() - offsetMs);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function adminContext() {
  return testEnv.authenticatedContext(ADMIN_UID, {
    email: "suchita9867@gmail.com",
    email_verified: true,
  });
}

function spoofedAdminContext() {
  return testEnv.authenticatedContext("spoof_uid", {
    email: "suchita9867@gmail.com",
    email_verified: false,
  });
}

function customerAContext() {
  return testEnv.authenticatedContext(CUSTOMER_A, {
    email: "customer_a@example.com",
    email_verified: true,
  });
}

function customerBContext() {
  return testEnv.authenticatedContext(CUSTOMER_B, {
    email: "customer_b@example.com",
    email_verified: true,
  });
}

test("Unauthenticated users cannot read or write any collection", async () => {
  const db = testEnv.unauthenticatedContext().firestore();
  await assertFails(db.collection("users").doc(CUSTOMER_A).get());
  await assertFails(db.collection("applications").get());
  await assertFails(db.collection("payments").get());
  await assertFails(db.collection("settings").doc("appConfig").get());
  await assertFails(db.collection("adminLogs").get());
});

test("Email spoofing with unverified admin email is rejected", async () => {
  const db = spoofedAdminContext().firestore();
  await assertFails(db.collection("applications").get());
  await assertFails(db.collection("users").get());
  await assertFails(
    db.collection("settings").doc("appConfig").set({
      businessName: "Spoofed",
      contactPhone: "9507402469",
      upiId: "evil@upi",
      upiRecipientName: "Evil",
      defaultAmount: 1,
      updatedBy: "spoof",
      updatedAt: pastTime(),
    })
  );
});

test("Customer profile isolation, self-update, and anti-escalation", async () => {
  const dbA = customerAContext().firestore();
  const dbB = customerBContext().firestore();
  const created = pastTime(10000);

  // Customer A creates own profile
  await assertSucceeds(
    dbA.collection("users").doc(CUSTOMER_A).set({
      uid: CUSTOMER_A,
      name: "Ravi Kumar",
      email: "customer_a@example.com",
      mobile: "9876543210",
      address: "Station Road, Patna",
      city: "Patna",
      pinCode: "800001",
      accountStatus: "ACTIVE",
      consentAccepted: true,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Shadow field injection rejected (role / isAdmin)
  await assertFails(
    dbB.collection("users").doc(CUSTOMER_B).set({
      uid: CUSTOMER_B,
      name: "Bob",
      email: "b@example.com",
      accountStatus: "ACTIVE",
      consentAccepted: true,
      role: "SUPER_ADMIN",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Customer B cannot read Customer A profile or list users
  await assertFails(dbB.collection("users").doc(CUSTOMER_A).get());
  await assertFails(dbB.collection("users").get());

  // Customer A can update own allowed profile fields
  await assertSucceeds(
    dbA.collection("users").doc(CUSTOMER_A).update({
      mobile: "9507402469",
      updatedAt: pastTime(1000),
    })
  );

  // Customer A cannot disable own accountStatus or mutate createdAt
  await assertFails(
    dbA.collection("users").doc(CUSTOMER_A).update({
      accountStatus: "DISABLED",
      updatedAt: pastTime(500),
    })
  );
  await assertFails(
    dbA.collection("users").doc(CUSTOMER_A).update({
      createdAt: pastTime(500),
      updatedAt: pastTime(500),
    })
  );

  // Customer A cannot promote self in admins collection
  await assertFails(
    dbA.collection("admins").doc(CUSTOMER_A).set({
      uid: CUSTOMER_A,
      email: "customer_a@example.com",
      name: "Ravi Kumar",
      role: "SUPER_ADMIN",
      active: true,
      createdBy: CUSTOMER_A,
      createdAt: created,
      updatedAt: created,
    })
  );
});

test("Applications: customer creation, isolation, no self-approval, and admin approval", async () => {
  const dbA = customerAContext().firestore();
  const dbB = customerBContext().firestore();
  const dbAdmin = adminContext().firestore();
  const created = pastTime(10000);

  // Customer A submits valid application
  await assertSucceeds(
    dbA.collection("applications").doc("app_001").set({
      applicationCode: "RAAG-2026-0001",
      customerUid: CUSTOMER_A,
      fullName: "Ravi Kumar",
      mobile: "9876543210",
      email: "customer_a@example.com",
      address: "Main Road, Patna",
      city: "Patna",
      pinCode: "800001",
      vehicleBrand: "Ola Electric",
      vehicleModel: "S1 Pro",
      registrationNumber: "BR01EV2626",
      batteryNumber: "BAT998877",
      documentIds: {},
      applicationStatus: "SUBMITTED",
      paymentStatus: "UNPAID",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Customer A cannot create with APPROVED status or VERIFIED payment
  await assertFails(
    dbA.collection("applications").doc("app_cheat").set({
      applicationCode: "RAAG-2026-9999",
      customerUid: CUSTOMER_A,
      fullName: "Cheat",
      mobile: "9876543210",
      address: "Test Address",
      applicationStatus: "APPROVED",
      paymentStatus: "VERIFIED",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Customer A filtered query succeeds; unfiltered query fails
  await assertSucceeds(
    dbA.collection("applications").where("customerUid", "==", CUSTOMER_A).get()
  );
  await assertFails(dbA.collection("applications").get());

  // Customer B cannot read Customer A's application
  await assertFails(dbB.collection("applications").doc("app_001").get());

  // Customer A cannot self-approve application or mark payment VERIFIED
  await assertFails(
    dbA.collection("applications").doc("app_001").update({
      applicationStatus: "APPROVED",
      updatedAt: pastTime(1000),
    })
  );
  await assertFails(
    dbA.collection("applications").doc("app_001").update({
      paymentStatus: "VERIFIED",
      updatedAt: pastTime(1000),
    })
  );

  // Admin can list all applications and approve with remarks
  await assertSucceeds(dbAdmin.collection("applications").get());
  await assertSucceeds(
    dbAdmin.collection("applications").doc("app_001").update({
      applicationStatus: "APPROVED",
      adminRemarks: "All photos and details verified.",
      updatedAt: pastTime(1000),
    })
  );
});

test("Payments & Private Documents: customer cannot self-verify or read others", async () => {
  const dbA = customerAContext().firestore();
  const dbB = customerBContext().firestore();
  const dbAdmin = adminContext().firestore();
  const created = pastTime(10000);

  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("applications").doc("app_002").set({
      applicationCode: "RAAG-2026-0002",
      customerUid: CUSTOMER_A,
      fullName: "Ravi Kumar",
      mobile: "9876543210",
      address: "Patna, Bihar",
      applicationStatus: "SUBMITTED",
      paymentStatus: "UNPAID",
      createdAt: created,
      updatedAt: created,
    });
  });

  // Customer A uploads payment screenshot doc
  await assertSucceeds(
    dbA.collection("applicationDocs").doc("doc_pay_1").set({
      customerUid: CUSTOMER_A,
      applicationId: "app_002",
      docType: "PAYMENT_SCREENSHOT",
      fileName: "payment_4500.jpg",
      mimeType: "image/jpeg",
      sizeBytes: 42000,
      payloadBase64: "data_base64_mock_payload_1234567890",
      createdAt: created,
    })
  );

  // Customer A cannot upload a POLICY_PDF
  await assertFails(
    dbA.collection("applicationDocs").doc("doc_fake_policy").set({
      customerUid: CUSTOMER_A,
      applicationId: "app_002",
      docType: "POLICY_PDF",
      fileName: "fake_policy.pdf",
      mimeType: "application/pdf",
      sizeBytes: 12000,
      payloadBase64: "data_base64_mock_payload_1234567890",
      createdAt: created,
    })
  );

  // Customer B cannot read Customer A's payment screenshot
  await assertFails(dbB.collection("applicationDocs").doc("doc_pay_1").get());

  // Customer A submits payment with PENDING_VERIFICATION
  await assertSucceeds(
    dbA.collection("payments").doc("pay_001").set({
      applicationId: "app_002",
      applicationCode: "RAAG-2026-0002",
      customerUid: CUSTOMER_A,
      customerName: "Ravi Kumar",
      customerMobile: "9876543210",
      amount: 4500,
      upiId: "9507402469-3@ibl",
      utrNumber: "UTR1234567890",
      screenshotDocId: "doc_pay_1",
      paymentStatus: "PENDING_VERIFICATION",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Customer A cannot self-verify payment
  await assertFails(
    dbA.collection("payments").doc("pay_001").update({
      paymentStatus: "VERIFIED",
      updatedAt: pastTime(500),
    })
  );

  // Admin verifies payment
  await assertSucceeds(
    dbAdmin.collection("payments").doc("pay_001").update({
      paymentStatus: "VERIFIED",
      verificationRemarks: "UTR matched in bank statement",
      verifiedBy: "suchita9867@gmail.com",
      verifiedAt: pastTime(500),
      updatedAt: pastTime(500),
    })
  );
});

test("Settings, Policies, Delegated Admin RBAC, and Audit Logs", async () => {
  const dbA = customerAContext().firestore();
  const dbAdmin = adminContext().firestore();
  const created = pastTime(10000);

  // Super Admin writes appConfig
  await assertSucceeds(
    dbAdmin.collection("settings").doc("appConfig").set({
      businessName: "RAAG AUTOMOBILES – EV INSURANCE",
      contactPhone: "9507402469",
      supportEmail: "suchita9867@gmail.com",
      upiId: "9507402469-3@ibl",
      upiRecipientName: "RAAG AUTOMOBILES",
      defaultAmount: 4500,
      paymentInstructions: "Scan QR or pay via UPI app and upload screenshot.",
      privacyPolicyUrl: "https://raagautomobiles.com/privacy",
      termsUrl: "https://raagautomobiles.com/terms",
      updatedBy: "suchita9867@gmail.com",
      updatedAt: created,
    })
  );

  // Customer can read appConfig but cannot modify it
  await assertSucceeds(dbA.collection("settings").doc("appConfig").get());
  await assertFails(
    dbA.collection("settings").doc("appConfig").update({
      defaultAmount: 1,
      updatedBy: CUSTOMER_A,
      updatedAt: pastTime(500),
    })
  );

  // Super Admin promotes delegated admin
  await assertSucceeds(
    dbAdmin.collection("admins").doc(DELEGATED_ADMIN_UID).set({
      uid: DELEGATED_ADMIN_UID,
      email: "manager@raag.com",
      name: "Branch Manager",
      role: "APPLICATION_MANAGER",
      active: true,
      createdBy: ADMIN_UID,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Delegated admin (APPLICATION_MANAGER) can list applications & write audit log, but cannot edit settings
  const dbDelegated = testEnv.authenticatedContext(DELEGATED_ADMIN_UID, {
    email: "manager@raag.com",
    email_verified: true,
  }).firestore();
  await assertSucceeds(dbDelegated.collection("applications").get());
  await assertSucceeds(
    dbDelegated.collection("adminLogs").doc("log_1").set({
      adminUid: DELEGATED_ADMIN_UID,
      adminEmail: "manager@raag.com",
      action: "APPLICATION_REVIEWED",
      recordId: "app_001",
      details: "Reviewed photos",
      createdAt: pastTime(500),
    })
  );
  await assertFails(
    dbDelegated.collection("settings").doc("appConfig").update({
      defaultAmount: 5000,
      updatedBy: "manager@raag.com",
      updatedAt: pastTime(500),
    })
  );

  // Audit logs are immutable
  await assertFails(
    dbAdmin.collection("adminLogs").doc("log_1").update({
      details: "Tampered",
    })
  );
});

test("Additional bootstrapped Super Admins (wpwebpole@gmail.com & vk725846@gmail.com) have verified access only", async () => {
  const created = pastTime(5000);
  const wpAdminDb = testEnv.authenticatedContext("wp_admin_uid", {
    email: "wpwebpole@gmail.com",
    email_verified: true,
  }).firestore();
  const vkAdminDb = testEnv.authenticatedContext("vk_admin_uid", {
    email: "vk725846@gmail.com",
    email_verified: true,
  }).firestore();
  const unverifiedVkDb = testEnv.authenticatedContext("vk_spoof_uid", {
    email: "vk725846@gmail.com",
    email_verified: false,
  }).firestore();

  await assertSucceeds(wpAdminDb.collection("applications").get());
  await assertSucceeds(
    wpAdminDb.collection("settings").doc("appConfig").set({
      businessName: "RAAG AUTOMOBILES – EV INSURANCE",
      contactPhone: "9507402469",
      upiId: "9507402469-3@ibl",
      upiRecipientName: "RAAG AUTOMOBILES",
      defaultAmount: 4500,
      updatedBy: "wpwebpole@gmail.com",
      updatedAt: created,
    })
  );

  await assertSucceeds(vkAdminDb.collection("users").get());
  await assertSucceeds(
    vkAdminDb.collection("adminLogs").doc("log_vk").set({
      adminUid: "vk_admin_uid",
      adminEmail: "vk725846@gmail.com",
      action: "ADMIN_VERIFIED",
      recordId: "appConfig",
      details: "Verified VK admin access",
      createdAt: pastTime(1000),
    })
  );

  await assertFails(unverifiedVkDb.collection("applications").get());
});

