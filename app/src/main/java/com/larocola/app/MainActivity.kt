package com.larocola.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.mutableStateOf
import com.larocola.app.core.ui.theme.LaRocolaTheme
import com.larocola.app.ui.navigation.AppNavGraph

private val SLUG_REGEX = Regex("^[a-z0-9-]{2,40}$")

class MainActivity : ComponentActivity() {

    private val slugState = mutableStateOf<String?>(null)

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        slugState.value = extraerSlug(intent)
        val app = application as LaRocolaApp
        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            LaRocolaTheme {
                AppNavGraph(
                    container = app.container,
                    slugInicial = slugState.value,
                    windowSizeClass = windowSizeClass,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extraerSlug(intent)?.let { slugState.value = it }
    }

    private fun extraerSlug(intent: Intent?): String? {
        val data = intent?.data ?: return null
        val slug = when (data.scheme) {
            "https" -> data.lastPathSegment
            "larocola" -> data.lastPathSegment
            else -> null
        }
        return slug?.takeIf { SLUG_REGEX.matches(it) }
    }
}
