package com.example.data.repository

import com.example.data.local.BusinessProfileDao
import com.example.data.model.BusinessProfileEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BusinessProfileRepository(private val profileDao: BusinessProfileDao) {

    val profile: Flow<BusinessProfileEntity?> = profileDao.getProfile().map { existing ->
        if (existing == null) {
            val defaultProfile = BusinessProfileEntity()
            profileDao.insertOrUpdateProfile(defaultProfile)
            defaultProfile
        } else if (existing.currencySymbol == "$" && existing.currencyCode == "USD") {
            // Upgrade previous template default from $ to INR ₹
            val updated = existing.copy(currencySymbol = "₹", currencyCode = "INR")
            profileDao.insertOrUpdateProfile(updated)
            updated
        } else {
            existing
        }
    }

    suspend fun getProfileSync(): BusinessProfileEntity {
        val existing = profileDao.getProfileSync()
        if (existing == null) {
            val defaultProfile = BusinessProfileEntity()
            profileDao.insertOrUpdateProfile(defaultProfile)
            return defaultProfile
        }
        if (existing.currencySymbol == "$" && existing.currencyCode == "USD") {
            val updated = existing.copy(currencySymbol = "₹", currencyCode = "INR")
            profileDao.insertOrUpdateProfile(updated)
            return updated
        }
        return existing
    }

    suspend fun updateProfile(profile: BusinessProfileEntity) {
        profileDao.insertOrUpdateProfile(profile.copy(id = 1))
    }
}
