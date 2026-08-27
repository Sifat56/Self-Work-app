package com.example.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.components.SelfWorkTopBar
import com.example.ui.screens.activework.ActiveWorkScreen
import com.example.ui.screens.admin.AdminPortalScreen
import com.example.ui.screens.auth.AuthScreen
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.notifications.NotificationsScreen
import com.example.ui.screens.profile.ProfileScreen
import com.example.ui.screens.referral.ReferralScreen
import com.example.ui.screens.tasks.TaskHistoryScreen
import com.example.ui.screens.tasks.TasksScreen
import com.example.ui.screens.wallet.WalletScreen
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.viewmodel.MainViewModel

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Tasks : Screen("tasks", "Tasks", Icons.Default.Work)
    object ActiveWork : Screen("active_work", "Active Work", Icons.Default.PlayArrow)
    object Wallet : Screen("wallet", "Wallet", Icons.Default.AccountBalanceWallet)
    object Referral : Screen("referral", "Referral", Icons.Default.People)
    object Profile : Screen("profile", "Profile", Icons.Default.Person)
    object Notifications : Screen("notifications", "Notifications")
    object TaskHistory : Screen("task_history", "Submissions")
    object Auth : Screen("auth", "Sign In")
    object AdminPortal : Screen("admin_portal", "Admin Portal")
}

@Composable
fun AppNavigation(
    viewModel: MainViewModel,
    navController: NavHostController = rememberNavController()
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val activeSession by viewModel.activeSession.collectAsState()
    val unreadNotificationsCount by viewModel.unreadNotificationsCount.collectAsState()
    val userMessage by viewModel.userMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(userMessage) {
        userMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomNavItems = listOf(
        Screen.Home,
        Screen.Tasks,
        Screen.Wallet,
        Screen.Referral,
        Screen.Profile
    )

    val showBottomBar = currentRoute in bottomNavItems.map { it.route } || currentRoute == Screen.ActiveWork.route
    val showTopBar = currentRoute in listOf(
        Screen.Home.route,
        Screen.Tasks.route,
        Screen.Wallet.route,
        Screen.Referral.route,
        Screen.Profile.route
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (showTopBar) {
                SelfWorkTopBar(
                    title = when (currentRoute) {
                        Screen.Home.route -> "Self Work"
                        Screen.Tasks.route -> "Work Tasks"
                        Screen.Wallet.route -> "Wallet & Earnings"
                        Screen.Referral.route -> "Invite & Earn"
                        Screen.Profile.route -> "Worker Profile"
                        else -> "Self Work"
                    },
                    unreadNotifications = unreadNotificationsCount,
                    onNotificationClick = {
                        navController.navigate(Screen.Notifications.route)
                    },
                    showAdminBadge = currentUser?.role == "ADMIN",
                    onAdminClick = {
                        navController.navigate(Screen.AdminPortal.route)
                    }
                )
            }
        },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    bottomNavItems.forEach { screen ->
                        val selected = currentRoute == screen.route
                        val hasActiveWork = screen == Screen.Tasks && activeSession != null &&
                                (activeSession?.status == "RUNNING" || activeSession?.status == "PAUSED")

                        NavigationBarItem(
                            icon = {
                                BadgedBox(
                                    badge = {
                                        if (hasActiveWork) {
                                            Badge(
                                                containerColor = EmeraldPrimary,
                                                contentColor = Color.Black
                                            ) {
                                                Text("1")
                                            }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = screen.icon ?: Icons.Default.Home,
                                        contentDescription = screen.title
                                    )
                                }
                            },
                            label = {
                                Text(
                                    text = screen.title,
                                    fontSize = 11.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            selected = selected,
                            onClick = {
                                if (currentRoute != screen.route) {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = EmeraldPrimary,
                                selectedTextColor = EmeraldPrimary,
                                indicatorColor = EmeraldPrimary.copy(alpha = 0.15f)
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel = viewModel,
                    onNavigateToTasks = { navController.navigate(Screen.Tasks.route) },
                    onNavigateToWallet = { navController.navigate(Screen.Wallet.route) },
                    onNavigateToActiveWork = { navController.navigate(Screen.ActiveWork.route) },
                    onNavigateToTaskDetails = {
                        navController.navigate(Screen.Tasks.route)
                    }
                )
            }

            composable(Screen.Tasks.route) {
                TasksScreen(
                    viewModel = viewModel,
                    onNavigateToActiveWork = { navController.navigate(Screen.ActiveWork.route) },
                    onNavigateToHistory = { navController.navigate(Screen.TaskHistory.route) }
                )
            }

            composable(Screen.ActiveWork.route) {
                ActiveWorkScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onSubmissionSuccess = {
                        navController.navigate(Screen.TaskHistory.route) {
                            popUpTo(Screen.Tasks.route)
                        }
                    }
                )
            }

            composable(Screen.TaskHistory.route) {
                TaskHistoryScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Wallet.route) {
                WalletScreen(
                    viewModel = viewModel
                )
            }

            composable(Screen.Referral.route) {
                ReferralScreen(
                    viewModel = viewModel
                )
            }

            composable(Screen.Profile.route) {
                ProfileScreen(
                    viewModel = viewModel,
                    onNavigateToHistory = { navController.navigate(Screen.TaskHistory.route) },
                    onNavigateToAdminPortal = { navController.navigate(Screen.AdminPortal.route) },
                    onNavigateToAuth = {
                        navController.navigate(Screen.Auth.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Notifications.route) {
                NotificationsScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Auth.route) {
                AuthScreen(
                    viewModel = viewModel,
                    onAuthSuccess = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.AdminPortal.route) {
                AdminPortalScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
