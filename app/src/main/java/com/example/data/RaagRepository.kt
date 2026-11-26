package com.example.data

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class RaagRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")

    private fun currentUserEmail(): String = auth.currentUser?.email ?: ""

    private fun <T> Flow<T>.logFirestoreErrors(op: OperationType, path: String, expectedUid: String): Flow<T> =
        this.catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val toLog = firestoreError ?: (error as? Exception)
            if (toLog != null && auth.currentUser?.uid == expectedUid) {
                handleFirestoreError(toLog, op, path)
            }
            throw error
        }

    // --- CUSTOMER OBSERVERS ---

    fun observeUserProfile(uid: String): Flow<UserProfile?> =
        db.collection("users").document(uid)
            .snapshots()
            .map { it.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .logFirestoreErrors(OperationType.GET, "users/$uid", uid)

    fun observeAdminRecord(uid: String): Flow<AdminRecord?> =
        db.collection("admins").document(uid)
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(AdminRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                } else null
            }
            .catch { error ->
                val root = unwrapFirestoreError(error)
                if (root is FirebaseFirestoreException && root.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                    emit(null)
                } else {
                    throw error
                }
            }

    fun observeAppSettings(uid: String): Flow<AppSettings> =
        db.collection("settings").document("appConfig")
            .snapshots()
            .map { snapshot ->
                snapshot.toObject(AppSettings::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: AppSettings()
            }
            .catch { error ->
                unwrapFirestoreError(error)
                emit(AppSettings())
            }

    fun observeCustomerApplications(uid: String): Flow<List<InsuranceApplication>> =
        db.collection("applications")
            .whereEqualTo("customerUid", uid)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(InsuranceApplication::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .logFirestoreErrors(OperationType.LIST, "applications", uid)

    fun observeCustomerPayments(uid: String): Flow<List<PaymentRecord>> =
        db.collection("payments")
            .whereEqualTo("customerUid", uid)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(PaymentRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .logFirestoreErrors(OperationType.LIST, "payments", uid)

    fun observeCustomerPolicies(uid: String): Flow<List<PolicyRecord>> =
        db.collection("policies")
            .whereEqualTo("customerUid", uid)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(PolicyRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .logFirestoreErrors(OperationType.LIST, "policies", uid)

    fun observeNotifications(uid: String): Flow<List<AppNotification>> =
        db.collection("notifications")
            .whereEqualTo("recipientUid", uid)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(AppNotification::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .logFirestoreErrors(OperationType.LIST, "notifications", uid)

    // --- ADMIN OBSERVERS (CALLED ONLY WHEN VERIFIED ADMIN) ---

    fun observeAllCustomers(adminUid: String): Flow<List<UserProfile>> =
        db.collection("users")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .logFirestoreErrors(OperationType.LIST, "users", adminUid)

    fun observeAllApplications(adminUid: String): Flow<List<InsuranceApplication>> =
        db.collection("applications")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(InsuranceApplication::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .logFirestoreErrors(OperationType.LIST, "applications", adminUid)

    fun observeAllPayments(adminUid: String): Flow<List<PaymentRecord>> =
        db.collection("payments")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(PaymentRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .logFirestoreErrors(OperationType.LIST, "payments", adminUid)

    fun observeAllPolicies(adminUid: String): Flow<List<PolicyRecord>> =
        db.collection("policies")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(PolicyRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .logFirestoreErrors(OperationType.LIST, "policies", adminUid)

    fun observeAllAdmins(adminUid: String): Flow<List<AdminRecord>> =
        db.collection("admins")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(AdminRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.name.lowercase() }
            }
            .logFirestoreErrors(OperationType.LIST, "admins", adminUid)

    fun observeAdminLogs(adminUid: String): Flow<List<AdminAuditLog>> =
        db.collection("adminLogs")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(AdminAuditLog::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .logFirestoreErrors(OperationType.LIST, "adminLogs", adminUid)

    // --- CUSTOMER MUTATIONS ---

    suspend fun upsertUserProfile(
        name: String,
        mobile: String,
        address: String,
        city: String,
        pinCode: String,
        consentAccepted: Boolean
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "users/$uid"
        return try {
            val docRef = db.collection("users").document(uid)
            val existingSnap = docRef.get().await()
            val cleanName = name.trim().ifBlank { auth.currentUser?.displayName ?: "Customer" }.take(120)
            if (existingSnap.exists()) {
                val updates = mutableMapOf<String, Any>(
                    "name" to cleanName,
                    "mobile" to mobile.trim().take(20),
                    "address" to address.trim().take(400),
                    "city" to city.trim().take(80),
                    "pinCode" to pinCode.trim().take(12),
                    "consentAccepted" to consentAccepted,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updates).await()
            } else {
                val payload = mapOf(
                    "uid" to uid,
                    "name" to cleanName,
                    "email" to currentUserEmail().take(180),
                    "mobile" to mobile.trim().take(20),
                    "address" to address.trim().take(400),
                    "city" to city.trim().take(80),
                    "pinCode" to pinCode.trim().take(12),
                    "accountStatus" to "ACTIVE",
                    "consentAccepted" to consentAccepted,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun uploadDocument(
        targetCustomerUid: String,
        applicationId: String,
        docType: DocumentType,
        fileName: String,
        mimeType: String,
        sizeBytes: Long,
        base64Payload: String
    ): Result<String> {
        var path = "applicationDocs"
        return try {
            requireUserId()
            val docId = "doc_${UUID.randomUUID().toString().replace("-", "").take(16)}"
            path = "applicationDocs/$docId"
            val payload = mapOf(
                "customerUid" to targetCustomerUid,
                "applicationId" to applicationId,
                "docType" to docType.name,
                "fileName" to fileName.take(160),
                "mimeType" to mimeType.take(80),
                "sizeBytes" to sizeBytes,
                "payloadBase64" to base64Payload,
                "createdAt" to FieldValue.serverTimestamp()
            )
            db.collection("applicationDocs").document(docId).set(payload).await()
            Result.success(docId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun getDocumentById(docId: String): Result<ApplicationDocument?> {
        if (docId.isBlank()) return Result.success(null)
        val path = "applicationDocs/$docId"
        return try {
            requireUserId()
            val snap = db.collection("applicationDocs").document(docId).get().await()
            Result.success(snap.toObject(ApplicationDocument::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            Result.failure(e)
        }
    }

    suspend fun getDocumentsForApplication(docIds: Collection<String>): List<ApplicationDocument> {
        val result = mutableListOf<ApplicationDocument>()
        for (id in docIds.distinct()) {
            if (id.isNotBlank()) {
                getDocumentById(id).getOrNull()?.let { result.add(it) }
            }
        }
        return result
    }

    suspend fun submitApplication(
        existingId: String?,
        existingCode: String?,
        fullName: String,
        mobile: String,
        email: String,
        address: String,
        city: String,
        pinCode: String,
        vehicleBrand: String,
        vehicleModel: String,
        registrationNumber: String,
        batteryNumber: String,
        vehicleNotes: String,
        documentIds: Map<String, String>,
        asDraft: Boolean = false
    ): Result<Pair<String, String>> {
        var path = "applications"
        return try {
            val uid = requireUserId()
            require(fullName.trim().isNotEmpty()) { "Full name is required" }
            require(mobile.trim().length >= 7) { "Valid mobile number is required" }
            require(address.trim().length >= 2) { "Address is required" }

            val status = if (asDraft) ApplicationStatus.DRAFT.name else ApplicationStatus.SUBMITTED.name
            val docId = existingId?.takeIf { it.isNotBlank() }
                ?: "app_${UUID.randomUUID().toString().replace("-", "").take(14)}"
            val appCode = existingCode?.takeIf { it.isNotBlank() }
                ?: "RAAG-${(100000..999999).random()}"
            path = "applications/$docId"

            if (existingId.isNullOrBlank()) {
                val payload = mapOf(
                    "applicationCode" to appCode,
                    "customerUid" to uid,
                    "fullName" to fullName.trim().take(120),
                    "mobile" to mobile.trim().take(20),
                    "email" to email.trim().take(180),
                    "address" to address.trim().take(500),
                    "city" to city.trim().take(80),
                    "pinCode" to pinCode.trim().take(12),
                    "vehicleBrand" to vehicleBrand.trim().take(80),
                    "vehicleModel" to vehicleModel.trim().take(80),
                    "registrationNumber" to registrationNumber.trim().take(60),
                    "batteryNumber" to batteryNumber.trim().take(80),
                    "vehicleNotes" to vehicleNotes.trim().take(400),
                    "documentIds" to documentIds,
                    "applicationStatus" to status,
                    "paymentStatus" to PaymentStatus.UNPAID.name,
                    "adminRemarks" to "",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                db.collection("applications").document(docId).set(payload).await()
                if (!asDraft) {
                    createNotification(
                        recipientUid = uid,
                        title = "Application Submitted ($appCode)",
                        message = "Your EV Insurance application for ${fullName.trim()} has been submitted and is pending review.",
                        type = "APPLICATION_SUBMITTED",
                        relatedId = docId
                    )
                }
            } else {
                val updates = mapOf(
                    "fullName" to fullName.trim().take(120),
                    "mobile" to mobile.trim().take(20),
                    "email" to email.trim().take(180),
                    "address" to address.trim().take(500),
                    "city" to city.trim().take(80),
                    "pinCode" to pinCode.trim().take(12),
                    "vehicleBrand" to vehicleBrand.trim().take(80),
                    "vehicleModel" to vehicleModel.trim().take(80),
                    "registrationNumber" to registrationNumber.trim().take(60),
                    "batteryNumber" to batteryNumber.trim().take(80),
                    "vehicleNotes" to vehicleNotes.trim().take(400),
                    "documentIds" to documentIds,
                    "applicationStatus" to status,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                db.collection("applications").document(docId).update(updates).await()
            }
            Result.success(docId to appCode)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun submitPaymentProof(
        application: InsuranceApplication,
        amount: Double,
        upiId: String,
        utrNumber: String,
        screenshotDocId: String
    ): Result<String> {
        var path = "payments"
        return try {
            val uid = requireUserId()
            require(application.id.isNotBlank()) { "Please select a saved application first." }
            require(screenshotDocId.isNotBlank()) { "Please attach a payment screenshot." }

            val paymentId = "pay_${UUID.randomUUID().toString().replace("-", "").take(14)}"
            path = "payments/$paymentId"

            val payload = mapOf(
                "applicationId" to application.id,
                "applicationCode" to application.applicationCode,
                "customerUid" to uid,
                "customerName" to application.fullName.take(120),
                "customerMobile" to application.mobile.take(20),
                "amount" to amount,
                "upiId" to upiId.trim().take(100),
                "utrNumber" to utrNumber.trim().take(60),
                "screenshotDocId" to screenshotDocId,
                "paymentStatus" to PaymentStatus.PENDING_VERIFICATION.name,
                "verificationRemarks" to "",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("payments").document(paymentId).set(payload).await()

            // Move application payment status to PENDING_VERIFICATION (never auto-verified)
            if (application.parsedStatus !in setOf(ApplicationStatus.APPROVED, ApplicationStatus.POLICY_ISSUED, ApplicationStatus.REJECTED)) {
                db.collection("applications").document(application.id).update(
                    mapOf(
                        "paymentStatus" to PaymentStatus.PENDING_VERIFICATION.name,
                        "applicationStatus" to ApplicationStatus.PAYMENT_VERIFICATION.name,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            createNotification(
                recipientUid = uid,
                title = "Payment Proof Submitted",
                message = "Payment proof of ₹${amount.toLong()} for ${application.applicationCode} is awaiting admin verification.",
                type = "PAYMENT_SUBMITTED",
                relatedId = paymentId
            )

            Result.success(paymentId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun createNotification(
        recipientUid: String,
        title: String,
        message: String,
        type: String,
        relatedId: String = ""
    ): Result<Unit> {
        val id = "noti_${UUID.randomUUID().toString().replace("-", "").take(14)}"
        val path = "notifications/$id"
        return try {
            requireUserId()
            val payload = mapOf(
                "recipientUid" to recipientUid,
                "title" to title.take(140),
                "message" to message.take(600),
                "type" to type.take(60),
                "relatedId" to relatedId.take(128),
                "read" to false,
                "createdAt" to FieldValue.serverTimestamp()
            )
            db.collection("notifications").document(id).set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun markNotificationRead(notificationId: String): Result<Unit> {
        val path = "notifications/$notificationId"
        return try {
            requireUserId()
            db.collection("notifications").document(notificationId)
                .update(mapOf("read" to true))
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- ADMIN MUTATIONS ---

    suspend fun updateApplicationAdmin(
        application: InsuranceApplication,
        newStatus: ApplicationStatus,
        adminRemarks: String
    ): Result<Unit> {
        val path = "applications/${application.id}"
        return try {
            val adminUid = requireUserId()
            db.collection("applications").document(application.id).update(
                mapOf(
                    "applicationStatus" to newStatus.name,
                    "adminRemarks" to adminRemarks.trim().take(600),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            logAdminAction(
                adminUid = adminUid,
                action = "APPLICATION_STATUS_${newStatus.name}",
                recordId = application.id,
                details = "${application.applicationCode}: ${newStatus.labelEn}. ${adminRemarks.trim()}"
            )
            createNotification(
                recipientUid = application.customerUid,
                title = "Application ${newStatus.labelEn} (${application.applicationCode})",
                message = adminRemarks.trim().ifBlank { "Your application status changed to ${newStatus.labelEn}." },
                type = "STATUS_${newStatus.name}",
                relatedId = application.id
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun verifyOrRejectPaymentAdmin(
        payment: PaymentRecord,
        approve: Boolean,
        remarks: String
    ): Result<Unit> {
        val path = "payments/${payment.id}"
        return try {
            val adminUid = requireUserId()
            val adminEmail = currentUserEmail().ifBlank { adminUid }
            val newPayStatus = if (approve) PaymentStatus.VERIFIED else PaymentStatus.REJECTED

            db.collection("payments").document(payment.id).update(
                mapOf(
                    "paymentStatus" to newPayStatus.name,
                    "verificationRemarks" to remarks.trim().take(500),
                    "verifiedBy" to adminEmail.take(180),
                    "verifiedAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            val appUpdate = mutableMapOf<String, Any>(
                "paymentStatus" to newPayStatus.name,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (approve) {
                appUpdate["applicationStatus"] = ApplicationStatus.UNDER_REVIEW.name
            } else {
                appUpdate["applicationStatus"] = ApplicationStatus.PAYMENT_PENDING.name
                if (remarks.isNotBlank()) {
                    appUpdate["adminRemarks"] = "Payment Rejected: ${remarks.trim()}".take(600)
                }
            }
            db.collection("applications").document(payment.applicationId).update(appUpdate).await()

            logAdminAction(
                adminUid = adminUid,
                action = if (approve) "PAYMENT_VERIFIED" else "PAYMENT_REJECTED",
                recordId = payment.id,
                details = "${payment.applicationCode} ₹${payment.amount.toLong()}: ${remarks.trim()}"
            )
            createNotification(
                recipientUid = payment.customerUid,
                title = if (approve) "Payment Verified (${payment.applicationCode})" else "Payment Rejected (${payment.applicationCode})",
                message = remarks.trim().ifBlank {
                    if (approve) "Your payment of ₹${payment.amount.toLong()} has been verified by RAAG AUTOMOBILES."
                    else "Your payment proof was rejected. Please re-submit a valid payment screenshot."
                },
                type = if (approve) "PAYMENT_VERIFIED" else "PAYMENT_REJECTED",
                relatedId = payment.applicationId
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun issuePolicyAdmin(
        application: InsuranceApplication,
        policyNumber: String,
        issueDate: String,
        policyDocId: String
    ): Result<Unit> {
        var path = "policies"
        return try {
            val adminUid = requireUserId()
            val adminEmail = currentUserEmail().ifBlank { adminUid }
            require(policyNumber.trim().length >= 3) { "Valid policy number is required" }
            require(policyDocId.isNotBlank()) { "Please upload the official policy PDF file" }

            val policyId = "pol_${UUID.randomUUID().toString().replace("-", "").take(14)}"
            path = "policies/$policyId"

            val payload = mapOf(
                "applicationId" to application.id,
                "applicationCode" to application.applicationCode,
                "customerUid" to application.customerUid,
                "customerName" to application.fullName.take(120),
                "policyNumber" to policyNumber.trim().take(80),
                "policyDocId" to policyDocId,
                "issueDate" to issueDate.trim().take(40),
                "issuedBy" to adminEmail.take(180),
                "createdAt" to FieldValue.serverTimestamp()
            )
            db.collection("policies").document(policyId).set(payload).await()

            db.collection("applications").document(application.id).update(
                mapOf(
                    "applicationStatus" to ApplicationStatus.POLICY_ISSUED.name,
                    "policyNumber" to policyNumber.trim().take(80),
                    "policyDocId" to policyDocId,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            logAdminAction(
                adminUid = adminUid,
                action = "POLICY_ISSUED",
                recordId = policyId,
                details = "Policy ${policyNumber.trim()} issued for ${application.applicationCode}"
            )
            createNotification(
                recipientUid = application.customerUid,
                title = "Policy Issued — ${policyNumber.trim()}",
                message = "Your EV Insurance policy for ${application.applicationCode} is now issued and ready to download.",
                type = "POLICY_ISSUED",
                relatedId = policyId
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun saveAppSettingsAdmin(settings: AppSettings): Result<Unit> {
        val path = "settings/appConfig"
        return try {
            val adminUid = requireUserId()
            val adminEmail = currentUserEmail().ifBlank { adminUid }
            val payload = mapOf(
                "businessName" to settings.businessName.trim().take(120),
                "contactPhone" to settings.contactPhone.trim().take(25),
                "supportEmail" to settings.supportEmail.trim().take(180),
                "upiId" to settings.upiId.trim().take(100),
                "upiRecipientName" to settings.upiRecipientName.trim().take(100),
                "defaultAmount" to settings.defaultAmount,
                "paymentInstructions" to settings.paymentInstructions.trim().take(600),
                "privacyPolicyUrl" to settings.privacyPolicyUrl.trim().take(300),
                "termsUrl" to settings.termsUrl.trim().take(300),
                "updatedBy" to adminEmail.take(180),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("settings").document("appConfig").set(payload).await()
            logAdminAction(
                adminUid = adminUid,
                action = "SETTINGS_UPDATED",
                recordId = "appConfig",
                details = "UPI=${settings.upiId}, Amount=₹${settings.defaultAmount.toLong()}"
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun updateCustomerAccountStatusAdmin(customerUid: String, active: Boolean): Result<Unit> {
        val path = "users/$customerUid"
        return try {
            val adminUid = requireUserId()
            val status = if (active) "ACTIVE" else "DISABLED"
            db.collection("users").document(customerUid).update(
                mapOf(
                    "accountStatus" to status,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            logAdminAction(
                adminUid = adminUid,
                action = "CUSTOMER_$status",
                recordId = customerUid,
                details = "Account marked $status"
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun upsertAdminRole(
        targetUid: String,
        targetEmail: String,
        targetName: String,
        role: AdminRole,
        active: Boolean
    ): Result<Unit> {
        val path = "admins/$targetUid"
        return try {
            val adminUid = requireUserId()
            val docRef = db.collection("admins").document(targetUid)
            val snap = docRef.get().await()
            if (snap.exists()) {
                docRef.update(
                    mapOf(
                        "email" to targetEmail.trim().take(180),
                        "name" to targetName.trim().take(120),
                        "role" to role.name,
                        "active" to active,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "uid" to targetUid,
                        "email" to targetEmail.trim().take(180),
                        "name" to targetName.trim().take(120),
                        "role" to role.name,
                        "active" to active,
                        "createdBy" to adminUid,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            logAdminAction(
                adminUid = adminUid,
                action = "ADMIN_ROLE_UPDATED",
                recordId = targetUid,
                details = "${targetEmail.trim()} -> ${role.name} (active=$active)"
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteAdminRole(targetUid: String): Result<Unit> {
        val path = "admins/$targetUid"
        return try {
            val adminUid = requireUserId()
            db.collection("admins").document(targetUid).delete().await()
            logAdminAction(
                adminUid = adminUid,
                action = "ADMIN_REMOVED",
                recordId = targetUid,
                details = "Removed admin access"
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    private suspend fun logAdminAction(adminUid: String, action: String, recordId: String, details: String) {
        val logId = "log_${UUID.randomUUID().toString().replace("-", "").take(14)}"
        try {
            db.collection("adminLogs").document(logId).set(
                mapOf(
                    "adminUid" to adminUid,
                    "adminEmail" to currentUserEmail().ifBlank { "admin@raag" }.take(180),
                    "action" to action.take(100),
                    "recordId" to recordId.take(128),
                    "details" to details.take(500),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "adminLogs/$logId")
        }
    }
}
