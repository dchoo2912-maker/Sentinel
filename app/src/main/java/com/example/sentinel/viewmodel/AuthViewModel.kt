package com.example.sentinel.viewmodel

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sentinel.data.FirestoreService
import com.example.sentinel.domain.User
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Authenticated(val user: User) : AuthState()
    data class Success(val message: String) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val firestoreService = FirestoreService()
    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        auth.currentUser?.let { firebaseUser ->
            _authState.value = AuthState.Authenticated(
                User(
                    id = firebaseUser.uid,
                    name = firebaseUser.displayName ?: "User",
                    email = firebaseUser.email ?: "",
                    isPro = true
                )
            )
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val result = auth.signInWithEmailAndPassword(email, password).await()
                val firebaseUser = result.user
                if (firebaseUser != null) {
                    val user = User(
                        id = firebaseUser.uid,
                        name = firebaseUser.displayName ?: "User",
                        email = firebaseUser.email ?: "",
                        isPro = true
                    )
                    firestoreService.saveUserProfile(user)
                    _authState.value = AuthState.Authenticated(user)
                } else {
                    _authState.value = AuthState.Error("Login failed")
                }
            } catch (e: Exception) {
                android.util.Log.e("AuthViewModel", "Sign-in error details", e)
                _authState.value = AuthState.Error(e.message ?: "Login failed")
            }
        }
    }

    fun signUp(email: String, password: String, name: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                val firebaseUser = result.user
                if (firebaseUser != null) {
                    val user = User(
                        id = firebaseUser.uid,
                        name = name,
                        email = firebaseUser.email ?: "",
                        isPro = false
                    )
                    firestoreService.saveUserProfile(user)
                    _authState.value = AuthState.Authenticated(user)
                } else {
                    _authState.value = AuthState.Error("Sign up failed")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Sign up failed")
            }
        }
    }

    fun signInWithGoogle(context: Context, serverClientId: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val credentialManager = CredentialManager.create(context)
            
            // Using GetGoogleIdOption with specific settings to ensure account picker shows
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false) // CRITICAL: Show all accounts, not just "saved" ones
                .setServerClientId(serverClientId)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            try {
                val result = credentialManager.getCredential(
                    context = context,
                    request = request
                )
                handleGoogleSignInResult(result)
            } catch (e: GetCredentialException) {
                // This logs the EXACT type of failure to your terminal
                android.util.Log.e("AuthViewModel", "Error Type: ${e.type}")
                android.util.Log.e("AuthViewModel", "Error Message: ${e.message}")
                
                val userFriendlyMessage = when (e.type) {
                    "androidx.credentials.TYPE_GET_CREDENTIAL_UNSUPPORTED_EXCEPTION" -> "Google Sign-In is not supported on this device."
                    "android.credentials.GetCredentialException.TYPE_USER_CANCELED" -> "Sign-in cancelled."
                    "android.credentials.GetCredentialException.TYPE_NO_CREDENTIAL" -> "No Google account found. Please add an account to your device settings."
                    else -> e.message ?: "Sign in failed"
                }
                _authState.value = AuthState.Error(userFriendlyMessage)
            } catch (e: Exception) {
                _authState.value = AuthState.Error("An unexpected error occurred")
            }
        }
    }

    private suspend fun handleGoogleSignInResult(result: GetCredentialResponse) {
        val credential = result.credential
        android.util.Log.d("AuthViewModel", "Received Credential Type: ${credential.type}")
        
        val idToken = when (credential) {
            is GoogleIdTokenCredential -> {
                credential.idToken
            }
            else -> {
                // Try to extract idToken manually from CustomCredential data if needed
                credential.data.getString("com.google.android.libraries.identity.googleid.BUNDLE_KEY_ID_TOKEN")
            }
        }

        if (idToken != null) {
            val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
            try {
                val authResult = auth.signInWithCredential(firebaseCredential).await()
                val firebaseUser = authResult.user
                if (firebaseUser != null) {
                    val user = User(
                        id = firebaseUser.uid,
                        name = firebaseUser.displayName ?: "User",
                        email = firebaseUser.email ?: "",
                        isPro = true
                    )
                    firestoreService.saveUserProfile(user)
                    _authState.value = AuthState.Authenticated(user)
                }
            } catch (e: Exception) {
                android.util.Log.e("AuthViewModel", "Firebase sign-in failed", e)
                _authState.value = AuthState.Error("Firebase sign-in failed: ${e.message}")
            }
        } else {
            android.util.Log.e("AuthViewModel", "Could not find ID Token in credential: ${credential.type}")
            _authState.value = AuthState.Error("Google Sign-In error: Token not found.")
        }
    }

    fun logout() {
        auth.signOut()
        _authState.value = AuthState.Idle
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }

    fun resetPassword(email: String) {
        if (email.isBlank()) {
            _authState.value = AuthState.Error("Please enter your email address first")
            return
        }
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                auth.sendPasswordResetEmail(email).await()
                _authState.value = AuthState.Success("Password reset email sent. Check your inbox.")
            } catch (e: Exception) {
                android.util.Log.e("AuthViewModel", "Reset Password Error", e)
                _authState.value = AuthState.Error(e.message ?: "Failed to send reset email")
            }
        }
    }
}
