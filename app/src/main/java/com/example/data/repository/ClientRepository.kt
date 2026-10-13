package com.example.data.repository

import com.example.data.local.ClientDao
import com.example.data.model.ClientEntity
import kotlinx.coroutines.flow.Flow

class ClientRepository(private val clientDao: ClientDao) {

    val allClients: Flow<List<ClientEntity>> = clientDao.getAllClients()
    val clientCount: Flow<Int> = clientDao.getClientCount()

    suspend fun getClientByName(name: String): ClientEntity? {
        return clientDao.getClientByName(name)
    }

    suspend fun saveClient(client: ClientEntity): Long {
        val trimmedName = client.name.trim()
        val existing = clientDao.getClientByName(trimmedName)
        return if (existing != null) {
            val updated = existing.copy(
                name = trimmedName,
                email = client.email?.trim()?.ifBlank { null } ?: existing.email,
                phone = client.phone?.trim()?.ifBlank { null } ?: existing.phone,
                address = client.address?.trim()?.ifBlank { null } ?: existing.address,
                notes = client.notes ?: existing.notes,
                updatedAt = System.currentTimeMillis()
            )
            clientDao.updateClient(updated)
            updated.id
        } else {
            clientDao.insertClient(
                client.copy(
                    id = 0,
                    name = trimmedName,
                    email = client.email?.trim()?.ifBlank { null },
                    phone = client.phone?.trim()?.ifBlank { null },
                    address = client.address?.trim()?.ifBlank { null },
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun saveClientIfNotExists(
        name: String,
        email: String?,
        phone: String?,
        address: String?
    ): ClientEntity {
        val trimmedName = name.trim()
        val existing = clientDao.getClientByName(trimmedName)
        if (existing != null) {
            val needsUpdate = (existing.email.isNullOrBlank() && !email.isNullOrBlank()) ||
                    (existing.phone.isNullOrBlank() && !phone.isNullOrBlank()) ||
                    (existing.address.isNullOrBlank() && !address.isNullOrBlank())
            if (needsUpdate) {
                val updated = existing.copy(
                    email = if (existing.email.isNullOrBlank()) email?.trim()?.ifBlank { null } else existing.email,
                    phone = if (existing.phone.isNullOrBlank()) phone?.trim()?.ifBlank { null } else existing.phone,
                    address = if (existing.address.isNullOrBlank()) address?.trim()?.ifBlank { null } else existing.address,
                    updatedAt = System.currentTimeMillis()
                )
                clientDao.updateClient(updated)
                return updated
            }
            return existing
        } else {
            val newClient = ClientEntity(
                name = trimmedName,
                email = email?.trim()?.ifBlank { null },
                phone = phone?.trim()?.ifBlank { null },
                address = address?.trim()?.ifBlank { null },
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            val newId = clientDao.insertClient(newClient)
            return newClient.copy(id = newId)
        }
    }

    suspend fun deleteClient(client: ClientEntity) {
        clientDao.deleteClient(client)
    }

    suspend fun clearAll() {
        clientDao.clearAllClients()
    }
}
