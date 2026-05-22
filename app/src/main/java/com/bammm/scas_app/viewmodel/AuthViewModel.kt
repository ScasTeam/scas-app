package com.bammm.scas_app.viewmodel

import android.content.Context
import android.provider.Settings
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bammm.scas_app.BuildConfig
import com.bammm.scas_app.data.api.ApiClient
import com.bammm.scas_app.data.model.GoogleLoginRequest
import com.bammm.scas_app.data.model.VerifyOtpRequest
import com.bammm.scas_app.data.preferences.UserPreferences
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class RequireOtp(val email: String) : AuthState()
    object Success : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel(
    private val userPreferences: UserPreferences,
    private val context: Context
) : ViewModel() {

    private val apiService = ApiClient.getService(userPreferences)

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    private fun getAndroidId(): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: UUID.randomUUID().toString()
    }

    fun signInWithGoogle(credentialManager: CredentialManager) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val hashedNonce = UUID.randomUUID().toString()
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(BuildConfig.WEB_CLIENT_ID)
                    .setNonce(hashedNonce)
                    .build()

                val signInWithGoogleOption = com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption.Builder(BuildConfig.WEB_CLIENT_ID)
                    .setNonce(hashedNonce)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .addCredentialOption(signInWithGoogleOption)
                    .build()

                val result = credentialManager.getCredential(context, request)
                val credential = result.credential

                if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val idToken = googleIdTokenCredential.idToken
                    
                    val loginRequest = GoogleLoginRequest(
                        googleToken = idToken,
                        clientType = "mobile",
                        androidId = getAndroidId()
                    )
                    
                    val response = apiService.googleLogin(loginRequest)
                    handleAuthResponse(response)
                    
                } else {
                    _authState.value = AuthState.Error("Invalid credential type")
                }
            } catch (e: androidx.credentials.exceptions.NoCredentialException) {
                Log.e("Auth", "No credentials available", e)
                _authState.value = AuthState.Error("No Google account found on this device. Please add one in Android Settings > Passwords & Accounts.")
            } catch (e: androidx.credentials.exceptions.GetCredentialCancellationException) {
                _authState.value = AuthState.Idle
            } catch (e: Exception) {
                Log.e("Auth", "Google sign in failed", e)
                _authState.value = AuthState.Error("Sign in failed: ${e.message}")
            }
        }
    }

    fun verifyOtp(email: String, otp: String) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val verifyRequest = VerifyOtpRequest(
                    email = email,
                    code = otp,
                    clientType = "mobile",
                    androidId = getAndroidId()
                )
                val response = apiService.verifyOtp(verifyRequest)
                handleAuthResponse(response)
            } catch (e: Exception) {
                _authState.value = AuthState.Error("OTP Verification failed: ${e.message}")
            }
        }
    }
    
    private suspend fun handleAuthResponse(response: retrofit2.Response<com.bammm.scas_app.data.model.AuthResponse>) {
        if (response.isSuccessful) {
            val body = response.body()
            if (body != null) {
                if (body.requireOtp == true) {
                    _authState.value = AuthState.RequireOtp(body.email ?: "")
                } else {
                    val token = body.token ?: ""
                    val user = body.user
                    if (user != null) {
                        userPreferences.saveAuthData(
                            token = token,
                            name = user.name,
                            email = user.email,
                            role = user.role
                        )
                        _authState.value = AuthState.Success
                    } else {
                        _authState.value = AuthState.Error("Invalid user data from server")
                    }
                }
            } else {
                _authState.value = AuthState.Error("Empty response body")
            }
        } else {
            val errorBody = response.errorBody()?.string()
            _authState.value = AuthState.Error("Server error: ${response.code()} $errorBody")
        }
    }
    
    fun resetState() {
        _authState.value = AuthState.Idle
    }
    
    fun logout() {
        viewModelScope.launch {
            try {
                apiService.logout()
            } catch (e: Exception) {
            }
            userPreferences.clearAuthData()
        }
    }
}

class AuthViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            val prefs = UserPreferences(context.applicationContext)
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(prefs, context.applicationContext) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
