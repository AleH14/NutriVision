package com.example.nutrivision.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nutrivision.data.model.AuthResponse
import com.example.nutrivision.data.model.LoginRequest
import com.example.nutrivision.data.model.RegisterRequest
import com.example.nutrivision.data.repository.NutriRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class AuthViewModel(private val repository: NutriRepository) : ViewModel() {

    private val _authResult = MutableLiveData<Result<AuthResponse>>()
    val authResult: LiveData<Result<AuthResponse>> = _authResult

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun login(request: LoginRequest) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = repository.login(request)
                if (response.isSuccessful && response.body() != null) {
                    _authResult.value = Result.success(response.body()!!)
                } else {
                    _authResult.value = Result.failure(Exception("Error en login: ${response.code()}"))
                }
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    _authResult.value = Result.failure(e)
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun login(email: String, password: String) {
        login(LoginRequest(email, password))
    }

    fun register(request: RegisterRequest) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = repository.register(request)
                if (response.isSuccessful && response.body() != null) {
                    _authResult.value = Result.success(response.body()!!)
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Error en registro: ${response.code()}"
                    _authResult.value = Result.failure(Exception(errorMsg))
                }
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    _authResult.value = Result.failure(e)
                }
            } finally {
                _isLoading.value = false
            }
        }
    }
}
