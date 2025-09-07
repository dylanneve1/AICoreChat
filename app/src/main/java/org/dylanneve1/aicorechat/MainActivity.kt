package org.dylanneve1.aicorechat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import org.dylanneve1.aicorechat.data.ChatViewModel
import org.dylanneve1.aicorechat.ui.chat.ChatScreen
import org.dylanneve1.aicorechat.ui.theme.AICoreChatTheme

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
                    val chatViewModel: ChatViewModel = viewModel()
                    ChatScreen(viewModel = chatViewModel)
                }
            }
        }
    }
}
