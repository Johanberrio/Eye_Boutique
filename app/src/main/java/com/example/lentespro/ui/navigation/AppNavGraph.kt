package com.example.lentespro.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.lentespro.AppContainer
import com.example.lentespro.ui.screens.DashboardScreen
import com.example.lentespro.ui.screens.EditProductScreen
import com.example.lentespro.ui.screens.InventoryScreen
import com.example.lentespro.ui.screens.NewRouteScreen
import com.example.lentespro.ui.screens.FinalizeRouteScreen
import com.example.lentespro.ui.viewmodel.EditProductViewModel
import com.example.lentespro.ui.viewmodel.EditProductViewModelFactory
import com.example.lentespro.ui.viewmodel.InventoryViewModel
import com.example.lentespro.ui.viewmodel.InventoryViewModelFactory
import com.example.lentespro.ui.viewmodel.NewRouteViewModel
import com.example.lentespro.ui.viewmodel.NewRouteViewModelFactory
import com.example.lentespro.ui.viewmodel.FinalizeRouteViewModel
import com.example.lentespro.ui.viewmodel.FinalizeRouteViewModelFactory
import com.example.lentespro.ui.screens.RoutesListScreen
import com.example.lentespro.ui.viewmodel.RoutesListViewModel
import com.example.lentespro.ui.viewmodel.RoutesListViewModelFactory


@Composable
fun AppNavGraph(
    container: AppContainer,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.Dashboard.route,
        modifier = modifier
    ) {
        composable(Routes.Dashboard.route) {
            val vm: InventoryViewModel = viewModel(
                factory = InventoryViewModelFactory(container.productRepository)
            )
            DashboardScreen(
                inventoryViewModel = vm,
                onGoToInventory = { navController.navigate(Routes.Inventory.route) },
                onAddProduct = { navController.navigate(Routes.EditProduct.create(-1L)) },
                onGoToRoutes = { navController.navigate(Routes.RoutesList.route) }

            )
        }

        composable(Routes.Inventory.route) {
            val vm: InventoryViewModel = viewModel(
                factory = InventoryViewModelFactory(container.productRepository)
            )
            InventoryScreen(
                inventoryViewModel = vm,
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

        // ✅ NUEVA RUTA: crear salida a ruta (descuenta inventario)
        composable(Routes.NewRoute.route) {
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
            val vm: RoutesListViewModel = viewModel(
                factory = RoutesListViewModelFactory(container.saleRepository)
            )

            RoutesListScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onNewRoute = { navController.navigate(Routes.NewRoute.route) },
                onFinalizeRoute = { saleId ->
                    navController.navigate(Routes.FinalizeRoute.create(saleId))
                }
            )
        }


        // ✅ NUEVA RUTA: finalizar salida (marcar vendido + devolver al inventario)
        composable(
            route = Routes.FinalizeRoute.route,
            arguments = listOf(
                navArgument("saleId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { backStackEntry ->
            val saleId = backStackEntry.arguments?.getLong("saleId") ?: -1L

            val vm: FinalizeRouteViewModel = viewModel(
                factory = FinalizeRouteViewModelFactory(
                    saleId = saleId,
                    repo = container.saleRepository
                )
            )

            FinalizeRouteScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
