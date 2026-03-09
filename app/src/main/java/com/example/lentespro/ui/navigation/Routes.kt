package com.example.lentespro.ui.navigation

sealed class Routes(val route: String) {

    // ✅ NUEVO: entrada de la app (decide Login vs Dashboard + Huella)
    data object AuthGate : Routes("authGate")

    // ✅ NUEVO: pantalla de login
    data object Login : Routes("login")

    data object Dashboard : Routes("dashboard")
    data object Inventory : Routes("inventory")

    data object SalesList : Routes("sales")
    data object NewRoute : Routes("newRoute")
    data object RoutesList : Routes("routes")

    data object Messengers : Routes("messengers")

    data object AdminUsers : Routes("adminUsers")

    data object RouteDetail : Routes("routeDetail?saleId={saleId}") {
        fun create(saleId: Long) = "routeDetail?saleId=$saleId"
    }

    data object FinalizeRoute : Routes("finalizeRoute?saleId={saleId}") {
        fun create(saleId: Long) = "finalizeRoute?saleId=$saleId"
    }

    // productId = -1 => nuevo
    data object EditProduct : Routes("editProduct?productId={productId}") {
        fun create(productId: Long): String = "editProduct?productId=$productId"
    }
}