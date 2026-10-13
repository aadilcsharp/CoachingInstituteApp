package com.example.ui.screens.clients

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ClientEntity
import com.example.ui.components.AppTopBar
import com.example.ui.components.ConfirmationDialog
import com.example.ui.components.EmptyStateView
import com.example.ui.viewmodel.BillViewModel

@Composable
fun ManageClientsScreen(
    viewModel: BillViewModel,
    onBack: () -> Unit,
    onNavigateToBroadcast: (() -> Unit)? = null,
    onNavigateToLicense: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val clients by viewModel.allClients.collectAsStateWithLifecycle()
    val licenseInfo by viewModel.licenseInfo.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }
    var showClientDialog by remember { mutableStateOf(false) }
    var showClientLimitDialog by remember { mutableStateOf(false) }
    var clientToEdit by remember { mutableStateOf<ClientEntity?>(null) }
    var clientToDelete by remember { mutableStateOf<ClientEntity?>(null) }

    fun checkAndOpenAddClient() {
        if (clients.size >= licenseInfo.tier.maxClients) {
            showClientLimitDialog = true
        } else {
            clientToEdit = null
            showClientDialog = true
        }
    }

    val filteredClients = remember(clients, searchQuery) {
        if (searchQuery.isBlank()) {
            clients
        } else {
            val q = searchQuery.trim().lowercase()
            clients.filter {
                it.name.lowercase().contains(q) ||
                (it.email?.lowercase()?.contains(q) == true) ||
                (it.phone?.lowercase()?.contains(q) == true) ||
                (it.address?.lowercase()?.contains(q) == true)
            }
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Client Directory",
                onBack = onBack,
                actions = {
                    if (onNavigateToBroadcast != null) {
                        IconButton(
                            onClick = onNavigateToBroadcast,
                            modifier = Modifier.testTag("btn_goto_broadcast")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Campaign,
                                contentDescription = "WhatsApp Broadcast",
                                tint = Color(0xFF25D366)
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { checkAndOpenAddClient() },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("fab_add_client")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Client")
            }
        },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search & Count Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search clients by name, email, phone...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search", modifier = Modifier.size(20.dp))
                    },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_clients_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (searchQuery.isBlank()) {
                            "${clients.size} total clients"
                        } else {
                            "${filteredClients.size} of ${clients.size} clients"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Autofills on bills & tracked on dashboard",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp
                    )
                }

                if (clients.isNotEmpty() && onNavigateToBroadcast != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        onClick = onNavigateToBroadcast,
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF25D366).copy(alpha = 0.12f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("broadcast_banner_card")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Campaign,
                                contentDescription = null,
                                tint = Color(0xFF25D366),
                                modifier = Modifier.size(22.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "WhatsApp Broadcast",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Send announcements or payment reminders to clients",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.Chat,
                                contentDescription = null,
                                tint = Color(0xFF25D366),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            if (filteredClients.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (clients.isEmpty()) {
                        EmptyStateView(
                            icon = Icons.Default.Person,
                            title = "No Clients in Directory Yet",
                            description = "Add clients to autofill them easily when creating bills. Client info is also automatically saved whenever you enter a new client on a bill.",
                            actionButtonText = "Add First Client",
                            onActionClick = { checkAndOpenAddClient() }
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "No clients match \"$searchQuery\"",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(onClick = { searchQuery = "" }) {
                                Text("Clear Search")
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredClients, key = { it.id }) { client ->
                        ClientItemCard(
                            client = client,
                            onEdit = {
                                clientToEdit = client
                                showClientDialog = true
                            },
                            onDelete = {
                                clientToDelete = client
                            }
                        )
                    }
                }
            }
        }
    }

    // Add / Edit Client Dialog
    if (showClientDialog) {
        val isEditing = clientToEdit != null
        var name by remember { mutableStateOf(clientToEdit?.name ?: "") }
        var email by remember { mutableStateOf(clientToEdit?.email ?: "") }
        var phone by remember { mutableStateOf(clientToEdit?.phone ?: "") }
        var address by remember { mutableStateOf(clientToEdit?.address ?: "") }
        var notes by remember { mutableStateOf(clientToEdit?.notes ?: "") }
        var nameError by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showClientDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isEditing) "Edit Client" else "Add New Client")
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            nameError = false
                        },
                        label = { Text("Client / Business Name *") },
                        placeholder = { Text("e.g. Nexus Tech Ltd") },
                        isError = nameError,
                        supportingText = if (nameError) {
                            { Text("Client name is required") }
                        } else null,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("manage_client_name_input")
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address") },
                        placeholder = { Text("contact@nexus.com") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("manage_client_email_input")
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone Number") },
                        placeholder = { Text("+91 98765 43210") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("manage_client_phone_input")
                    )

                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Billing Address") },
                        placeholder = { Text("Street, City, State, ZIP") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth().testTag("manage_client_address_input")
                    )

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Internal Notes (Optional)") },
                        placeholder = { Text("GSTIN, payment terms, contact person...") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth().testTag("manage_client_notes_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isBlank()) {
                            nameError = true
                            return@Button
                        }
                        val entity = (clientToEdit ?: ClientEntity(name = "")).copy(
                            name = name.trim(),
                            email = email.trim().ifEmpty { null },
                            phone = phone.trim().ifEmpty { null },
                            address = address.trim().ifEmpty { null },
                            notes = notes.trim().ifEmpty { null }
                        )
                        viewModel.saveClient(entity)
                        showClientDialog = false
                    },
                    modifier = Modifier.testTag("save_manage_client_button")
                ) {
                    Text(if (isEditing) "Save Changes" else "Add Client")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClientDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Client Confirmation Dialog
    if (clientToDelete != null) {
        val client = clientToDelete!!
        ConfirmationDialog(
            title = "Delete Client?",
            message = "Are you sure you want to remove \"${client.name}\" from the directory? Existing bills issued to this client will not be deleted.",
            confirmText = "Delete",
            isDestructive = true,
            onConfirm = {
                viewModel.deleteClient(client)
                clientToDelete = null
            },
            onDismiss = { clientToDelete = null }
        )
    }

    // Subscription Client Limit Exceeded Dialog
    if (showClientLimitDialog) {
        AlertDialog(
            onDismissRequest = { showClientLimitDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Client Directory Limit")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "You have reached the maximum of ${licenseInfo.tier.maxClients} clients allowed on the ${licenseInfo.tier.title}.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "To add more clients (up to 10 on Basic, 100 on Premium, or Unlimited on Enterprise), enter an offline SHA-256 license key.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showClientLimitDialog = false
                        onNavigateToLicense?.invoke()
                    },
                    modifier = Modifier.testTag("btn_upgrade_from_client_limit")
                ) {
                    Text("Upgrade License")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClientLimitDialog = false }) {
                    Text("Dismiss")
                }
            }
        )
    }
}

@Composable
fun ClientItemCard(
    client: ClientEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = client.name.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = client.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (!client.email.isNullOrBlank()) {
                    Text(
                        text = client.email,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (!client.phone.isNullOrBlank()) {
                    Text(
                        text = client.phone,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (!client.address.isNullOrBlank()) {
                    Text(
                        text = client.address,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        maxLines = 1
                    )
                }
            }

            Row {
                IconButton(onClick = onEdit) {
                    Icon(
                        Icons.Outlined.Edit,
                        contentDescription = "Edit Client",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = "Delete Client",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
