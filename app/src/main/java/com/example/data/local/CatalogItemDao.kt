package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.CatalogItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CatalogItemDao {

    @Query("SELECT * FROM catalog_items ORDER BY name ASC")
    fun getAllCatalogItems(): Flow<List<CatalogItemEntity>>

    @Query("SELECT * FROM catalog_items WHERE id = :id LIMIT 1")
    fun getCatalogItemById(id: Long): Flow<CatalogItemEntity?>

    @Query("SELECT * FROM catalog_items WHERE qrCode = :qrCode LIMIT 1")
    suspend fun getCatalogItemByQrCode(qrCode: String): CatalogItemEntity?

    @Query("SELECT * FROM catalog_items WHERE name LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchCatalogItems(query: String): Flow<List<CatalogItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCatalogItem(item: CatalogItemEntity): Long

    @Update
    suspend fun updateCatalogItem(item: CatalogItemEntity)

    @Delete
    suspend fun deleteCatalogItem(item: CatalogItemEntity)

    @Query("DELETE FROM catalog_items")
    suspend fun clearAllCatalogItems()
}
