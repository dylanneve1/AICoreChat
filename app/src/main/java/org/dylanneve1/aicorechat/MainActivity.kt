package org.dylanneve1.aicorechat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
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
                    color = MaterialTheme.colorScheme.background,
                ) {
                    var supportStatus by remember { mutableStateOf<DeviceSupportStatus?>(null) }

                    LaunchedEffect(Unit) {
                        supportStatus = checkDeviceSupport(applicationContext)
                    }

                    val chatViewModel: ChatViewModel = viewModel()
                    val uiState by chatViewModel.uiState.collectAsState()

                    var onboardingShown by remember {
                        mutableStateOf(
                            getSharedPreferences("AICoreChatPrefs", MODE_PRIVATE)
                                .getBoolean("onboarding_shown", false),
                        )
                    }

                    val appScreen = when (val status = supportStatus) {
                        null -> MainScreen.Loading
                        is DeviceSupportStatus.Supported -> if (!onboardingShown) {
                            MainScreen.Onboarding
                        } else {
                            MainScreen.Chat
                        }
                        is DeviceSupportStatus.AICoreMissing -> MainScreen.Unsupported(
                            message = "AICore app is not installed.",
                        )
                        is DeviceSupportStatus.NotReady -> MainScreen.Unsupported(
                            message = status.reason ?: "Device is not ready yet.",
                        )
                    }

                    @OptIn(ExperimentalAnimationApi::class)
                    AnimatedContent(
                        targetState = appScreen,
                        transitionSpec = {
                            val isOnboardingToChat = initialState === MainScreen.Onboarding && targetState === MainScreen.Chat
                            val isChatToOnboarding = initialState === MainScreen.Chat && targetState === MainScreen.Onboarding

                            val transition = if (isOnboardingToChat || isChatToOnboarding) {
                                val direction = if (isOnboardingToChat) 1 else -1
                                (
                                    slideInHorizontally(
                                        animationSpec = tween(durationMillis = 420),
                                        initialOffsetX = { fullWidth -> direction * fullWidth },
                                    ) + fadeIn(animationSpec = tween(durationMillis = 240))
                                    ) togetherWith (
                                    slideOutHorizontally(
                                        animationSpec = tween(durationMillis = 420),
                                        targetOffsetX = { fullWidth -> -direction * fullWidth },
                                    ) + fadeOut(animationSpec = tween(durationMillis = 200))
                                    )
                            } else {
                                fadeIn(animationSpec = tween(durationMillis = 220)) togetherWith
                                    fadeOut(animationSpec = tween(durationMillis = 160))
                            }

                            transition.using(SizeTransform(clip = false))
                        },
                        label = "MainScreenTransition",
                    ) { screen ->
                        when (screen) {
                            MainScreen.Loading -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center,
                                ) { CircularProgressIndicator() }
                            }

                            MainScreen.Onboarding -> {
                                OnboardingScreen(
                                    initialName = uiState.userName,
                                    initialPersonalContextEnabled = uiState.personalContextEnabled,
                                    onComplete = {
                                            name, personalEnabled, webSearchEnabled, multimodalEnabled,
                                            memoryContextEnabled, bioContextEnabled, bioName, bioAge,
                                            bioOccupation, bioLocation, customInstructions, customInstructionsEnabled,
                                        ->
                                        chatViewModel.updateUserName(name)
                                        chatViewModel.updatePersonalContextEnabled(personalEnabled)
                                        chatViewModel.updateWebSearchEnabled(webSearchEnabled)
                                        chatViewModel.updateMultimodalEnabled(multimodalEnabled)
                                        chatViewModel.updateMemoryContextEnabled(memoryContextEnabled)
                                        chatViewModel.updateBioContextEnabled(bioContextEnabled)
                                        chatViewModel.updateBioInformation(bioName, bioAge, bioOccupation, bioLocation)
                                        chatViewModel.updateCustomInstructions(
                                            customInstructions,
                                            customInstructionsEnabled,
                                        )
                                        getSharedPreferences("AICoreChatPrefs", MODE_PRIVATE)
                                            .edit().putBoolean("onboarding_shown", true).apply()
                                        onboardingShown = true
                                    },
                                )
                            }

                            MainScreen.Chat -> {
                                ChatScreen(viewModel = chatViewModel)
                            }

                            is MainScreen.Unsupported -> {
                                UnsupportedDeviceScreen(
                                    message = screen.message,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private sealed class MainScreen {
    data object Loading : MainScreen()
    data object Onboarding : MainScreen()
    data object Chat : MainScreen()
    data class Unsupported(val message: String) : MainScreen()
}
