package com.example.data.model

import androidx.room.Embedded
import androidx.room.Relation

data class BillWithItems(
    @Embedded
    val bill: BillEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "billId"
    )
    val items: List<BillItemEntity>
)
