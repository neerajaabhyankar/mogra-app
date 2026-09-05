package com.mogralabs.mogra.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.sp
import com.mogralabs.mogra.R
import androidx.compose.ui.unit.dp
import com.mogralabs.mogra.ui.theme.Mogra

/**
 * Every screen is the same shape: ink to the edges, content inset 24dp, and the system bars
 * kept clear so nothing lands under the clock or the gesture pill.
 */
@Composable
fun MograScreen(content: @Composable ColumnScope.() -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Mogra.Ink)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(PaddingValues(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 20.dp)),
            content = content,
        )
    }
}

@Composable
fun FlowHeader(title: String, step: String?, onBack: () -> Unit) {
    val back = stringResource(R.string.cd_back)
    Row(
        Modifier.fillMaxWidth().height(44.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Box(
            Modifier.size(44.dp).clickable(onClick = onBack)
                .semantics { contentDescription = back; role = Role.Button },
            contentAlignment = Alignment.CenterStart,
        ) { BackIcon(Mogra.Cream.copy(alpha = 0.70f)) }
        MicroLabel(title, color = Mogra.TextMuted)
        Box(Modifier.size(44.dp), contentAlignment = Alignment.CenterEnd) {
            if (step != null) {
                Text(step, style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                    color = Mogra.Cream.copy(alpha = 0.28f))
            }
        }
    }
}

/** The small-caps section label used throughout: TOOLS, SA, MOST LIKELY. */
@Composable
fun MicroLabel(text: String, modifier: Modifier = Modifier, color: androidx.compose.ui.graphics.Color = Mogra.TextFaint) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = modifier,
    )
}

@Composable
fun Hairline(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Mogra.Hairline)
    )
}
