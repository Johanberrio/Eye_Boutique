package com.example.lentespro.ui.navigation

sealed class Routes(val route: String) {

    data object AuthGate : Routes("authGate")
    data object Login : Routes("login")
    data object Dashboard : Routes("dashboard")
    data object Inventory : Routes("inventory")
    data object SalesList : Routes("sales")
    data object NewRoute : Routes("newRoute")
    data object RoutesList : Routes("routes")
    data object Messengers : Routes("messengers")
    data object AdminUsers : Routes("adminUsers")
    data object SuperAdminNotes : Routes("superAdminNotes")

    data object RouteDetail : Routes("routeDetail?saleId={saleId}") {
        fun create(saleId: String) = "routeDetail?saleId=$saleId"
    }

    data object FinalizeRoute : Routes("finalizeRoute?saleId={saleId}") {
        fun create(saleId: String) = "finalizeRoute?saleId=$saleId"
    }

    // productId = "new" => nuevo
    data object EditProduct : Routes("editProduct?productId={productId}") {
        fun create(productId: String): String = "editProduct?productId=$productId"
    }
}
