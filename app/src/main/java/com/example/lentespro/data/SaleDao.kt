package com.example.lentespro.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

data class SaleWithItems(
    @Embedded val sale: SaleEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "saleId"
    )
    val items: List<SaleItemEntity>
)
@Dao
interface SaleDao {

    @Query("SELECT * FROM sales ORDER BY createdAtEpochMillis DESC")
    fun observeSales(): Flow<List<SaleEntity>>

    @Transaction
    @Query("SELECT * FROM sales WHERE id = :saleId LIMIT 1")
    fun observeSaleWithItems(saleId: Long): Flow<SaleWithItems?>

    @Transaction
    @Query("SELECT * FROM sales WHERE id = :saleId LIMIT 1")
    suspend fun getSaleWithItems(saleId: Long): SaleWithItems?

    @Query("""
    SELECT
        s.id AS saleId,
        COALESCE(s.finalizedAtEpochMillis, s.createdAtEpochMillis) AS soldAtEpochMillis,
        s.messengerName AS messengerName,
        s.total AS total,
        MIN(i.productName) AS productName,
        COALESCE(SUM(COALESCE(i.soldQty, 0)), 0) AS soldQty,
        s.customerName AS customerName,
        s.customerPhone1 AS customerPhone1
    FROM sales s
    INNER JOIN sale_items i ON i.saleId = s.id
    WHERE s.finalizedAtEpochMillis IS NOT NULL
    GROUP BY s.id, soldAtEpochMillis, s.messengerName, s.total, s.customerName
    ORDER BY soldAtEpochMillis DESC, s.id DESC
    """)
    fun observeSaleHistoryCards(): kotlinx.coroutines.flow.Flow<List<com.example.lentespro.data.SaleHistoryCard>>


    @Insert
    suspend fun insertSale(sale: SaleEntity): Long

    @Insert
    suspend fun insertItems(items: List<SaleItemEntity>)

    @Update
    suspend fun updateSale(sale: SaleEntity)

    @Update
    suspend fun updateItems(items: List<SaleItemEntity>)

    @Query("SELECT * FROM products WHERE id = :productId LIMIT 1")
    suspend fun getProductById(productId: Long): ProductEntity?

    @Update
    suspend fun updateProduct(product: ProductEntity)
}
