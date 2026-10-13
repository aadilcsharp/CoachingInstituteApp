package com.example.data.repository

import com.example.data.local.BillDao
import com.example.data.model.BillEntity
import com.example.data.model.BillItemEntity
import com.example.data.model.BillWithItems
import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import java.util.Locale

class BillRepository(private val billDao: BillDao) {

    val allBillsWithItems: Flow<List<BillWithItems>> = billDao.getAllBillsWithItems()
    val totalBillCount: Flow<Int> = billDao.getBillCount()

    fun getBillWithItemsById(id: Long): Flow<BillWithItems?> = billDao.getBillWithItemsById(id)

    suspend fun getBillWithItemsByIdSync(id: Long): BillWithItems? = billDao.getBillWithItemsByIdSync(id)

    suspend fun saveBill(bill: BillEntity, items: List<BillItemEntity>): Long {
        return billDao.saveBillWithItems(bill, items)
    }

    suspend fun updateBill(bill: BillEntity) {
        billDao.updateBill(bill)
    }

    suspend fun deleteBill(bill: BillEntity) {
        billDao.deleteBillWithItems(bill)
    }

    suspend fun isBillNumberTaken(billNumber: String, excludeId: Long): Boolean {
        return billDao.isBillNumberTaken(billNumber, excludeId)
    }

    suspend fun generateNextBillNumber(): String {
        val maxId = billDao.getMaxBillId() ?: 0L
        val nextSeq = maxId + 1
        val year = Calendar.getInstance().get(Calendar.YEAR)
        return String.format(Locale.US, "INV-%d-%04d", year, nextSeq)
    }

    suspend fun clearAll() {
        billDao.clearAllBillItems()
        billDao.clearAllBills()
    }
}
