package com.nikka.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import java.awt.Desktop
import java.net.URI

@Composable
fun SettingsScreen(
    onNavigateToLicense: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 8.dp),
        ) {
            SettingsItem(
                icon = Icons.Rounded.Description,
                title = "ライセンス",
                subtitle = "使用しているライブラリのライセンス情報",
                trailingIcon = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                onClick = onNavigateToLicense,
            )
            SectionDivider(title = "クレジット")
            SettingsItem(
                icon = Icons.Rounded.Brush,
                title = "アプリアイコン",
                subtitle = "みかしぎ",
                trailingIcon = Icons.Rounded.OpenInNew,
                onClick = {
                    Desktop.getDesktop().browse(URI("https://x.com/mechashigi"))
                },
            )
        }
    }
}

@Composable
private fun SectionDivider(title: String) {
    Column {
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            color = MaterialTheme.colorScheme.outlineVariant,
        )
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 20.dp, top = 8.dp, bottom = 4.dp),
        )
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    trailingIcon: ImageVector,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            imageVector = trailingIcon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
