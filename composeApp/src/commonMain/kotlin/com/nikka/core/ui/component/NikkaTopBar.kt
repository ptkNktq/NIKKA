package com.nikka.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nikka.core.ui.theme.PastelPinkLight

@Composable
fun NikkaTopBar(
    modifier: Modifier = Modifier,
    actions: @Composable () -> Unit = {},
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
            .background(
                color = PastelPinkLight,
                shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
            )
            .padding(horizontal = 20.dp, vertical = 14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "🌸",
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = "NIKKA",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    text = "にっか",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            actions()
        }
    }
}
