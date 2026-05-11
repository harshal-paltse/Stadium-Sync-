package com.stadiumsync.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.stadiumsync.app.core.datastore.UserPreferences
import com.stadiumsync.app.presentation.navigation.StadiumSyncNavHost
import com.stadiumsync.app.presentation.theme.StadiumSyncTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var userPreferences: UserPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val isDarkMode by userPreferences.isDarkMode.collectAsState(initial = false)
            StadiumSyncTheme(darkTheme = isDarkMode) {
                StadiumSyncNavHost()
            }
        }
    }
}
