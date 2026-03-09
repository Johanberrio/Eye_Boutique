package com.example.lentespro.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.lentespro.AppContainer
import com.example.lentespro.ui.screens.*
import com.example.lentespro.ui.viewmodel.*
import com.google.firebase.auth.FirebaseAuth

@Composable
fun AppNavGraph(
    container: AppContainer,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()

    val profileVm: UserProfileViewModel = viewModel(
        factory = UserProfileViewModelFactory(container.authProfileRepository)
    )
    val profileState by profileVm.ui.collectAsState()
    val isAdmin = profileState.isAdmin

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

            DashboardScreen(
                inventoryViewModel = inventoryVm,
                onGoToInventory = { navController.navigate(Routes.Inventory.route) },
                onAddProduct = { navController.navigate(Routes.EditProduct.create(-1L)) },
                onGoToRoutes = { navController.navigate(Routes.RoutesList.route) },
                onGoToMessengers = { navController.navigate(Routes.Messengers.route) },
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
                onBack = { navController.popBackStack() },
                onAddProduct = { navController.navigate(Routes.EditProduct.create(-1L)) },
                onEditProduct = { id -> navController.navigate(Routes.EditProduct.create(id)) }
            )
        }

        composable(
            route = Routes.EditProduct.route,
            arguments = listOf(
                navArgument("productId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { backStackEntry ->
            if (!isAdmin) {
                LaunchedEffect(Unit) { navController.popBackStack() }
                return@composable
            }

            val productId = backStackEntry.arguments?.getLong("productId") ?: -1L
            val vm: EditProductViewModel = viewModel(
                factory = EditProductViewModelFactory(
                    repo = container.productRepository,
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
                    saleRepo = container.saleRepository
                )
            )
            NewRouteScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.RoutesList.route) {
            if (!isAdmin) {
                LaunchedEffect(Unit) { navController.popBackStack() }
                return@composable
            }
            val vm: RoutesListViewModel = viewModel(
                factory = RoutesListViewModelFactory(container.saleRepository)
            )
            RoutesListScreen(
                viewModel = vm,
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
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { backStackEntry ->
            if (!isAdmin) {
                LaunchedEffect(Unit) { navController.popBackStack() }
                return@composable
            }
            val saleId = backStackEntry.arguments?.getLong("saleId") ?: -1L
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
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { backStackEntry ->
            if (!isAdmin) {
                LaunchedEffect(Unit) { navController.popBackStack() }
                return@composable
            }
            val saleId = backStackEntry.arguments?.getLong("saleId") ?: -1L
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
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.AdminUsers.route) {
            if (!isAdmin) {
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
