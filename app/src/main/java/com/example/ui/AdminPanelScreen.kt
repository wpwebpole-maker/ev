package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.ApplicationStatus
import com.example.data.InsuranceApplication
import com.example.data.PaymentRecord
import com.example.data.PaymentStatus
import com.example.data.UiState
import com.example.ui.theme.RaagBackground
import com.example.ui.theme.RaagBorder
import com.example.ui.theme.RaagCrimson
import com.example.ui.theme.RaagDarkRed
import com.example.ui.theme.RaagGold
import com.example.ui.theme.RaagInk
import com.example.ui.theme.RaagMuted
import com.example.ui.theme.RaagWhite
import com.example.ui.theme.StatusSuccessInk
import com.example.viewmodel.AdminViewModel

private enum class AdminTab(val label: String) {
    OVERVIEW("Overview"),
    APPLICATIONS("Applications"),
    PAYMENTS("Payments"),
    CUSTOMERS("Customers"),
    SETTINGS("Settings & Logs")
}

@Composable
fun AdminPanelScreen(
    viewModel: AdminViewModel,
    onBackToCustomerApp: () -> Unit,
    onSignOut: () -> Unit
) {
    BackHandler { onBackToCustomerApp() }

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val appsState by viewModel.applications.collectAsStateWithLifecycle()
    val paymentsState by viewModel.payments.collectAsStateWithLifecycle()
    val customersState by viewModel.customers.collectAsStateWithLifecycle()
    val staff by viewModel.adminStaff.collectAsStateWithLifecycle()
    val logs by viewModel.auditLogs.collectAsStateWithLifecycle()
    val settings by viewModel.appSettings.collectAsStateWithLifecycle()
    val actionState by viewModel.actionState.collectAsStateWithLifecycle()
    val docCache by viewModel.docCache.collectAsStateWithLifecycle()

    var currentTab by remember { mutableStateOf(AdminTab.OVERVIEW) }
    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf<ApplicationStatus?>(null) }
    var selectedApp by remember { mutableStateOf<InsuranceApplication?>(null) }
    var selectedPayment by remember { mutableStateOf<PaymentRecord?>(null) }

    LaunchedEffect(actionState.message, actionState.error) {
        actionState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
        actionState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    val applications = (appsState as? UiState.Success)?.data.orEmpty()
    val payments = (paymentsState as? UiState.Success)?.data.orEmpty()
    val customers = (customersState as? UiState.Success)?.data.orEmpty()

    selectedApp?.let { app ->
        val freshApp = applications.firstOrNull { it.id == app.id } ?: app
        AdminApplicationReviewDialog(
            application = freshApp,
            docCache = docCache,
            busy = actionState.busy,
            onLoadDoc = viewModel::loadDocument,
            onUpdateStatus = { st, remarks ->
                viewModel.updateApplicationStatus(freshApp, st, remarks)
                selectedApp = null
            },
            onIssuePolicy = { policyNo, uri ->
                viewModel.issuePolicyWithPdf(context, freshApp, policyNo, uri)
                selectedApp = null
            },
            onPrintPdf = { viewModel.generateApplicationPdf(context, freshApp, printDirectly = true) },
            onSharePdf = { viewModel.generateApplicationPdf(context, freshApp, printDirectly = false) },
            onDismiss = { selectedApp = null }
        )
    }

    selectedPayment?.let { pay ->
        val freshPay = payments.firstOrNull { it.id == pay.id } ?: pay
        AdminPaymentReviewDialog(
            payment = freshPay,
            docCache = docCache,
            busy = actionState.busy,
            onLoadDoc = viewModel::loadDocument,
            onVerify = { approve, remarks ->
                viewModel.verifyOrRejectPayment(freshPay, approve, remarks)
                selectedPayment = null
            },
            onDismiss = { selectedPayment = null }
        )
    }

    Scaffold(
        containerColor = RaagBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.horizontalGradient(listOf(RaagDarkRed, RaagCrimson, RaagDarkRed)))
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBackToCustomerApp,
                        modifier = Modifier.testTag("admin_back_to_customer")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Customer App", tint = RaagWhite)
                    }
                    Image(
                        painter = painterResource(id = R.drawable.raag_logo),
                        contentDescription = "RAAG Logo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .border(1.5.dp, RaagGold, CircleShape)
                            .background(RaagWhite)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("RAAG ADMIN PANEL", style = MaterialTheme.typography.titleMedium, color = RaagWhite, fontWeight = FontWeight.Bold)
                        Text(viewModel.adminRole.displayName.uppercase(), style = MaterialTheme.typography.labelSmall, color = RaagGold, letterSpacing = 1.sp)
                    }
                    IconButton(onClick = onSignOut) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Sign Out", tint = RaagGold)
                    }
                }

                // Admin Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(RaagWhite)
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AdminTab.entries.forEach { tab ->
                        val active = currentTab == tab
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable { currentTab = tab }
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                                .testTag("admin_tab_${tab.name.lowercase()}")
                        ) {
                            Text(tab.label, style = MaterialTheme.typography.labelLarge, color = if (active) RaagCrimson else RaagMuted)
                            Spacer(modifier = Modifier.height(5.dp))
                            Box(
                                modifier = Modifier
                                    .width(48.dp)
                                    .height(3.dp)
                                    .background(if (active) RaagCrimson else Color.Transparent, RoundedCornerShape(2.dp))
                            )
                        }
                    }
                }
                HorizontalDivider(color = RaagBorder)
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 900.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                when (currentTab) {
                    AdminTab.OVERVIEW -> {
                        val pendingReview = applications.count {
                            it.parsedStatus in setOf(
                                ApplicationStatus.SUBMITTED,
                                ApplicationStatus.UNDER_REVIEW,
                                ApplicationStatus.DOCUMENTS_REQUIRED,
                                ApplicationStatus.PAYMENT_PENDING,
                                ApplicationStatus.PAYMENT_VERIFICATION
                            )
                        }
                        val pendingPay = payments.count { it.parsedStatus == PaymentStatus.PENDING_VERIFICATION }
                        val issuedCount = applications.count { it.parsedStatus == ApplicationStatus.POLICY_ISSUED }
                        val revenue = payments.filter { it.parsedStatus == PaymentStatus.VERIFIED }.sumOf { it.amount }.toLong()

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            AdminKpiTile("Applications", applications.size.toString(), RaagDarkRed, Modifier.weight(1f))
                            AdminKpiTile("Pending Review", pendingReview.toString(), Color(0xFFB4690E), Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            AdminKpiTile("Pending Pay", pendingPay.toString(), RaagCrimson, Modifier.weight(1f))
                            AdminKpiTile("Issued", issuedCount.toString(), StatusSuccessInk, Modifier.weight(1f))
                            AdminKpiTile("Verified ₹", "₹$revenue", RaagDarkRed, Modifier.weight(1f))
                        }

                        Text("Recent Applications", style = MaterialTheme.typography.titleMedium, color = RaagDarkRed)
                        if (appsState is UiState.Loading) {
                            CircularProgressIndicator(color = RaagCrimson)
                        } else if (applications.isEmpty()) {
                            Text("No customer applications submitted yet.", style = MaterialTheme.typography.bodySmall, color = RaagMuted)
                        } else {
                            applications.take(8).forEach { app ->
                                AdminAppRowCard(app = app, onReview = { selectedApp = app })
                            }
                        }
                    }

                    AdminTab.APPLICATIONS -> {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search name, mobile, code, reg no...") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                onClick = { statusFilter = null },
                                shape = RoundedCornerShape(18.dp),
                                color = if (statusFilter == null) RaagCrimson else RaagWhite,
                                border = BorderStroke(1.dp, if (statusFilter == null) RaagCrimson else RaagBorder)
                            ) {
                                Text("All (${applications.size})", style = MaterialTheme.typography.labelSmall, color = if (statusFilter == null) RaagWhite else RaagMuted, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                            }
                            ApplicationStatus.entries.forEach { st ->
                                val active = statusFilter == st
                                Surface(
                                    onClick = { statusFilter = st },
                                    shape = RoundedCornerShape(18.dp),
                                    color = if (active) RaagCrimson else RaagWhite,
                                    border = BorderStroke(1.dp, if (active) RaagCrimson else RaagBorder)
                                ) {
                                    Text(st.labelEn, style = MaterialTheme.typography.labelSmall, color = if (active) RaagWhite else RaagMuted, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                                }
                            }
                        }

                        val filtered = applications.filter { app ->
                            val q = searchQuery.trim().lowercase()
                            val matchQ = q.isEmpty() ||
                                app.fullName.lowercase().contains(q) ||
                                app.mobile.lowercase().contains(q) ||
                                app.applicationCode.lowercase().contains(q) ||
                                app.registrationNumber.lowercase().contains(q)
                            val matchS = statusFilter == null || app.parsedStatus == statusFilter
                            matchQ && matchS
                        }
                        if (filtered.isEmpty()) {
                            Text("No matching applications.", style = MaterialTheme.typography.bodySmall, color = RaagMuted)
                        } else {
                            filtered.forEach { app ->
                                AdminAppRowCard(app = app, onReview = { selectedApp = app })
                            }
                        }
                    }

                    AdminTab.PAYMENTS -> {
                        Text("Payment Proofs (${payments.size})", style = MaterialTheme.typography.titleMedium, color = RaagDarkRed)
                        if (paymentsState is UiState.Loading) {
                            CircularProgressIndicator(color = RaagCrimson)
                        } else if (payments.isEmpty()) {
                            Text("No payment screenshots uploaded yet.", style = MaterialTheme.typography.bodySmall, color = RaagMuted)
                        } else {
                            payments.forEach { pay ->
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = RaagWhite),
                                    border = BorderStroke(1.dp, RaagBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("${pay.customerName} · ₹${pay.amount.toLong()}", style = MaterialTheme.typography.titleSmall, color = RaagInk)
                                                Text("${pay.applicationCode} · ${pay.customerMobile}", style = MaterialTheme.typography.bodySmall, color = RaagMuted)
                                            }
                                            PaymentStatusBadge(pay.parsedStatus)
                                        }
                                        Text("UPI: ${pay.upiId} | UTR: ${pay.utrNumber.ifBlank { "—" }}", style = MaterialTheme.typography.bodySmall, color = RaagMuted)
                                        Button(
                                            onClick = { selectedPayment = pay },
                                            colors = ButtonDefaults.buttonColors(containerColor = RaagDarkRed),
                                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                        ) {
                                            Text("View Screenshot & Verify")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    AdminTab.CUSTOMERS -> {
                        AdminCustomersTab(
                            customers = customers,
                            busy = actionState.busy,
                            onToggleStatus = viewModel::toggleCustomerAccountStatus
                        )
                    }

                    AdminTab.SETTINGS -> {
                        AdminSettingsAndStaffTab(
                            settings = settings,
                            adminRole = viewModel.adminRole,
                            staff = staff,
                            logs = logs,
                            busy = actionState.busy,
                            onSaveSettings = viewModel::saveAppSettings,
                            onAssignAdmin = viewModel::assignAdminRole,
                            onRemoveAdmin = viewModel::removeAdminRole
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminKpiTile(label: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = RaagWhite),
        border = BorderStroke(1.dp, RaagBorder),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.titleLarge, color = accent, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = RaagMuted, maxLines = 1)
        }
    }
}

@Composable
private fun AdminAppRowCard(app: InsuranceApplication, onReview: () -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = RaagWhite),
        border = BorderStroke(1.dp, RaagBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(app.fullName, style = MaterialTheme.typography.titleSmall, color = RaagInk, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${app.applicationCode} · ${app.mobile}", style = MaterialTheme.typography.bodySmall, color = RaagMuted)
                }
                ApplicationStatusBadge(app.parsedStatus)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                PaymentStatusBadge(app.parsedPaymentStatus)
                Text("· ${app.documentIds.size} photos · Reg: ${app.registrationNumber.ifBlank { "—" }}", style = MaterialTheme.typography.labelSmall, color = RaagMuted)
            }
            Button(
                onClick = onReview,
                colors = ButtonDefaults.buttonColors(containerColor = RaagCrimson),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text("Review / Update / Issue Policy")
            }
        }
    }
}
