package com.mogralabs.mogra

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.mogralabs.mogra.ui.MograApp
import com.mogralabs.mogra.ui.theme.MograTheme

class MainActivity : ComponentActivity() {

    /** Every resource lookup in the Activity goes through the chosen language. */
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(Language.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // The app is dark in every state, so the bars are told so outright rather than
        // left to follow the system theme.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        setContent {
            MograTheme { MograApp() }
        }
    }
}
