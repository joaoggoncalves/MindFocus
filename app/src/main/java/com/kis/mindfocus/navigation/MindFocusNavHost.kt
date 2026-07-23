package com.kis.mindfocus.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.kis.mindfocus.feature.session.FocusSessionScreen
import com.kis.mindfocus.feature.sessiondetail.SessionDetailScreen
import kotlinx.serialization.Serializable

@Serializable
object SessionListRoute

@Serializable
data class SessionDetailRoute(val sessionId: String)

@Composable
fun MindFocusNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = SessionListRoute,
        modifier = modifier,
    ) {
        composable<SessionListRoute> {
            FocusSessionScreen(
                onNavigateToSession = { sessionId ->
                    navController.navigate(SessionDetailRoute(sessionId))
                },
            )
        }

        composable<SessionDetailRoute> { backStackEntry ->
            SessionDetailScreen(
                sessionId = backStackEntry.toRoute<SessionDetailRoute>().sessionId,
                onNavigateBack = navController::popBackStack,
            )
        }
    }
}
