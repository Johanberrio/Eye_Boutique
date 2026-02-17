package com.example.lentespro.data


import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {

    @Query("SELECT * FROM products ORDER BY nombre COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<ProductEntity>>

    @Query("""
        SELECT * FROM products
        WHERE nombre LIKE '%' || :q || '%'
           OR marca LIKE '%' || :q || '%'
           OR tipo LIKE '%' || :q || '%'
        ORDER BY nombre COLLATE NOCASE ASC
    """)
    fun observeSearch(q: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): ProductEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ProductEntity): Long

    @Update
    suspend fun update(entity: ProductEntity)

    @Delete
    suspend fun delete(entity: ProductEntity)
}
