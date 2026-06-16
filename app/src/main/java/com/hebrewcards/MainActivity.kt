package com.hebrewcards

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.hebrewcards.ui.navigation.HebrewCardsNavGraph
import com.hebrewcards.ui.theme.HebrewCardsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HebrewCardsTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    HebrewCardsNavGraph()
                }
            }
        }
    }
}
