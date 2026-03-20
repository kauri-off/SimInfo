package com.kauri.siminfo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kauri.siminfo.SimCardInfo
import com.kauri.siminfo.ui.theme.SimInfoTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimDetailScreen(
    sim: SimCardInfo,
    onBack: () -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(sim.displayName.ifBlank { "SIM ${sim.slotIndex + 1}" })
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
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
                InfoRow("Subscription ID", sim.subscriptionId.toString(), index = 0)
                InfoRow("ICCID (last 4)", sim.iccIdLast4 ?: "N/A", index = 1)
                InfoRow("Slot", "SIM ${sim.slotIndex + 1}", index = 2)
                InfoRow("Display Name", sim.displayName.ifBlank { "N/A" }, index = 3)
                InfoRow("Carrier Name", sim.carrierName.ifBlank { "N/A" }, index = 4)
                InfoRow("Phone Number", sim.phoneNumber ?: "N/A", index = 5)
                InfoRow("Country ISO", sim.countryIso.uppercase().ifBlank { "N/A" }, index = 6)
                InfoRow("MCC", sim.mcc.ifBlank { "N/A" }, index = 7)
                InfoRow("MNC", sim.mnc.ifBlank { "N/A" }, index = 8)
                InfoRow("Data Roaming", if (sim.dataRoaming) "Enabled" else "Disabled", index = 9, isLast = true)
            }

            SectionCard(title = "Network") {
                InfoRow("Operator Name", sim.networkOperatorName.ifBlank { "N/A" }, index = 0)
                InfoRow("Country ISO", sim.networkCountryIso.uppercase().ifBlank { "N/A" }, index = 1)
                InfoRow("Network Type", sim.networkTypeLabel, index = 2)
                InfoRow("Roaming", if (sim.isNetworkRoaming) "Yes" else "No", index = 3, isLast = true)
            }

            SectionCard(title = "Status") {
                InfoRow("Phone Type", sim.phoneTypeLabel, index = 0)
                InfoRow("SIM State", sim.simStateLabel, index = 1)
                InfoRow("Call State", sim.callStateLabel, index = 2)
                InfoRow("Data State", sim.dataStateLabel, index = 3, isLast = true)
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(vertical = 12.dp, horizontal = 0.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String, index: Int, isLast: Boolean = false) {
    val isEven = index % 2 == 0
    val rowBg = if (isEven)
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    else
        androidx.compose.ui.graphics.Color.Transparent

    val shape = when {
        isLast -> RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)
        else -> RoundedCornerShape(0.dp)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(rowBg)
            .padding(horizontal = 16.dp, vertical = 10.dp),
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

@Preview(name = "SIM Detail")
@Composable
private fun SimDetailScreenPreview() {
    SimInfoTheme {
        Scaffold {
            Box(modifier = Modifier.padding(it)) {
                SimDetailScreen(sim = previewSim2, onBack = {})
            }
        }
    }
}
