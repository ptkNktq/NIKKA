package com.nikka.feature.license

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import com.mikepenz.aboutlibraries.ui.compose.m3.LibraryDefaults
import com.nikka.core.ui.theme.DarkSurface
import com.nikka.core.ui.theme.LavenderPrimary
import com.nikka.core.ui.theme.TextPrimary

@Composable
fun LicenseScreen(
    aboutLibsJson: String,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        LibrariesContainer(
            aboutLibsJson = aboutLibsJson,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            colors = LibraryDefaults.libraryColors(
                backgroundColor = MaterialTheme.colorScheme.background,
                contentColor = TextPrimary,
                badgeBackgroundColor = LavenderPrimary,
                badgeContentColor = DarkSurface,
                dialogConfirmButtonColor = LavenderPrimary,
            ),
        )
    }
}
