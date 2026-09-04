package com.mogralabs.mogra.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mogralabs.mogra.identifier.IdentifierViewModel
import com.mogralabs.mogra.identifier.IdentifierViewModel.SaTab
import com.mogralabs.mogra.identifier.IdentifierViewModel.Step
import com.mogralabs.mogra.identifier.Sa
import com.mogralabs.mogra.ui.theme.Mogra
import com.mogralabs.mogra.ui.theme.Tiro
import kotlin.math.roundToInt

private val Card = RoundedCornerShape(4.dp)
private const val KEY_W = 46
private const val BLACK_W = 30

/**
 * The Raag Identifier, all four steps. One view model holds the flow, so backing out of
 * Record and changing Sa does not throw away a recording.
 */
@Composable
fun IdentifierFlow(onLeave: () -> Unit) {
    val vm: IdentifierViewModel = viewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var pendingRecord = remember { arrayOf(false) }
    val askMic = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted && pendingRecord[0]) vm.toggleRecording()
        pendingRecord[0] = false
    }
    fun withMic(action: () -> Unit) {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) action() else { pendingRecord[0] = true; askMic.launch(Manifest.permission.RECORD_AUDIO) }
    }

    when (state.step) {
        Step.SET_SA -> SetSaScreen(state, vm, onBack = onLeave, onHum = { withMic { vm.listenForSa() } })
        Step.RECORD -> RecordScreen(state, vm, onRecord = { withMic { vm.toggleRecording() } })
        Step.ANALYSING -> AnalysingScreen(state, onBack = vm::cancelAnalysis)
        Step.RESULT -> ResultScreen(state, vm)
    }
}

// ------------------------------------------------------------------ shared furniture

@Composable
private fun FlowHeader(title: String, step: String?, onBack: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(44.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Box(Modifier.size(44.dp).clickable(onClick = onBack), contentAlignment = Alignment.CenterStart) {
            BackIcon(Mogra.Cream.copy(alpha = 0.70f))
        }
        MicroLabel(title, color = Mogra.TextMuted)
        Box(Modifier.size(44.dp), contentAlignment = Alignment.CenterEnd) {
            if (step != null) {
                Text(step, style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                    color = Mogra.Cream.copy(alpha = 0.28f))
            }
        }
    }
}

@Composable
private fun PrimaryButton(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth().height(54.dp).clip(Card)
            .background(if (enabled) Mogra.Crimson else Mogra.Cream.copy(alpha = 0.10f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = if (enabled) Color(0xFFFFF1F3) else Mogra.TextFaint)
    }
}

@Composable
private fun ErrorLine(message: String?) {
    if (message == null) return
    Text(message, style = MaterialTheme.typography.bodyMedium, color = Mogra.CrimsonSoft,
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp), textAlign = TextAlign.Center)
}

// ------------------------------------------------------------------ 1. set Sa

@Composable
private fun SetSaScreen(
    state: IdentifierViewModel.State,
    vm: IdentifierViewModel,
    onBack: () -> Unit,
    onHum: () -> Unit,
) = MograScreen {
    FlowHeader("Raag Identifier", "1 / 2", onBack)

    Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
        Spacer(Modifier.height(22.dp))
        Text("Where is your Sa?", style = MaterialTheme.typography.displayMedium, color = Mogra.TextPrimary)
        Spacer(Modifier.height(10.dp))
        Text("Needed for better accuracy!", style = MaterialTheme.typography.bodyLarge,
            color = Mogra.Cream.copy(alpha = 0.48f))

        Spacer(Modifier.height(24.dp))
        Tabs(state.saTab, vm::selectTab)

        Spacer(Modifier.height(24.dp))
        when (state.saTab) {
            SaTab.KEYBOARD -> Keyboard(state.keyboardMidi, vm::selectMidi)
            SaTab.HUM -> HumPanel(state, onHum)
            SaTab.HZ -> HzPanel(state, vm::setSaHz)
        }

        Spacer(Modifier.height(24.dp))
        SaCard(state.saHz, vm::playSa)
        ErrorLine(state.error)
        Spacer(Modifier.height(28.dp))
    }

    PrimaryButton("Next: record", onClick = vm::confirmSa)
    Spacer(Modifier.height(12.dp))
    Text("Your Sa is remembered for next time.", style = MaterialTheme.typography.bodyMedium,
        color = Mogra.TextMuted, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
}

@Composable
private fun Tabs(selected: SaTab, onSelect: (SaTab) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(Card)
            .background(Mogra.Cream.copy(alpha = 0.045f))
            .border(1.dp, Mogra.Hairline, Card).padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        listOf(SaTab.HUM to "Hum it", SaTab.KEYBOARD to "Pick a note", SaTab.HZ to "Enter Hz")
            .forEach { (tab, label) ->
                val on = tab == selected
                Box(
                    Modifier.weight(1f).height(42.dp).clip(RoundedCornerShape(3.dp))
                        .background(if (on) Mogra.Crimson.copy(alpha = 0.20f) else Color.Transparent)
                        .border(1.dp, if (on) Mogra.Crimson.copy(alpha = 0.36f) else Color.Transparent,
                            RoundedCornerShape(3.dp))
                        .clickable { onSelect(tab) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(label, style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium),
                        color = if (on) Mogra.TextPrimary else Mogra.Cream.copy(alpha = 0.45f))
                }
            }
    }
}

/**
 * One octave on screen, dragged sideways through A2 to E4.
 *
 * The strip is wider than the viewport and scrolls; it opens on C3 so the common male Sa is
 * under the thumb without dragging, and A2 to E4 covers everyone else in both directions.
 */
@Composable
private fun Keyboard(selected: Int, onSelect: (Int) -> Unit) {
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
                        Modifier.width((KEY_W - 1).dp).height(124.dp)
                            .padding(end = 1.dp)
                            .clip(RoundedCornerShape(bottomStart = 3.dp, bottomEnd = 3.dp))
                            .background(if (on) Mogra.Crimson else Color(0xFF211A1C))
                            .border(1.dp, if (on) Mogra.Crimson else Mogra.Hairline,
                                RoundedCornerShape(bottomStart = 3.dp, bottomEnd = 3.dp))
                            .clickable { onSelect(midi) },
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
                    Modifier
                        .offset(x = ((below + 1) * KEY_W - BLACK_W / 2).dp)
                        .width(BLACK_W.dp).height(78.dp)
                        .clip(RoundedCornerShape(bottomStart = 3.dp, bottomEnd = 3.dp))
                        .background(if (on) Mogra.Crimson else Color(0xFF0C090B))
                        .border(1.dp, if (on) Mogra.Crimson else Mogra.Cream.copy(alpha = 0.14f),
                            RoundedCornerShape(bottomStart = 3.dp, bottomEnd = 3.dp))
                        .clickable { onSelect(midi) },
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
        // edge fades, so it reads as a strip that continues rather than a keyboard that ends
        Box(Modifier.fillMaxSize()) {
            Box(Modifier.width(26.dp).height(124.dp).align(Alignment.CenterStart).background(
                Brush.horizontalGradient(listOf(Mogra.Ink, Color.Transparent))))
            Box(Modifier.width(26.dp).height(124.dp).align(Alignment.CenterEnd).background(
                Brush.horizontalGradient(listOf(Color.Transparent, Mogra.Ink))))
        }
    }
    Spacer(Modifier.height(14.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically) {
        Text("A2", style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
            color = Mogra.Cream.copy(alpha = 0.34f))
        Text("Drag the keys sideways", style = MaterialTheme.typography.bodyMedium,
            color = Mogra.Cream.copy(alpha = 0.46f))
        Text("E4", style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
            color = Mogra.Cream.copy(alpha = 0.34f))
    }
}

@Composable
private fun HumPanel(state: IdentifierViewModel.State, onHum: () -> Unit) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            if (state.listening) "Listening — hold one steady note."
            else "Hold one steady note.",
            style = MaterialTheme.typography.bodyLarge, color = Mogra.Cream.copy(alpha = 0.48f))
        Spacer(Modifier.height(18.dp))
        Box(
            Modifier.size(148.dp).clip(RoundedCornerShape(74.dp))
                .background(if (state.listening) Mogra.Crimson.copy(alpha = 0.18f) else Mogra.CrimsonTint)
                .border(1.dp, Mogra.Crimson.copy(alpha = if (state.listening) 0.55f else 0.32f),
                    RoundedCornerShape(74.dp))
                .clickable(onClick = onHum),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    state.heardHz?.let { "%.1f".format(it) } ?: if (state.listening) "…" else "Hum",
                    style = MaterialTheme.typography.displayMedium.copy(fontSize = 30.sp),
                    color = Mogra.TextPrimary)
                if (state.heardHz != null) {
                    Text("Hz", style = MaterialTheme.typography.bodyMedium, color = Mogra.TextMuted)
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Text(
            state.heardHz?.let { Sa.describe(it) } ?: "Tap, then hold Sa for about five seconds.",
            style = MaterialTheme.typography.bodyMedium, color = Mogra.TextSecondary)
    }
}

@Composable
private fun HzPanel(state: IdentifierViewModel.State, onSet: (Double) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Text("Nudge to the exact frequency.", style = MaterialTheme.typography.bodyLarge,
            color = Mogra.Cream.copy(alpha = 0.48f))
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically) {
            listOf(-1.0, -0.1, 0.1, 1.0).forEach { delta ->
                Box(
                    Modifier.weight(1f).height(48.dp).clip(Card)
                        .border(1.dp, Mogra.Cream.copy(alpha = 0.12f), Card)
                        .clickable { onSet(state.saHz + delta) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(if (delta > 0) "+$delta" else "$delta",
                        style = MaterialTheme.typography.bodyLarge, color = Mogra.TextSecondary)
                }
            }
        }
    }
}

@Composable
private fun SaCard(saHz: Double, onPlay: () -> Unit) {
    val (midi, _) = Sa.nearest(saHz)
    Row(
        Modifier.fillMaxWidth().clip(Card).background(Mogra.SurfaceTint)
            .border(1.dp, Mogra.Hairline, Card).padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            MicroLabel("Sa", color = Mogra.TextFaint)
            Spacer(Modifier.height(5.dp))
            Text(Sa.fullNameOf(midi), fontFamily = Tiro, fontSize = 30.sp, color = Mogra.TextPrimary)
            Spacer(Modifier.height(5.dp))
            Text(
                listOfNotNull(Sa.pegName(midi), "%.2f Hz".format(saHz)).joinToString(" · "),
                style = MaterialTheme.typography.bodyLarge, color = Mogra.Cream.copy(alpha = 0.45f))
        }
        Box(
            Modifier.height(44.dp).clip(Card)
                .border(1.dp, Mogra.Crimson.copy(alpha = 0.42f), Card)
                .clickable(onClick = onPlay).padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("▶  Play Sa", style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.SemiBold), color = Mogra.CrimsonSoft)
        }
    }
}

// ------------------------------------------------------------------ 2. record

@Composable
private fun RecordScreen(
    state: IdentifierViewModel.State,
    vm: IdentifierViewModel,
    onRecord: () -> Unit,
) = MograScreen {
    FlowHeader("Raag Identifier", "2 / 2", onBack = vm::changeSa)

    Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
        Spacer(Modifier.height(22.dp))
        Row(
            Modifier.fillMaxWidth().height(44.dp).clip(Card).background(Mogra.SurfaceTint)
                .border(1.dp, Mogra.Hairline, Card).padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                MicroLabel("Sa", color = Mogra.TextFaint)
                Text(Sa.fullNameOf(Sa.nearest(state.saHz).first),
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                    color = Mogra.TextPrimary)
                Text("%.2f Hz".format(state.saHz), style = MaterialTheme.typography.bodyLarge,
                    color = Mogra.TextMuted)
            }
            Text("Change", style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.SemiBold), color = Mogra.CrimsonSoft,
                modifier = Modifier.clickable(onClick = vm::changeSa))
        }

        Spacer(Modifier.height(24.dp))
        Text("Sing, hum or play", style = MaterialTheme.typography.displayMedium, color = Mogra.TextPrimary)
        Spacer(Modifier.height(9.dp))
        Text("One voice or instrument, close to the phone. A tanpura or tabla in the background is fine.",
            style = MaterialTheme.typography.bodyLarge, color = Mogra.Cream.copy(alpha = 0.48f))

        Spacer(Modifier.height(28.dp))
        LevelMeter(state.level, state.recording)

        Spacer(Modifier.height(24.dp))
        Text(
            "%d:%02d".format((state.elapsed / 60).toInt(), (state.elapsed % 60).toInt()),
            fontFamily = Tiro, fontSize = 44.sp, color = Mogra.TextPrimary,
            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Box(Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp))
            .background(Mogra.Cream.copy(alpha = 0.09f))) {
            Box(Modifier
                .fillMaxWidth((state.elapsed / IdentifierViewModel.MIN_SECONDS).coerceIn(0.0, 1.0).toFloat())
                .height(3.dp).background(Mogra.Crimson))
        }
        Spacer(Modifier.height(10.dp))
        Text(
            if (state.canAnalyse) "Past the 20-second minimum. Longer is better — an alap or a full bandish gives it much more to go on."
            else "At least 20 seconds.",
            style = MaterialTheme.typography.bodyLarge, color = Mogra.Cream.copy(alpha = 0.52f))
        ErrorLine(state.error)
        Spacer(Modifier.height(28.dp))
    }

    PrimaryButton(
        when {
            state.recording -> "Stop"
            state.canAnalyse -> "Identify the raag"
            else -> "Record"
        },
        onClick = { if (state.recording || !state.canAnalyse) onRecord() else vm.analyse() },
    )
}

@Composable
private fun LevelMeter(level: Float, live: Boolean) {
    Row(
        Modifier.fillMaxWidth().height(118.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        val bars = 52
        repeat(bars) { i ->
            // a fixed profile scaled by the live level: the shape is decoration, the height
            // is the only thing carrying information
            val shape = 0.35f + 0.65f * kotlin.math.abs(kotlin.math.sin(i * 1.7f))
            val h = if (live) (12f + 100f * level * shape).coerceIn(4f, 116f) else 4f
            Box(Modifier.weight(1f).height(h.dp).clip(RoundedCornerShape(2.dp))
                .background(Mogra.Crimson.copy(alpha = if (live) 0.92f else 0.18f)))
        }
    }
}

// ------------------------------------------------------------------ 3. analysing

@Composable
private fun AnalysingScreen(state: IdentifierViewModel.State, onBack: () -> Unit) = MograScreen {
    FlowHeader("Raag Identifier", null, onBack)
    val spin = rememberInfiniteTransition(label = "analysing")
    val phase by spin.animateFloat(
        0f, 1f, infiniteRepeatable(tween(1400, easing = LinearEasing)), label = "phase")

    Column(
        Modifier.fillMaxWidth().weight(1f),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically) {
            repeat(5) { i ->
                val t = ((phase * 5f) - i + 5f) % 5f
                val h = 14f + 34f * (1f - kotlin.math.min(1f, kotlin.math.abs(t - 1f)))
                Box(Modifier.width(4.dp).height(h.dp).clip(RoundedCornerShape(2.dp))
                    .background(Mogra.Crimson.copy(alpha = 0.85f)))
            }
        }
        Spacer(Modifier.height(26.dp))
        Text("Listening to it properly", style = MaterialTheme.typography.displayMedium,
            color = Mogra.TextPrimary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Text(
            if (state.windows > 0) "${state.windows} window${if (state.windows == 1) "" else "s"} of 20 seconds."
            else "Setting up the filter bank.",
            style = MaterialTheme.typography.bodyLarge, color = Mogra.TextSecondary)
        Spacer(Modifier.height(6.dp))
        Text("Nothing is uploaded — this runs on your phone.",
            style = MaterialTheme.typography.bodyMedium, color = Mogra.Cream.copy(alpha = 0.46f))
    }
}

// ------------------------------------------------------------------ 4. result

@Composable
private fun ResultScreen(state: IdentifierViewModel.State, vm: IdentifierViewModel) = MograScreen {
    FlowHeader("Result", null, onBack = vm::recordAgain)

    Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
        Spacer(Modifier.height(22.dp))
        MicroLabel("Most likely", color = Mogra.TextFaint)
        Spacer(Modifier.height(10.dp))

        state.predictions.forEachIndexed { i, p ->
            val lead = i == 0
            Column(
                Modifier.fillMaxWidth().padding(bottom = 10.dp).clip(Card)
                    .background(if (lead) Mogra.CrimsonTint else Color.Transparent)
                    .border(1.dp, if (lead) Mogra.CrimsonEdge else Mogra.Cream.copy(alpha = 0.085f), Card)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(p.raag, fontFamily = Tiro, fontSize = if (lead) 30.sp else 20.sp,
                        color = if (lead) Mogra.TextPrimary else Mogra.Cream.copy(alpha = 0.72f))
                    Text("${(p.probability * 100).roundToInt()}%",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (lead) Mogra.CrimsonSoft else Mogra.TextMuted)
                }
                Spacer(Modifier.height(9.dp))
                Box(Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp))
                    .background(Mogra.Cream.copy(alpha = 0.09f))) {
                    Box(Modifier.fillMaxWidth(p.probability.toFloat().coerceIn(0f, 1f))
                        .height(3.dp).background(
                            if (lead) Mogra.Crimson else Mogra.Crimson.copy(alpha = 0.45f)))
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        Column(
            Modifier.fillMaxWidth().clip(Card).background(Mogra.SurfaceTint)
                .border(1.dp, Mogra.Hairline, Card).padding(16.dp),
        ) {
            MicroLabel("How much to trust this", color = Mogra.TextFaint)
            Spacer(Modifier.height(9.dp))
            Text(
                "The algorithm knows only 50 raags and will still guess if yours is not one of " +
                    "them. On professional recordings, the top guess is right about half the time, " +
                    "and the true raag is somewhere in these five about four times in five. " +
                    "Expect worse with casual humming.",
                style = MaterialTheme.typography.bodyLarge,
                color = Mogra.Cream.copy(alpha = 0.56f))
        }
        Spacer(Modifier.height(28.dp))
    }

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.weight(1f)) { PrimaryButton("Record again", onClick = vm::recordAgain) }
        Box(
            Modifier.weight(1f).height(54.dp).clip(Card)
                .border(1.dp, Mogra.Cream.copy(alpha = 0.16f), Card)
                .clickable(onClick = vm::changeSa),
            contentAlignment = Alignment.Center,
        ) {
            Text("Change Sa", style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                color = Mogra.Cream.copy(alpha = 0.78f))
        }
    }
    Spacer(Modifier.height(12.dp))
    Text(
        "Sa %s · %.0f s · %d window%s · 50 raags · %.1f s".format(
            Sa.fullNameOf(Sa.nearest(state.saHz).first), state.analysedSeconds,
            state.windows, if (state.windows == 1) "" else "s", state.elapsedMillis / 1000.0),
        style = MaterialTheme.typography.bodyMedium, color = Mogra.Cream.copy(alpha = 0.45f),
        modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
}
