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
