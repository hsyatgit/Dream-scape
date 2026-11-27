package com.hsyatgit.dreamscape

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.hsyatgit.dreamscape.ui.DreamscapeTheme
import com.hsyatgit.dreamscape.ui.DreamscapeNav

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DreamscapeTheme {
                DreamscapeNav()
            }
        }
    }
}
