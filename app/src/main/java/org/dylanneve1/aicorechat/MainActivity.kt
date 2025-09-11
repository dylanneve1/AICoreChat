package org.dylanneve1.aicorechat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.*
import org.dylanneve1.aicorechat.data.ChatViewModel
import org.dylanneve1.aicorechat.ui.chat.ChatScreen
import org.dylanneve1.aicorechat.ui.chat.OnboardingScreen
import org.dylanneve1.aicorechat.ui.chat.UnsupportedDeviceScreen
import org.dylanneve1.aicorechat.ui.theme.AICoreChatTheme
import org.dylanneve1.aicorechat.util.DeviceSupportStatus
import org.dylanneve1.aicorechat.util.checkDeviceSupport

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Make navigation bar transparent
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            AICoreChatTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var supportStatus by remember { mutableStateOf<DeviceSupportStatus?>(null) }

                    LaunchedEffect(Unit) {
                        supportStatus = checkDeviceSupport(applicationContext)
                    }

                    when (val status = supportStatus) {
                        null -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) { CircularProgressIndicator() }
                        }
                        is DeviceSupportStatus.Supported -> {
                            val chatViewModel: ChatViewModel = viewModel()

                            var onboardingShown by remember {
                                mutableStateOf(
                                    getSharedPreferences("AICoreChatPrefs", MODE_PRIVATE)
                                        .getBoolean("onboarding_shown", false)
                                )
                            }

                            if (!onboardingShown) {
                                OnboardingScreen(
                                    initialName = chatViewModel.uiState.collectAsState().value.userName,
                                    initialPersonalContextEnabled = chatViewModel.uiState.collectAsState().value.personalContextEnabled,
                                    onComplete = { name, enabled ->
                                        chatViewModel.updateUserName(name)
                                        chatViewModel.updatePersonalContextEnabled(enabled)
                                        getSharedPreferences("AICoreChatPrefs", MODE_PRIVATE)
                                            .edit().putBoolean("onboarding_shown", true).apply()
                                        onboardingShown = true
                                    }
                                )
                            } else {
                                ChatScreen(viewModel = chatViewModel)
                            }
                        }
                        is DeviceSupportStatus.AICoreMissing -> {
                            UnsupportedDeviceScreen(
                                message = "AICore app is not installed."
                            )
                        }
                        is DeviceSupportStatus.NotReady -> {
                            UnsupportedDeviceScreen(
                                message = status.reason
                            )
                        }
                    }
                }
            }
        }
    }
}
