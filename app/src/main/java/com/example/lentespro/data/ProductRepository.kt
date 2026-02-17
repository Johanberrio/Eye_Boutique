package com.example.lentespro.data


import kotlinx.coroutines.flow.Flow

class ProductRepository(
    private val dao: ProductDao
) {
    fun observeAll(): Flow<List<ProductEntity>> = dao.observeAll()
    fun observeSearch(q: String): Flow<List<ProductEntity>> = dao.observeSearch(q)

    suspend fun getById(id: Long): ProductEntity? = dao.getById(id)

    suspend fun upsert(entity: ProductEntity): Long {
        return if (entity.id == 0L) dao.insert(entity) else {
            dao.update(entity)
            entity.id
        }
    }

    suspend fun delete(entity: ProductEntity) = dao.delete(entity)
}
