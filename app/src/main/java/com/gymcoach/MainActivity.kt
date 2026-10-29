package com.gymcoach

import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import com.gymcoach.ui.GymCoachRoot
import com.gymcoach.ui.LocalRepository
import com.gymcoach.ui.theme.GymCoachTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The app is always dark, so keep the system bar icons light.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        val repository = (application as GymCoachApp).repository
        setContent {
            GymCoachTheme {
                CompositionLocalProvider(LocalRepository provides repository) {
                    GymCoachRoot()
                }
            }
        }
    }
}
