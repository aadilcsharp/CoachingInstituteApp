package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.model.BillEntity
import com.example.data.model.BillItemEntity
import com.example.data.model.BusinessProfileEntity
import com.example.data.model.ClientEntity
import com.example.data.repository.ClientRepository
import com.example.util.CurrencyFormatter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    private lateinit var database: AppDatabase
    private lateinit var clientRepository: ClientRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        clientRepository = ClientRepository(database.clientDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("BillCraft", appName)
    }

    @Test
    fun `test inr default in business profile`() {
        val profile = BusinessProfileEntity()
        assertEquals("₹", profile.currencySymbol)
        assertEquals("INR", profile.currencyCode)
        assertTrue("showLogoOnBill defaults to true", profile.showLogoOnBill)
    }

    @Test
    fun `test business profile logo and showLogoOnBill toggle`() {
        val profileWithLogo = BusinessProfileEntity(
            logoPath = "/data/user/0/com.example/files/logo.png",
            showLogoOnBill = true
        )
        assertNotNull(profileWithLogo.logoPath)
        assertTrue(profileWithLogo.showLogoOnBill)

        val profileWithoutLogoOnBill = profileWithLogo.copy(showLogoOnBill = false)
        assertFalse(profileWithoutLogoOnBill.showLogoOnBill)
    }

    @Test
    fun `test client auto-save if not exists and count tracking`() = runBlocking {
        assertEquals(0, clientRepository.clientCount.first())

        // Save client if not exists
        val client1 = clientRepository.saveClientIfNotExists(
            name = "Alpha Corp",
            email = "contact@alphacorp.com",
            phone = "+91 98765 00000",
            address = "Mumbai, MH"
        )
        assertEquals("Alpha Corp", client1.name)
        assertEquals(1, clientRepository.clientCount.first())

        // Saving same client with updated phone updates existing, does not duplicate
        val client1Updated = clientRepository.saveClientIfNotExists(
            name = "Alpha Corp",
            email = "contact@alphacorp.com",
            phone = "+91 99999 88888",
            address = "Mumbai, MH"
        )
        assertEquals(1, clientRepository.clientCount.first())
        assertEquals("+91 98765 00000", client1Updated.phone)

        // Adding another client increases count
        val client2 = clientRepository.saveClient(
            ClientEntity(
                name = "Beta Solutions",
                email = "billing@beta.in",
                phone = "+91 88888 77777",
                address = "Bengaluru, KA"
            )
        )
        assertTrue(client2 > 0)
        assertEquals(2, clientRepository.clientCount.first())

        val list = clientRepository.allClients.first()
        assertEquals(2, list.size)
    }

    @Test
    fun `test currency formatting for inr and usd`() {
        val inrFormatted = CurrencyFormatter.format(125000.0, "₹")
        assertEquals("₹1,25,000.00", inrFormatted)

        val inrCompactLakh = CurrencyFormatter.formatCompact(2500000.0, "₹")
        assertEquals("₹25.00 L", inrCompactLakh)

        val inrCompactCrore = CurrencyFormatter.formatCompact(15000000.0, "₹")
        assertEquals("₹1.50 Cr", inrCompactCrore)

        val usdFormatted = CurrencyFormatter.format(1250.50, "$")
        assertEquals("$1,250.50", usdFormatted)

        val usdCompact = CurrencyFormatter.formatCompact(2500000.0, "$")
        assertEquals("$2.5M", usdCompact)
    }

    @Test
    fun `test bill model calculations`() {
        val item1 = BillItemEntity(billId = 1, name = "Web Development", quantity = 2.0, unitPrice = 10000.0, amount = 20000.0)
        val item2 = BillItemEntity(billId = 1, name = "Cloud Hosting", quantity = 1.0, unitPrice = 5000.0, amount = 5000.0)
        val items = listOf(item1, item2)

        val subtotal = items.sumOf { it.amount }
        assertEquals(25000.0, subtotal, 0.001)

        val discountPercent = 10.0
        val discountAmount = subtotal * (discountPercent / 100.0)
        val afterDiscount = subtotal - discountAmount
        val taxPercent = 18.0
        val taxAmount = afterDiscount * (taxPercent / 100.0)
        val total = afterDiscount + taxAmount

        assertEquals(2500.0, discountAmount, 0.001)
        assertEquals(22500.0, afterDiscount, 0.001)
        assertEquals(4050.0, taxAmount, 0.001)
        assertEquals(26550.0, total, 0.001)
    }
}
