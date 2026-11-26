package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.ApplicationStatus
import com.example.data.DocumentType
import com.example.data.InsuranceApplication
import com.example.data.PolicyRecord
import com.example.data.UiState
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
import com.example.viewmodel.CustomerViewModel
import kotlinx.coroutines.launch

private enum class CustomerTab(val title: String) {
    HOME("⌂  Home"),
    FORM("▣  Customer Form"),
    PDF("▤  PDF Tools"),
    ID_PHOTO("▣  ID Photo")
}

private enum class CustomerFilter(val label: String) {
    ALL("All docs"),
    RECENT("◷ Recent"),
    PENDING("★ Pending"),
    APPROVED("✓ Approved"),
    ISSUED("Policy Issued")
}

private enum class CustomerSubScreen {
    NONE,
    APPLICATION_DETAIL,
    NOTIFICATIONS,
    PROFILE,
    SUPPORT_LEGAL
}

@Composable
fun CustomerPortalScreen(
    viewModel: CustomerViewModel,
    onSecretAdminUnlock: () -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val snackbarHostState = remember { SnackbarHostState() }

    val profileState by viewModel.userProfile.collectAsStateWithLifecycle()
    val settings by viewModel.appSettings.collectAsStateWithLifecycle()
    val appsState by viewModel.applications.collectAsStateWithLifecycle()
    val paymentsState by viewModel.payments.collectAsStateWithLifecycle()
    val policiesState by viewModel.policies.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val unreadCount by viewModel.unreadNotificationCount.collectAsStateWithLifecycle()
    val formState by viewModel.formState.collectAsStateWithLifecycle()
    val actionState by viewModel.actionState.collectAsStateWithLifecycle()
    val paymentScreenshot by viewModel.paymentScreenshot.collectAsStateWithLifecycle()
    val docCache by viewModel.loadedDocCache.collectAsStateWithLifecycle()

    var currentTab by remember { mutableStateOf(CustomerTab.HOME) }
    var activeFilter by remember { mutableStateOf(CustomerFilter.ALL) }
    var showSearchBar by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var subScreen by remember { mutableStateOf(CustomerSubScreen.NONE) }
    var selectedApplicationId by remember { mutableStateOf<String?>(null) }

    // Camera / Gallery picker tracking
    var pendingPhotoTarget by remember { mutableStateOf<DocumentType?>(null) }
    var pendingCaptureUri by remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        val target = pendingPhotoTarget
        if (uri != null && target != null) {
            if (target == DocumentType.PAYMENT_SCREENSHOT) {
                viewModel.attachPaymentScreenshot(context, uri)
            } else {
                viewModel.attachFormPhoto(context, target, uri)
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = pendingCaptureUri
        val target = pendingPhotoTarget
        if (success && uri != null && target != null) {
            if (target == DocumentType.PAYMENT_SCREENSHOT) {
                viewModel.attachPaymentScreenshot(context, uri)
            } else {
                viewModel.attachFormPhoto(context, target, uri)
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val target = pendingPhotoTarget ?: return@rememberLauncherForActivityResult
            val uri = ImageCompressionUtil.createCameraCaptureUri(context, target.name.lowercase())
            pendingCaptureUri = uri
            cameraLauncher.launch(uri)
        } else {
            scope.launch {
                snackbarHostState.showSnackbar("Camera permission denied. You can still upload photos from Gallery.")
            }
        }
    }

    fun launchGallery(docType: DocumentType) {
        pendingPhotoTarget = docType
        galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    fun launchCamera(docType: DocumentType) {
        pendingPhotoTarget = docType
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            val uri = ImageCompressionUtil.createCameraCaptureUri(context, docType.name.lowercase())
            pendingCaptureUri = uri
            cameraLauncher.launch(uri)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

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
    val policies = (policiesState as? UiState.Success)?.data.orEmpty()
    val profile = (profileState as? UiState.Success)?.data

    // Sub-screen overlays with BackHandler
    when (subScreen) {
        CustomerSubScreen.APPLICATION_DETAIL -> {
            val app = applications.firstOrNull { it.id == selectedApplicationId }
            if (app != null) {
                ApplicationDetailScreen(
                    application = app,
                    docCache = docCache,
                    onLoadDoc = viewModel::loadDocument,
                    onPrintPdf = { viewModel.generateAndExportPdf(context, app, printDirectly = true) },
                    onSharePdf = { viewModel.generateAndExportPdf(context, app, printDirectly = false) },
                    onEditApplication = {
                        viewModel.loadApplicationIntoForm(app)
                        subScreen = CustomerSubScreen.NONE
                        currentTab = CustomerTab.FORM
                    },
                    onBack = { subScreen = CustomerSubScreen.NONE }
                )
                return
            } else {
                subScreen = CustomerSubScreen.NONE
            }
        }
        CustomerSubScreen.NOTIFICATIONS -> {
            NotificationsScreen(
                notifications = notifications,
                onMarkRead = viewModel::markNotificationRead,
                onBack = { subScreen = CustomerSubScreen.NONE }
            )
            return
        }
        CustomerSubScreen.PROFILE -> {
            ProfileEditorScreen(
                profile = profile,
                email = viewModel.currentUserEmail,
                defaultName = viewModel.currentUserDisplayName,
                busy = actionState.busy,
                onSave = { n, m, a, c, p, consent ->
                    viewModel.saveUserProfile(n, m, a, c, p, consent)
                },
                onBack = { subScreen = CustomerSubScreen.NONE }
            )
            return
        }
        CustomerSubScreen.SUPPORT_LEGAL -> {
            SupportAndLegalScreen(
                settings = settings,
                onBack = { subScreen = CustomerSubScreen.NONE }
            )
            return
        }
        CustomerSubScreen.NONE -> Unit
    }

    val filteredApplications = remember(applications, activeFilter, searchQuery) {
        applications.filter { app ->
            val matchesFilter = when (activeFilter) {
                CustomerFilter.ALL -> true
                CustomerFilter.RECENT -> true
                CustomerFilter.PENDING -> app.parsedStatus in setOf(
                    ApplicationStatus.DRAFT,
                    ApplicationStatus.SUBMITTED,
                    ApplicationStatus.UNDER_REVIEW,
                    ApplicationStatus.DOCUMENTS_REQUIRED,
                    ApplicationStatus.PAYMENT_PENDING,
                    ApplicationStatus.PAYMENT_VERIFICATION
                )
                CustomerFilter.APPROVED -> app.parsedStatus == ApplicationStatus.APPROVED
                CustomerFilter.ISSUED -> app.parsedStatus == ApplicationStatus.POLICY_ISSUED
            }
            val q = searchQuery.trim().lowercase()
            val matchesSearch = q.isEmpty() ||
                app.fullName.lowercase().contains(q) ||
                app.mobile.lowercase().contains(q) ||
                app.applicationCode.lowercase().contains(q) ||
                app.registrationNumber.lowercase().contains(q)
            matchesFilter && matchesSearch
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(drawerContainerColor = RaagWhite) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(RaagDarkRed)
                        .padding(20.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.raag_logo),
                        contentDescription = "RAAG Logo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .border(2.dp, RaagGold, CircleShape)
                            .background(RaagWhite)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = profile?.name?.ifBlank { viewModel.currentUserDisplayName } ?: viewModel.currentUserDisplayName,
                        style = MaterialTheme.typography.titleMedium,
                        color = RaagWhite
                    )
                    Text(
                        text = viewModel.currentUserEmail,
                        style = MaterialTheme.typography.bodySmall,
                        color = RaagGoldSoft
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                NavigationDrawerItem(
                    label = { Text("Home Dashboard") },
                    selected = currentTab == CustomerTab.HOME,
                    icon = { Icon(Icons.Filled.Home, contentDescription = null, tint = RaagCrimson) },
                    onClick = {
                        currentTab = CustomerTab.HOME
                        scope.launch { drawerState.close() }
                    }
                )
                NavigationDrawerItem(
                    label = { Text("New Insurance Application") },
                    selected = currentTab == CustomerTab.FORM,
                    icon = { Icon(Icons.Filled.Description, contentDescription = null, tint = RaagCrimson) },
                    onClick = {
                        currentTab = CustomerTab.FORM
                        scope.launch { drawerState.close() }
                    }
                )
                NavigationDrawerItem(
                    label = { Text("PDF Tools & Policies") },
                    selected = currentTab == CustomerTab.PDF,
                    icon = { Icon(Icons.Filled.PictureAsPdf, contentDescription = null, tint = RaagCrimson) },
                    onClick = {
                        currentTab = CustomerTab.PDF
                        scope.launch { drawerState.close() }
                    }
                )
                NavigationDrawerItem(
                    label = { Text("Notifications ($unreadCount)") },
                    selected = false,
                    icon = { Icon(Icons.Filled.Notifications, contentDescription = null, tint = RaagCrimson) },
                    onClick = {
                        scope.launch { drawerState.close() }
                        subScreen = CustomerSubScreen.NOTIFICATIONS
                    }
                )
                NavigationDrawerItem(
                    label = { Text("My Profile") },
                    selected = false,
                    icon = { Icon(Icons.Filled.Person, contentDescription = null, tint = RaagCrimson) },
                    onClick = {
                        scope.launch { drawerState.close() }
                        subScreen = CustomerSubScreen.PROFILE
                    }
                )
                NavigationDrawerItem(
                    label = { Text("Support, Privacy & Terms") },
                    selected = false,
                    icon = { Icon(Icons.Filled.SupportAgent, contentDescription = null, tint = RaagCrimson) },
                    onClick = {
                        scope.launch { drawerState.close() }
                        subScreen = CustomerSubScreen.SUPPORT_LEGAL
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = RaagBorder)
                NavigationDrawerItem(
                    label = { Text("Sign Out", color = RaagCrimson) },
                    selected = false,
                    icon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, tint = RaagCrimson) },
                    onClick = {
                        scope.launch { drawerState.close() }
                        onSignOut()
                    },
                    modifier = Modifier.testTag("drawer_sign_out")
                )
            }
        }
    ) {
        Scaffold(
            containerColor = RaagBackground,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                Column {
                    RaagDocScannerTopBar(
                        businessName = settings.businessName,
                        contactPhone = settings.contactPhone,
                        unreadNotifications = unreadCount,
                        onMenuClick = { scope.launch { drawerState.open() } },
                        onSearchToggle = { showSearchBar = !showSearchBar },
                        onNotificationsClick = { subScreen = CustomerSubScreen.NOTIFICATIONS },
                        onSecretAdminUnlock = onSecretAdminUnlock
                    )

                    // Exact HTML .tabs bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(RaagWhite)
                            .horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CustomerTab.entries.forEach { tab ->
                            val selected = currentTab == tab
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clickable { currentTab = tab }
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                                    .testTag("tab_${tab.name.lowercase()}")
                            ) {
                                Text(
                                    text = tab.title,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (selected) RaagCrimson else RaagMuted
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Box(
                                    modifier = Modifier
                                        .width(56.dp)
                                        .height(3.dp)
                                        .background(if (selected) RaagCrimson else Color.Transparent, RoundedCornerShape(2.dp))
                                )
                            }
                        }
                    }
                    HorizontalDivider(color = RaagBorder)

                    // Toggleable search bar matching HTML #searchBar
                    AnimatedVisibility(visible = showSearchBar) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(RaagWhite)
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("ग्राहक का नाम, मोबाइल या Application ID खोजें...") },
                                singleLine = true,
                                shape = RoundedCornerShape(22.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // Exact HTML .filters horizontal pills bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(RaagWhite)
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CustomerFilter.entries.forEach { filter ->
                            val active = activeFilter == filter
                            Surface(
                                onClick = { activeFilter = filter },
                                shape = RoundedCornerShape(24.dp),
                                color = if (active) RaagCrimson else RaagWhite,
                                border = BorderStroke(1.dp, if (active) RaagCrimson else RaagBorder)
                            ) {
                                Text(
                                    text = filter.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (active) RaagWhite else RaagMuted,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                    HorizontalDivider(color = RaagBorder)
                }
            },
            // Exact HTML .fab dual-action floating pill (📷 | ＋)
            floatingActionButton = {
                Surface(
                    shape = RoundedCornerShape(38.dp),
                    color = RaagCrimson,
                    shadowElevation = 8.dp,
                    border = BorderStroke(1.5.dp, RaagGold),
                    modifier = Modifier.navigationBarsPadding()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .clickable { currentTab = CustomerTab.ID_PHOTO }
                                .testTag("fab_camera")
                        ) {
                            Icon(Icons.Filled.PhotoCamera, contentDescription = "Upload EV Photos", tint = RaagGold)
                        }
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(24.dp)
                                .background(RaagWhite.copy(alpha = 0.5f))
                        )
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .clickable {
                                    viewModel.resetForm()
                                    currentTab = CustomerTab.FORM
                                }
                                .testTag("fab_add_application")
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "New Application", tint = RaagWhite)
                        }
                    }
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
                        .widthIn(max = 860.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 14.dp)
                        .padding(bottom = 86.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    when (currentTab) {
                        CustomerTab.HOME -> {
                            // 1. Exact HTML Section 1: Scan Auto Scanner 4500
                            AutoScannerPaymentCard(settings = settings)

                            // 2. Exact HTML Section 2: Scanner Payment Proof Save
                            PaymentProofCard(
                                applications = applications,
                                payments = payments,
                                settings = settings,
                                selectedScreenshot = paymentScreenshot,
                                busy = actionState.busy,
                                onPickFromGallery = { launchGallery(DocumentType.PAYMENT_SCREENSHOT) },
                                onCaptureFromCamera = { launchCamera(DocumentType.PAYMENT_SCREENSHOT) },
                                onClearScreenshot = viewModel::clearPaymentScreenshot,
                                onSubmitPayment = { app, utr -> viewModel.submitPaymentProof(app, utr) }
                            )

                            // 3. Exact HTML Promo Banner
                            RaagPromoBanner(
                                onAddCustomerClick = {
                                    viewModel.resetForm()
                                    currentTab = CustomerTab.FORM
                                }
                            )

                            // 4. Customer Stats Strip (Live Firebase counts)
                            CustomerStatsStrip(applications = applications)

                            // 5. Exact HTML Doc-Scanner Folders Grid
                            Text(
                                text = "Folders (4)",
                                style = MaterialTheme.typography.titleSmall,
                                color = RaagMuted,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                            DocScannerFoldersGrid(
                                appCount = applications.size,
                                photoCount = applications.sumOf { it.documentIds.size },
                                paymentCount = payments.size,
                                policyCount = policies.size,
                                onOpenForm = { currentTab = CustomerTab.FORM },
                                onOpenPhotos = { currentTab = CustomerTab.ID_PHOTO },
                                onOpenPdf = { currentTab = CustomerTab.PDF }
                            )

                            // 6. Exact HTML Saved Customers & PDFs Panel
                            SavedApplicationsPanel(
                                appsState = appsState,
                                applications = filteredApplications,
                                onTrack = { app ->
                                    selectedApplicationId = app.id
                                    subScreen = CustomerSubScreen.APPLICATION_DETAIL
                                },
                                onPdfPrint = { app -> viewModel.generateAndExportPdf(context, app, printDirectly = true) },
                                onPdfShare = { app -> viewModel.generateAndExportPdf(context, app, printDirectly = false) }
                            )
                        }

                        CustomerTab.FORM -> {
                            CustomerFormSection(
                                formState = formState,
                                busy = actionState.busy,
                                focusPhotosOnly = false,
                                onUpdateForm = viewModel::updateForm,
                                onPickPhotoGallery = ::launchGallery,
                                onCapturePhotoCamera = ::launchCamera,
                                onRemovePhoto = viewModel::removeFormPhoto,
                                onSubmitApplication = { asDraft ->
                                    viewModel.submitApplication(asDraft) { _, _ ->
                                        currentTab = CustomerTab.HOME
                                    }
                                },
                                onResetForm = viewModel::resetForm
                            )
                        }

                        CustomerTab.PDF -> {
                            IssuedPoliciesPanel(
                                policies = policies,
                                onDownloadPolicy = { policy -> viewModel.downloadPolicyPdf(context, policy) }
                            )
                            SavedApplicationsPanel(
                                appsState = appsState,
                                applications = filteredApplications,
                                onTrack = { app ->
                                    selectedApplicationId = app.id
                                    subScreen = CustomerSubScreen.APPLICATION_DETAIL
                                },
                                onPdfPrint = { app -> viewModel.generateAndExportPdf(context, app, printDirectly = true) },
                                onPdfShare = { app -> viewModel.generateAndExportPdf(context, app, printDirectly = false) }
                            )
                        }

                        CustomerTab.ID_PHOTO -> {
                            CustomerFormSection(
                                formState = formState,
                                busy = actionState.busy,
                                focusPhotosOnly = true,
                                onUpdateForm = viewModel::updateForm,
                                onPickPhotoGallery = ::launchGallery,
                                onCapturePhotoCamera = ::launchCamera,
                                onRemovePhoto = viewModel::removeFormPhoto,
                                onSubmitApplication = { asDraft ->
                                    viewModel.submitApplication(asDraft) { _, _ ->
                                        currentTab = CustomerTab.HOME
                                    }
                                },
                                onResetForm = viewModel::resetForm
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomerStatsStrip(applications: List<InsuranceApplication>) {
    val total = applications.size
    val pending = applications.count {
        it.parsedStatus in setOf(
            ApplicationStatus.DRAFT, ApplicationStatus.SUBMITTED, ApplicationStatus.UNDER_REVIEW,
            ApplicationStatus.DOCUMENTS_REQUIRED, ApplicationStatus.PAYMENT_PENDING, ApplicationStatus.PAYMENT_VERIFICATION
        )
    }
    val approved = applications.count { it.parsedStatus == ApplicationStatus.APPROVED }
    val issued = applications.count { it.parsedStatus == ApplicationStatus.POLICY_ISSUED }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MiniStatCard("Total", total, RaagDarkRed, Modifier.weight(1f))
        MiniStatCard("Pending", pending, Color(0xFFB4690E), Modifier.weight(1f))
        MiniStatCard("Approved", approved, StatusSuccessInk, Modifier.weight(1f))
        MiniStatCard("Issued", issued, RaagCrimson, Modifier.weight(1f))
    }
}

@Composable
private fun MiniStatCard(label: String, value: Int, accent: Color, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = RaagWhite),
        border = BorderStroke(1.dp, RaagBorder),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value.toString(), style = MaterialTheme.typography.titleLarge, color = accent, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = RaagMuted, maxLines = 1)
        }
    }
}

@Composable
private fun DocScannerFoldersGrid(
    appCount: Int,
    photoCount: Int,
    paymentCount: Int,
    policyCount: Int,
    onOpenForm: () -> Unit,
    onOpenPhotos: () -> Unit,
    onOpenPdf: () -> Unit
) {
    val items = listOf(
        Triple("My Applications", "$appCount customer records", Icons.Filled.Folder to onOpenForm),
        Triple("ID Cards & Photos", "आधार एवं वाहन फोटो ($photoCount)", Icons.Filled.PhotoCamera to onOpenPhotos),
        Triple("UPI Receipts", "₹4,500 payments ($paymentCount)", Icons.Filled.ReceiptLong to onOpenPdf),
        Triple("Insurance PDFs", "Issued policies ($policyCount)", Icons.Filled.Policy to onOpenPdf)
    )
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val twoCols = maxWidth >= 520.dp
        if (twoCols) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items.chunked(2).forEach { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        pair.forEach { (title, sub, pairIcon) ->
                            FolderCardItem(title, sub, pairIcon.first, pairIcon.second, Modifier.weight(1f))
                        }
                    }
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items.forEach { (title, sub, pairIcon) ->
                    FolderCardItem(title, sub, pairIcon.first, pairIcon.second, Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun FolderCardItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(13.dp),
        colors = CardDefaults.cardColors(containerColor = RaagWhite),
        border = BorderStroke(1.dp, RaagBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(RaagGoldSoft)
            ) {
                Icon(icon, contentDescription = null, tint = RaagCrimson, modifier = Modifier.size(28.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = RaagInk)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = RaagMuted)
            }
        }
    }
}

@Composable
private fun SavedApplicationsPanel(
    appsState: UiState<List<InsuranceApplication>>,
    applications: List<InsuranceApplication>,
    onTrack: (InsuranceApplication) -> Unit,
    onPdfPrint: (InsuranceApplication) -> Unit,
    onPdfShare: (InsuranceApplication) -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = RaagWhite),
        border = BorderStroke(1.dp, RaagBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "सेव किए गए ग्राहक और PDF (${applications.size})",
                style = MaterialTheme.typography.titleMedium,
                color = RaagDarkRed
            )
            when (appsState) {
                is UiState.Loading -> {
                    Box(modifier = Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = RaagCrimson)
                    }
                }
                is UiState.Error -> {
                    Text(appsState.message, color = RaagCrimson, style = MaterialTheme.typography.bodySmall)
                }
                is UiState.Success -> {
                    if (applications.isEmpty()) {
                        Text(
                            text = "अभी कोई रिकॉर्ड सेव नहीं है। नीचे ＋ बटन या “Customer Form” टैब से नया आवेदन जोड़ें।",
                            style = MaterialTheme.typography.bodySmall,
                            color = RaagMuted
                        )
                    } else {
                        applications.forEach { app ->
                            Surface(
                                color = Color(0xFFFCF9F7),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, RaagBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(app.fullName, style = MaterialTheme.typography.titleSmall, color = RaagInk, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text("${app.applicationCode} · ${app.mobile}", style = MaterialTheme.typography.bodySmall, color = RaagMuted)
                                        }
                                        ApplicationStatusBadge(app.parsedStatus)
                                    }
                                    Text(
                                        text = "स्कूटी 26/Reg: ${app.registrationNumber.ifBlank { "—" }}  |  बैटरी: ${app.batteryNumber.ifBlank { "—" }}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = RaagMuted
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text("Payment:", style = MaterialTheme.typography.labelSmall, color = RaagMuted)
                                        PaymentStatusBadge(app.parsedPaymentStatus)
                                        Text("· ${app.documentIds.size} photos", style = MaterialTheme.typography.labelSmall, color = RaagMuted)
                                    }
                                    if (app.adminRemarks.isNotBlank()) {
                                        Text(
                                            text = "Admin Note: ${app.adminRemarks}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = RaagDarkRed,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = { onTrack(app) },
                                            border = BorderStroke(1.dp, RaagDarkRed),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Filled.TrackChanges, contentDescription = null, tint = RaagDarkRed, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Track", color = RaagDarkRed, style = MaterialTheme.typography.labelSmall)
                                        }
                                        Button(
                                            onClick = { onPdfPrint(app) },
                                            colors = ButtonDefaults.buttonColors(containerColor = RaagCrimson, contentColor = RaagWhite),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                            modifier = Modifier.weight(1.2f)
                                        ) {
                                            Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("PDF सेव", style = MaterialTheme.typography.labelSmall)
                                        }
                                        OutlinedButton(
                                            onClick = { onPdfShare(app) },
                                            border = BorderStroke(1.dp, RaagCrimson),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Text("Share", color = RaagCrimson, style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IssuedPoliciesPanel(
    policies: List<PolicyRecord>,
    onDownloadPolicy: (PolicyRecord) -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = RaagWhite),
        border = BorderStroke(1.dp, RaagGold),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Policy, contentDescription = null, tint = StatusSuccessInk)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Issued EV Insurance Policies (${policies.size})", style = MaterialTheme.typography.titleMedium, color = RaagDarkRed)
            }
            if (policies.isEmpty()) {
                Text(
                    text = "No official policy issued yet. Policies appear here only after admin verification and policy PDF upload.",
                    style = MaterialTheme.typography.bodySmall,
                    color = RaagMuted
                )
            } else {
                policies.forEach { policy ->
                    Surface(
                        color = StatusSuccessBg,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Policy #${policy.policyNumber}", style = MaterialTheme.typography.titleSmall, color = StatusSuccessInk)
                                Text("App: ${policy.applicationCode} · Issued: ${policy.issueDate}", style = MaterialTheme.typography.bodySmall, color = RaagInk)
                            }
                            Button(
                                onClick = { onDownloadPolicy(policy) },
                                colors = ButtonDefaults.buttonColors(containerColor = StatusSuccessInk)
                            ) {
                                Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("PDF")
                            }
                        }
                    }
                }
            }
        }
    }
}
