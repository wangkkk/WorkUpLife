package com.workuplife

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.workuplife.data.PreferenceStore
import com.workuplife.ui.DashboardScreen
import com.workuplife.ui.MainUiState
import com.workuplife.ui.MainViewModel
import com.workuplife.ui.SettingsScreen
import com.workuplife.ui.theme.WorkUplifeTheme
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.compose.runtime.LaunchedEffect
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> /* 权限结果处理 */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Android 13+ 必须请求通知权限，否则前台服务会被禁止
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        val preferenceStore = PreferenceStore(applicationContext)
        val viewModelFactory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return MainViewModel(preferenceStore) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }

        setContent {
            WorkUplifeTheme {
                val navController = rememberNavController()
                val viewModel: MainViewModel = viewModel(factory = viewModelFactory)
                val uiState by viewModel.uiState.collectAsState()
                val scope = rememberCoroutineScope()

                // 核心修复：更稳定的重定向逻辑
                var hasCheckedConfig by remember { mutableStateOf(false) }
                
                LaunchedEffect(uiState) {
                    val state = uiState
                    if (!hasCheckedConfig && state is MainUiState.Success) {
                        hasCheckedConfig = true
                        if (state.config.monthlySalary <= 0) {
                            navController.navigate("settings") {
                                // 第一次进入时，将 dashboard 从栈中移除，确保设置页是唯一的根
                                popUpTo("dashboard") { inclusive = true }
                            }
                        }
                    }
                }

                NavHost(navController = navController, startDestination = "dashboard") {
                    composable("dashboard") {
                        when (val state = uiState) {
                            is MainUiState.Loading -> { /* Show loading */ }
                            is MainUiState.Success -> {
                                DashboardScreen(
                                    uiState = state,
                                    onNavigateToSettings = { navController.navigate("settings") }
                                )
                            }
                        }
                    }
                    composable("settings") {
                        when (val state = uiState) {
                            is MainUiState.Success -> {
                                SettingsScreen(
                                    currentConfig = state.config,
                                    showBack = state.config.monthlySalary > 0, // 第一次进入时不显示返回
                                    onSave = { newConfig ->
                                        scope.launch {
                                            viewModel.updateConfig(newConfig)
                                            // 保存成功后，尝试返回。如果栈空（第一次进入的情况），则开启 dashboard
                                            if (!navController.popBackStack()) {
                                                navController.navigate("dashboard") {
                                                    popUpTo("settings") { inclusive = true }
                                                }
                                            }
                                        }
                                    },
                                    onBack = { 
                                        // 只有在薪资已设置的情况下才允许返回
                                        if (state.config.monthlySalary > 0) {
                                            navController.popBackStack()
                                        }
                                    }
                                )
                            }
                            else -> { /* Handle loading */ }
                        }
                    }
                }
            }
        }
    }
}
