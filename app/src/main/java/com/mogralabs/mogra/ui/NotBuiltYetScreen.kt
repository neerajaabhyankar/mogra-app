package com.mogralabs.mogra.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mogralabs.mogra.R
import com.mogralabs.mogra.ui.theme.Mogra

/** A placeholder with a real back affordance, so navigation can be tested before the tools exist. */
@Composable
fun NotBuiltYetScreen(title: String, onBack: () -> Unit) = MograScreen {
    Row(
        Modifier.fillMaxWidth().height(44.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Box(Modifier.size(44.dp).clickable(onClick = onBack), contentAlignment = Alignment.CenterStart) {
            BackIcon(Mogra.Cream.copy(alpha = 0.70f))
        }
        MicroLabel(title, color = Mogra.TextMuted)
        Spacer(Modifier.size(44.dp))
    }

    Column(
        Modifier.fillMaxWidth().weight(1f),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.wip_title),
            style = MaterialTheme.typography.displayMedium,
            color = Mogra.TextPrimary,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.wip_body),
            style = MaterialTheme.typography.bodyLarge,
            color = Mogra.TextSecondary,
        )
    }
}
