package com.example.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.AdminAuditLog
import com.example.data.AdminRecord
import com.example.data.AdminRole
import com.example.data.AppSettings
import com.example.data.ApplicationDocument
import com.example.data.ApplicationStatus
import com.example.data.InsuranceApplication
import com.example.data.PaymentRecord
import com.example.data.PaymentStatus
import com.example.data.UserProfile
import com.example.ui.theme.RaagBorder
import com.example.ui.theme.RaagCrimson
import com.example.ui.theme.RaagDarkRed
import com.example.ui.theme.RaagGold
import com.example.ui.theme.RaagGoldSoft
import com.example.ui.theme.RaagInk
import com.example.ui.theme.RaagMuted
import com.example.ui.theme.RaagWhite
import com.example.ui.theme.StatusDangerInk
import com.example.ui.theme.StatusSuccessBg
import com.example.ui.theme.StatusSuccessInk
import com.example.util.ImageCompressionUtil
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun AdminApplicationReviewDialog(
    application: InsuranceApplication,
    docCache: Map<String, ApplicationDocument>,
    busy: Boolean,
    onLoadDoc: (String) -> Unit,
    onUpdateStatus: (ApplicationStatus, String) -> Unit,
    onIssuePolicy: (String, Uri) -> Unit,
    onPrintPdf: () -> Unit,
    onSharePdf: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedStatus by remember(application.id) { mutableStateOf(application.parsedStatus) }
    var remarks by remember(application.id) { mutableStateOf(application.adminRemarks) }
    var policyNumber by remember(application.id) { mutableStateOf(application.policyNumber) }

    val pdfPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            onIssuePolicy(policyNumber, uri)
        }
    }

    LaunchedEffect(application.id) {
        application.documentIds.values.forEach { docId -> onLoadDoc(docId) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = RaagWhite,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(application.fullName, style = MaterialTheme.typography.titleMedium, color = RaagDarkRed)
                    Text("${application.applicationCode} · ${application.mobile}", style = MaterialTheme.typography.bodySmall, color = RaagMuted)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close", tint = RaagMuted)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = Color(0xFFFAF6F5),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, RaagBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("Address: ${application.address}, ${application.city} ${application.pinCode}", style = MaterialTheme.typography.bodySmall, color = RaagInk)
                        Text("Vehicle: ${application.vehicleBrand} ${application.vehicleModel}", style = MaterialTheme.typography.bodySmall, color = RaagInk)
                        Text("26/Reg No: ${application.registrationNumber.ifBlank { "—" }} | Battery: ${application.batteryNumber.ifBlank { "—" }}", style = MaterialTheme.typography.bodySmall, color = RaagInk)
                        if (application.policyNumber.isNotBlank()) {
                            Text("Issued Policy: ${application.policyNumber}", style = MaterialTheme.typography.labelMedium, color = StatusSuccessInk)
                        }
                    }
                }

                // PDF Actions
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = onPrintPdf, enabled = !busy, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Print A4")
                    }
                    OutlinedButton(onClick = onSharePdf, enabled = !busy, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share PDF")
                    }
                }

                // Uploaded Photos Preview
                if (application.documentIds.isNotEmpty()) {
                    Text("Uploaded Documents (${application.documentIds.size})", style = MaterialTheme.typography.labelLarge, color = RaagDarkRed)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        application.documentIds.forEach { (typeKey, docId) ->
                            val doc = docCache[docId]
                            val bmp = remember(doc?.payloadBase64) {
                                doc?.payloadBase64?.let { ImageCompressionUtil.decodeBase64ToBitmap(it) }
                            }
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .width(106.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(1.dp, RaagBorder, RoundedCornerShape(10.dp))
                                    .padding(6.dp)
                            ) {
                                if (bmp != null) {
                                    Image(
                                        bitmap = bmp.asImageBitmap(),
                                        contentDescription = typeKey,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(86.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                    )
                                } else {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(86.dp)
                                            .background(RaagGoldSoft, RoundedCornerShape(8.dp))
                                    ) {
                                        Text("Loading...", style = MaterialTheme.typography.labelSmall, color = RaagMuted)
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(typeKey.replace("_", " "), style = MaterialTheme.typography.labelSmall, color = RaagInk, maxLines = 1)
                            }
                        }
                    }
                }

                HorizontalDivider(color = RaagBorder)

                // Status Selector Pills
                Text("Update Application Status", style = MaterialTheme.typography.labelLarge, color = RaagDarkRed)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ApplicationStatus.entries.forEach { st ->
                        val active = selectedStatus == st
                        Surface(
                            onClick = { selectedStatus = st },
                            shape = RoundedCornerShape(20.dp),
                            color = if (active) RaagCrimson else RaagWhite,
                            border = BorderStroke(1.dp, if (active) RaagCrimson else RaagBorder)
                        ) {
                            Text(
                                text = st.labelEn,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (active) RaagWhite else RaagInk,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = remarks,
                    onValueChange = { remarks = it },
                    label = { Text("Admin Remarks (visible to customer)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                Button(
                    onClick = { onUpdateStatus(selectedStatus, remarks) },
                    enabled = !busy,
                    colors = ButtonDefaults.buttonColors(containerColor = RaagCrimson),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_save_status_button")
                ) {
                    Text("Save Status & Notify Customer")
                }

                HorizontalDivider(color = RaagBorder)

                // Issue Policy PDF
                Text("Issue Official Policy PDF (Max 300 KB)", style = MaterialTheme.typography.labelLarge, color = RaagDarkRed)
                OutlinedTextField(
                    value = policyNumber,
                    onValueChange = { policyNumber = it },
                    label = { Text("Policy Number (e.g. RAAG-EV-2026-001)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = { pdfPicker.launch("application/pdf") },
                    enabled = !busy && policyNumber.trim().length >= 3,
                    colors = ButtonDefaults.buttonColors(containerColor = StatusSuccessInk),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_issue_policy_button")
                ) {
                    Icon(Icons.Filled.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Pick Policy PDF & Mark POLICY_ISSUED")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done", color = RaagDarkRed) }
        }
    )
}

@Composable
fun AdminPaymentReviewDialog(
    payment: PaymentRecord,
    docCache: Map<String, ApplicationDocument>,
    busy: Boolean,
    onLoadDoc: (String) -> Unit,
    onVerify: (Boolean, String) -> Unit,
    onDismiss: () -> Unit
) {
    var remarks by remember(payment.id) { mutableStateOf(payment.verificationRemarks) }
    LaunchedEffect(payment.id) {
        if (payment.screenshotDocId.isNotBlank()) onLoadDoc(payment.screenshotDocId)
    }
    val doc = docCache[payment.screenshotDocId]
    val bmp = remember(doc?.payloadBase64) {
        doc?.payloadBase64?.let { ImageCompressionUtil.decodeBase64ToBitmap(it) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = RaagWhite,
        title = {
            Text("Verify UPI Payment · ${payment.applicationCode}", style = MaterialTheme.typography.titleMedium, color = RaagDarkRed)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Customer: ${payment.customerName} (${payment.customerMobile})", style = MaterialTheme.typography.bodySmall, color = RaagInk)
                Text("Amount: ₹${payment.amount.toLong()}  |  UPI: ${payment.upiId}", style = MaterialTheme.typography.bodySmall, color = RaagInk, fontWeight = FontWeight.Bold)
                Text("UTR / Ref: ${payment.utrNumber.ifBlank { "Not provided" }}", style = MaterialTheme.typography.bodySmall, color = RaagMuted)
                PaymentStatusBadge(payment.parsedStatus)

                if (bmp != null) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Payment Screenshot",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, RaagBorder, RoundedCornerShape(10.dp))
                    )
                } else {
                    Surface(
                        color = RaagGoldSoft,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Loading payment screenshot...",
                            style = MaterialTheme.typography.bodySmall,
                            color = RaagMuted,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = remarks,
                    onValueChange = { remarks = it },
                    label = { Text("Verification Remarks") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { onVerify(true, remarks) },
                        enabled = !busy,
                        colors = ButtonDefaults.buttonColors(containerColor = StatusSuccessInk),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("admin_verify_payment_btn")
                    ) {
                        Text("Approve")
                    }
                    Button(
                        onClick = { onVerify(false, remarks) },
                        enabled = !busy,
                        colors = ButtonDefaults.buttonColors(containerColor = StatusDangerInk),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("admin_reject_payment_btn")
                    ) {
                        Text("Reject")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close", color = RaagDarkRed) }
        }
    )
}

@Composable
fun AdminCustomersTab(
    customers: List<UserProfile>,
    busy: Boolean,
    onToggleStatus: (UserProfile) -> Unit
) {
    val clipboard = LocalClipboardManager.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Registered Customers (${customers.size})", style = MaterialTheme.typography.titleMedium, color = RaagDarkRed)
        if (customers.isEmpty()) {
            Text("No registered customers yet.", style = MaterialTheme.typography.bodySmall, color = RaagMuted)
        } else {
            customers.forEach { user ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = RaagWhite),
                    border = BorderStroke(1.dp, RaagBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(user.name.ifBlank { "Customer" }, style = MaterialTheme.typography.titleSmall, color = RaagInk)
                                Text("${user.email} · ${user.mobile.ifBlank { "No phone" }}", style = MaterialTheme.typography.bodySmall, color = RaagMuted)
                            }
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (user.accountStatus == "ACTIVE") StatusSuccessBg else Color(0xFFFDECEC)
                            ) {
                                Text(
                                    text = user.accountStatus,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (user.accountStatus == "ACTIVE") StatusSuccessInk else StatusDangerInk,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { clipboard.setText(AnnotatedString(user.uid.ifBlank { user.id })) },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("Copy UID", style = MaterialTheme.typography.labelSmall)
                            }
                            OutlinedButton(
                                onClick = { onToggleStatus(user) },
                                enabled = !busy,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (user.accountStatus == "ACTIVE") "Disable Account" else "Activate Account",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = RaagDarkRed
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminSettingsAndStaffTab(
    settings: AppSettings,
    adminRole: AdminRole,
    staff: List<AdminRecord>,
    logs: List<AdminAuditLog>,
    busy: Boolean,
    onSaveSettings: (AppSettings) -> Unit,
    onAssignAdmin: (String, String, String, AdminRole) -> Unit,
    onRemoveAdmin: (String) -> Unit
) {
    var businessName by remember(settings) { mutableStateOf(settings.businessName) }
    var contactPhone by remember(settings) { mutableStateOf(settings.contactPhone) }
    var supportEmail by remember(settings) { mutableStateOf(settings.supportEmail) }
    var upiId by remember(settings) { mutableStateOf(settings.upiId) }
    var upiRecipient by remember(settings) { mutableStateOf(settings.upiRecipientName) }
    var amountText by remember(settings) { mutableStateOf(settings.defaultAmount.toLong().toString()) }
    var instructions by remember(settings) { mutableStateOf(settings.paymentInstructions) }

    var newUid by remember { mutableStateOf("") }
    var newEmail by remember { mutableStateOf("") }
    var newName by remember { mutableStateOf("") }
    var newRole by remember { mutableStateOf(AdminRole.APPLICATION_MANAGER) }
    val sdf = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.US) }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Global UPI & Branding Settings
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = RaagWhite),
            border = BorderStroke(1.dp, RaagBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("UPI & Platform Settings (/settings/appConfig)", style = MaterialTheme.typography.titleMedium, color = RaagDarkRed)
                OutlinedTextField(value = businessName, onValueChange = { businessName = it }, label = { Text("Business Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = contactPhone, onValueChange = { contactPhone = it }, label = { Text("Helpline Phone") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = supportEmail, onValueChange = { supportEmail = it }, label = { Text("Support Email") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = upiId, onValueChange = { upiId = it }, label = { Text("Official UPI ID") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = upiRecipient, onValueChange = { upiRecipient = it }, label = { Text("UPI Recipient Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = amountText, onValueChange = { amountText = it.filter { ch -> ch.isDigit() } }, label = { Text("Default Insurance Amount (₹)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = instructions, onValueChange = { instructions = it }, label = { Text("Payment Instructions") }, maxLines = 3, modifier = Modifier.fillMaxWidth())

                Button(
                    onClick = {
                        val amt = amountText.toDoubleOrNull()?.takeIf { it > 0 } ?: 4500.0
                        onSaveSettings(
                            settings.copy(
                                businessName = businessName,
                                contactPhone = contactPhone,
                                supportEmail = supportEmail,
                                upiId = upiId,
                                upiRecipientName = upiRecipient,
                                defaultAmount = amt,
                                paymentInstructions = instructions
                            )
                        )
                    },
                    enabled = !busy && adminRole == AdminRole.SUPER_ADMIN,
                    colors = ButtonDefaults.buttonColors(containerColor = RaagCrimson),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (adminRole == AdminRole.SUPER_ADMIN) "Save Settings Live" else "Super Admin Only")
                }
            }
        }

        // Staff RBAC
        if (adminRole == AdminRole.SUPER_ADMIN) {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = RaagWhite),
                border = BorderStroke(1.dp, RaagBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Admin Staff Management", style = MaterialTheme.typography.titleMedium, color = RaagDarkRed)
                    OutlinedTextField(value = newUid, onValueChange = { newUid = it }, label = { Text("User Firebase UID (copy from Customers)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = newEmail, onValueChange = { newEmail = it }, label = { Text("Staff Email") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = newName, onValueChange = { newName = it }, label = { Text("Staff Full Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AdminRole.entries.forEach { role ->
                            Surface(
                                onClick = { newRole = role },
                                shape = RoundedCornerShape(18.dp),
                                color = if (newRole == role) RaagDarkRed else RaagWhite,
                                border = BorderStroke(1.dp, if (newRole == role) RaagDarkRed else RaagBorder)
                            ) {
                                Text(role.displayName, style = MaterialTheme.typography.labelSmall, color = if (newRole == role) RaagGold else RaagInk, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                            }
                        }
                    }
                    Button(
                        onClick = {
                            onAssignAdmin(newUid, newEmail, newName, newRole)
                            newUid = ""; newEmail = ""; newName = ""
                        },
                        enabled = !busy,
                        colors = ButtonDefaults.buttonColors(containerColor = RaagDarkRed),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Grant Admin Role")
                    }

                    staff.forEach { member ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${member.name} (${member.parsedRole.displayName})", style = MaterialTheme.typography.labelLarge, color = RaagInk)
                                Text(member.email, style = MaterialTheme.typography.bodySmall, color = RaagMuted)
                            }
                            IconButton(onClick = { onRemoveAdmin(member.uid.ifBlank { member.id }) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Remove admin", tint = StatusDangerInk)
                            }
                        }
                    }
                }
            }
        }

        // Immutable Audit Logs
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = RaagWhite),
            border = BorderStroke(1.dp, RaagBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Admin Audit Logs (${logs.size})", style = MaterialTheme.typography.titleMedium, color = RaagDarkRed)
                if (logs.isEmpty()) {
                    Text("No admin actions logged yet.", style = MaterialTheme.typography.bodySmall, color = RaagMuted)
                } else {
                    logs.take(30).forEach { log ->
                        val time = log.createdAt?.toDate()?.let { sdf.format(it) } ?: "Now"
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text("${log.action} · $time", style = MaterialTheme.typography.labelMedium, color = RaagDarkRed)
                            Text("${log.adminEmail}: ${log.details}", style = MaterialTheme.typography.bodySmall, color = RaagMuted)
                            HorizontalDivider(color = RaagBorder, modifier = Modifier.padding(top = 6.dp))
                        }
                    }
                }
            }
        }
    }
}
