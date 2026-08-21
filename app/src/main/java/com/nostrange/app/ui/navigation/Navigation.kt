package com.nostrange.app.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.nostrange.app.ui.chats.ChatDetailScreen
import com.nostrange.app.ui.chats.ChatsScreen
import com.nostrange.app.ui.matches.MatchesScreen
import com.nostrange.app.ui.me.MeScreen
import com.nostrange.app.ui.settings.SettingsScreen
import com.nostrange.app.ui.theme.DarkBackground
import com.nostrange.app.ui.theme.DarkSurface
import com.nostrange.app.ui.theme.DarkSurfaceVariant
import com.nostrange.app.ui.theme.PrimaryPurple
import com.nostrange.app.ui.theme.TextMuted
import com.nostrange.app.ui.theme.TextPrimary

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    object Me : Screen("me", "Me", Icons.Default.Person)
    object Matches : Screen("matches", "Matches", Icons.Default.Favorite)
    object Chats : Screen("chats", "Chats", Icons.Default.Chat)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    object ChatDetail : Screen("chat_detail/{pubkey}", "Chat") {
        fun createRoute(pubkey: String) = "chat_detail/$pubkey"
    }
}

val bottomNavItems = listOf(
    Screen.Me,
    Screen.Matches,
    Screen.Chats,
    Screen.Settings
)

@Composable
fun MainAppNavigation(
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val isBottomBarVisible = bottomNavItems.any { it.route == currentRoute }

    Scaffold(
        modifier = Modifier.fillMaxSize().background(DarkBackground),
        bottomBar = {
            if (isBottomBarVisible) {
                NavigationBar(
                    containerColor = DarkSurface,
                    tonalElevation = 4.dp
                ) {
                    bottomNavItems.forEach { screen ->
                        val selected = currentRoute == screen.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                screen.icon?.let {
                                    Icon(
                                        imageVector = it,
                                        contentDescription = screen.title,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            },
                            label = { Text(screen.title) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = PrimaryPurple,
                                selectedTextColor = PrimaryPurple,
                                indicatorColor = DarkSurfaceVariant,
                                unselectedIconColor = TextMuted,
                                unselectedTextColor = TextMuted
                            )
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Me.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screen.Me.route) {
                MeScreen(
                    onNavigateToMatches = { navController.navigate(Screen.Matches.route) }
                )
            }
            composable(Screen.Matches.route) {
                MatchesScreen(
                    onOpenChat = { pubkey ->
                        navController.navigate(Screen.ChatDetail.createRoute(pubkey))
                    }
                )
            }
            composable(Screen.Chats.route) {
                ChatsScreen(
                    onOpenChat = { pubkey ->
                        navController.navigate(Screen.ChatDetail.createRoute(pubkey))
                    }
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen()
            }
            composable(
                route = Screen.ChatDetail.route,
                arguments = listOf(navArgument("pubkey") { type = NavType.StringType })
            ) { backStackEntry ->
                val pubkey = backStackEntry.arguments?.getString("pubkey") ?: ""
                ChatDetailScreen(
                    partnerPubkey = pubkey,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
