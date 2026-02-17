package com.example.lentespro.ui.navigation


sealed class Routes(val route: String) {
    data object Dashboard : Routes("dashboard")
    data object Inventory : Routes("inventory")

    data object SalesList : Routes("sales")
    data object NewRoute : Routes("newRoute")
    data object RoutesList : Routes("routes")


    data object FinalizeRoute : Routes("finalizeRoute?saleId={saleId}") {
        fun create(saleId: Long) = "finalizeRoute?saleId=$saleId"
    }

    // productId = -1 => nuevo
    data object EditProduct : Routes("editProduct?productId={productId}") {
        fun create(productId: Long): String = "editProduct?productId=$productId"
    }
}
















