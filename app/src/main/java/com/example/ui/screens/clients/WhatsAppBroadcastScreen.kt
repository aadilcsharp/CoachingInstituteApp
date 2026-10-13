package com.example.ui.screens.clients

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ClientEntity
import com.example.ui.components.AppTopBar
import com.example.ui.components.EmptyStateView
import com.example.ui.viewmodel.BillViewModel
import com.example.util.ShareHelper

private val WHATSAPP_GREEN = Color(0xFF25D366)

@Composable
fun WhatsAppBroadcastScreen(
    viewModel: BillViewModel,
    onBack: () -> Unit,
    onNavigateToLicense: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val clients by viewModel.allClients.collectAsStateWithLifecycle()
    val profile by viewModel.profileState.collectAsStateWithLifecycle()
    val licenseInfo by viewModel.licenseInfo.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var onlyWithPhone by remember { mutableStateOf(false) }
    var messageText by remember {
        mutableStateOf("Hello {client_name}, wishing you a great day from {business_name}! Please let us know if you need any products or services.")
    }

    // Selected client IDs
    val selectedClientIds = remember { mutableStateListOf<Long>() }

    // Sequential broadcast dialog state
    var showBroadcastDialog by remember { mutableStateOf(false) }
    var currentBroadcastIndex by remember { mutableIntStateOf(0) }

    // Filtered clients based on search and phone availability
    val displayedClients = remember(clients, searchQuery, onlyWithPhone) {
        clients.filter { c ->
            val matchesSearch = if (searchQuery.isBlank()) true else {
                val q = searchQuery.trim().lowercase()
                c.name.lowercase().contains(q) ||
                        (c.phone?.lowercase()?.contains(q) == true) ||
                        (c.email?.lowercase()?.contains(q) == true)
            }
            val matchesPhone = if (onlyWithPhone) !c.phone.isNullOrBlank() else true
            matchesSearch && matchesPhone
        }
    }

    val clientsWithPhoneCount = remember(clients) {
        clients.count { !it.phone.isNullOrBlank() }
    }

    fun personalizeMessage(template: String, client: ClientEntity?): String {
        val clientName = client?.name ?: "Valued Customer"
        val businessName = profile.businessName.ifEmpty { "BillCraft" }
        val clientPhone = client?.phone ?: ""
        return template
            .replace("{client_name}", clientName)
            .replace("{business_name}", businessName)
            .replace("{phone}", clientPhone)
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "WhatsApp Broadcast",
                onBack = onBack,
                actions = {
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Broadcast Message", messageText)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Message copied to clipboard", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy message")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val validSelected = clients.filter { selectedClientIds.contains(it.id) && !it.phone.isNullOrBlank() }

                    // General WhatsApp Share
                    OutlinedButton(
                        onClick = {
                            if (messageText.isBlank()) {
                                Toast.makeText(context, "Please enter a message", Toast.LENGTH_SHORT).show()
                                return@OutlinedButton
                            }
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, personalizeMessage(messageText, null))
                                setPackage("com.whatsapp")
                            }
                            try {
                                context.startActivity(shareIntent)
                            } catch (_: Exception) {
                                val chooser = Intent.createChooser(
                                    Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, personalizeMessage(messageText, null))
                                    },
                                    "Share Message"
                                )
                                context.startActivity(chooser)
                            }
                        },
                        modifier = Modifier.weight(1f).testTag("broadcast_share_btn")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share Text", fontSize = 13.sp)
                    }

                    // Direct Send to selected contacts
                    Button(
                        onClick = {
                            if (validSelected.isEmpty()) {
                                Toast.makeText(context, "Select at least one client with a phone number", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (messageText.isBlank()) {
                                Toast.makeText(context, "Please enter a message", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (validSelected.size == 1) {
                                val single = validSelected.first()
                                val msg = personalizeMessage(messageText, single)
                                ShareHelper.openWhatsAppChat(context, single.phone!!, msg)
                            } else {
                                currentBroadcastIndex = 0
                                showBroadcastDialog = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WHATSAPP_GREEN,
                            contentColor = Color.White
                        ),
                        modifier = Modifier.weight(1.3f).testTag("broadcast_send_selected_btn")
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Send (${validSelected.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        },
        modifier = modifier
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Tier Lock Banner if broadcast not allowed
            if (!licenseInfo.tier.isBroadcastAllowed) {
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
                                Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                Text(
                                    text = "Premium & Enterprise Feature",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "WhatsApp Bulk Broadcast is available on PREMIUM & ENTERPRISE tiers (currently on ${licenseInfo.tier.title}).",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            if (onNavigateToLicense != null) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = onNavigateToLicense,
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                    modifier = Modifier.testTag("btn_upgrade_broadcast_premium")
                                ) {
                                    Text("Upgrade to Premium")
                                }
                            }
                        }
                    }
                }
            }

            // 1. Message Composer Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Campaign,
                                contentDescription = null,
                                tint = WHATSAPP_GREEN,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "Compose Broadcast Message",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedTextField(
                            value = messageText,
                            onValueChange = { messageText = it },
                            placeholder = { Text("Write announcement, offer, or payment reminder...") },
                            minLines = 3,
                            maxLines = 6,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("broadcast_message_input")
                        )

                        // Personalization tags
                        Text(
                            text = "Tap to insert personalized tags:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            FilterChip(
                                selected = false,
                                onClick = { messageText += " {client_name}" },
                                label = { Text("{client_name}", fontSize = 11.sp) }
                            )
                            FilterChip(
                                selected = false,
                                onClick = { messageText += " {business_name}" },
                                label = { Text("{business_name}", fontSize = 11.sp) }
                            )
                        }

                        // Preset message template chips
                        Text(
                            text = "Quick Templates:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            item {
                                FilterChip(
                                    selected = false,
                                    onClick = {
                                        messageText = "Dear {client_name}, this is a gentle reminder regarding payment for your pending invoices. Kindly settle at your earliest convenience. Thank you! - {business_name}"
                                    },
                                    label = { Text("Payment Reminder", fontSize = 12.sp) }
                                )
                            }
                            item {
                                FilterChip(
                                    selected = false,
                                    onClick = {
                                        messageText = "Hello {client_name}! We are offering exclusive discounts on our products & services this month. Reach out to know more! - {business_name}"
                                    },
                                    label = { Text("Special Offers", fontSize = 12.sp) }
                                )
                            }
                            item {
                                FilterChip(
                                    selected = false,
                                    onClick = {
                                        messageText = "Warm greetings to {client_name}! Wishing you a wonderful festive season and prosperous business ahead from all of us at {business_name}."
                                    },
                                    label = { Text("Greetings", fontSize = 12.sp) }
                                )
                            }
                            item {
                                FilterChip(
                                    selected = false,
                                    onClick = {
                                        messageText = "Dear {client_name}, we have updated our catalog and business offerings. Contact us for any requirements! - {business_name}"
                                    },
                                    label = { Text("Catalog Update", fontSize = 12.sp) }
                                )
                            }
                        }
                    }
                }
            }

            // 2. Client Selection Header & Filter Bar
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Select Recipients (${selectedClientIds.size} / ${clients.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                TextButton(
                                    onClick = {
                                        selectedClientIds.clear()
                                        selectedClientIds.addAll(
                                            if (onlyWithPhone) clients.filter { !it.phone.isNullOrBlank() }.map { it.id }
                                            else clients.map { it.id }
                                        )
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text("Select All", fontSize = 12.sp)
                                }
                                TextButton(
                                    onClick = { selectedClientIds.clear() },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text("Deselect", fontSize = 12.sp)
                                }
                            }
                        }

                        // Search and Phone filter
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Filter recipients by name or phone...") },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                            trailingIcon = {
                                if (searchQuery.isNotBlank()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                                    }
                                }
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = onlyWithPhone,
                                onClick = { onlyWithPhone = !onlyWithPhone },
                                label = { Text("Only With Phone Numbers ($clientsWithPhoneCount)") },
                                leadingIcon = if (onlyWithPhone) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null
                            )
                        }
                    }
                }
            }

            // 3. Client List items
            if (displayedClients.isEmpty()) {
                item {
                    EmptyStateView(
                        icon = Icons.Outlined.Campaign,
                        title = "No Matching Clients",
                        description = "Add clients with phone numbers to broadcast messages.",
                        actionButtonText = "Clear Filter",
                        onActionClick = {
                            searchQuery = ""
                            onlyWithPhone = false
                        }
                    )
                }
            } else {
                items(displayedClients, key = { it.id }) { client ->
                    val isSelected = selectedClientIds.contains(client.id)
                    val hasPhone = !client.phone.isNullOrBlank()

                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                            else MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (isSelected) selectedClientIds.remove(client.id)
                                else selectedClientIds.add(client.id)
                            }
                            .testTag("broadcast_client_${client.id}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { checked ->
                                    if (checked) selectedClientIds.add(client.id)
                                    else selectedClientIds.remove(client.id)
                                }
                            )

                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (hasPhone) WHATSAPP_GREEN.copy(alpha = 0.15f)
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (hasPhone) Icons.Default.Chat else Icons.Default.Person,
                                    contentDescription = null,
                                    tint = if (hasPhone) WHATSAPP_GREEN else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = client.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (hasPhone) {
                                    Text(
                                        text = client.phone!!,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    Text(
                                        text = "No phone number added",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }

                            if (hasPhone) {
                                IconButton(
                                    onClick = {
                                        val singleMsg = personalizeMessage(messageText, client)
                                        ShareHelper.openWhatsAppChat(context, client.phone!!, singleMsg)
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Send,
                                        contentDescription = "Send direct",
                                        tint = WHATSAPP_GREEN,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Sequential Broadcast Dialog for multiple recipients
    if (showBroadcastDialog) {
        val validSelected = clients.filter { selectedClientIds.contains(it.id) && !it.phone.isNullOrBlank() }
        val currentClient = validSelected.getOrNull(currentBroadcastIndex)

        AlertDialog(
            onDismissRequest = { showBroadcastDialog = false },
            title = {
                Text(
                    text = "Sending Broadcast (${currentBroadcastIndex + 1} of ${validSelected.size})",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (currentClient != null) {
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Recipient: ${currentClient.name}",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "Phone: ${currentClient.phone}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        val personalized = personalizeMessage(messageText, currentClient)
                        Text(
                            text = "Message preview:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = personalized,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        )
                    } else {
                        Text("Broadcast complete!")
                    }
                }
            },
            confirmButton = {
                if (currentClient != null) {
                    Button(
                        onClick = {
                            val personalized = personalizeMessage(messageText, currentClient)
                            ShareHelper.openWhatsAppChat(context, currentClient.phone!!, personalized)
                            if (currentBroadcastIndex < validSelected.size - 1) {
                                currentBroadcastIndex++
                            } else {
                                showBroadcastDialog = false
                                Toast.makeText(context, "All messages sent!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = WHATSAPP_GREEN)
                    ) {
                        Text("Open WhatsApp")
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                } else {
                    Button(onClick = { showBroadcastDialog = false }) {
                        Text("Close")
                    }
                }
            },
            dismissButton = {
                if (currentClient != null && currentBroadcastIndex < validSelected.size - 1) {
                    TextButton(onClick = { currentBroadcastIndex++ }) {
                        Text("Skip")
                    }
                } else {
                    TextButton(onClick = { showBroadcastDialog = false }) {
                        Text("Cancel")
                    }
                }
            }
        )
    }
}
