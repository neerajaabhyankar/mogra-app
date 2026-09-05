package com.mogralabs.mogra.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mogralabs.mogra.R
import com.mogralabs.mogra.identifier.Sa
import com.mogralabs.mogra.ui.theme.Mogra
import java.util.Locale

private val Card = RoundedCornerShape(4.dp)
private const val KEY_W = 46
private const val BLACK_W = 30

/** Frequencies are read as instrument numbers, so they stay Latin in every language. */
fun hzText(hz: Double) = String.format(Locale.ROOT, "%.2f Hz", hz)

/**
 * The Sa strip: which tonic is in force, and a way to change it.
 *
 * Shared so the Record screen and both raagfinders say it identically — the raagfinders use
 * Sa only to sound notes out, but a second way of showing the same fact would be a second
 * thing to learn.
 */
@Composable
fun SaBar(saHz: Double, onChange: () -> Unit) {
    val changeSa = stringResource(R.string.cd_change_sa)
    Row(
        Modifier.fillMaxWidth().height(44.dp).clip(Card).background(Mogra.SurfaceTint)
            .border(1.dp, Mogra.Hairline, Card).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            MicroLabel(stringResource(R.string.sa_label), color = Mogra.TextFaint)
            Text(Sa.fullNameOf(Sa.nearest(saHz).first),
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                color = Mogra.TextPrimary)
            Text(hzText(saHz), style = MaterialTheme.typography.bodyLarge, color = Mogra.TextMuted)
        }
        Text(
            stringResource(R.string.rec_change_sa),
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            color = Mogra.CrimsonSoft,
            modifier = Modifier.clickable(onClick = onChange)
                .semantics { contentDescription = changeSa; role = Role.Button },
        )
    }
}

/**
 * One octave on screen, dragged sideways through A2 to E4. It opens on C3 so a common male
 * Sa is under the thumb without dragging.
 */
@Composable
fun SaKeyboard(selected: Int, onSelect: (Int) -> Unit) {
    val scroll = rememberScrollState()
    val density = LocalDensity.current
    val whites = remember { (Sa.LOWEST_MIDI..Sa.HIGHEST_MIDI).filter { !Sa.isAccidental(it) } }
    val blacks = remember { (Sa.LOWEST_MIDI..Sa.HIGHEST_MIDI).filter { Sa.isAccidental(it) } }

    LaunchedEffect(Unit) {
        val c3 = whites.indexOfFirst { it >= 48 }.coerceAtLeast(0)
        scroll.scrollTo(with(density) { (c3 * KEY_W).dp.roundToPx() })
    }

    Box(Modifier.fillMaxWidth().height(124.dp)) {
        Box(Modifier.horizontalScroll(scroll)) {
            Row {
                whites.forEach { midi ->
                    val on = midi == selected
                    Box(
                        Modifier.width((KEY_W - 1).dp).height(124.dp).padding(end = 1.dp)
                            .clip(RoundedCornerShape(bottomStart = 3.dp, bottomEnd = 3.dp))
                            .background(if (on) Mogra.Crimson else Color(0xFF211A1C))
                            .border(1.dp, if (on) Mogra.Crimson else Mogra.Hairline,
                                RoundedCornerShape(bottomStart = 3.dp, bottomEnd = 3.dp))
                            .clickable { onSelect(midi) }
                            .semantics {
                                contentDescription = Sa.fullNameOf(midi)
                                role = Role.RadioButton
                                this.selected = on
                            },
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        Text(
                            if (Sa.nameOf(midi) == "C") Sa.fullNameOf(midi) else Sa.nameOf(midi),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                            color = if (on) Color(0xFFFFF1F3) else Mogra.Cream.copy(alpha = 0.40f),
                            modifier = Modifier.padding(bottom = 9.dp),
                        )
                    }
                }
            }
            blacks.forEach { midi ->
                val below = whites.indexOfLast { it < midi }
                val on = midi == selected
                Box(
                    Modifier.offset(x = ((below + 1) * KEY_W - BLACK_W / 2).dp)
                        .width(BLACK_W.dp).height(78.dp)
                        .clip(RoundedCornerShape(bottomStart = 3.dp, bottomEnd = 3.dp))
                        .background(if (on) Mogra.Crimson else Color(0xFF0C090B))
                        .border(1.dp, if (on) Mogra.Crimson else Mogra.Cream.copy(alpha = 0.14f),
                            RoundedCornerShape(bottomStart = 3.dp, bottomEnd = 3.dp))
                        .clickable { onSelect(midi) }
                        .semantics {
                            contentDescription = Sa.fullNameOf(midi)
                            role = Role.RadioButton
                            this.selected = on
                        },
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    if (on) {
                        Text(Sa.nameOf(midi), style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp, letterSpacing = 0.sp),
                            color = Color(0xFFFFF1F3), modifier = Modifier.padding(bottom = 8.dp))
                    }
                }
            }
        }
        Box(Modifier.fillMaxSize()) {
            Box(Modifier.width(26.dp).height(124.dp).align(Alignment.CenterStart).background(
                Brush.horizontalGradient(listOf(Mogra.Ink, Color.Transparent))))
            Box(Modifier.width(26.dp).height(124.dp).align(Alignment.CenterEnd).background(
                Brush.horizontalGradient(listOf(Color.Transparent, Mogra.Ink))))
        }
    }
    Spacer(Modifier.height(12.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically) {
        Text("A2", style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
            color = Mogra.Cream.copy(alpha = 0.34f))
        Text(stringResource(R.string.sa_keyboard_hint), style = MaterialTheme.typography.bodyMedium,
            color = Mogra.Cream.copy(alpha = 0.46f))
        Text("E4", style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
            color = Mogra.Cream.copy(alpha = 0.34f))
    }
}

