package org.dylanneve1.aicorechat.ui.chat

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.dylanneve1.aicorechat.ui.chat.tools.ToolToggleTags
import org.dylanneve1.aicorechat.ui.chat.tools.ToolsSheet
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ToolsSheetTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun togglesUpdateState() {
        var webSearchEnabled = false
        var personalContextEnabled = false
        var imageAnalysisEnabled = false

        composeRule.setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                var web by remember { mutableStateOf(webSearchEnabled) }
                var personal by remember { mutableStateOf(personalContextEnabled) }
                var image by remember { mutableStateOf(imageAnalysisEnabled) }

                ToolsSheet(
                    webSearchEnabled = web,
                    onWebSearchToggle = { web = it },
                    personalContextEnabled = personal,
                    onPersonalContextToggle = { personal = it },
                    multimodalEnabled = image,
                    onMultimodalToggle = { image = it },
                    onDismiss = {},
                )

                webSearchEnabled = web
                personalContextEnabled = personal
                imageAnalysisEnabled = image
            }
        }

        composeRule.onNodeWithTag(ToolToggleTags.WEB_SEARCH).performClick()
        composeRule.onNodeWithTag(ToolToggleTags.WEB_SEARCH).assertIsOn()

        composeRule.onNodeWithTag(ToolToggleTags.PERSONAL_CONTEXT).performClick()
        composeRule.onNodeWithTag(ToolToggleTags.PERSONAL_CONTEXT).assertIsOn()

        composeRule.onNodeWithTag(ToolToggleTags.IMAGE_ANALYSIS).performClick()
        composeRule.onNodeWithTag(ToolToggleTags.IMAGE_ANALYSIS).assertIsOn()

        composeRule.runOnIdle {
            assertTrue(webSearchEnabled)
            assertTrue(personalContextEnabled)
            assertTrue(imageAnalysisEnabled)
        }
    }
}
