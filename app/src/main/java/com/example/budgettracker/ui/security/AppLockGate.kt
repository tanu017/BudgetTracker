package com.example.budgettracker.ui.security

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity

/**
 * A security gate that protects the app content with Biometric authentication.
 */
@Composable
fun AppLockGate(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    
    var isUnlocked by rememberSaveable { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }

    // Use a trigger flag to avoid multiple prompts
    var triggerAuth by remember { mutableStateOf(false) }

    val biometricHelper = remember(activity) { 
        activity?.let { BiometricHelper(it) } 
    }

    fun launchAuth() {
        if (biometricHelper == null) {
            authError = "Security Error: FragmentActivity not found."
            return
        }
        authError = null
        biometricHelper.authenticate(
            onSuccess = { isUnlocked = true },
            onError = { error -> authError = error },
            onFailed = { authError = "Authentication failed" }
        )
    }

    // Auto-launch once on start
    LaunchedEffect(activity) {
        if (!isUnlocked && !triggerAuth && activity != null) {
            triggerAuth = true
            launchAuth()
        }
    }

    if (isUnlocked) {
        content()
    } else {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "Your data is protected",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = "Please authenticate to continue",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 8.dp)
                )

                if (authError != null) {
                    Text(
                        text = authError!!,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(onClick = { launchAuth() }) {
                    Text("Unlock App")
                }
            }
        }
    }
}
