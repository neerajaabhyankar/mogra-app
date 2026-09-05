package com.mogralabs.mogra.ui

import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.integerResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mogralabs.mogra.Language
import com.mogralabs.mogra.R
import com.mogralabs.mogra.ui.theme.Mogra
import com.mogralabs.mogra.ui.theme.Mukta
import com.mogralabs.mogra.ui.theme.Tiro

private val CardShape = RoundedCornerShape(4.dp)

@Composable
fun HomeScreen(onOpenTool: (String) -> Unit) = MograScreen {
    // The masthead and the cards scroll; the language row stays pinned to the bottom. Without
    // this the two collide at large font scales, which is what a Pixel-sized screen does to it.
    Column(
        Modifier
            .weight(1f)
            .verticalScroll(rememberScrollState()),
    ) {
        Spacer(Modifier.height(12.dp))

        Image(
            painter = painterResource(R.drawable.logo_mogra),
            contentDescription = null,
            modifier = Modifier.size(58.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.displayLarge.copy(
                fontSize = integerResource(R.integer.wordmark_size_sp).sp,
                letterSpacing = (integerResource(R.integer.wordmark_tracking_hundredths) / 100f).sp,
            ),
            color = Mogra.TextPrimary,
            // the wordmark is one word in every language and must never break across lines
            maxLines = 1,
            softWrap = false,
        )
        Spacer(Modifier.height(9.dp))
        Text(
            text = stringResource(R.string.app_subtitle),
            style = MaterialTheme.typography.bodyMedium.copy(letterSpacing = 0.6.sp),
            color = Mogra.Cream.copy(alpha = 0.50f),
        )

        Spacer(Modifier.height(22.dp))
        Hairline()
        Spacer(Modifier.height(16.dp))
        MicroLabel(stringResource(R.string.section_tools), color = Mogra.Cream.copy(alpha = 0.32f))
        Spacer(Modifier.height(16.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ToolCard(
                title = stringResource(R.string.tool_identifier_title),
                blurb = stringResource(R.string.tool_identifier_blurb),
                icon = { WaveformIcon(Mogra.Crimson) },
                enabled = true,
                onClick = { onOpenTool(Routes.IDENTIFIER) },
            )
            ToolCard(
                title = stringResource(R.string.tool_by_notes_title),
                blurb = stringResource(R.string.tool_by_notes_blurb),
                icon = { ContourIcon(Mogra.Crimson) },
                enabled = true,
                onClick = { onOpenTool(Routes.BY_NOTES) },
            )
            ToolCard(
                title = stringResource(R.string.tool_by_name_title),
                blurb = stringResource(R.string.tool_by_name_blurb),
                icon = { SearchIcon(Mogra.Crimson) },
                enabled = true,
                onClick = { onOpenTool(Routes.BY_NAME) },
            )
        }

        Spacer(Modifier.height(16.dp))
    }

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Changing the language recreates the Activity, which is what the framework does
        // for a configuration change anyway. The chips always show each language's own
        // name, so someone who cannot read the current one can still get out of it.
        val context = LocalContext.current
        val current = Language.current(context)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                "en" to stringResource(R.string.lang_en),
                "mr" to stringResource(R.string.lang_mr),
                "hi" to stringResource(R.string.lang_hi),
            ).forEach { (tag, label) ->
                LanguageChip(label, selected = tag == current) {
                    if (tag != current) {
                        Language.choose(context, tag)
                        (context as? Activity)?.recreate()
                    }
                }
            }
        }
        Text(
            text = "0.1.0",
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.4.sp),
            color = Mogra.Cream.copy(alpha = 0.24f),
        )
    }
}

@Composable
private fun ToolCard(
    title: String,
    blurb: String,
    icon: @Composable () -> Unit,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val comingSoon = stringResource(R.string.cd_coming_soon)
    val border = if (enabled) Mogra.CrimsonEdge else Mogra.Cream.copy(alpha = 0.085f)
    val fill = if (enabled) Mogra.CrimsonTint else Color.Transparent
    val titleColor = if (enabled) Mogra.TextPrimary else Mogra.Cream.copy(alpha = 0.50f)
    val blurbColor = if (enabled) Mogra.TextSecondary else Mogra.Cream.copy(alpha = 0.30f)

    Row(
        Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(fill)
            .border(1.dp, border, CardShape)
            .clickable(enabled = enabled, onClick = onClick)
            .semantics(mergeDescendants = true) {
                contentDescription =
                    if (enabled) "$title. $blurb" else "$title. $blurb. $comingSoon"
                role = Role.Button
                if (!enabled) disabled()
            }
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        icon()
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = titleColor)
            Text(blurb, style = MaterialTheme.typography.bodyMedium, color = blurbColor)
        }
        if (enabled) {
            ChevronIcon(Mogra.Crimson)
        } else {
            Box(
                Modifier
                    .border(1.dp, Mogra.Cream.copy(alpha = 0.14f), RoundedCornerShape(3.dp))
                    .padding(horizontal = 7.dp, vertical = 5.dp)
            ) {
                Text(
                    text = stringResource(R.string.badge_soon),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, letterSpacing = 1.4.sp),
                    color = Mogra.Cream.copy(alpha = 0.34f),
                )
            }
        }
    }
}

@Composable
private fun LanguageChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(4.dp)
    Box(
        Modifier
            .clip(shape)
            .clickable(onClick = onClick)
            .background(if (selected) Mogra.Crimson.copy(alpha = 0.08f) else Color.Transparent)
            .border(
                1.dp,
                if (selected) Mogra.Crimson.copy(alpha = 0.34f) else Mogra.Cream.copy(alpha = 0.09f),
                shape,
            )
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .semantics { contentDescription = label; role = Role.RadioButton },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                fontFamily = if (selected) Mukta else Tiro,
                fontSize = if (selected) 13.sp else 15.sp,
            ),
            color = if (selected) Mogra.TextPrimary else Mogra.Cream.copy(alpha = 0.30f),
        )
    }
}
