package com.example.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AdminRecord
import com.example.data.AdminRole
import com.example.data.AppNotification
import com.example.data.AppSettings
import com.example.data.ApplicationDocument
import com.example.data.BrandConstants
import com.example.data.DocumentType
import com.example.data.InsuranceApplication
import com.example.data.PaymentRecord
import com.example.data.PolicyRecord
import com.example.data.RaagRepository
import com.example.data.UiState
import com.example.data.UserProfile
import com.example.data.unwrapFirestoreError
import com.example.util.EncodedUploadFile
import com.example.util.ImageCompressionUtil
import com.example.util.PdfGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class CustomerFormState(
    val editingApplicationId: String? = null,
    val editingApplicationCode: String? = null,
    val fullName: String = "",
    val mobile: String = "",
    val email: String = "",
    val address: String = "",
    val city: String = "",
    val pinCode: String = "",
    val vehicleBrand: String = "",
    val vehicleModel: String = "",
    val registrationNumber: String = "",
    val batteryNumber: String = "",
    val vehicleNotes: String = "",
    val uploadedDocuments: Map<DocumentType, EncodedUploadFile> = emptyMap(),
    val existingDocumentIds: Map<String, String> = emptyMap()
)

data class CustomerActionState(
    val busy: Boolean = false,
    val message: String? = null,
    val error: String? = null
)

class CustomerViewModel(
    private val repository: RaagRepository,
    val currentUserId: String,
    val currentUserEmail: String,
    val currentUserEmailVerified: Boolean,
    val currentUserDisplayName: String
) : ViewModel() {

    val userProfile: StateFlow<UiState<UserProfile?>> = repository.observeUserProfile(currentUserId)
        .map<UserProfile?, UiState<UserProfile?>> { UiState.Success(it) }
        .catch { error ->
            val root = unwrapFirestoreError(error)
            Log.w(TAG, "Error observing user profile", root)
            emit(UiState.Error(root.message ?: "Failed to load profile"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val adminRecord: StateFlow<AdminRecord?> = repository.observeAdminRecord(currentUserId)
        .catch { error ->
            unwrapFirestoreError(error)
            emit(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val isAdminEligible: StateFlow<Boolean> = adminRecord
        .map { record ->
            val isBootstrapped = BrandConstants.isBootstrappedAdminEmail(currentUserEmail) && currentUserEmailVerified
            isBootstrapped || (record != null && record.active)
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000L),
            BrandConstants.isBootstrappedAdminEmail(currentUserEmail) && currentUserEmailVerified
        )

    val effectiveAdminRole: StateFlow<AdminRole?> = adminRecord
        .map { record ->
            val isBootstrapped = BrandConstants.isBootstrappedAdminEmail(currentUserEmail) && currentUserEmailVerified
            when {
                isBootstrapped -> AdminRole.SUPER_ADMIN
                record != null && record.active -> record.parsedRole
                else -> null
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000L),
            if (BrandConstants.isBootstrappedAdminEmail(currentUserEmail) && currentUserEmailVerified) AdminRole.SUPER_ADMIN else null
        )

    val appSettings: StateFlow<AppSettings> = repository.observeAppSettings(currentUserId)
        .catch { error ->
            unwrapFirestoreError(error)
            emit(AppSettings())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), AppSettings())

    val applications: StateFlow<UiState<List<InsuranceApplication>>> = repository.observeCustomerApplications(currentUserId)
        .map<List<InsuranceApplication>, UiState<List<InsuranceApplication>>> { UiState.Success(it) }
        .catch { error ->
            val root = unwrapFirestoreError(error)
            Log.w(TAG, "Error observing applications", root)
            emit(UiState.Error(root.message ?: "Failed to load applications"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val payments: StateFlow<UiState<List<PaymentRecord>>> = repository.observeCustomerPayments(currentUserId)
        .map<List<PaymentRecord>, UiState<List<PaymentRecord>>> { UiState.Success(it) }
        .catch { error ->
            val root = unwrapFirestoreError(error)
            Log.w(TAG, "Error observing payments", root)
            emit(UiState.Error(root.message ?: "Failed to load payments"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val policies: StateFlow<UiState<List<PolicyRecord>>> = repository.observeCustomerPolicies(currentUserId)
        .map<List<PolicyRecord>, UiState<List<PolicyRecord>>> { UiState.Success(it) }
        .catch { error ->
            val root = unwrapFirestoreError(error)
            Log.w(TAG, "Error observing policies", root)
            emit(UiState.Error(root.message ?: "Failed to load policies"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val notifications: StateFlow<List<AppNotification>> = repository.observeNotifications(currentUserId)
        .catch { error ->
            unwrapFirestoreError(error)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val unreadNotificationCount: StateFlow<Int> = notifications
        .map { list -> list.count { !it.read } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 0)

    private val _formState = MutableStateFlow(
        CustomerFormState(
            fullName = currentUserDisplayName,
            email = currentUserEmail
        )
    )
    val formState: StateFlow<CustomerFormState> = _formState.asStateFlow()

    private val _actionState = MutableStateFlow(CustomerActionState())
    val actionState: StateFlow<CustomerActionState> = _actionState.asStateFlow()

    private val _paymentScreenshot = MutableStateFlow<EncodedUploadFile?>(null)
    val paymentScreenshot: StateFlow<EncodedUploadFile?> = _paymentScreenshot.asStateFlow()

    private val _loadedDocCache = MutableStateFlow<Map<String, ApplicationDocument>>(emptyMap())
    val loadedDocCache: StateFlow<Map<String, ApplicationDocument>> = _loadedDocCache.asStateFlow()

    init {
        ensureProfileInitialized()
    }

    private fun ensureProfileInitialized() {
        viewModelScope.launch {
            val current = userProfile.value
            if (current is UiState.Success && current.data != null) return@launch
            // Wait briefly or check via upsert only if user saves profile; prefill form from profile when loaded
            userProfile.collect { state ->
                if (state is UiState.Success) {
                    val profile = state.data
                    if (profile != null && _formState.value.mobile.isBlank() && _formState.value.editingApplicationId == null) {
                        _formState.value = _formState.value.copy(
                            fullName = profile.name.ifBlank { currentUserDisplayName },
                            mobile = profile.mobile,
                            email = profile.email.ifBlank { currentUserEmail },
                            address = profile.address,
                            city = profile.city,
                            pinCode = profile.pinCode
                        )
                    }
                }
            }
        }
    }

    fun clearMessage() {
        _actionState.value = _actionState.value.copy(message = null, error = null)
    }

    fun updateForm(transform: (CustomerFormState) -> CustomerFormState) {
        _formState.value = transform(_formState.value)
    }

    fun resetForm() {
        val profile = (userProfile.value as? UiState.Success)?.data
        _formState.value = CustomerFormState(
            fullName = profile?.name ?: currentUserDisplayName,
            mobile = profile?.mobile ?: "",
            email = profile?.email ?: currentUserEmail,
            address = profile?.address ?: "",
            city = profile?.city ?: "",
            pinCode = profile?.pinCode ?: ""
        )
    }

    fun loadApplicationIntoForm(app: InsuranceApplication) {
        _formState.value = CustomerFormState(
            editingApplicationId = app.id,
            editingApplicationCode = app.applicationCode,
            fullName = app.fullName,
            mobile = app.mobile,
            email = app.email,
            address = app.address,
            city = app.city,
            pinCode = app.pinCode,
            vehicleBrand = app.vehicleBrand,
            vehicleModel = app.vehicleModel,
            registrationNumber = app.registrationNumber,
            batteryNumber = app.batteryNumber,
            vehicleNotes = app.vehicleNotes,
            uploadedDocuments = emptyMap(),
            existingDocumentIds = app.documentIds
        )
    }

    fun attachFormPhoto(context: Context, docType: DocumentType, uri: Uri) {
        viewModelScope.launch {
            _actionState.value = CustomerActionState(busy = true)
            val result = withContext(Dispatchers.IO) {
                ImageCompressionUtil.compressImageFromUri(context, uri, docType.name.lowercase())
            }
            result.onSuccess { encoded ->
                _formState.value = _formState.value.copy(
                    uploadedDocuments = _formState.value.uploadedDocuments + (docType to encoded)
                )
                _actionState.value = CustomerActionState(message = "${docType.labelEn} ready for upload.")
            }.onFailure { err ->
                _actionState.value = CustomerActionState(error = err.message ?: "Failed to process image")
            }
        }
    }

    fun removeFormPhoto(docType: DocumentType) {
        _formState.value = _formState.value.copy(
            uploadedDocuments = _formState.value.uploadedDocuments - docType,
            existingDocumentIds = _formState.value.existingDocumentIds - docType.name
        )
    }

    fun attachPaymentScreenshot(context: Context, uri: Uri) {
        viewModelScope.launch {
            _actionState.value = CustomerActionState(busy = true)
            val result = withContext(Dispatchers.IO) {
                ImageCompressionUtil.compressImageFromUri(context, uri, "payment_screenshot")
            }
            result.onSuccess { encoded ->
                _paymentScreenshot.value = encoded
                _actionState.value = CustomerActionState(message = "स्क्रीनशॉट चुन लिया गया है। अब सेव करें।")
            }.onFailure { err ->
                _actionState.value = CustomerActionState(error = err.message ?: "Unable to read payment screenshot")
            }
        }
    }

    fun clearPaymentScreenshot() {
        _paymentScreenshot.value = null
    }

    fun saveUserProfile(
        name: String,
        mobile: String,
        address: String,
        city: String,
        pinCode: String,
        consentAccepted: Boolean
    ) {
        viewModelScope.launch {
            if (name.isBlank()) {
                _actionState.value = CustomerActionState(error = "कृपया पूरा नाम दर्ज करें।")
                return@launch
            }
            if (!consentAccepted) {
                _actionState.value = CustomerActionState(error = "कृपया Privacy Policy और Terms स्वीकार करें।")
                return@launch
            }
            _actionState.value = CustomerActionState(busy = true)
            repository.upsertUserProfile(name, mobile, address, city, pinCode, consentAccepted)
                .onSuccess {
                    _actionState.value = CustomerActionState(message = "प्रोफ़ाइल सुरक्षित रूप से सेव हो गई।")
                }
                .onFailure { err ->
                    _actionState.value = CustomerActionState(error = err.message ?: "Profile update failed")
                }
        }
    }

    fun submitApplication(asDraft: Boolean = false, onSuccess: (String, String) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            val form = _formState.value
            if (form.fullName.isBlank() || form.mobile.trim().length < 7 || form.address.isBlank()) {
                _actionState.value = CustomerActionState(error = "कृपया पूरा नाम, सही मोबाइल नंबर और पूरा पता भरें।")
                return@launch
            }
            _actionState.value = CustomerActionState(busy = true)

            val docIds = form.existingDocumentIds.toMutableMap()
            val tempAppRef = form.editingApplicationId ?: "pending_${System.currentTimeMillis()}"

            for ((type, encoded) in form.uploadedDocuments) {
                val uploadRes = repository.uploadDocument(
                    targetCustomerUid = currentUserId,
                    applicationId = tempAppRef,
                    docType = type,
                    fileName = encoded.fileName,
                    mimeType = encoded.mimeType,
                    sizeBytes = encoded.sizeBytes,
                    base64Payload = encoded.base64Payload
                )
                if (uploadRes.isFailure) {
                    _actionState.value = CustomerActionState(
                        error = "Photo upload failed (${type.labelEn}): ${uploadRes.exceptionOrNull()?.message}"
                    )
                    return@launch
                }
                docIds[type.name] = uploadRes.getOrThrow()
            }

            // Ensure profile exists for this customer in Firestore
            repository.upsertUserProfile(
                name = form.fullName,
                mobile = form.mobile,
                address = form.address,
                city = form.city,
                pinCode = form.pinCode,
                consentAccepted = true
            )

            repository.submitApplication(
                existingId = form.editingApplicationId,
                existingCode = form.editingApplicationCode,
                fullName = form.fullName,
                mobile = form.mobile,
                email = form.email.ifBlank { currentUserEmail },
                address = form.address,
                city = form.city,
                pinCode = form.pinCode,
                vehicleBrand = form.vehicleBrand,
                vehicleModel = form.vehicleModel,
                registrationNumber = form.registrationNumber,
                batteryNumber = form.batteryNumber,
                vehicleNotes = form.vehicleNotes,
                documentIds = docIds,
                asDraft = asDraft
            ).onSuccess { (appId, appCode) ->
                _actionState.value = CustomerActionState(
                    message = if (asDraft) "ड्राफ्ट सेव हो गया ($appCode)" else "आवेदन सफलतापूर्वक सबमिट हो गया ($appCode)!"
                )
                resetForm()
                onSuccess(appId, appCode)
            }.onFailure { err ->
                _actionState.value = CustomerActionState(error = err.message ?: "Application submission failed")
            }
        }
    }

    fun submitPaymentProof(application: InsuranceApplication, utrNumber: String) {
        viewModelScope.launch {
            val screenshot = _paymentScreenshot.value
            if (screenshot == null) {
                _actionState.value = CustomerActionState(error = "पहले पेमेंट स्क्रीनशॉट चुनें।")
                return@launch
            }
            _actionState.value = CustomerActionState(busy = true)
            val uploadRes = repository.uploadDocument(
                targetCustomerUid = currentUserId,
                applicationId = application.id,
                docType = DocumentType.PAYMENT_SCREENSHOT,
                fileName = screenshot.fileName,
                mimeType = screenshot.mimeType,
                sizeBytes = screenshot.sizeBytes,
                base64Payload = screenshot.base64Payload
            )
            if (uploadRes.isFailure) {
                _actionState.value = CustomerActionState(error = uploadRes.exceptionOrNull()?.message ?: "Screenshot upload failed")
                return@launch
            }
            val screenshotDocId = uploadRes.getOrThrow()
            val settings = appSettings.value
            repository.submitPaymentProof(
                application = application,
                amount = settings.defaultAmount,
                upiId = settings.upiId,
                utrNumber = utrNumber,
                screenshotDocId = screenshotDocId
            ).onSuccess {
                _paymentScreenshot.value = null
                _actionState.value = CustomerActionState(
                    message = "पेमेंट स्क्रीनशॉट सेव हो गया! एडमिन सत्यापन (Pending Verification) के बाद स्थिति अपडेट होगी।"
                )
            }.onFailure { err ->
                _actionState.value = CustomerActionState(error = err.message ?: "Payment submission failed")
            }
        }
    }

    fun loadDocument(docId: String) {
        if (docId.isBlank() || _loadedDocCache.value.containsKey(docId)) return
        viewModelScope.launch {
            repository.getDocumentById(docId).onSuccess { doc ->
                if (doc != null) {
                    _loadedDocCache.value = _loadedDocCache.value + (docId to doc)
                }
            }
        }
    }

    fun generateAndExportPdf(
        context: Context,
        application: InsuranceApplication,
        printDirectly: Boolean,
        onReady: (File) -> Unit = {}
    ) {
        viewModelScope.launch {
            _actionState.value = CustomerActionState(busy = true)
            val allPayments = (payments.value as? UiState.Success)?.data.orEmpty()
            val linkedPayment = allPayments.firstOrNull { it.applicationId == application.id }

            val docIds = application.documentIds.values.toMutableList()
            if (linkedPayment != null && linkedPayment.screenshotDocId.isNotBlank()) {
                docIds.add(linkedPayment.screenshotDocId)
            }
            val docs = repository.getDocumentsForApplication(docIds)
            val settings = appSettings.value

            val pdfResult = withContext(Dispatchers.IO) {
                PdfGenerator.generateApplicationPdf(context, application, docs, linkedPayment, settings)
            }
            pdfResult.onSuccess { file ->
                _actionState.value = CustomerActionState(message = "A4 PDF तैयार है: ${file.name}")
                if (printDirectly) {
                    PdfGenerator.printPdfFile(context, file, "RAAG_Insurance_${application.applicationCode}")
                } else {
                    PdfGenerator.shareOrOpenPdf(context, file)
                }
                onReady(file)
            }.onFailure { err ->
                _actionState.value = CustomerActionState(error = err.message ?: "PDF generation failed")
            }
        }
    }

    fun downloadPolicyPdf(context: Context, policy: PolicyRecord) {
        viewModelScope.launch {
            _actionState.value = CustomerActionState(busy = true)
            repository.getDocumentById(policy.policyDocId).onSuccess { doc ->
                if (doc == null || doc.payloadBase64.isBlank()) {
                    _actionState.value = CustomerActionState(error = "Policy document file not found.")
                    return@onSuccess
                }
                val file = withContext(Dispatchers.IO) {
                    ImageCompressionUtil.decodeBase64ToTempFile(context, doc.payloadBase64, doc.fileName.ifBlank { "${policy.policyNumber}.pdf" })
                }
                if (file != null) {
                    _actionState.value = CustomerActionState(message = "Policy PDF ready: ${policy.policyNumber}")
                    PdfGenerator.shareOrOpenPdf(context, file)
                } else {
                    _actionState.value = CustomerActionState(error = "Unable to decode policy file.")
                }
            }.onFailure { err ->
                _actionState.value = CustomerActionState(error = err.message ?: "Failed to fetch policy")
            }
        }
    }

    fun markNotificationRead(notificationId: String) {
        viewModelScope.launch {
            repository.markNotificationRead(notificationId)
        }
    }

    private companion object {
        const val TAG = "CustomerVM"
    }
}
