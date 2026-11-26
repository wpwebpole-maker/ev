package com.example.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AdminAuditLog
import com.example.data.AdminRecord
import com.example.data.AdminRole
import com.example.data.AppSettings
import com.example.data.ApplicationDocument
import com.example.data.ApplicationStatus
import com.example.data.DocumentType
import com.example.data.InsuranceApplication
import com.example.data.PaymentRecord
import com.example.data.PolicyRecord
import com.example.data.RaagRepository
import com.example.data.UiState
import com.example.data.UserProfile
import com.example.data.unwrapFirestoreError
import com.example.util.ImageCompressionUtil
import com.example.util.PdfGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class AdminActionState(
    val busy: Boolean = false,
    val message: String? = null,
    val error: String? = null
)

class AdminViewModel(
    private val repository: RaagRepository,
    val adminUid: String,
    val adminRole: AdminRole
) : ViewModel() {

    val applications: StateFlow<UiState<List<InsuranceApplication>>> = repository.observeAllApplications(adminUid)
        .map<List<InsuranceApplication>, UiState<List<InsuranceApplication>>> { UiState.Success(it) }
        .catch { err ->
            val root = unwrapFirestoreError(err)
            Log.w(TAG, "Admin applications observer error", root)
            emit(UiState.Error(root.message ?: "Failed to load applications"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val payments: StateFlow<UiState<List<PaymentRecord>>> = repository.observeAllPayments(adminUid)
        .map<List<PaymentRecord>, UiState<List<PaymentRecord>>> { UiState.Success(it) }
        .catch { err ->
            val root = unwrapFirestoreError(err)
            emit(UiState.Error(root.message ?: "Failed to load payments"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val customers: StateFlow<UiState<List<UserProfile>>> = repository.observeAllCustomers(adminUid)
        .map<List<UserProfile>, UiState<List<UserProfile>>> { UiState.Success(it) }
        .catch { err ->
            val root = unwrapFirestoreError(err)
            emit(UiState.Error(root.message ?: "Failed to load customers"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val policies: StateFlow<List<PolicyRecord>> = repository.observeAllPolicies(adminUid)
        .catch { err ->
            unwrapFirestoreError(err)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val adminStaff: StateFlow<List<AdminRecord>> = repository.observeAllAdmins(adminUid)
        .catch { err ->
            unwrapFirestoreError(err)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val auditLogs: StateFlow<List<AdminAuditLog>> = repository.observeAdminLogs(adminUid)
        .catch { err ->
            unwrapFirestoreError(err)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val appSettings: StateFlow<AppSettings> = repository.observeAppSettings(adminUid)
        .catch { err ->
            unwrapFirestoreError(err)
            emit(AppSettings())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), AppSettings())

    private val _actionState = MutableStateFlow(AdminActionState())
    val actionState: StateFlow<AdminActionState> = _actionState.asStateFlow()

    private val _docCache = MutableStateFlow<Map<String, ApplicationDocument>>(emptyMap())
    val docCache: StateFlow<Map<String, ApplicationDocument>> = _docCache.asStateFlow()

    fun clearMessage() {
        _actionState.value = _actionState.value.copy(message = null, error = null)
    }

    fun loadDocument(docId: String) {
        if (docId.isBlank() || _docCache.value.containsKey(docId)) return
        viewModelScope.launch {
            repository.getDocumentById(docId).onSuccess { doc ->
                if (doc != null) {
                    _docCache.value = _docCache.value + (docId to doc)
                }
            }
        }
    }

    fun updateApplicationStatus(application: InsuranceApplication, status: ApplicationStatus, remarks: String) {
        viewModelScope.launch {
            _actionState.value = AdminActionState(busy = true)
            repository.updateApplicationAdmin(application, status, remarks)
                .onSuccess {
                    _actionState.value = AdminActionState(message = "${application.applicationCode} updated to ${status.labelEn}")
                }
                .onFailure { err ->
                    _actionState.value = AdminActionState(error = err.message ?: "Status update failed")
                }
        }
    }

    fun verifyOrRejectPayment(payment: PaymentRecord, approve: Boolean, remarks: String) {
        viewModelScope.launch {
            _actionState.value = AdminActionState(busy = true)
            repository.verifyOrRejectPaymentAdmin(payment, approve, remarks)
                .onSuccess {
                    _actionState.value = AdminActionState(
                        message = if (approve) "Payment verified for ${payment.applicationCode}" else "Payment rejected for ${payment.applicationCode}"
                    )
                }
                .onFailure { err ->
                    _actionState.value = AdminActionState(error = err.message ?: "Payment verification failed")
                }
        }
    }

    fun issuePolicyWithPdf(
        context: Context,
        application: InsuranceApplication,
        policyNumber: String,
        pdfUri: Uri
    ) {
        viewModelScope.launch {
            if (policyNumber.trim().length < 3) {
                _actionState.value = AdminActionState(error = "Enter a valid Policy Number (at least 3 chars).")
                return@launch
            }
            _actionState.value = AdminActionState(busy = true)
            val encodedRes = withContext(Dispatchers.IO) {
                ImageCompressionUtil.readPdfFromUri(context, pdfUri, "Policy_${policyNumber.trim()}")
            }
            if (encodedRes.isFailure) {
                _actionState.value = AdminActionState(error = encodedRes.exceptionOrNull()?.message ?: "Unable to read PDF")
                return@launch
            }
            val encoded = encodedRes.getOrThrow()
            val uploadRes = repository.uploadDocument(
                targetCustomerUid = application.customerUid,
                applicationId = application.id,
                docType = DocumentType.POLICY_PDF,
                fileName = encoded.fileName,
                mimeType = encoded.mimeType,
                sizeBytes = encoded.sizeBytes,
                base64Payload = encoded.base64Payload
            )
            if (uploadRes.isFailure) {
                _actionState.value = AdminActionState(error = uploadRes.exceptionOrNull()?.message ?: "Policy upload failed")
                return@launch
            }
            val issueDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            repository.issuePolicyAdmin(application, policyNumber.trim(), issueDate, uploadRes.getOrThrow())
                .onSuccess {
                    _actionState.value = AdminActionState(message = "Policy ${policyNumber.trim()} issued and sent to customer!")
                }
                .onFailure { err ->
                    _actionState.value = AdminActionState(error = err.message ?: "Policy issuance failed")
                }
        }
    }

    fun generateApplicationPdf(context: Context, application: InsuranceApplication, printDirectly: Boolean) {
        viewModelScope.launch {
            _actionState.value = AdminActionState(busy = true)
            val allPayments = (payments.value as? UiState.Success)?.data.orEmpty()
            val linkedPay = allPayments.firstOrNull { it.applicationId == application.id }
            val docIds = application.documentIds.values.toMutableList()
            if (linkedPay != null && linkedPay.screenshotDocId.isNotBlank()) {
                docIds.add(linkedPay.screenshotDocId)
            }
            val docs = repository.getDocumentsForApplication(docIds)
            val settings = appSettings.value
            val pdfRes = withContext(Dispatchers.IO) {
                PdfGenerator.generateApplicationPdf(context, application, docs, linkedPay, settings)
            }
            pdfRes.onSuccess { file ->
                _actionState.value = AdminActionState(message = "A4 PDF ready: ${file.name}")
                if (printDirectly) {
                    PdfGenerator.printPdfFile(context, file, "RAAG_Admin_${application.applicationCode}")
                } else {
                    PdfGenerator.shareOrOpenPdf(context, file)
                }
            }.onFailure { err ->
                _actionState.value = AdminActionState(error = err.message ?: "PDF generation failed")
            }
        }
    }

    fun toggleCustomerAccountStatus(customer: UserProfile) {
        viewModelScope.launch {
            _actionState.value = AdminActionState(busy = true)
            val enable = customer.accountStatus == "DISABLED"
            repository.updateCustomerAccountStatusAdmin(customer.uid.ifBlank { customer.id }, enable)
                .onSuccess {
                    _actionState.value = AdminActionState(
                        message = if (enable) "${customer.name} account activated" else "${customer.name} account disabled"
                    )
                }
                .onFailure { err ->
                    _actionState.value = AdminActionState(error = err.message ?: "Failed to update customer status")
                }
        }
    }

    fun saveAppSettings(updated: AppSettings) {
        viewModelScope.launch {
            if (adminRole != AdminRole.SUPER_ADMIN) {
                _actionState.value = AdminActionState(error = "Only Super Admins can modify global settings.")
                return@launch
            }
            _actionState.value = AdminActionState(busy = true)
            repository.saveAppSettingsAdmin(updated)
                .onSuccess {
                    _actionState.value = AdminActionState(message = "Global settings & UPI config saved!")
                }
                .onFailure { err ->
                    _actionState.value = AdminActionState(error = err.message ?: "Settings update failed")
                }
        }
    }

    fun assignAdminRole(uid: String, email: String, name: String, role: AdminRole) {
        viewModelScope.launch {
            if (adminRole != AdminRole.SUPER_ADMIN) {
                _actionState.value = AdminActionState(error = "Only Super Admins can manage admin accounts.")
                return@launch
            }
            if (uid.isBlank() || email.isBlank() || name.isBlank()) {
                _actionState.value = AdminActionState(error = "UID, email, and full name are required.")
                return@launch
            }
            _actionState.value = AdminActionState(busy = true)
            repository.upsertAdminRole(uid.trim(), email.trim(), name.trim(), role, true)
                .onSuccess {
                    _actionState.value = AdminActionState(message = "Assigned ${role.displayName} to ${email.trim()}")
                }
                .onFailure { err ->
                    _actionState.value = AdminActionState(error = err.message ?: "Failed to assign role")
                }
        }
    }

    fun removeAdminRole(targetUid: String) {
        viewModelScope.launch {
            if (adminRole != AdminRole.SUPER_ADMIN) {
                _actionState.value = AdminActionState(error = "Only Super Admins can remove admins.")
                return@launch
            }
            _actionState.value = AdminActionState(busy = true)
            repository.deleteAdminRole(targetUid)
                .onSuccess {
                    _actionState.value = AdminActionState(message = "Admin access removed.")
                }
                .onFailure { err ->
                    _actionState.value = AdminActionState(error = err.message ?: "Failed to remove admin")
                }
        }
    }

    private companion object {
        const val TAG = "AdminVM"
    }
}
