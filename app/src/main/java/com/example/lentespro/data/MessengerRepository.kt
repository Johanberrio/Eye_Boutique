package com.example.lentespro.data

class MessengerRepository(private val dao: MessengerDao) {
    fun observeAll() = dao.observeAll()

    suspend fun create(name: String, phone: String, address: String?) {
        dao.insert(
            MessengerEntity(
                name = name.trim(),
                phone = phone.trim(),
                address = address?.trim()?.ifBlank { null }
            )
        )
    }

    suspend fun delete(entity: MessengerEntity) = dao.delete(entity)
}
