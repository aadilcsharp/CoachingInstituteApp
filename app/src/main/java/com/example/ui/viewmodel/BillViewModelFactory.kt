package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.data.repository.BackupRepository
import com.example.data.repository.BillRepository
import com.example.data.repository.BusinessProfileRepository
import com.example.data.repository.CatalogRepository
import com.example.data.repository.ClientRepository

class BillViewModelFactory(
    private val billRepository: BillRepository,
    private val profileRepository: BusinessProfileRepository,
    private val backupRepository: BackupRepository,
    private val catalogRepository: CatalogRepository,
    private val clientRepository: ClientRepository,
    private val context: Context
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BillViewModel::class.java)) {
            return BillViewModel(
                billRepository,
                profileRepository,
                backupRepository,
                catalogRepository,
                clientRepository,
                context
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
