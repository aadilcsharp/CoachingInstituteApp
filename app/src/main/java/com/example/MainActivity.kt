package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.AppDatabase
import com.example.data.repository.BackupRepository
import com.example.data.repository.BillRepository
import com.example.data.repository.BusinessProfileRepository
import com.example.data.repository.CatalogRepository
import com.example.data.repository.ClientRepository
import com.example.ui.components.AppBottomBar
import com.example.ui.navigation.Screen
import com.example.ui.screens.bills.BillDetailScreen
import com.example.ui.screens.bills.BillsListScreen
import com.example.ui.screens.bills.CreateEditBillScreen
import com.example.ui.screens.clients.ManageClientsScreen
import com.example.ui.screens.clients.WhatsAppBroadcastScreen
import com.example.ui.screens.dashboard.DashboardScreen
import com.example.ui.screens.items.ManageItemsScreen
import com.example.ui.screens.reports.ReportsScreen
import com.example.ui.screens.settings.BackupRestoreScreen
import com.example.ui.screens.settings.BusinessProfileScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.settings.SubscriptionLicenseScreen
import com.example.ui.screens.settings.TemplatesGalleryScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.BillViewModel
import com.example.ui.viewmodel.BillViewModelFactory

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Check & request notification permission for expiry alerts on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                registerForActivityResult(ActivityResultContracts.RequestPermission()) {}.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        val database = AppDatabase.getDatabase(applicationContext)
        val billRepository = BillRepository(database.billDao())
        val profileRepository = BusinessProfileRepository(database.businessProfileDao())
        val backupRepository = BackupRepository(database)
        val catalogRepository = CatalogRepository(database.catalogItemDao())
        val clientRepository = ClientRepository(database.clientDao())

        val factory = BillViewModelFactory(
            billRepository,
            profileRepository,
            backupRepository,
            catalogRepository,
            clientRepository,
            applicationContext
        )
        val viewModel = ViewModelProvider(this, factory)[BillViewModel::class.java]

        // Check offline SHA-256 license key on app launch
        viewModel.refreshLicenseStatus()
        val license = viewModel.licenseInfo.value
        if (license.isExpiringSoon) {
            viewModel.setUserMessage("⚠️ License expiring in ${license.daysRemaining} days (${license.endDateStr}). Please renew.")
        }

        setContent {
            MyApplicationTheme {
                BillCraftApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun BillCraftApp(viewModel: BillViewModel) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Dashboard) }
    val screenStack = remember { mutableStateListOf<Screen>() }

    fun navigateTo(screen: Screen) {
        if (currentScreen != screen) {
            screenStack.add(currentScreen)
            currentScreen = screen
        }
    }

    fun navigateBack() {
        if (screenStack.isNotEmpty()) {
            currentScreen = screenStack.removeAt(screenStack.lastIndex)
        } else {
            currentScreen = Screen.Dashboard
        }
    }

    fun navigateToTab(tab: Screen) {
        screenStack.clear()
        currentScreen = tab
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val userMessage by viewModel.userMessage.collectAsStateWithLifecycle()

    LaunchedEffect(userMessage) {
        userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUserMessage()
        }
    }

    val isMainTab = currentScreen is Screen.Dashboard ||
            currentScreen is Screen.Bills ||
            currentScreen is Screen.Reports ||
            currentScreen is Screen.Settings

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (isMainTab) {
                AppBottomBar(
                    currentScreen = currentScreen,
                    onNavigate = { tab -> navigateToTab(tab) }
                )
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val screen = currentScreen) {
                is Screen.Dashboard -> {
                    DashboardScreen(
                        viewModel = viewModel,
                        onNavigate = { target -> navigateTo(target) }
                    )
                }

                is Screen.Bills -> {
                    BillsListScreen(
                        viewModel = viewModel,
                        onNavigate = { target -> navigateTo(target) }
                    )
                }

                is Screen.Reports -> {
                    ReportsScreen(
                        viewModel = viewModel,
                        onNavigate = { target -> navigateTo(target) }
                    )
                }

                is Screen.Settings -> {
                    SettingsScreen(
                        viewModel = viewModel,
                        onNavigate = { target -> navigateTo(target) }
                    )
                }

                is Screen.CreateEditBill -> {
                    CreateEditBillScreen(
                        billId = screen.billId,
                        viewModel = viewModel,
                        onBack = { navigateBack() },
                        onSaved = { savedId ->
                            screenStack.clear()
                            screenStack.add(Screen.Bills)
                            currentScreen = Screen.BillDetail(savedId)
                        }
                    )
                }

                is Screen.BillDetail -> {
                    BillDetailScreen(
                        billId = screen.billId,
                        viewModel = viewModel,
                        onBack = { navigateBack() },
                        onEdit = { id -> navigateTo(Screen.CreateEditBill(id)) }
                    )
                }

                is Screen.BusinessProfile -> {
                    BusinessProfileScreen(
                        viewModel = viewModel,
                        onBack = { navigateBack() },
                        onNavigateToLicense = { navigateTo(Screen.SubscriptionLicense) }
                    )
                }

                is Screen.TemplateManager -> {
                    TemplatesGalleryScreen(
                        viewModel = viewModel,
                        onBack = { navigateBack() },
                        onNavigateToLicense = { navigateTo(Screen.SubscriptionLicense) }
                    )
                }

                is Screen.BackupRestore -> {
                    BackupRestoreScreen(
                        viewModel = viewModel,
                        onBack = { navigateBack() },
                        onNavigateToLicense = { navigateTo(Screen.SubscriptionLicense) }
                    )
                }

                is Screen.ManageItems -> {
                    ManageItemsScreen(
                        viewModel = viewModel,
                        onBack = { navigateBack() },
                        onNavigateToLicense = { navigateTo(Screen.SubscriptionLicense) }
                    )
                }

                is Screen.ManageClients -> {
                    ManageClientsScreen(
                        viewModel = viewModel,
                        onBack = { navigateBack() },
                        onNavigateToBroadcast = { navigateTo(Screen.WhatsAppBroadcast) },
                        onNavigateToLicense = { navigateTo(Screen.SubscriptionLicense) }
                    )
                }

                is Screen.WhatsAppBroadcast -> {
                    WhatsAppBroadcastScreen(
                        viewModel = viewModel,
                        onBack = { navigateBack() },
                        onNavigateToLicense = { navigateTo(Screen.SubscriptionLicense) }
                    )
                }

                is Screen.SubscriptionLicense -> {
                    SubscriptionLicenseScreen(
                        viewModel = viewModel,
                        onBack = { navigateBack() }
                    )
                }

                else -> {
                    DashboardScreen(
                        viewModel = viewModel,
                        onNavigate = { target -> navigateTo(target) }
                    )
                }
            }
        }
    }
}
