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

import kotlinx.coroutines.flow.first

import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class RequireOtp(val email: String) : AuthState()
    data class Success(val role: String?) : AuthState()
    data class Error(val message: String) : AuthState()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val userPreferences: UserPreferences,
    private val apiService: com.bammm.scas_app.data.api.ApiService,
    @ApplicationContext private val context: Context
) : ViewModel() {

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

    fun assignRole(role: String, onRoleAssigned: () -> Unit) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val token = userPreferences.authToken.first() ?: ""
                val response = apiService.assignRole(com.bammm.scas_app.data.model.AssignRoleRequest(role))
                if (response.isSuccessful && response.body()?.status == "success") {
                    val updatedUser = response.body()?.user
                    if (updatedUser != null) {
                        userPreferences.saveAuthData(
                            token = token,
                            name = updatedUser.name,
                            email = updatedUser.email,
                            role = updatedUser.role ?: ""
                        )
                        _authState.value = AuthState.Success(updatedUser.role)
                        onRoleAssigned()
                    } else {
                        _authState.value = AuthState.Error("Invalid user data from server")
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Unknown error"
                    _authState.value = AuthState.Error("Failed to assign role: $errorBody")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Role assignment failed: ${e.message}")
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
                            role = user.role ?: ""
                        )
                        _authState.value = AuthState.Success(user.role)
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
            userPreferences.clearData()
        }
    }
}
