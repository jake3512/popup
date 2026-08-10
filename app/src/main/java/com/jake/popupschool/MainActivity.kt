package com.jake.popupschool

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.jake.popupschool.ui.dday.DdayScreen
import com.jake.popupschool.ui.home.HomeScreen
import com.jake.popupschool.ui.settings.SettingsScreen
import com.jake.popupschool.ui.theme.PopupSchoolTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PopupSchoolTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    PopupSchoolApp()
                }
            }
        }
    }
}

@Composable
private fun PopupSchoolApp() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "home") {
        composable("home") { HomeScreen(navController) }
        composable("settings") { SettingsScreen(onBack = { navController.popBackStack() }) }
        composable("dday") { DdayScreen(onBack = { navController.popBackStack() }) }
    }
}
