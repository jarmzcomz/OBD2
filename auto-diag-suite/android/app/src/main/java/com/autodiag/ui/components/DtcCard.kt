package com.autodiag.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autodiag.model.DtcCode
import com.autodiag.model.DtcSeverity

@Composable
fun DtcCard(dtc: DtcCode, modifier: Modifier = Modifier) {
    val severityColor = when (dtc.severity) {
        DtcSeverity.HIGH -> Color(0xFFEF4444)
        DtcSeverity.MEDIUM -> Color(0xFFF59E0B)
        DtcSeverity.LOW -> Color(0xFF22C55E)
        DtcSeverity.UNKNOWN -> Color(0xFF7C8AA5)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF131C2E)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "DTC Warning",
                tint = severityColor,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = dtc.code,
                    color = severityColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                if (dtc.description.isNotEmpty()) {
                    Text(
                        text = dtc.description,
                        color = Color(0xFFE5EAF2),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                val severidad = when (dtc.severity) {
                    DtcSeverity.HIGH -> "Severidad alta"
                    DtcSeverity.MEDIUM -> "Severidad media"
                    DtcSeverity.LOW -> "Severidad baja"
                    DtcSeverity.UNKNOWN -> "Severidad desconocida"
                }
                Text(
                    text = severidad,
                    color = Color(0xFF7C8AA5),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
