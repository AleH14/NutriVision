package com.example.nutrivision.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nutrivision.data.model.AuthResponse
import com.example.nutrivision.data.model.LoginRequest
import com.example.nutrivision.data.model.User
import com.example.nutrivision.data.repository.NutriRepository
import kotlinx.coroutines.launch

class AuthViewModel(private val repository: NutriRepository) : ViewModel() {

    private val _authResult = MutableLiveData<Result<AuthResponse>>()
    val authResult: LiveData<Result<AuthResponse>> = _authResult

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = repository.login(LoginRequest(email, password))
                if (response.isSuccessful && response.body() != null) {
                    _authResult.value = Result.success(response.body()!!)
                } else {
                    _authResult.value = Result.failure(Exception("Error en login: ${response.code()}"))
                }
            } catch (e: Exception) {
                _authResult.value = Result.failure(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun register(user: User) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = repository.register(user)
                if (response.isSuccessful && response.body() != null) {
                    _authResult.value = Result.success(response.body()!!)
                } else {
                    _authResult.value = Result.failure(Exception("Error en registro: ${response.code()}"))
                }
            } catch (e: Exception) {
                _authResult.value = Result.failure(e)
            } finally {
                _isLoading.value = false
            }
        }
    }
}
