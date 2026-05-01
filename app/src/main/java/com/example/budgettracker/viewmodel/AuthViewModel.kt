package com.example.budgettracker.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgettracker.repository.AuthRepository
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch

class AuthViewModel(private val repository: AuthRepository = AuthRepository()) : ViewModel() {

    private val _user = mutableStateOf<FirebaseUser?>(repository.getCurrentUser())
    val user: State<FirebaseUser?> = _user

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _errorMessage = mutableStateOf<String?>(null)
    val errorMessage: State<String?> = _errorMessage

    fun login(email: String, password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val result = repository.login(email, password)
            _isLoading.value = false
            result.onSuccess {
                _user.value = it
                onSuccess()
            }.onFailure {
                _errorMessage.value = it.message
            }
        }
    }

    fun register(email: String, password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val result = repository.register(email, password)
            _isLoading.value = false
            result.onSuccess {
                _user.value = it
                onSuccess()
            }.onFailure {
                _errorMessage.value = it.message
            }
        }
    }

    fun logout(onSuccess: () -> Unit) {
        repository.logout()
        _user.value = null
        onSuccess()
    }
    
    fun clearError() {
        _errorMessage.value = null
    }
}
