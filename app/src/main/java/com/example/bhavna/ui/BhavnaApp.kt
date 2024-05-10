package com.example.bhavna.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

enum class BhavnaScreens {
    ResultsList,
    Result
}

@Composable
fun BhavnaApp(navController: NavHostController = rememberNavController()) {
    NavHost(
        navController = navController,
        startDestination = BhavnaScreens.ResultsList.name,
        modifier = Modifier.fillMaxSize()
    ) {
        composable(BhavnaScreens.ResultsList.name) {
            ResultsListScreen(navigateToResult = {
                navController.navigate("${BhavnaScreens.Result.name}/$it")
            })
        }
        composable("${BhavnaScreens.Result.name}/{dirName}") {
            ResultScreen(it.arguments?.getString("dirName")!!)
        }
    }
}