package com.example.data

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class AdminRole(val displayName: String) {
    SUPER_ADMIN("Super Admin"),
    APPLICATION_MANAGER("Application Manager"),
    PAYMENT_MANAGER("Payment Manager"),
    DOCUMENT_MANAGER("Document Manager");

    companion object {
        fun fromRaw(raw: String?): AdminRole =
            entries.firstOrNull { it.name == raw } ?: APPLICATION_MANAGER
    }
}

enum class ApplicationStatus(
    val labelEn: String,
    val labelHi: String,
    val stepIndex: Int
) {
    DRAFT("Draft", "ड्राफ्ट", 0),
    SUBMITTED("Submitted", "सबमिट किया गया", 1),
    UNDER_REVIEW("Under Review", "समीक्षाधीन", 2),
    DOCUMENTS_REQUIRED("Documents Required", "दस्तावेज़ आवश्यक", 3),
    PAYMENT_PENDING("Payment Pending", "भुगतान लंबित", 4),
    PAYMENT_VERIFICATION("Payment Verification", "भुगतान सत्यापन", 5),
    APPROVED("Approved", "स्वीकृत", 6),
    POLICY_ISSUED("Policy Issued", "पॉलिसी जारी", 7),
    REJECTED("Rejected", "अस्वीकृत", 8);

    companion object {
        fun fromRaw(raw: String?): ApplicationStatus =
            entries.firstOrNull { it.name == raw } ?: SUBMITTED
    }
}

enum class PaymentStatus(val labelEn: String, val labelHi: String) {
    UNPAID("Unpaid", "अभुगतान"),
    PENDING_VERIFICATION("Pending Verification", "सत्यापन लंबित"),
    VERIFIED("Verified", "सत्यापित"),
    REJECTED("Rejected", "अस्वीकृत");

    companion object {
        fun fromRaw(raw: String?): PaymentStatus =
            entries.firstOrNull { it.name == raw } ?: PENDING_VERIFICATION
    }
}

enum class DocumentType(
    val labelHi: String,
    val labelEn: String,
    val optional: Boolean = false
) {
    AADHAAR_FRONT("आधार कार्ड - आगे (वैकल्पिक)", "Aadhaar Front (Optional)", true),
    AADHAAR_BACK("आधार कार्ड - पीछे (वैकल्पिक)", "Aadhaar Back (Optional)", true),
    SCOOTY_FRONT("स्कूटी - सामने", "Scooty Front Photo", false),
    SCOOTY_BACK("स्कूटी - पीछे", "Scooty Back Photo", false),
    SCOOTY_LEFT("स्कूटी - बायाँ हिस्सा", "Scooty Left Side", false),
    SCOOTY_RIGHT("स्कूटी - दायाँ हिस्सा", "Scooty Right Side", false),
    BATTERY_PHOTO("बैटरी की फोटो", "Battery Photo", false),
    PAYMENT_SCREENSHOT("पेमेंट स्क्रीनशॉट", "Payment Screenshot", false),
    POLICY_PDF("जारी पॉलिसी PDF", "Issued Policy PDF", false);

    companion object {
        val applicationPhotoTypes: List<DocumentType> = listOf(
            AADHAAR_FRONT,
            AADHAAR_BACK,
            SCOOTY_FRONT,
            SCOOTY_BACK,
            SCOOTY_LEFT,
            SCOOTY_RIGHT,
            BATTERY_PHOTO
        )

        fun fromRaw(raw: String?): DocumentType =
            entries.firstOrNull { it.name == raw } ?: SCOOTY_FRONT
    }
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val mobile: String = "",
    val address: String = "",
    val city: String = "",
    val pinCode: String = "",
    val accountStatus: String = "ACTIVE",
    val consentAccepted: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AdminRecord(
    @DocumentId val id: String = "",
    val uid: String = "",
    val email: String = "",
    val name: String = "",
    val role: String = AdminRole.APPLICATION_MANAGER.name,
    val active: Boolean = true,
    val createdBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val parsedRole: AdminRole get() = AdminRole.fromRaw(role)
}

@Keep
@IgnoreExtraProperties
data class InsuranceApplication(
    @DocumentId val id: String = "",
    val applicationCode: String = "",
    val customerUid: String = "",
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
    val documentIds: Map<String, String> = emptyMap(),
    val applicationStatus: String = ApplicationStatus.SUBMITTED.name,
    val paymentStatus: String = PaymentStatus.UNPAID.name,
    val adminRemarks: String = "",
    val policyNumber: String = "",
    val policyDocId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val parsedStatus: ApplicationStatus get() = ApplicationStatus.fromRaw(applicationStatus)
    val parsedPaymentStatus: PaymentStatus get() = PaymentStatus.fromRaw(paymentStatus)
}

@Keep
@IgnoreExtraProperties
data class ApplicationDocument(
    @DocumentId val id: String = "",
    val customerUid: String = "",
    val applicationId: String = "",
    val docType: String = DocumentType.SCOOTY_FRONT.name,
    val fileName: String = "",
    val mimeType: String = "image/jpeg",
    val sizeBytes: Long = 0L,
    val payloadBase64: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    val parsedType: DocumentType get() = DocumentType.fromRaw(docType)
}

@Keep
@IgnoreExtraProperties
data class PaymentRecord(
    @DocumentId val id: String = "",
    val applicationId: String = "",
    val applicationCode: String = "",
    val customerUid: String = "",
    val customerName: String = "",
    val customerMobile: String = "",
    val amount: Double = 4500.0,
    val upiId: String = "9507402469-3@ibl",
    val utrNumber: String = "",
    val screenshotDocId: String = "",
    val paymentStatus: String = PaymentStatus.PENDING_VERIFICATION.name,
    val verificationRemarks: String = "",
    val verifiedBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
    val verifiedAt: Timestamp? = null
) {
    val parsedStatus: PaymentStatus get() = PaymentStatus.fromRaw(paymentStatus)
}

@Keep
@IgnoreExtraProperties
data class PolicyRecord(
    @DocumentId val id: String = "",
    val applicationId: String = "",
    val applicationCode: String = "",
    val customerUid: String = "",
    val customerName: String = "",
    val policyNumber: String = "",
    val policyDocId: String = "",
    val issueDate: String = "",
    val issuedBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AppNotification(
    @DocumentId val id: String = "",
    val recipientUid: String = "",
    val title: String = "",
    val message: String = "",
    val type: String = "GENERAL",
    val relatedId: String = "",
    val read: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AppSettings(
    @DocumentId val id: String = "appConfig",
    val businessName: String = "RAAG AUTOMOBILES – EV INSURANCE",
    val contactPhone: String = "9507402469",
    val supportEmail: String = "suchita9867@gmail.com",
    val upiId: String = "9507402469-3@ibl",
    val upiRecipientName: String = "RAAG AUTOMOBILES",
    val defaultAmount: Double = 4500.0,
    val paymentInstructions: String = "UPI पेमेंट के लिए QR स्कैन करें या UPI ऐप से भुगतान करें और स्क्रीनशॉट अपलोड करें। एडमिन सत्यापन के बाद भुगतान स्वीकृत होगा।",
    val privacyPolicyUrl: String = "https://raagautomobiles.com/privacy-policy",
    val termsUrl: String = "https://raagautomobiles.com/terms",
    val updatedBy: String = "system",
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AdminAuditLog(
    @DocumentId val id: String = "",
    val adminUid: String = "",
    val adminEmail: String = "",
    val action: String = "",
    val recordId: String = "",
    val details: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

object BrandConstants {
    const val BOOTSTRAPPED_ADMIN_EMAIL = "suchita9867@gmail.com"
    val BOOTSTRAPPED_ADMIN_EMAILS: Set<String> = setOf(
        "suchita9867@gmail.com",
        "wpwebpole@gmail.com",
        "vk725846@gmail.com"
    )
    const val DEFAULT_UPI_ID = "9507402469-3@ibl"
    const val DEFAULT_UPI_NAME = "RAAG AUTOMOBILES"
    const val DEFAULT_AMOUNT = 4500.0
    const val DEFAULT_PHONE = "9507402469"

    fun isBootstrappedAdminEmail(email: String?): Boolean {
        val normalized = email?.trim()?.lowercase() ?: return false
        return normalized in BOOTSTRAPPED_ADMIN_EMAILS
    }
}
