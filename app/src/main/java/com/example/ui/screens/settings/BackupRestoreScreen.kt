package com.example.ui.screens.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.AppTopBar
import com.example.ui.viewmodel.BillViewModel
import com.example.util.ShareHelper

@Composable
fun BackupRestoreScreen(
    viewModel: BillViewModel,
    onBack: () -> Unit,
    onNavigateToLicense: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allBills by viewModel.allBills.collectAsStateWithLifecycle()
    val licenseInfo by viewModel.licenseInfo.collectAsStateWithLifecycle()
    var importJsonText by remember { mutableStateOf("") }
    var importMessage by remember { mutableStateOf<String?>(null) }
    var isSuccess by remember { mutableStateOf(false) }

    BackHandler { onBack() }

    Scaffold(
        topBar = { AppTopBar(title = "Backup & Restore", onBack = onBack) },
        modifier = modifier
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Enterprise Tier Check Banner
            if (!licenseInfo.tier.isBackupRestoreAllowed) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Outlined.Security, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                Text(
                                    text = "Enterprise Feature",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Full database JSON export and restore is reserved for the ENTERPRISE tier (currently on ${licenseInfo.tier.title}).",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            if (onNavigateToLicense != null) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = onNavigateToLicense,
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                    modifier = Modifier.testTag("btn_upgrade_backup_enterprise")
                                ) {
                                    Text("Upgrade to Enterprise")
                                }
                            }
                        }
                    }
                }
            }

            // Security Info Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Your financial data is stored 100% locally on your device in SQLite. Exporting backups ensures you can restore your invoices anytime.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Export Backup Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Export Database Backup",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Current database contains ${allBills.size} bills and your business profile. Exporting generates a portable JSON file.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Button(
                            onClick = {
                                if (!licenseInfo.tier.isBackupRestoreAllowed) {
                                    onNavigateToLicense?.invoke()
                                    return@Button
                                }
                                viewModel.exportDatabaseBackup { jsonContent ->
                                    ShareHelper.shareBackupJson(
                                        context = context,
                                        jsonContent = jsonContent,
                                        fileName = "BillCraft_Backup_${System.currentTimeMillis()}.json"
                                    )
                                }
                            },
                            enabled = licenseInfo.tier.isBackupRestoreAllowed,
                            modifier = Modifier.fillMaxWidth().testTag("export_backup_button")
                        ) {
                            Icon(Icons.Outlined.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export & Share Backup JSON")
                        }
                    }
                }
            }

            // Restore Backup Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Restore / Import Backup",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Paste the exported BillCraft JSON backup below to restore your bills and profile.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = importJsonText,
                            onValueChange = {
                                importJsonText = it
                                importMessage = null
                            },
                            label = { Text("Backup JSON Content") },
                            placeholder = { Text("{\n  \"version\": 1,\n  \"bills\": [...]\n}") },
                            minLines = 4,
                            maxLines = 8,
                            enabled = licenseInfo.tier.isBackupRestoreAllowed,
                            modifier = Modifier.fillMaxWidth().testTag("import_json_input")
                        )

                        if (importMessage != null) {
                            Text(
                                text = importMessage ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isSuccess) Color(0xFF059669) else MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Button(
                            onClick = {
                                if (!licenseInfo.tier.isBackupRestoreAllowed) {
                                    onNavigateToLicense?.invoke()
                                    return@Button
                                }
                                if (importJsonText.isBlank()) {
                                    importMessage = "Please paste JSON backup content first."
                                    isSuccess = false
                                    return@Button
                                }
                                viewModel.importDatabaseBackup(importJsonText)
                                importMessage = "Backup processed! Check bills list."
                                isSuccess = true
                                importJsonText = ""
                            },
                            enabled = licenseInfo.tier.isBackupRestoreAllowed,
                            modifier = Modifier.fillMaxWidth().testTag("restore_backup_button")
                        ) {
                            Icon(Icons.Outlined.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Restore Database")
                        }
                    }
                }
            }
        }
    }
}
