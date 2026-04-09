package com.dkshe.audiobookplayer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import com.dkshe.audiobookplayer.app.AudiobookApp
import com.dkshe.audiobookplayer.ui.theme.AudiobookPlayerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AudiobookPlayerTheme {
                Surface {
                    AudiobookApp()
                }
            }
        }
    }
}

