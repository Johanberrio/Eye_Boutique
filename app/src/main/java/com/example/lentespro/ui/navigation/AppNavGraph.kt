package com.example.lentespro.ui.navigation

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.lentespro.AppContainer
import com.example.lentespro.ui.screens.*
import com.example.lentespro.ui.viewmodel.*
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@Composable
fun AppNavGraph(
    container: AppContainer,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()

    val profileVm: UserProfileViewModel = viewModel(
        factory = UserProfileViewModelFactory(container.authProfileRepository)
    )
    val profileState by profileVm.ui.collectAsState()
    val isAdmin = profileState.isAdmin
    val isSuperAdmin = profileState.isSuperAdmin

    NavHost(
        navController = navController,
        startDestination = Routes.AuthGate.route,
        modifier = modifier
    ) {

        composable(Routes.AuthGate.route) {
            AuthGateScreen(
                biometricPrefs = container.biometricPrefs,
                onGoDashboard = {
                    profileVm.refresh()
                    navController.navigate(Routes.Dashboard.route) {
                        popUpTo(Routes.AuthGate.route) { inclusive = true }
                    }
                },
                onGoLogin = {
                    navController.navigate(Routes.Login.route) {
                        popUpTo(Routes.AuthGate.route) { inclusive = true }
                    }
                },
                forceBiometricIfEnabled = true
            )
        }

        composable(Routes.Login.route) {
            LoginScreen(
                biometricPrefs = container.biometricPrefs,
                authRepo = container.authRepository,
                onLoggedIn = {
                    profileVm.refresh()
                    navController.navigate(Routes.Dashboard.route) {
                        popUpTo(Routes.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.Dashboard.route) {
            val inventoryVm: InventoryViewModel = viewModel(
                factory = InventoryViewModelFactory(
                    repo = container.productRepository,
                    saleRepo = container.saleRepository
                )
            )
            
            val darkModePref by container.biometricPrefs.darkModeFlow.collectAsState(initial = null)
            val currentDark = darkModePref ?: isSystemInDarkTheme()

            DashboardScreen(
                inventoryViewModel = inventoryVm,
                isDarkMode = currentDark,
                onToggleDarkMode = { enabled ->
                    scope.launch { container.biometricPrefs.setDarkMode(enabled) }
                },
                onGoToInventory = { navController.navigate(Routes.Inventory.route) },
                onAddProduct = { navController.navigate(Routes.EditProduct.create("new")) },
                onGoToRoutes = { navController.navigate(Routes.RoutesList.route) },
                onGoToMessengers = { navController.navigate(Routes.Messengers.route) },
                onGoToHistory = { navController.navigate(Routes.SalesHistory.route) },
                onGoToGemini = { navController.navigate(Routes.GeminiChat.route) },
                isAdmin = isAdmin,
                onGoToAdminUsers = { navController.navigate(Routes.AdminUsers.route) },
                onLogout = { 
                    FirebaseAuth.getInstance().signOut()
                    navController.navigate(Routes.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.Inventory.route) {
            val vm: InventoryViewModel = viewModel(
                factory = InventoryViewModelFactory(
                    repo = container.productRepository,
                    saleRepo = container.saleRepository
                )
            )
            InventoryScreen(
                inventoryViewModel = vm,
                isAdmin = isAdmin,
                isSuperAdmin = isSuperAdmin,
                onBack = { navController.popBackStack() },
                onAddProduct = { navController.navigate(Routes.EditProduct.create("new")) },
                onGoToAdminNotes = { navController.navigate(Routes.SuperAdminNotes.route) },
                onEditProduct = { id -> navController.navigate(Routes.EditProduct.create(id)) }
            )
        }

        composable(Routes.SuperAdminNotes.route) {
            val vm: AdminNotesViewModel = viewModel(
                factory = AdminNotesViewModelFactory(container.adminNotesRepository)
            )
            AdminNotesScreen(
                viewModel = vm,
                isSuperAdmin = isSuperAdmin,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SalesHistory.route) {
            val vm: SalesHistoryViewModel = viewModel(
                factory = SalesHistoryViewModelFactory(container.saleRepository)
            )
            SalesHistoryScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.GeminiChat.route) {
            val vm: GeminiChatViewModel = viewModel(
                factory = GeminiChatViewModelFactory(
                    geminiRepo = container.geminiRepository,
                    productRepo = container.productRepository,
                    saleRepo = container.saleRepository
                )
            )
            GeminiChatScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onNavigateCommand = { target ->
                    when(target) {
                        "INVENTORY" -> navController.navigate(Routes.Inventory.route)
                        "ROUTES" -> navController.navigate(Routes.RoutesList.route)
                        "HISTORY" -> navController.navigate(Routes.SalesHistory.route)
                    }
                }
            )
        }

        composable(
            route = Routes.EditProduct.route,
            arguments = listOf(
                navArgument("productId") {
                    type = NavType.StringType
                    defaultValue = "new"
                }
            )
        ) { backStackEntry ->
            if (!isAdmin) {
                LaunchedEffect(Unit) { navController.popBackStack() }
                return@composable
            }

            val productId = backStackEntry.arguments?.getString("productId") ?: "new"
            val vm: EditProductViewModel = viewModel(
                factory = EditProductViewModelFactory(
                    repo = container.productRepository,
                    adminNotesRepo = container.adminNotesRepository,
                    authProfileRepo = container.authProfileRepository,
                    geminiRepo = container.geminiRepository,
                    productId = productId
                )
            )

            EditProductScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.NewRoute.route) {
            if (!isAdmin) {
                LaunchedEffect(Unit) { navController.popBackStack() }
                return@composable
            }
            val vm: NewRouteViewModel = viewModel(
                factory = NewRouteViewModelFactory(
                    productRepo = container.productRepository,
                    saleRepo = container.saleRepository,
                    messengerRepo = container.messengerRepository,
                    geminiRepo = container.geminiRepository // ✅ Se añadió el parámetro faltante
                )
            )
            NewRouteScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.RoutesList.route) {
            val vm: RoutesListViewModel = viewModel(
                factory = RoutesListViewModelFactory(
                    repo = container.saleRepository,
                    messengerRepo = container.messengerRepository
                )
            )
            RoutesListScreen(
                viewModel = vm,
                isAdmin = isAdmin,
                onBack = { navController.popBackStack() },
                onNewRoute = { navController.navigate(Routes.NewRoute.route) },
                onFinalizeRoute = { saleId -> navController.navigate(Routes.FinalizeRoute.create(saleId)) },
                onOpenDetail = { id -> navController.navigate(Routes.RouteDetail.create(id)) }
            )
        }

        composable(
            route = Routes.FinalizeRoute.route,
            arguments = listOf(
                navArgument("saleId") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val saleId = backStackEntry.arguments?.getString("saleId") ?: ""
            val vm: FinalizeRouteViewModel = viewModel(
                factory = FinalizeRouteViewModelFactory(
                    saleId = saleId,
                    repo = container.saleRepository,
                    authProfileRepo = container.authProfileRepository,
                    usersRemoteRepo = container.usersRemoteRepository
                )
            )
            FinalizeRouteScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.RouteDetail.route,
            arguments = listOf(
                navArgument("saleId") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val saleId = backStackEntry.arguments?.getString("saleId") ?: ""
            val vm: RouteDetailViewModel = viewModel(
                factory = RouteDetailViewModelFactory(
                    saleId = saleId,
                    repo = container.saleRepository
                )
            )
            RouteDetailScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.Messengers.route) {
            if (!isAdmin) {
                LaunchedEffect(Unit) { navController.popBackStack() }
                return@composable
            }
            val vm: MessengersViewModel = viewModel(
                factory = MessengersViewModelFactory(container.messengerRepository)
            )
            MessengersScreen(
                viewModel = vm,
                isSuperAdmin = isSuperAdmin,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.AdminUsers.route) {
            if (!isSuperAdmin) {
                LaunchedEffect(Unit) { navController.popBackStack() }
                return@composable
            }
            val vm: AdminUsersViewModel = viewModel(
                factory = AdminUsersViewModelFactory(
                    repo = container.adminUsersRepository,
                    authProfileRepo = container.authProfileRepository
                )
            )
            AdminUsersScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
