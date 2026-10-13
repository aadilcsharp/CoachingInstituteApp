package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.BillEntity
import com.example.data.model.BillItemEntity
import com.example.data.model.BusinessProfileEntity
import com.example.data.model.CatalogItemEntity
import com.example.data.model.ClientEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        BillEntity::class,
        BillItemEntity::class,
        BusinessProfileEntity::class,
        CatalogItemEntity::class,
        ClientEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun billDao(): BillDao
    abstract fun businessProfileDao(): BusinessProfileDao
    abstract fun catalogItemDao(): CatalogItemDao
    abstract fun clientDao(): ClientDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE bill_items ADD COLUMN imagePath TEXT")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS catalog_items (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        description TEXT,
                        defaultUnitPrice REAL NOT NULL,
                        unit TEXT,
                        category TEXT,
                        imagePath TEXT,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_catalog_items_name ON catalog_items(name)")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS clients (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        email TEXT,
                        phone TEXT,
                        address TEXT,
                        notes TEXT,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_clients_name ON clients(name)")
                // Seed any existing distinct clients from bills table into clients
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO clients (name, email, phone, address, notes, createdAt, updatedAt)
                    SELECT clientName, clientEmail, clientPhone, clientAddress, NULL, MIN(createdAt), MAX(updatedAt)
                    FROM bills
                    WHERE clientName IS NOT NULL AND TRIM(clientName) != ''
                    GROUP BY clientName
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE business_profile ADD COLUMN showLogoOnBill INTEGER NOT NULL DEFAULT 1")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE business_profile ADD COLUMN selectedCategories TEXT NOT NULL DEFAULT 'Services,Products,Consulting,Utilities,Maintenance,Office,Freelance,Other'")
                db.execSQL("ALTER TABLE business_profile ADD COLUMN paymentQrText TEXT NOT NULL DEFAULT 'business@upi'")
                db.execSQL("ALTER TABLE business_profile ADD COLUMN showPaymentQrOnBill INTEGER NOT NULL DEFAULT 1")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE catalog_items ADD COLUMN qrCode TEXT NOT NULL DEFAULT ''")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_catalog_items_qrCode ON catalog_items(qrCode)")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "billcraft_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            CoroutineScope(Dispatchers.IO).launch {
                                INSTANCE?.businessProfileDao()?.insertOrUpdateProfile(
                                    BusinessProfileEntity()
                                )
                                // Seed standard clients for instant usability
                                INSTANCE?.clientDao()?.let { dao ->
                                    dao.insertClient(
                                        ClientEntity(
                                            name = "Nexus Retailers Ltd",
                                            email = "contact@nexusretail.com",
                                            phone = "+1 (555) 321-7788",
                                            address = "742 Evergreen Terrace, Springfield, OR"
                                        )
                                    )
                                    dao.insertClient(
                                        ClientEntity(
                                            name = "Acrobatic Technologies",
                                            email = "finance@acrobatic.io",
                                            phone = "+1 (555) 890-1234",
                                            address = "101 Silicon Valley Blvd, San Jose, CA"
                                        )
                                    )
                                    dao.insertClient(
                                        ClientEntity(
                                            name = "Vertex Creative Studio",
                                            email = "accounts@vertexstudio.com",
                                            phone = "+1 (555) 445-9900",
                                            address = "500 Art District Ave, Austin, TX"
                                        )
                                    )
                                    dao.insertClient(
                                        ClientEntity(
                                            name = "BlueHorizon Logistics",
                                            email = "dispatch@bluehorizon.net",
                                            phone = "+1 (555) 678-4321",
                                            address = "12 Harbor Way, Seattle, WA"
                                        )
                                    )
                                }
                                // Seed some standard catalog items for instant usability
                                INSTANCE?.catalogItemDao()?.let { dao ->
                                    dao.insertCatalogItem(
                                        CatalogItemEntity(
                                            name = "Professional Consulting",
                                            description = "Specialized domain consulting & advisory",
                                            defaultUnitPrice = 2500.0,
                                            unit = "hrs",
                                            category = "Services"
                                        )
                                    )
                                    dao.insertCatalogItem(
                                        CatalogItemEntity(
                                            name = "Software & Web Development",
                                            description = "Full-stack frontend and backend engineering",
                                            defaultUnitPrice = 15000.0,
                                            unit = "module",
                                            category = "Development"
                                        )
                                    )
                                    dao.insertCatalogItem(
                                        CatalogItemEntity(
                                            name = "Annual Maintenance & Support",
                                            description = "24/7 server monitoring, backups & SLA",
                                            defaultUnitPrice = 5000.0,
                                            unit = "month",
                                            category = "Support"
                                        )
                                    )
                                }
                            }
                        }
                    })
                    .fallbackToDestructiveMigration(dropAllTables = false)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
