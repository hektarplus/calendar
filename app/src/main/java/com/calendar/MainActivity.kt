package com.calendar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.calendar.data.WorkDayRepository
import com.calendar.ui.navigation.AppNavigation
import com.calendar.ui.theme.CalendarTheme
import com.calendar.ui.viewmodel.CalendarViewModel
import com.calendar.ui.viewmodel.CalendarViewModelFactory
import com.jakewharton.threetenabp.AndroidThreeTen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AndroidThreeTen.init(this)
        enableEdgeToEdge()
        
        val repository = WorkDayRepository(applicationContext)
        val viewModelFactory = CalendarViewModelFactory(repository)
        
        setContent {
            CalendarTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val viewModel: CalendarViewModel = viewModel(factory = viewModelFactory)
                    
                    AppNavigation(
                        navController = navController,
                        viewModel = viewModel,
                        onExit = { finish() }
                    )
                }
            }
        }
    }
}