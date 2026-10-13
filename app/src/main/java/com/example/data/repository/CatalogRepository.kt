package com.example.data.repository

import com.example.data.local.CatalogItemDao
import com.example.data.model.CatalogItemEntity
import kotlinx.coroutines.flow.Flow

class CatalogRepository(private val catalogItemDao: CatalogItemDao) {

    val allCatalogItems: Flow<List<CatalogItemEntity>> = catalogItemDao.getAllCatalogItems()

    fun searchCatalogItems(query: String): Flow<List<CatalogItemEntity>> =
        catalogItemDao.searchCatalogItems(query)

    suspend fun getItemByQrCode(qrCode: String): CatalogItemEntity? {
        return catalogItemDao.getCatalogItemByQrCode(qrCode)
    }

    suspend fun saveCatalogItem(item: CatalogItemEntity): Long {
        return if (item.id == 0L) {
            catalogItemDao.insertCatalogItem(item)
        } else {
            catalogItemDao.updateCatalogItem(item)
            item.id
        }
    }

    suspend fun deleteCatalogItem(item: CatalogItemEntity) {
        catalogItemDao.deleteCatalogItem(item)
    }

    suspend fun clearAll() {
        catalogItemDao.clearAllCatalogItems()
    }
}
