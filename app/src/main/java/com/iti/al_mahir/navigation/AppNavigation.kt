package com.iti.al_mahir.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.mushaf.presentation.MushafScreen
import com.iti.presentation.auth.navigation.AuthGraph
import com.iti.presentation.auth.navigation.authGraph
import kotlinx.serialization.Serializable

@Serializable
data object MushafRoute

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = AuthGraph,
        modifier = modifier
    ) {
        authGraph(
            navController = navController,
            onNavigateToHome = {
                navController.navigate(MushafRoute) {
                    popUpTo(AuthGraph) { inclusive = true }
                }
            },
            onShowMessage = { /* TODO: Show snackbar or toast */ }
        )

        composable<MushafRoute> {
            MushafScreen()
        }
    }
}
