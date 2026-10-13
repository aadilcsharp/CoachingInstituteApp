package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.model.BillEntity
import com.example.data.model.BillItemEntity
import com.example.data.model.BusinessProfileEntity
import com.example.data.model.CatalogItemEntity
import com.example.data.model.ClientEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class BackupRepository(private val database: AppDatabase) {

    suspend fun exportToJson(): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("version", 2)
        root.put("exportedAt", System.currentTimeMillis())

        val profile = database.businessProfileDao().getProfileSync() ?: BusinessProfileEntity()
        val profileJson = JSONObject().apply {
            put("businessName", profile.businessName)
            put("ownerName", profile.ownerName)
            put("email", profile.email)
            put("phone", profile.phone)
            put("address", profile.address)
            put("taxId", profile.taxId)
            put("currencySymbol", profile.currencySymbol)
            put("currencyCode", profile.currencyCode)
            put("paymentDetails", profile.paymentDetails)
            put("defaultTemplateId", profile.defaultTemplateId)
            put("defaultTaxPercent", profile.defaultTaxPercent)
            put("defaultNotes", profile.defaultNotes)
            put("defaultTerms", profile.defaultTerms)
            put("logoPath", profile.logoPath ?: JSONObject.NULL)
            put("showLogoOnBill", profile.showLogoOnBill)
            put("selectedCategories", profile.selectedCategories)
            put("paymentQrText", profile.paymentQrText)
            put("showPaymentQrOnBill", profile.showPaymentQrOnBill)
        }
        root.put("businessProfile", profileJson)

        // Export Catalog Items
        val catalog = database.catalogItemDao().getAllCatalogItems().first()
        val catalogArray = JSONArray()
        for (item in catalog) {
            val itemJson = JSONObject().apply {
                put("name", item.name)
                put("description", item.description ?: JSONObject.NULL)
                put("defaultUnitPrice", item.defaultUnitPrice)
                put("unit", item.unit ?: JSONObject.NULL)
                put("category", item.category ?: JSONObject.NULL)
                put("imagePath", item.imagePath ?: JSONObject.NULL)
            }
            catalogArray.put(itemJson)
        }
        root.put("catalogItems", catalogArray)

        // Export Clients
        val clients = database.clientDao().getAllClients().first()
        val clientsArray = JSONArray()
        for (client in clients) {
            val clientJson = JSONObject().apply {
                put("name", client.name)
                put("email", client.email ?: JSONObject.NULL)
                put("phone", client.phone ?: JSONObject.NULL)
                put("address", client.address ?: JSONObject.NULL)
                put("notes", client.notes ?: JSONObject.NULL)
            }
            clientsArray.put(clientJson)
        }
        root.put("clients", clientsArray)

        // Export Bills & Line Items
        val bills = database.billDao().getAllBillsWithItems().first()
        val billsArray = JSONArray()

        for (billWithItems in bills) {
            val b = billWithItems.bill
            val billJson = JSONObject().apply {
                put("billNumber", b.billNumber)
                put("title", b.title)
                put("billDate", b.billDate)
                put("dueDate", b.dueDate ?: JSONObject.NULL)
                put("clientName", b.clientName)
                put("clientEmail", b.clientEmail ?: JSONObject.NULL)
                put("clientPhone", b.clientPhone ?: JSONObject.NULL)
                put("clientAddress", b.clientAddress ?: JSONObject.NULL)
                put("subtotal", b.subtotal)
                put("taxPercent", b.taxPercent)
                put("taxAmount", b.taxAmount)
                put("discountPercent", b.discountPercent)
                put("discountAmount", b.discountAmount)
                put("totalAmount", b.totalAmount)
                put("paidAmount", b.paidAmount)
                put("paymentStatus", b.paymentStatus)
                put("category", b.category)
                put("templateId", b.templateId)
                put("notes", b.notes ?: JSONObject.NULL)
                put("terms", b.terms ?: JSONObject.NULL)
                put("paymentMethod", b.paymentMethod ?: JSONObject.NULL)
                put("createdAt", b.createdAt)
                put("updatedAt", b.updatedAt)

                val itemsArray = JSONArray()
                for (item in billWithItems.items) {
                    val itemJson = JSONObject().apply {
                        put("name", item.name)
                        put("description", item.description ?: JSONObject.NULL)
                        put("quantity", item.quantity)
                        put("unitPrice", item.unitPrice)
                        put("unit", item.unit ?: JSONObject.NULL)
                        put("amount", item.amount)
                        put("imagePath", item.imagePath ?: JSONObject.NULL)
                    }
                    itemsArray.put(itemJson)
                }
                put("items", itemsArray)
            }
            billsArray.put(billJson)
        }
        root.put("bills", billsArray)

        root.toString(2)
    }

    suspend fun importFromJson(jsonString: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonString)
            if (root.has("businessProfile")) {
                val p = root.getJSONObject("businessProfile")
                val profile = BusinessProfileEntity(
                    id = 1,
                    businessName = p.optString("businessName", "My Business"),
                    ownerName = p.optString("ownerName", ""),
                    email = p.optString("email", ""),
                    phone = p.optString("phone", ""),
                    address = p.optString("address", ""),
                    taxId = p.optString("taxId", ""),
                    currencySymbol = p.optString("currencySymbol", "₹"),
                    currencyCode = p.optString("currencyCode", "INR"),
                    paymentDetails = p.optString("paymentDetails", ""),
                    defaultTemplateId = p.optString("defaultTemplateId", "MODERN_CLEAN"),
                    defaultTaxPercent = p.optDouble("defaultTaxPercent", 18.0),
                    defaultNotes = p.optString("defaultNotes", ""),
                    defaultTerms = p.optString("defaultTerms", ""),
                    logoPath = if (p.isNull("logoPath")) null else p.optString("logoPath"),
                    showLogoOnBill = p.optBoolean("showLogoOnBill", true),
                    selectedCategories = p.optString("selectedCategories", "Services,Products,Consulting,Utilities,Maintenance,Office,Freelance,Other"),
                    paymentQrText = p.optString("paymentQrText", "business@upi"),
                    showPaymentQrOnBill = p.optBoolean("showPaymentQrOnBill", true)
                )
                database.businessProfileDao().insertOrUpdateProfile(profile)
            }

            if (root.has("catalogItems")) {
                val catalogArray = root.getJSONArray("catalogItems")
                for (i in 0 until catalogArray.length()) {
                    val c = catalogArray.getJSONObject(i)
                    val catalogItem = CatalogItemEntity(
                        id = 0,
                        name = c.getString("name"),
                        description = if (c.isNull("description")) null else c.optString("description"),
                        defaultUnitPrice = c.optDouble("defaultUnitPrice", 0.0),
                        unit = if (c.isNull("unit")) "pcs" else c.optString("unit"),
                        category = if (c.isNull("category")) "General" else c.optString("category"),
                        imagePath = if (c.isNull("imagePath")) null else c.optString("imagePath")
                    )
                    database.catalogItemDao().insertCatalogItem(catalogItem)
                }
            }

            if (root.has("clients")) {
                val clientsArray = root.getJSONArray("clients")
                for (i in 0 until clientsArray.length()) {
                    val cl = clientsArray.getJSONObject(i)
                    val clientEntity = ClientEntity(
                        id = 0,
                        name = cl.getString("name"),
                        email = if (cl.isNull("email")) null else cl.optString("email"),
                        phone = if (cl.isNull("phone")) null else cl.optString("phone"),
                        address = if (cl.isNull("address")) null else cl.optString("address"),
                        notes = if (cl.isNull("notes")) null else cl.optString("notes")
                    )
                    database.clientDao().insertClient(clientEntity)
                }
            }

            if (root.has("bills")) {
                val billsArray = root.getJSONArray("bills")
                var count = 0
                for (i in 0 until billsArray.length()) {
                    val b = billsArray.getJSONObject(i)
                    val billEntity = BillEntity(
                        id = 0,
                        billNumber = b.getString("billNumber"),
                        title = b.optString("title", ""),
                        billDate = b.getLong("billDate"),
                        dueDate = if (b.isNull("dueDate")) null else b.optLong("dueDate"),
                        clientName = b.getString("clientName"),
                        clientEmail = if (b.isNull("clientEmail")) null else b.optString("clientEmail"),
                        clientPhone = if (b.isNull("clientPhone")) null else b.optString("clientPhone"),
                        clientAddress = if (b.isNull("clientAddress")) null else b.optString("clientAddress"),
                        subtotal = b.getDouble("subtotal"),
                        taxPercent = b.getDouble("taxPercent"),
                        taxAmount = b.getDouble("taxAmount"),
                        discountPercent = b.getDouble("discountPercent"),
                        discountAmount = b.getDouble("discountAmount"),
                        totalAmount = b.getDouble("totalAmount"),
                        paidAmount = b.getDouble("paidAmount"),
                        paymentStatus = b.optString("paymentStatus", "UNPAID"),
                        category = b.optString("category", "SERVICES"),
                        templateId = b.optString("templateId", "MODERN_CLEAN"),
                        notes = if (b.isNull("notes")) null else b.optString("notes"),
                        terms = if (b.isNull("terms")) null else b.optString("terms"),
                        paymentMethod = if (b.isNull("paymentMethod")) null else b.optString("paymentMethod"),
                        createdAt = b.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = b.optLong("updatedAt", System.currentTimeMillis())
                    )

                    val items = mutableListOf<BillItemEntity>()
                    if (b.has("items")) {
                        val itemsArray = b.getJSONArray("items")
                        for (j in 0 until itemsArray.length()) {
                            val itm = itemsArray.getJSONObject(j)
                            items.add(
                                BillItemEntity(
                                    id = 0,
                                    billId = 0,
                                    name = itm.getString("name"),
                                    description = if (itm.isNull("description")) null else itm.optString("description"),
                                    quantity = itm.getDouble("quantity"),
                                    unitPrice = itm.getDouble("unitPrice"),
                                    unit = if (itm.isNull("unit")) null else itm.optString("unit"),
                                    amount = itm.getDouble("amount"),
                                    imagePath = if (itm.isNull("imagePath")) null else itm.optString("imagePath")
                                )
                            )
                        }
                    }

                    database.billDao().saveBillWithItems(billEntity, items)
                    count++
                }
                Result.success(count)
            } else {
                Result.failure(IllegalArgumentException("Invalid backup file: no 'bills' array found."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
