package com.example.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.AppNotification
import com.example.data.AppSettings
import com.example.data.ApplicationDocument
import com.example.data.DocumentType
import com.example.data.InsuranceApplication
import com.example.data.UserProfile
import com.example.ui.theme.RaagBackground
import com.example.ui.theme.RaagBorder
import com.example.ui.theme.RaagCrimson
import com.example.ui.theme.RaagDarkRed
import com.example.ui.theme.RaagGold
import com.example.ui.theme.RaagGoldSoft
import com.example.ui.theme.RaagInk
import com.example.ui.theme.RaagMuted
import com.example.ui.theme.RaagWhite
import com.example.ui.theme.StatusSuccessBg
import com.example.ui.theme.StatusSuccessInk
import com.example.util.ImageCompressionUtil
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun SubScreenHeader(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(RaagDarkRed)
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = RaagWhite)
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = RaagWhite
        )
    }
}

@Composable
fun ApplicationDetailScreen(
    application: InsuranceApplication,
    docCache: Map<String, ApplicationDocument>,
    onLoadDoc: (String) -> Unit,
    onPrintPdf: () -> Unit,
    onSharePdf: () -> Unit,
    onEditApplication: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    LaunchedEffect(application.documentIds) {
        application.documentIds.values.forEach { docId -> onLoadDoc(docId) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(RaagBackground)
            .navigationBarsPadding()
    ) {
        SubScreenHeader(title = "Track & Details (${application.applicationCode})", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Status & Timeline Card
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = RaagWhite),
                border = BorderStroke(1.dp, RaagBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(application.applicationCode, style = MaterialTheme.typography.titleLarge, color = RaagDarkRed)
                        ApplicationStatusBadge(application.parsedStatus)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Payment Status:", style = MaterialTheme.typography.labelMedium, color = RaagMuted)
                        PaymentStatusBadge(application.parsedPaymentStatus)
                    }
                    if (application.adminRemarks.isNotBlank()) {
                        Surface(
                            color = RaagGoldSoft,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Admin Remarks: ${application.adminRemarks}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = RaagDarkRed,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                    HorizontalDivider(color = RaagBorder)
                    Text("Live Progress Timeline", style = MaterialTheme.typography.titleSmall, color = RaagDarkRed)
                    ApplicationTimelineStepper(application.parsedStatus)
                }
            }

            // Customer & Vehicle Summary Card
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = RaagWhite),
                border = BorderStroke(1.dp, RaagBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Customer & Vehicle Details", style = MaterialTheme.typography.titleMedium, color = RaagDarkRed)
                    DetailLine("Full Name / नाम", application.fullName)
                    DetailLine("Mobile / मोबाइल", application.mobile)
                    DetailLine("Email", application.email)
                    DetailLine("Address / पता", "${application.address} ${application.city} ${application.pinCode}".trim())
                    DetailLine("Vehicle Brand & Model", "${application.vehicleBrand} ${application.vehicleModel}".trim())
                    DetailLine("Scooty 26 / Reg No.", application.registrationNumber)
                    DetailLine("Battery Number", application.batteryNumber)
                    if (application.policyNumber.isNotBlank()) {
                        DetailLine("Issued Policy Number", application.policyNumber)
                    }
                }
            }

            // Uploaded Photos Preview
            if (application.documentIds.isNotEmpty()) {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = RaagWhite),
                    border = BorderStroke(1.dp, RaagBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Submitted Photos (${application.documentIds.size})", style = MaterialTheme.typography.titleMedium, color = RaagDarkRed)
                        application.documentIds.forEach { (typeKey, docId) ->
                            val doc = docCache[docId]
                            val label = DocumentType.fromRaw(typeKey).labelEn
                            val bmp = remember(doc?.payloadBase64) {
                                doc?.payloadBase64?.let { ImageCompressionUtil.decodeBase64ToBitmap(it) }
                            }
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(1.dp, RaagBorder, RoundedCornerShape(10.dp))
                                    .padding(10.dp)
                            ) {
                                Text(label, style = MaterialTheme.typography.labelLarge, color = RaagDarkRed)
                                Spacer(modifier = Modifier.height(6.dp))
                                if (bmp != null) {
                                    Image(
                                        bitmap = bmp.asImageBitmap(),
                                        contentDescription = label,
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(160.dp)
                                    )
                                } else {
                                    Text("Loading photo from cloud...", style = MaterialTheme.typography.bodySmall, color = RaagMuted)
                                }
                            }
                        }
                    }
                }
            }

            // Action Buttons
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onPrintPdf,
                    colors = ButtonDefaults.buttonColors(containerColor = RaagCrimson, contentColor = RaagWhite),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                ) {
                    Icon(Icons.Filled.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Print / Save PDF")
                }
                OutlinedButton(
                    onClick = onSharePdf,
                    border = BorderStroke(1.dp, RaagCrimson),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null, tint = RaagCrimson, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share A4 PDF", color = RaagCrimson)
                }
            }

            if (application.parsedStatus.name !in setOf("APPROVED", "POLICY_ISSUED", "REJECTED")) {
                OutlinedButton(
                    onClick = onEditApplication,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("Edit / Upload Missing Documents", color = RaagDarkRed)
                }
            }
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = RaagMuted, modifier = Modifier.weight(0.45f))
        Text(
            text = value.ifBlank { "—" },
            style = MaterialTheme.typography.bodyMedium,
            color = RaagInk,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(0.55f)
        )
    }
}

@Composable
fun NotificationsScreen(
    notifications: List<AppNotification>,
    onMarkRead: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val fmt = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(RaagBackground)
            .navigationBarsPadding()
    ) {
        SubScreenHeader(title = "Notifications (${notifications.size})", onBack = onBack)
        if (notifications.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.Notifications, contentDescription = null, tint = RaagMuted, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No notifications yet", style = MaterialTheme.typography.titleMedium, color = RaagMuted)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(notifications, key = { it.id }) { item ->
                    Card(
                        onClick = { if (!item.read) onMarkRead(item.id) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (item.read) RaagWhite else RaagGoldSoft.copy(alpha = 0.55f)
                        ),
                        border = BorderStroke(1.dp, if (item.read) RaagBorder else RaagGold),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(item.title, style = MaterialTheme.typography.titleSmall, color = RaagDarkRed)
                                if (!item.read) {
                                    Surface(color = RaagCrimson, shape = RoundedCornerShape(8.dp)) {
                                        Text("NEW", color = RaagWhite, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(item.message, style = MaterialTheme.typography.bodySmall, color = RaagInk)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = item.createdAt?.toDate()?.let { fmt.format(it) } ?: "Just now",
                                style = MaterialTheme.typography.labelSmall,
                                color = RaagMuted
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileEditorScreen(
    profile: UserProfile?,
    email: String,
    defaultName: String,
    busy: Boolean,
    onSave: (String, String, String, String, String, Boolean) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    var name by remember(profile) { mutableStateOf(profile?.name?.ifBlank { defaultName } ?: defaultName) }
    var mobile by remember(profile) { mutableStateOf(profile?.mobile ?: "") }
    var address by remember(profile) { mutableStateOf(profile?.address ?: "") }
    var city by remember(profile) { mutableStateOf(profile?.city ?: "") }
    var pinCode by remember(profile) { mutableStateOf(profile?.pinCode ?: "") }
    var consent by remember(profile) { mutableStateOf(profile?.consentAccepted ?: true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(RaagBackground)
            .navigationBarsPadding()
    ) {
        SubScreenHeader(title = "Customer Profile", onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = RaagWhite),
                border = BorderStroke(1.dp, RaagBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Account Email: $email", style = MaterialTheme.typography.labelLarge, color = RaagDarkRed)
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full Name *") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = mobile, onValueChange = { mobile = it.take(15) }, label = { Text("Mobile Number *") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Address") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = city, onValueChange = { city = it }, label = { Text("City") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = pinCode, onValueChange = { pinCode = it.take(6) }, label = { Text("PIN Code") }, modifier = Modifier.weight(1f))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { consent = !consent }) {
                        Checkbox(checked = consent, onCheckedChange = { consent = it }, colors = CheckboxDefaults.colors(checkedColor = RaagCrimson))
                        Text("I accept RAAG AUTOMOBILES Privacy Policy & Terms of Service", style = MaterialTheme.typography.bodySmall, color = RaagInk)
                    }
                    Button(
                        onClick = { onSave(name, mobile, address, city, pinCode, consent) },
                        enabled = !busy,
                        colors = ButtonDefaults.buttonColors(containerColor = RaagCrimson, contentColor = RaagWhite),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text("Save Profile to Cloud")
                    }
                }
            }
        }
    }
}

@Composable
fun SupportAndLegalScreen(
    settings: AppSettings,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(RaagBackground)
            .navigationBarsPadding()
    ) {
        SubScreenHeader(title = "Support, Privacy & Terms", onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = RaagWhite),
                border = BorderStroke(1.dp, RaagBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(settings.businessName, style = MaterialTheme.typography.titleMedium, color = RaagDarkRed)
                    Text("Official Helpline: ${settings.contactPhone}", style = MaterialTheme.typography.bodyMedium)
                    Text("Support Email: ${settings.supportEmail}", style = MaterialTheme.typography.bodyMedium)
                    Text("Authorized UPI ID: ${settings.upiId}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = RaagCrimson)

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${settings.contactPhone}"))
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = RaagCrimson),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Call Helpline")
                        }
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${settings.supportEmail}"))
                                context.startActivity(intent)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Email, contentDescription = null, tint = RaagDarkRed, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Email", color = RaagDarkRed)
                        }
                    }
                }
            }

            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = RaagWhite),
                border = BorderStroke(1.dp, RaagBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Privacy Policy", style = MaterialTheme.typography.titleMedium, color = RaagDarkRed)
                    Text(
                        text = "RAAG AUTOMOBILES collects your name, mobile number, address, vehicle/battery numbers, and optional Aadhaar/vehicle photos solely to process your EV insurance application. All documents are isolated per user account in Firebase with strict server-side security rules. No other customer can view your records.",
                        style = MaterialTheme.typography.bodySmall,
                        color = RaagInk
                    )
                    HorizontalDivider(color = RaagBorder)
                    Text("Terms & Conditions", style = MaterialTheme.typography.titleMedium, color = RaagDarkRed)
                    Text(
                        text = "1. Submitting an application or uploading a UPI payment screenshot does not automatically activate insurance coverage.\n2. All UPI payments (default ₹${settings.defaultAmount.toLong()} to ${settings.upiId}) are manually verified by authorized RAAG AUTOMOBILES administrators.\n3. Insurance coverage is active only after the official Policy Document is uploaded and marked as Policy Issued.",
                        style = MaterialTheme.typography.bodySmall,
                        color = RaagInk
                    )
                }
            }
        }
    }
}
