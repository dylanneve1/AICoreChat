package org.dylanneve1.aicorechat.ui.chat

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.dylanneve1.aicorechat.data.MemoryEntry
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun navigation_toPersonalization_showsPersonalizationContent() {
        composeRule.setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                SettingsScreen(
                    temperature = 0.4f,
                    topK = 40,
                    onTemperatureChange = {},
                    onTopKChange = {},
                    onResetModelSettings = {},
                    userName = "Alex",
                    personalContextEnabled = true,
                    onUserNameChange = {},
                    onPersonalContextToggle = {},
                    webSearchEnabled = true,
                    onWebSearchToggle = {},
                    multimodalEnabled = true,
                    onMultimodalToggle = {},
                    onWipeAllChats = {},
                    onDismiss = {},
                    memoryEntries = listOf(MemoryEntry(content = "Enjoys hiking")),
                )
            }
        }

        composeRule.onNodeWithText("Settings").assertIsDisplayed()
        composeRule.onNodeWithText("Personalization").performClick()

        composeRule.onNodeWithText("Customize how the AI understands and responds to you").assertIsDisplayed()
    }

    @Test
    fun wipeAllChats_confirmationCallsCallback() {
        var wipeInvoked = false

        composeRule.setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                SettingsScreen(
                    temperature = 0.5f,
                    topK = 32,
                    onTemperatureChange = {},
                    onTopKChange = {},
                    onResetModelSettings = {},
                    userName = "",
                    personalContextEnabled = false,
                    onUserNameChange = {},
                    onPersonalContextToggle = {},
                    webSearchEnabled = false,
                    onWebSearchToggle = {},
                    multimodalEnabled = false,
                    onMultimodalToggle = {},
                    onWipeAllChats = { wipeInvoked = true },
                    onDismiss = {},
                )
            }
        }

        composeRule.onNodeWithText("Wipe all chats").performClick()
        composeRule.onNodeWithText("Wipe all chats?").assertIsDisplayed()
        composeRule.onNodeWithText("Wipe").performClick()

        composeRule.runOnIdle {
            assertTrue(wipeInvoked)
        }
    }
}
