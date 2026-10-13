package com.example.ui.screens.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.filled.Lock
import com.example.data.model.BillTemplateType
import com.example.ui.components.AppTopBar
import com.example.ui.viewmodel.BillViewModel

@Composable
fun TemplatesGalleryScreen(
    viewModel: BillViewModel,
    onBack: () -> Unit,
    onNavigateToLicense: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val profile by viewModel.profileState.collectAsStateWithLifecycle()
    val licenseInfo by viewModel.licenseInfo.collectAsStateWithLifecycle()

    BackHandler { onBack() }

    Scaffold(
        topBar = { AppTopBar(title = "Bill Templates Gallery", onBack = onBack) },
        modifier = modifier
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Professional PDF Templates",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Each template applies distinct formatting, color palettes, and typography to generated PDFs. Available templates depend on your subscription tier (currently ${licenseInfo.tier.title} with ${licenseInfo.tier.maxTemplates} template(s)).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            itemsIndexed(BillTemplateType.entries) { index, tmpl ->
                val isUnlocked = index < licenseInfo.tier.maxTemplates
                val requiredPlan = if (index == 1) "BASIC" else "PREMIUM"
                val isDefault = profile.defaultTemplateId == tmpl.templateId
                val primaryColor = Color(android.graphics.Color.parseColor(tmpl.primaryColorHex))
                val secondaryColor = Color(android.graphics.Color.parseColor(tmpl.secondaryColorHex))

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isUnlocked) MaterialTheme.colorScheme.surface
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    border = if (isDefault) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth().testTag("gallery_tmpl_${tmpl.templateId}")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(primaryColor, RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(14.dp)
                                            .background(secondaryColor, RoundedCornerShape(4.dp))
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = tmpl.displayName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = tmpl.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (isDefault) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Active Default", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            } else if (!isUnlocked) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.errorContainer
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.error)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Requires $requiredPlan", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Visual Mockup Preview of Template layout
                        TemplateMockupCanvas(
                            tmpl = tmpl,
                            primaryColor = primaryColor,
                            secondaryColor = secondaryColor
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        if (!isDefault) {
                            if (isUnlocked) {
                                Button(
                                    onClick = {
                                        viewModel.updateProfile(profile.copy(defaultTemplateId = tmpl.templateId))
                                    },
                                    modifier = Modifier.fillMaxWidth().testTag("set_default_tmpl_${tmpl.templateId}")
                                ) {
                                    Text("Set as Default Template")
                                }
                            } else {
                                OutlinedButton(
                                    onClick = { onNavigateToLicense?.invoke() },
                                    modifier = Modifier.fillMaxWidth().testTag("unlock_tmpl_${tmpl.templateId}")
                                ) {
                                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Upgrade to $requiredPlan to Unlock", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TemplateMockupCanvas(
    tmpl: BillTemplateType,
    primaryColor: Color,
    secondaryColor: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .background(Color(0xFFF8FAFC), RoundedCornerShape(10.dp))
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
            .padding(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            when (tmpl) {
                BillTemplateType.EXECUTIVE_NAVY -> {
                    // Solid dark header
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp)
                            .background(primaryColor, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text("INVOICE • EXECUTIVE", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
                BillTemplateType.MODERN_CLEAN -> {
                    // Top accent bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .background(primaryColor, RoundedCornerShape(2.dp))
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Business Name", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = primaryColor)
                        Surface(shape = RoundedCornerShape(4.dp), color = secondaryColor) {
                            Text("INVOICE", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = primaryColor, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                        }
                    }
                }
                BillTemplateType.EMERALD_CORP -> {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.width(4.dp).height(20.dp).background(primaryColor))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("EMERALD PRO", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = primaryColor)
                    }
                }
                BillTemplateType.MINIMALIST_SLATE -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("TAX INVOICE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = primaryColor)
                        Text("#INV-2026", fontSize = 9.sp, color = Color.Gray)
                    }
                    HorizontalDivider(color = Color(0xFFCBD5E1), modifier = Modifier.padding(vertical = 4.dp))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Simulated Table Rows
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(secondaryColor.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 3.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Item Description", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = primaryColor)
                Text("Total", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = primaryColor)
            }

            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Professional Services", fontSize = 8.sp, color = Color.DarkGray)
                Text("$1,200.00", fontSize = 8.sp, fontWeight = FontWeight.SemiBold, color = Color.DarkGray)
            }

            Spacer(modifier = Modifier.weight(1f))

            // Simulated Total badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Surface(shape = RoundedCornerShape(4.dp), color = secondaryColor) {
                    Text("Total Due: $1,200.00", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = primaryColor, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
            }
        }
    }
}
