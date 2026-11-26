package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.auth.authStateFlow
import com.example.auth.performSignOut
import com.example.data.AdminRole
import com.example.data.RaagRepository
import com.example.ui.AdminPanelScreen
import com.example.ui.AuthScreen
import com.example.ui.CustomerPortalScreen
import com.example.ui.theme.RaagInsuranceTheme
import com.example.viewmodel.AdminViewModel
import com.example.viewmodel.CustomerViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RaagInsuranceTheme {
                RaagAppRoot()
            }
        }
    }
}

@Composable
fun RaagAppRoot() {
    val auth = Firebase.auth
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val user = currentUser
    if (user == null) {
        AuthScreen(
            onAuthSuccess = { /* Reactive AuthStateListener transitions automatically */ }
        )
    } else {
        val uid = user.uid
        key(uid) {
            AuthenticatedSessionHost(
                uid = uid,
                email = user.email.orEmpty(),
                emailVerified = user.isEmailVerified,
                displayName = user.displayName.orEmpty(),
                onSignOut = {
                    performSignOut(credentialManager, scope)
                }
            )
        }
    }
}

@Composable
private fun AuthenticatedSessionHost(
    uid: String,
    email: String,
    emailVerified: Boolean,
    displayName: String,
    onSignOut: () -> Unit
) {
    val customerVm: CustomerViewModel = viewModel(
        key = "customer_$uid",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                CustomerViewModel(
                    repository = RaagRepository(db),
                    currentUserId = uid,
                    currentUserEmail = email,
                    currentUserEmailVerified = emailVerified,
                    currentUserDisplayName = displayName
                )
            }
        }
    )

    val isAdminEligible by customerVm.isAdminEligible.collectAsStateWithLifecycle()
    val effectiveAdminRole by customerVm.effectiveAdminRole.collectAsStateWithLifecycle()
    var secretAdminOpen by remember(uid) { mutableStateOf(false) }

    if (secretAdminOpen && isAdminEligible && effectiveAdminRole != null) {
        val role: AdminRole = effectiveAdminRole ?: AdminRole.APPLICATION_MANAGER
        val adminVm: AdminViewModel = viewModel(
            key = "admin_${uid}_${role.name}",
            factory = viewModelFactory {
                initializer {
                    val app = checkNotNull(this[APPLICATION_KEY])
                    val databaseId = app.getString(R.string.firestore_database_id)
                    val db = FirebaseFirestore.getInstance(databaseId)
                    AdminViewModel(
                        repository = RaagRepository(db),
                        adminUid = uid,
                        adminRole = role
                    )
                }
            }
        )
        AdminPanelScreen(
            viewModel = adminVm,
            onBackToCustomerApp = { secretAdminOpen = false },
            onSignOut = {
                secretAdminOpen = false
                onSignOut()
            }
        )
    } else {
        CustomerPortalScreen(
            viewModel = customerVm,
            onSecretAdminUnlock = {
                // Silent no-op for normal customers; opens Admin Dashboard ONLY for verified Admins
                if (isAdminEligible && effectiveAdminRole != null) {
                    secretAdminOpen = true
                }
            },
            onSignOut = onSignOut
        )
    }
}
