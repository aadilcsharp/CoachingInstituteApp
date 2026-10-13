package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.model.BillEntity
import com.example.data.model.BillItemEntity
import com.example.data.model.BillWithItems
import kotlinx.coroutines.flow.Flow

@Dao
interface BillDao {

    @Transaction
    @Query("SELECT * FROM bills ORDER BY billDate DESC, id DESC")
    fun getAllBillsWithItems(): Flow<List<BillWithItems>>

    @Transaction
    @Query("SELECT * FROM bills WHERE id = :id")
    fun getBillWithItemsById(id: Long): Flow<BillWithItems?>

    @Transaction
    @Query("SELECT * FROM bills WHERE id = :id")
    suspend fun getBillWithItemsByIdSync(id: Long): BillWithItems?

    @Query("SELECT COUNT(*) FROM bills")
    fun getBillCount(): Flow<Int>

    @Query("SELECT MAX(id) FROM bills")
    suspend fun getMaxBillId(): Long?

    @Query("SELECT COUNT(*) > 0 FROM bills WHERE billNumber = :billNumber AND id != :excludeId")
    suspend fun isBillNumberTaken(billNumber: String, excludeId: Long): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBill(bill: BillEntity): Long

    @Update
    suspend fun updateBill(bill: BillEntity)

    @Delete
    suspend fun deleteBill(bill: BillEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBillItems(items: List<BillItemEntity>)

    @Query("DELETE FROM bill_items WHERE billId = :billId")
    suspend fun deleteBillItemsByBillId(billId: Long)

    @Query("DELETE FROM bills")
    suspend fun clearAllBills()

    @Query("DELETE FROM bill_items")
    suspend fun clearAllBillItems()

    @Transaction
    suspend fun saveBillWithItems(bill: BillEntity, items: List<BillItemEntity>): Long {
        val billId = if (bill.id == 0L) {
            insertBill(bill)
        } else {
            updateBill(bill)
            bill.id
        }

        deleteBillItemsByBillId(billId)
        val itemsWithBillId = items.map { it.copy(id = 0L, billId = billId) }
        insertBillItems(itemsWithBillId)

        return billId
    }

    @Transaction
    suspend fun deleteBillWithItems(bill: BillEntity) {
        deleteBillItemsByBillId(bill.id)
        deleteBill(bill)
    }
}
