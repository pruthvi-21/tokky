package com.ps.tokky.navigation

import android.net.Uri
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.ps.tokky.ui.screens.AuthenticationScreen
import com.ps.tokky.ui.screens.ExportTokensScreen
import com.ps.tokky.ui.screens.HomeScreen
import com.ps.tokky.ui.screens.ImportTokensScreen
import com.ps.tokky.ui.screens.SettingsScreen
import com.ps.tokky.ui.screens.TokenSetupScreen
import com.ps.tokky.ui.viewmodels.SettingsViewModel

fun NavGraphBuilder.addAuthRoute(
    navController: NavController,
) {
    composable(Routes.Auth.base) {
        AuthenticationScreen(
            navController = navController,
        )
    }
}

fun NavGraphBuilder.addHomeRoute(
    navController: NavController,
) {
    composable(Routes.Home.base) {
        HomeScreen(
            navController = navController,
        )
    }
}

fun NavGraphBuilder.addTokenSetupRoute(
    navController: NavController,
) {
    composable(
        route = "${Routes.TokenSetup.base}?token_id={token_id}&auth_url={auth_url}",
        arguments = listOf(
            navArgument("token_id") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            },
            navArgument("auth_url") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            }
        )
    ) { navBackStackEntry ->
        val tokenId = navBackStackEntry.arguments?.getString("token_id")
        val authUrl = navBackStackEntry.arguments?.getString("auth_url")?.let { Uri.decode(it) }
        TokenSetupScreen(
            tokenId = tokenId,
            authUrl = authUrl,
            navController = navController
        )
    }
}

fun NavGraphBuilder.addSettingsRoute(
    settingsViewModel: SettingsViewModel,
    navController: NavController,
) {
    composable(Routes.Settings.base) {
        SettingsScreen(
            settingsViewModel = settingsViewModel,
            navController = navController,
        )
    }
}

fun NavGraphBuilder.addExportTokensRoute() {
    composable(Routes.ExportTokens.base) {
        ExportTokensScreen()
    }
}

fun NavGraphBuilder.addImportTokensRoute(
    navController: NavController,
) {
    composable(
        route = "${Routes.ImportTokens.base}?file_uri={file_uri}",
        arguments = listOf(
            navArgument("file_uri") {
                type = NavType.StringType
            }
        )
    ) { navBackStackEntry ->
        val fileUri = navBackStackEntry.arguments?.getString("file_uri")?.let { Uri.parse(it) }
        ImportTokensScreen(fileUri!!, navController)
    }
}
