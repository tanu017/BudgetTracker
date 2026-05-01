package com.example.budgettracker.viewmodel

import android.app.Application
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgettracker.repository.AuthRepository
import com.example.budgettracker.utils.UserPreferences
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AuthRepository = AuthRepository()
    private val userPreferences = UserPreferences(application)

    private val _user = mutableStateOf<FirebaseUser?>(repository.getCurrentUser())
    val user: State<FirebaseUser?> = _user

    private val _displayName = mutableStateOf<String?>(repository.getCurrentUser()?.displayName)
    val displayName: State<String?> = _displayName

    val themeMode: StateFlow<String> = userPreferences.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "light")

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
                _displayName.value = it.displayName
                onSuccess()
            }.onFailure {
                _errorMessage.value = it.message
            }
        }
    }

    fun setTheme(mode: String) {
        viewModelScope.launch {
            userPreferences.saveThemeMode(mode)
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
                _displayName.value = it.displayName
                onSuccess()
            }.onFailure {
                _errorMessage.value = it.message
            }
        }
    }

    fun updateUserName(newName: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val result = repository.updateProfile(newName)
            _isLoading.value = false
            result.onSuccess {
                val currentUser = repository.getCurrentUser()
                _user.value = currentUser
                _displayName.value = currentUser?.displayName
            }.onFailure {
                _errorMessage.value = it.message
            }
        }
    }

    fun logout(onSuccess: () -> Unit) {
        repository.logout()
        _user.value = null
        _displayName.value = null
        onSuccess()
    }
    
    fun clearError() {
        _errorMessage.value = null
    }
}
