package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.AppSettings
import com.example.data.ApplicationStatus
import com.example.data.DocumentType
import com.example.data.InsuranceApplication
import com.example.data.PaymentRecord
import com.example.data.PaymentStatus
import com.example.ui.theme.RaagBorder
import com.example.ui.theme.RaagCrimson
import com.example.ui.theme.RaagCrimsonLight
import com.example.ui.theme.RaagDarkRed
import com.example.ui.theme.RaagGold
import com.example.ui.theme.RaagGoldSoft
import com.example.ui.theme.RaagInk
import com.example.ui.theme.RaagMuted
import com.example.ui.theme.RaagWhite
import com.example.ui.theme.StatusDangerBg
import com.example.ui.theme.StatusDangerInk
import com.example.ui.theme.StatusInfoBg
import com.example.ui.theme.StatusInfoInk
import com.example.ui.theme.StatusSuccessBg
import com.example.ui.theme.StatusSuccessInk
import com.example.ui.theme.StatusWarningBg
import com.example.ui.theme.StatusWarningInk
import com.example.util.EncodedUploadFile
import com.example.util.QrCodeGenerator
import java.text.SimpleDateFormat
import java.util.Locale

// Exact HTML .topbar layout upgraded to RAAG AUTOMOBILES Red-Gold-White Branding
@Composable
fun RaagDocScannerTopBar(
    businessName: String,
    contactPhone: String,
    unreadNotifications: Int,
    onMenuClick: () -> Unit,
    onSearchToggle: () -> Unit,
    onNotificationsClick: () -> Unit,
    onSecretAdminUnlock: () -> Unit = {}
) {
    var logoTapCount by remember { mutableIntStateOf(0) }
    var lastTapTimeMs by remember { mutableLongStateOf(0L) }
    val noRipple = remember { MutableInteractionSource() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(listOf(RaagDarkRed, RaagCrimson, RaagDarkRed))
            )
            .statusBarsPadding()
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onMenuClick,
                modifier = Modifier.testTag("topbar_menu_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.Menu,
                    contentDescription = "Open menu",
                    tint = RaagWhite
                )
            }

            Image(
                painter = painterResource(id = R.drawable.raag_logo),
                contentDescription = "RAAG AUTOMOBILES Logo",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, RaagGold, CircleShape)
                    .background(RaagWhite)
                    .clickable(
                        interactionSource = noRipple,
                        indication = null
                    ) {
                        val now = System.currentTimeMillis()
                        if (now - lastTapTimeMs > 3000L) {
                            logoTapCount = 1
                        } else {
                            logoTapCount += 1
                        }
                        lastTapTimeMs = now
                        if (logoTapCount >= 5) {
                            logoTapCount = 0
                            onSecretAdminUnlock()
                        }
                    }
                    .testTag("topbar_raag_logo")
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "RAAG AUTOMOBILES",
                    style = MaterialTheme.typography.titleMedium,
                    color = RaagWhite,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "EV INSURANCE",
                    style = MaterialTheme.typography.labelSmall,
                    color = RaagGold,
                    letterSpacing = 1.2.sp,
                    maxLines = 1
                )
            }

            IconButton(
                onClick = onSearchToggle,
                modifier = Modifier.testTag("topbar_search_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = "Search records",
                    tint = RaagWhite
                )
            }

            IconButton(
                onClick = onNotificationsClick,
                modifier = Modifier.testTag("topbar_notifications_button")
            ) {
                BadgedBox(
                    badge = {
                        if (unreadNotifications > 0) {
                            Badge(containerColor = RaagGold, contentColor = RaagDarkRed) {
                                Text(unreadNotifications.coerceAtMost(99).toString())
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Notifications,
                        contentDescription = "Notifications",
                        tint = RaagWhite
                    )
                }
            }
        }

        // Exact HTML .top-mobile second line inside topbar
        Text(
            text = "मोबाइल नंबर: $contactPhone",
            style = MaterialTheme.typography.labelMedium,
            color = RaagGoldSoft,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 52.dp, bottom = 4.dp)
        )
    }
}

// Exact HTML #autoScanner4500 UPI Payment QR Panel
@Composable
fun AutoScannerPaymentCard(
    settings: AppSettings,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var recipientName by remember(settings.upiRecipientName) { mutableStateOf(settings.upiRecipientName) }
    var statusText by remember { mutableStateOf("₹${settings.defaultAmount.toLong()} का फिक्स UPI QR तैयार है।") }
    var qrVersion by remember { mutableStateOf(0) }

    val upiUri = remember(settings.upiId, recipientName, settings.defaultAmount, qrVersion) {
        QrCodeGenerator.buildUpiUri(
            upiId = settings.upiId,
            recipientName = recipientName,
            amount = settings.defaultAmount,
            transactionNote = "RAAG EV Insurance"
        )
    }

    val qrBitmap: Bitmap? = remember(upiUri) {
        QrCodeGenerator.generateQrBitmap(upiUri, 440)
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = RaagWhite),
        border = BorderStroke(1.dp, RaagBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.QrCode2,
                    contentDescription = null,
                    tint = RaagCrimson,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Scan Auto Scanner ${settings.defaultAmount.toLong()}",
                    style = MaterialTheme.typography.titleMedium,
                    color = RaagDarkRed,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "UPI पेमेंट के लिए QR स्कैन करें। इसमें ₹${settings.defaultAmount.toLong()} की राशि पहले से फिक्स है।",
                style = MaterialTheme.typography.bodySmall,
                color = RaagMuted
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Centered crisp QR code box matching HTML 220x220 box
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = RaagWhite,
                    border = BorderStroke(2.dp, RaagGold),
                    shadowElevation = 2.dp,
                    modifier = Modifier.size(216.dp)
                ) {
                    if (qrBitmap != null) {
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "UPI QR Code for ${settings.upiId}",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp)
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center) {
                            Text("QR लोड हो रहा है…", style = MaterialTheme.typography.bodySmall, color = RaagMuted)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Surface(
                color = Color(0xFFFFF8E7),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, RaagGold.copy(alpha = 0.7f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("UPI ID", style = MaterialTheme.typography.labelSmall, color = RaagMuted)
                        Text(
                            text = settings.upiId,
                            style = MaterialTheme.typography.titleSmall,
                            color = RaagDarkRed,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Surface(
                        color = RaagCrimson,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "₹${settings.defaultAmount.toLong()}",
                            color = RaagWhite,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = recipientName,
                onValueChange = { recipientName = it },
                label = { Text("प्राप्तकर्ता का नाम (वैकल्पिक)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = "₹${settings.defaultAmount.toLong()}",
                onValueChange = {},
                readOnly = true,
                label = { Text("फिक्स भुगतान राशि ₹ (Admin Controlled)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        qrVersion++
                        statusText = "₹${settings.defaultAmount.toLong()} का फिक्स UPI QR अपडेट हो गया।"
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RaagCrimson, contentColor = RaagWhite),
                    shape = RoundedCornerShape(9.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("qr_update_button")
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("QR अपडेट", maxLines = 1, style = MaterialTheme.typography.labelMedium)
                }

                OutlinedButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("UPI ID", settings.upiId))
                        statusText = "UPI ID कॉपी हो गई: ${settings.upiId}"
                    },
                    border = BorderStroke(1.dp, RaagCrimson),
                    shape = RoundedCornerShape(9.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("copy_upi_button")
                ) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = null, tint = RaagCrimson, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("UPI कॉपी", color = RaagCrimson, maxLines = 1, style = MaterialTheme.typography.labelMedium)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(upiUri))
                        context.startActivity(intent)
                        statusText = "UPI ऐप खोला जा रहा है…"
                    } catch (_: Exception) {
                        statusText = "इस डिवाइस पर कोई UPI ऐप नहीं मिला। कृपया QR स्कैन करें या UPI ID कॉपी करें।"
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = RaagGold, contentColor = RaagDarkRed),
                shape = RoundedCornerShape(9.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("pay_upi_app_button")
            ) {
                Icon(Icons.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("UPI ऐप से भुगतान करें (₹${settings.defaultAmount.toLong()})", style = MaterialTheme.typography.labelLarge)
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodySmall,
                color = StatusSuccessInk,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// Exact HTML #scannerPaymentProof Card
@Composable
fun PaymentProofCard(
    applications: List<InsuranceApplication>,
    payments: List<PaymentRecord>,
    settings: AppSettings,
    selectedScreenshot: EncodedUploadFile?,
    busy: Boolean,
    onPickFromGallery: () -> Unit,
    onCaptureFromCamera: () -> Unit,
    onClearScreenshot: () -> Unit,
    onSubmitPayment: (InsuranceApplication, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expandedDropdown by remember { mutableStateOf(false) }
    var selectedAppId by remember(applications) {
        mutableStateOf(applications.firstOrNull()?.id ?: "")
    }
    var utrNumber by remember { mutableStateOf("") }
    val selectedApp = applications.firstOrNull { it.id == selectedAppId } ?: applications.firstOrNull()

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = RaagWhite),
        border = BorderStroke(1.dp, RaagBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.ReceiptLong, contentDescription = null, tint = RaagCrimson, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "स्कैनर पेमेंट का स्क्रीनशॉट सेव करें",
                    style = MaterialTheme.typography.titleMedium,
                    color = RaagDarkRed,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "ऊपर दिए गए ₹${settings.defaultAmount.toLong()} UPI स्कैनर से भुगतान की रसीद/स्क्रीनशॉट यहाँ अपलोड करें। एडमिन जांच के बाद ही भुगतान Verified होगा।",
                style = MaterialTheme.typography.bodySmall,
                color = RaagMuted
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (applications.isEmpty()) {
                Surface(
                    color = StatusWarningBg,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "पेमेंट स्क्रीनशॉट लिंक करने के लिए पहले “Customer Form” टैब में ग्राहक आवेदन सेव करें।",
                        color = StatusWarningInk,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            } else {
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedApp?.let { "${it.applicationCode} — ${it.fullName} (${it.mobile})" } ?: "आवेदन चुनें",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("संबंधित आवेदन चुनें (Linked Application)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandedDropdown = true }
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { expandedDropdown = true }
                    )
                    DropdownMenu(
                        expanded = expandedDropdown,
                        onDismissRequest = { expandedDropdown = false }
                    ) {
                        applications.forEach { app ->
                            DropdownMenuItem(
                                text = { Text("${app.applicationCode} · ${app.fullName} (${app.mobile})") },
                                onClick = {
                                    selectedAppId = app.id
                                    expandedDropdown = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = utrNumber,
                    onValueChange = { utrNumber = it },
                    label = { Text("UTR / Transaction Reference No. (वैकल्पिक)") },
                    placeholder = { Text("12-digit UPI UTR number") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("payment_utr_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onPickFromGallery,
                        border = BorderStroke(1.dp, RaagCrimson),
                        shape = RoundedCornerShape(9.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("payment_pick_gallery")
                    ) {
                        Icon(Icons.Filled.PhotoLibrary, contentDescription = null, tint = RaagCrimson, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("गैलरी से चुनें", color = RaagCrimson, maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = onCaptureFromCamera,
                        border = BorderStroke(1.dp, RaagDarkRed),
                        shape = RoundedCornerShape(9.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("payment_capture_camera")
                    ) {
                        Icon(Icons.Filled.CameraAlt, contentDescription = null, tint = RaagDarkRed, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("कैमरा", color = RaagDarkRed, maxLines = 1)
                    }
                }

                if (selectedScreenshot?.previewBitmap != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Image(
                        bitmap = selectedScreenshot.previewBitmap.asImageBitmap(),
                        contentDescription = "Selected payment screenshot preview",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(170.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, RaagGold, RoundedCornerShape(10.dp))
                            .background(Color(0xFFFAF6F4))
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            if (selectedApp != null) {
                                onSubmitPayment(selectedApp, utrNumber)
                                utrNumber = ""
                            }
                        },
                        enabled = !busy && selectedScreenshot != null && selectedApp != null,
                        colors = ButtonDefaults.buttonColors(containerColor = RaagCrimson, contentColor = RaagWhite),
                        shape = RoundedCornerShape(9.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("payment_submit_button")
                    ) {
                        Text("स्क्रीनशॉट सेव करें", style = MaterialTheme.typography.labelLarge)
                    }

                    OutlinedButton(
                        onClick = {
                            onClearScreenshot()
                            utrNumber = ""
                        },
                        shape = RoundedCornerShape(9.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text("साफ करें", color = RaagMuted)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "सेव किए गए पेमेंट स्क्रीनशॉट (${payments.size})",
                style = MaterialTheme.typography.titleSmall,
                color = RaagDarkRed
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (payments.isEmpty()) {
                Text(
                    text = "अभी कोई पेमेंट स्क्रीनशॉट सेव नहीं है।",
                    style = MaterialTheme.typography.bodySmall,
                    color = RaagMuted
                )
            } else {
                val dateFmt = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    payments.forEach { pay ->
                        Surface(
                            color = Color(0xFFFCF9F7),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, RaagBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${pay.applicationCode} · ₹${pay.amount.toLong()}",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = RaagInk
                                    )
                                    PaymentStatusBadge(pay.parsedStatus)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${pay.customerName} | ${pay.customerMobile}${if (pay.utrNumber.isNotBlank()) " | UTR: ${pay.utrNumber}" else ""}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = RaagMuted
                                )
                                val dateStr = pay.createdAt?.toDate()?.let { dateFmt.format(it) } ?: "Just now"
                                Text(
                                    text = "Submitted: $dateStr",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = RaagMuted
                                )
                                if (pay.verificationRemarks.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Admin Note: ${pay.verificationRemarks}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = RaagDarkRed,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Exact HTML .promo Rounded Banner Card in RAAG Red-Gold
@Composable
fun RaagPromoBanner(
    onAddCustomerClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onAddCustomerClick,
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 4.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag("promo_banner_card")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(listOf(RaagCrimson, RaagDarkRed))
                )
                .border(1.5.dp, RaagGold, RoundedCornerShape(20.dp))
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.raag_logo),
                contentDescription = "RAAG Scooty Insurance",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(58.dp)
                    .clip(CircleShape)
                    .border(2.dp, RaagGold, CircleShape)
                    .background(RaagWhite)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "आपके सभी स्कूटी इंश्योरेंस रिकॉर्ड एक जगह!",
                    style = MaterialTheme.typography.titleMedium,
                    color = RaagWhite,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "ग्राहक जोड़ें   •   PDF बनाएं ›",
                    style = MaterialTheme.typography.labelLarge,
                    color = RaagGold
                )
            }
        }
    }
}

@Composable
fun ApplicationStatusBadge(status: ApplicationStatus) {
    val (bg, fg) = when (status) {
        ApplicationStatus.APPROVED, ApplicationStatus.POLICY_ISSUED -> StatusSuccessBg to StatusSuccessInk
        ApplicationStatus.REJECTED, ApplicationStatus.DOCUMENTS_REQUIRED -> StatusDangerBg to StatusDangerInk
        ApplicationStatus.PAYMENT_PENDING, ApplicationStatus.PAYMENT_VERIFICATION, ApplicationStatus.UNDER_REVIEW -> StatusWarningBg to StatusWarningInk
        ApplicationStatus.DRAFT, ApplicationStatus.SUBMITTED -> StatusInfoBg to StatusInfoInk
    }
    Surface(color = bg, shape = RoundedCornerShape(20.dp)) {
        Text(
            text = status.labelEn,
            color = fg,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun PaymentStatusBadge(status: PaymentStatus) {
    val (bg, fg) = when (status) {
        PaymentStatus.VERIFIED -> StatusSuccessBg to StatusSuccessInk
        PaymentStatus.REJECTED -> StatusDangerBg to StatusDangerInk
        PaymentStatus.PENDING_VERIFICATION -> StatusWarningBg to StatusWarningInk
        PaymentStatus.UNPAID -> StatusInfoBg to StatusInfoInk
    }
    Surface(color = bg, shape = RoundedCornerShape(20.dp)) {
        Text(
            text = status.labelEn,
            color = fg,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun ApplicationTimelineStepper(currentStatus: ApplicationStatus) {
    val steps = listOf(
        ApplicationStatus.SUBMITTED,
        ApplicationStatus.UNDER_REVIEW,
        ApplicationStatus.PAYMENT_VERIFICATION,
        ApplicationStatus.APPROVED,
        ApplicationStatus.POLICY_ISSUED
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        steps.forEach { step ->
            val reached = currentStatus != ApplicationStatus.REJECTED && currentStatus.stepIndex >= step.stepIndex
            val isCurrent = currentStatus == step
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = when {
                        isCurrent -> RaagCrimson
                        reached -> StatusSuccessInk
                        else -> RaagBorder
                    },
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "${step.labelEn} (${step.labelHi})",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                    color = if (reached || isCurrent) RaagInk else RaagMuted
                )
            }
        }
        if (currentStatus == ApplicationStatus.DOCUMENTS_REQUIRED || currentStatus == ApplicationStatus.REJECTED) {
            Surface(color = StatusDangerBg, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Current Status: ${currentStatus.labelEn} (${currentStatus.labelHi})",
                    color = StatusDangerInk,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }
    }
}

@Composable
fun PhotoSlotCard(
    docType: DocumentType,
    uploaded: EncodedUploadFile?,
    hasExistingCloudDoc: Boolean,
    onGalleryClick: () -> Unit,
    onCameraClick: () -> Unit,
    onRemoveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFCF9F7)),
        border = BorderStroke(
            width = 1.dp,
            color = if (uploaded != null || hasExistingCloudDoc) StatusSuccessInk.copy(alpha = 0.6f) else RaagBorder
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = docType.labelHi,
                        style = MaterialTheme.typography.labelLarge,
                        color = RaagDarkRed
                    )
                    Text(
                        text = docType.labelEn,
                        style = MaterialTheme.typography.labelSmall,
                        color = RaagMuted
                    )
                }
                if (uploaded != null || hasExistingCloudDoc) {
                    IconButton(onClick = onRemoveClick) {
                        Icon(Icons.Filled.Delete, contentDescription = "Remove ${docType.labelEn}", tint = StatusDangerInk)
                    }
                }
            }

            if (uploaded?.previewBitmap != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Image(
                    bitmap = uploaded.previewBitmap.asImageBitmap(),
                    contentDescription = docType.labelEn,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(RaagWhite)
                )
            } else if (hasExistingCloudDoc) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(color = StatusSuccessBg, shape = RoundedCornerShape(8.dp)) {
                    Text(
                        text = "✓ Uploaded in Cloud (Tap Camera/Gallery to replace)",
                        color = StatusSuccessInk,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onCameraClick,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.CameraAlt, contentDescription = null, tint = RaagDarkRed, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Camera", color = RaagDarkRed, style = MaterialTheme.typography.labelSmall)
                }
                OutlinedButton(
                    onClick = onGalleryClick,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.PhotoLibrary, contentDescription = null, tint = RaagCrimson, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Gallery", color = RaagCrimson, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
