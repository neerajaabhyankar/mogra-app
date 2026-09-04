package com.mogralabs.mogra.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.booleanResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mogralabs.mogra.R
import com.mogralabs.mogra.digits
import com.mogralabs.mogra.identifier.IdentifierViewModel
import com.mogralabs.mogra.identifier.IdentifierViewModel.SaTab
import com.mogralabs.mogra.identifier.IdentifierViewModel.Step
import com.mogralabs.mogra.identifier.Sa
import com.mogralabs.mogra.ui.theme.Mogra
import com.mogralabs.mogra.ui.theme.Tiro
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

private val Card = RoundedCornerShape(4.dp)
private const val KEY_W = 46
private const val BLACK_W = 30

/**
 * The Raag Identifier, all four steps. One view model holds the flow, so backing out of
 * Record to change Sa does not throw away a recording.
 */
@Composable
fun IdentifierFlow(onLeave: () -> Unit) {
    val vm: IdentifierViewModel = viewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val pending = remember { arrayOf<(() -> Unit)?>(null) }
    val askMic = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) pending[0]?.invoke()
        pending[0] = null
    }
    fun withMic(action: () -> Unit) {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) action() else { pending[0] = action; askMic.launch(Manifest.permission.RECORD_AUDIO) }
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

/** The one red bar. It only ever means "go on to the next thing", never "record". */
@Composable
private fun PrimaryButton(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth().height(54.dp).clip(Card)
            .background(if (enabled) Mogra.Crimson else Mogra.Cream.copy(alpha = 0.10f))
            .clickable(enabled = enabled, onClick = onClick)
            .semantics { role = Role.Button },
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = if (enabled) Color(0xFFFFF1F3) else Mogra.TextFaint)
    }
}

@Composable
private fun ErrorLine(message: Int?) {
    if (message == null) return
    Text(stringResource(message), style = MaterialTheme.typography.bodyMedium, color = Mogra.CrimsonSoft,
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp), textAlign = TextAlign.Center)
}

/**
 * The record control, shared by "hum your Sa" and "record the raag" so the two screens teach
 * the same gesture.
 *
 * Idle it is an outlined circle with a word in it. Live it is solid crimson with a white
 * square — the universal stop symbol. No ring: the timer underneath keeps time, and a ring
 * that fills implies a length the recording is supposed to reach.
 */
@Composable
private fun RecordCircle(
    active: Boolean,
    level: Float,
    idleLabel: String,
    diameter: Int = 176,
    spokenIdle: String = idleLabel,
    onClick: () -> Unit,
) {
    val stopping = stringResource(R.string.cd_record_stop)
    val swell by animateFloatAsState(if (active) level.coerceIn(0f, 1f) else 0f, label = "level")
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val size = (diameter + 14 * swell).toInt()
        Box(
            Modifier.size(size.dp).clip(CircleShape)
                .background(if (active) Mogra.Crimson else Mogra.Crimson.copy(alpha = 0.10f))
                .border(1.dp, Mogra.Crimson.copy(alpha = if (active) 0f else 0.36f), CircleShape)
                .clickable(onClick = onClick)
                .semantics {
                    contentDescription = if (active) stopping else spokenIdle
                    role = Role.Button
                },
            contentAlignment = Alignment.Center,
        ) {
            if (active) {
                Box(Modifier.size(46.dp).clip(RoundedCornerShape(5.dp)).background(Color.White))
            } else {
                Text(idleLabel, style = MaterialTheme.typography.titleMedium, color = Mogra.CrimsonSoft)
            }
        }
    }
}

// ------------------------------------------------------------------ 1. set Sa

@Composable
private fun SetSaScreen(
    state: IdentifierViewModel.State,
    vm: IdentifierViewModel,
    onBack: () -> Unit,
    onHum: () -> Unit,
) = MograScreen {
    FlowHeader(stringResource(R.string.tool_identifier_title), stringResource(R.string.step_of, digits(1), digits(2)), onBack)

    Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
        Spacer(Modifier.height(18.dp))
        Text(stringResource(R.string.sa_title), style = MaterialTheme.typography.displayMedium, color = Mogra.TextPrimary)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.sa_subtitle), style = MaterialTheme.typography.bodyLarge,
            color = Mogra.Cream.copy(alpha = 0.48f))

        Spacer(Modifier.height(20.dp))
        Tabs(state.saTab, vm::selectTab)

        Spacer(Modifier.height(20.dp))
        when (state.saTab) {
            SaTab.KEYBOARD -> Keyboard(state.keyboardMidi, vm::selectMidi)
            SaTab.HUM -> HumPanel(state, onHum)
            SaTab.HZ -> HzPanel(state, vm::setSaHz)
        }

        Spacer(Modifier.height(20.dp))
        SaCard(state.saHz, vm::playSa)
        ErrorLine(state.error)
        Spacer(Modifier.height(20.dp))
    }

    PrimaryButton(stringResource(R.string.sa_next), onClick = vm::confirmSa)
    Spacer(Modifier.height(10.dp))
    Text(stringResource(R.string.sa_remembered), style = MaterialTheme.typography.bodyMedium,
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
        listOf(
            SaTab.HUM to stringResource(R.string.sa_tab_hum),
            SaTab.KEYBOARD to stringResource(R.string.sa_tab_note),
            SaTab.HZ to stringResource(R.string.sa_tab_hz),
        ).forEach { (tab, label) ->
                val on = tab == selected
                Box(
                    Modifier.weight(1f).height(42.dp).clip(RoundedCornerShape(3.dp))
                        .background(if (on) Mogra.Crimson.copy(alpha = 0.20f) else Color.Transparent)
                        .border(1.dp, if (on) Mogra.Crimson.copy(alpha = 0.36f) else Color.Transparent,
                            RoundedCornerShape(3.dp))
                        .clickable { onSelect(tab) }
                        .semantics { role = Role.Tab; this.selected = on },
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
 * One octave on screen, dragged sideways through A2 to E4. It opens on C3 so a common male
 * Sa is under the thumb without dragging.
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

@Composable
private fun HumPanel(state: IdentifierViewModel.State, onHum: () -> Unit) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        RecordCircle(
            active = state.listening,
            level = state.level,
            idleLabel = stringResource(R.string.sa_hum_idle),
            diameter = 156,
            spokenIdle = stringResource(R.string.cd_hum_sa),
            onClick = onHum,
        )
        Spacer(Modifier.height(14.dp))
        val heard = state.heardHz
        Text(
            when {
                state.listening -> stringResource(R.string.sa_hum_holding)
                heard != null -> heardLine(heard)
                else -> stringResource(R.string.sa_hum_prompt)
            },
            style = MaterialTheme.typography.bodyLarge, color = Mogra.TextSecondary,
            textAlign = TextAlign.Center,
        )
    }
}

/** "146.8 Hz · D3 +4 cents" — the Hz reading stays in Latin digits in every language. */
@Composable
private fun heardLine(hz: Double): String {
    val (note, cents) = Sa.describe(hz)
    val offset = cents?.let {
        " " + stringResource(if (it > 0) R.string.cents_sharp else R.string.cents_flat, digits(abs(it)))
    } ?: ""
    return "${hzText(hz)} · $note$offset"
}

/** Frequencies and the timer are read as instrument numbers, so they stay Latin. */
private fun hzText(hz: Double) = String.format(Locale.ROOT, "%.2f Hz", hz)

@Composable
private fun HzPanel(state: IdentifierViewModel.State, onSet: (Double) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.sa_hz_hint), style = MaterialTheme.typography.bodyLarge,
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
    val context = LocalContext.current
    val peg = Sa.pegName(context, midi)
    // English readers know C#3; Marathi and Hindi builds put the harmonium peg first
    val pegLeads = booleanResource(R.bool.peg_name_leads) && peg != null
    val playSa = stringResource(R.string.cd_play_sa)

    Row(
        Modifier.fillMaxWidth().clip(Card).background(Mogra.SurfaceTint)
            .border(1.dp, Mogra.Hairline, Card).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            MicroLabel(stringResource(R.string.sa_label), color = Mogra.TextFaint)
            Spacer(Modifier.height(4.dp))
            Text(
                if (pegLeads) peg!! else Sa.fullNameOf(midi),
                fontFamily = Tiro, fontSize = 28.sp, color = Mogra.TextPrimary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                listOfNotNull(if (pegLeads) Sa.fullNameOf(midi) else peg, hzText(saHz))
                    .joinToString(" · "),
                style = MaterialTheme.typography.bodyLarge, color = Mogra.Cream.copy(alpha = 0.45f))
        }
        Box(
            Modifier.height(44.dp).clip(Card)
                .border(1.dp, Mogra.Crimson.copy(alpha = 0.42f), Card)
                .clickable(onClick = onPlay)
                .semantics { contentDescription = playSa; role = Role.Button }
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("▶  " + stringResource(R.string.sa_play),
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = Mogra.CrimsonSoft)
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
    FlowHeader(stringResource(R.string.tool_identifier_title), stringResource(R.string.step_of, digits(2), digits(2)), onBack = vm::changeSa)
    val changeSa = stringResource(R.string.cd_change_sa)

    Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
        Spacer(Modifier.height(16.dp))
        Row(
            Modifier.fillMaxWidth().height(44.dp).clip(Card).background(Mogra.SurfaceTint)
                .border(1.dp, Mogra.Hairline, Card).padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                MicroLabel(stringResource(R.string.sa_label), color = Mogra.TextFaint)
                Text(Sa.fullNameOf(Sa.nearest(state.saHz).first),
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                    color = Mogra.TextPrimary)
                Text(hzText(state.saHz), style = MaterialTheme.typography.bodyLarge,
                    color = Mogra.TextMuted)
            }
            Text(stringResource(R.string.rec_change_sa), style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.SemiBold), color = Mogra.CrimsonSoft,
                modifier = Modifier.clickable(onClick = vm::changeSa)
                    .semantics { contentDescription = changeSa; role = Role.Button })
        }

        Spacer(Modifier.height(18.dp))
        Text(stringResource(R.string.rec_title), style = MaterialTheme.typography.displayMedium, color = Mogra.TextPrimary)
        Spacer(Modifier.height(6.dp))
        Text(stringResource(R.string.rec_blurb),
            style = MaterialTheme.typography.bodyLarge, color = Mogra.Cream.copy(alpha = 0.48f))

        Spacer(Modifier.height(22.dp))
        RecordCircle(
            active = state.recording,
            level = state.level,
            idleLabel = stringResource(if (state.hasTake) R.string.rec_again else R.string.rec_start),
            spokenIdle = stringResource(
                if (state.hasTake) R.string.cd_record_again else R.string.cd_record_start),
            onClick = onRecord,
        )

        Spacer(Modifier.height(16.dp))
        Text(
            String.format(Locale.ROOT, "%d:%02d",
                (state.elapsed / 60).toInt(), (state.elapsed % 60).toInt()),
            fontFamily = Tiro, fontSize = 38.sp, color = Mogra.TextPrimary,
            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(
                if (state.canAnalyse) R.string.rec_keep_going_more else R.string.rec_keep_going_min),
            style = MaterialTheme.typography.bodyLarge,
            color = Mogra.Cream.copy(alpha = 0.52f),
            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        if (state.recording) {
            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.rec_stay_open),
                style = MaterialTheme.typography.bodyMedium,
                color = Mogra.Cream.copy(alpha = 0.52f),
                modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        }
        ErrorLine(state.error)
        Spacer(Modifier.height(18.dp))
    }

    // no separate "redo": the circle already says "Again" once there is a take
    if (state.hasTake && !state.recording) {
        PrimaryButton(stringResource(R.string.rec_identify), enabled = state.canAnalyse, onClick = vm::analyse)
    }
}

// ------------------------------------------------------------------ 3. analysing

@Composable
private fun AnalysingScreen(state: IdentifierViewModel.State, onBack: () -> Unit) = MograScreen {
    FlowHeader(stringResource(R.string.tool_identifier_title), null, onBack)
    val spin = rememberInfiniteTransition(label = "analysing")
    val angle by spin.animateFloat(
        0f, 360f, infiniteRepeatable(tween(18_000, easing = LinearEasing)), label = "angle")

    Column(
        Modifier.fillMaxWidth().weight(1f),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(R.drawable.logo_mogra),
            contentDescription = null,
            modifier = Modifier.size(96.dp).rotate(angle),
        )
        Spacer(Modifier.height(26.dp))
        Text(stringResource(R.string.analysing_title), style = MaterialTheme.typography.displayMedium, color = Mogra.TextPrimary)
        Spacer(Modifier.height(10.dp))
        Text(stringResource(R.string.analysing_private),
            style = MaterialTheme.typography.bodyLarge, color = Mogra.TextSecondary)
    }
}

// ------------------------------------------------------------------ 4. result

@Composable
private fun ResultScreen(state: IdentifierViewModel.State, vm: IdentifierViewModel) = MograScreen {
    FlowHeader(stringResource(R.string.result_title), null, onBack = vm::recordAgain)

    Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
        Spacer(Modifier.height(16.dp))
        MicroLabel(stringResource(R.string.result_most_likely), color = Mogra.TextFaint)
        Spacer(Modifier.height(10.dp))

        // all five listed alike: the model's own probabilities say how confident to be, and
        // dressing the first one up says it louder than the numbers justify
        val names = stringArrayResource(R.array.raag_names)
        state.predictions.forEach { p ->
            val name = names.getOrElse(p.index) { p.raag }
            val percent = (p.probability * 100).roundToInt()
            val row = stringResource(R.string.cd_result_row, name, digits(percent))
            Column(
                Modifier.fillMaxWidth().padding(bottom = 8.dp).clip(Card)
                    .border(1.dp, Mogra.Cream.copy(alpha = 0.085f), Card)
                    .padding(horizontal = 14.dp, vertical = 11.dp)
                    .semantics(mergeDescendants = true) {
                        contentDescription = row
                    },
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(name, fontFamily = Tiro, fontSize = 21.sp,
                        color = Mogra.Cream.copy(alpha = 0.88f))
                    Text(stringResource(R.string.percent_only, digits(percent)),
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                        color = Mogra.TextMuted)
                }
                Spacer(Modifier.height(7.dp))
                Box(Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp))
                    .background(Mogra.Cream.copy(alpha = 0.09f))) {
                    Box(Modifier.fillMaxWidth(p.probability.toFloat().coerceIn(0f, 1f))
                        .height(3.dp).background(Mogra.Crimson.copy(alpha = 0.75f)))
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        Column(
            Modifier.fillMaxWidth().clip(Card).background(Mogra.SurfaceTint)
                .border(1.dp, Mogra.Hairline, Card).padding(14.dp),
        ) {
            MicroLabel(stringResource(R.string.result_trust_title), color = Mogra.TextFaint)
            Spacer(Modifier.height(7.dp))
            Text(
                stringResource(R.string.result_trust_body),
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 18.sp),
                color = Mogra.Cream.copy(alpha = 0.56f))
        }
        Spacer(Modifier.height(16.dp))
    }

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.weight(1f)) { PrimaryButton(stringResource(R.string.result_record_again), onClick = vm::recordAgain) }
        Box(
            Modifier.weight(1f).height(54.dp).clip(Card)
                .border(1.dp, Mogra.Cream.copy(alpha = 0.16f), Card)
                .clickable(onClick = vm::changeSa),
            contentAlignment = Alignment.Center,
        ) {
            Text(stringResource(R.string.result_change_sa), style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                color = Mogra.Cream.copy(alpha = 0.78f))
        }
    }
    Spacer(Modifier.height(10.dp))
    Text(
        stringResource(
            R.string.result_footer,
            Sa.fullNameOf(Sa.nearest(state.saHz).first),
            digits(state.analysedSeconds.roundToInt()), digits(state.windows)),
        style = MaterialTheme.typography.bodyMedium, color = Mogra.Cream.copy(alpha = 0.45f),
        modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
}
