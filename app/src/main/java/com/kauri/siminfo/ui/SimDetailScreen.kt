package com.kauri.siminfo.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kauri.siminfo.SimCardInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimDetailScreen(
    sim: SimCardInfo,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(sim.displayName.ifBlank { "SIM ${sim.slotIndex + 1}" }) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionCard(title = "SIM Identity") {
                InfoRow("Subscription ID", sim.subscriptionId.toString())
                InfoRow("ICCID (last 4)", sim.iccIdLast4 ?: "N/A")
                InfoRow("Slot", "SIM ${sim.slotIndex + 1}")
                InfoRow("Display Name", sim.displayName.ifBlank { "N/A" })
                InfoRow("Carrier Name", sim.carrierName.ifBlank { "N/A" })
                InfoRow("Phone Number", sim.phoneNumber ?: "N/A")
                InfoRow("Country ISO", sim.countryIso.uppercase().ifBlank { "N/A" })
                InfoRow("MCC", sim.mcc.ifBlank { "N/A" })
                InfoRow("MNC", sim.mnc.ifBlank { "N/A" })
                InfoRow("Data Roaming", if (sim.dataRoaming) "Enabled" else "Disabled", isLast = true)
            }

            SectionCard(title = "Network") {
                InfoRow("Operator Name", sim.networkOperatorName.ifBlank { "N/A" })
                InfoRow("Country ISO", sim.networkCountryIso.uppercase().ifBlank { "N/A" })
                InfoRow("Network Type", sim.networkTypeLabel)
                InfoRow("Roaming", if (sim.isNetworkRoaming) "Yes" else "No", isLast = true)
            }

            SectionCard(title = "Status") {
                InfoRow("Phone Type", sim.phoneTypeLabel)
                InfoRow("SIM State", sim.simStateLabel)
                InfoRow("Call State", sim.callStateLabel)
                InfoRow("Data State", sim.dataStateLabel, isLast = true)
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String, isLast: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f),
        )
    }
    if (!isLast) {
        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant,
        )
    }
}
