package com.nostrange.app

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.nostrange.app.ui.navigation.MainAppNavigation
import com.nostrange.app.ui.theme.DarkBackground
import com.nostrange.app.ui.theme.NostrangeTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleIncomingSharedIntent(intent)

        setContent {
            NostrangeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkBackground
                ) {
                    MainAppNavigation()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingSharedIntent(intent)
    }

    /**
     * Level 2 AI Integration: Receives shared text/JSON directly from external AI apps
     * (ChatGPT, Gemini, Grok, etc.) via Android Intent.ACTION_SEND
     */
    private fun handleIncomingSharedIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (!sharedText.isNullOrBlank()) {
                val app = application as NostrangeApp
                lifecycleScope.launch {
                    // Try importing as Profile JSON or Matching Result JSON
                    val profileRes = app.profileRepository.importProfileJson(sharedText)
                    if (profileRes.isSuccess) {
                        Toast.makeText(
                            this@MainActivity,
                            "پروفایل هوش مصنوعی دریافت و ذخیره شد!",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        val matchRes = app.candidateRepository.importAiMatchingResult(sharedText)
                        if (matchRes.isSuccess) {
                            Toast.makeText(
                                this@MainActivity,
                                "رتبه‌بندی ۲۰ مورد برتر AI دریافت شد!",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }
        }
    }
}
