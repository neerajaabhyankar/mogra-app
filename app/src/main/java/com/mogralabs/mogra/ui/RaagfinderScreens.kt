package com.mogralabs.mogra.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mogralabs.mogra.R
import androidx.compose.ui.unit.TextUnit
import com.mogralabs.mogra.identifier.Audio
import com.mogralabs.mogra.identifier.Sa
import com.mogralabs.mogra.raagdb.Notation
import com.mogralabs.mogra.raagdb.Tuning
import com.mogralabs.mogra.raagdb.RaagDb
import com.mogralabs.mogra.ui.theme.Mogra
import com.mogralabs.mogra.ui.theme.Tiro
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

private val Card = RoundedCornerShape(4.dp)

/** How swars, raag names, thaats and prahars are written in the language in force. */
private class Script(
    val swars: List<String>,
    val mandra: String,
    val taara: String,
    val raagNames: List<String>,
    val thaats: Map<String, String>,
    val prahars: Map<String, String>,
) {
    fun tokens(raw: String) = Notation.parse(raw, swars)
    fun name(index: Int, fallback: String) = raagNames.getOrElse(index) { fallback }
    fun thaat(raw: String) = thaats[raw] ?: raw
    fun prahar(raw: String) = prahars[raw] ?: raw

    /** The spoken form, where a mark has to be a character rather than something drawn. */
    fun spoken(raw: String) = Notation.phrase(raw, swars, mandra, taara)
}

/** The database's own spellings, mapped to the strings the table carries. */
private val THAAT_KEYS = mapOf(
    "Bilawal" to R.string.thaat_bilawal, "Khamaj" to R.string.thaat_khamaj,
    "Kafi" to R.string.thaat_kafi, "Asawari" to R.string.thaat_asawari,
    "Bhairavi" to R.string.thaat_bhairavi, "Bhairav" to R.string.thaat_bhairav,
    "Kalyan" to R.string.thaat_kalyan, "Marwa" to R.string.thaat_marwa,
    "Poorvi" to R.string.thaat_poorvi, "Todi" to R.string.thaat_todi,
    "Carnatic Music System" to R.string.thaat_carnatic,
    "Not Defined" to R.string.thaat_undefined,
)

private val PRAHAR_KEYS = mapOf(
    "day 1st" to R.string.prahar_day_1, "day 2nd" to R.string.prahar_day_2,
    "day 3rd" to R.string.prahar_day_3, "day 4th" to R.string.prahar_day_4,
    "night 1st" to R.string.prahar_night_1, "night 2nd" to R.string.prahar_night_2,
    "night 3rd" to R.string.prahar_night_3, "night 4th" to R.string.prahar_night_4,
)

@Composable
private fun rememberScript(): Script = Script(
    swars = stringArrayResource(R.array.swar_names).toList(),
    mandra = stringResource(R.string.saptak_mandra),
    taara = stringResource(R.string.saptak_taara),
    raagNames = stringArrayResource(R.array.db_raag_names).toList(),
    thaats = THAAT_KEYS.mapValues { stringResource(it.value) },
    prahars = PRAHAR_KEYS.mapValues { stringResource(it.value) },
)

// ------------------------------------------------------------------ swars on screen

/**
 * A swar, with its saptak dot drawn rather than typed.
 *
 * Unicode's combining dots land on the shirorekha above and on the komal line below, so at
 * these sizes they merge with both. Drawing them puts the gap under our control.
 */
@Composable
private fun SwarToken(
    token: Notation.Token, script: Script, fontSize: TextUnit, color: Color,
) {
    Box(contentAlignment = Alignment.Center) {
        Text(token.text, fontFamily = Tiro, fontSize = fontSize, color = color,
            modifier = Modifier.padding(vertical = 7.dp))
        if (token.saptak != 0) {
            val toward = if (token.saptak > 0) 1.4.dp else (-1.4).dp   // 20% of the 7dp gap
            Canvas(
                Modifier
                    .align(if (token.saptak > 0) Alignment.TopCenter else Alignment.BottomCenter)
                    .offset(y = toward)
                    .size(4.dp),
            ) { drawCircle(color, radius = size.minDimension / 2f) }
        }
    }
}

/** A phrase of swars, wrapping where it must. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SwarPhrase(
    raw: String, script: Script, color: Color, fontSize: TextUnit = 15.sp,
) {
    // Des and the other long avarohas run past the width; they wrap into the column beside
    // the play button rather than under it
    FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        script.tokens(raw).forEach { SwarToken(it, script, fontSize, color) }
    }
}

/** The small crimson play button that sounds a phrase out. */
@Composable
private fun PlayButton(label: String, onClick: () -> Unit) {
    Box(
        Modifier.size(24.dp).clip(CircleShape).background(Mogra.Crimson)
            .clickable(onClick = onClick)
            .semantics { contentDescription = label; role = Role.Button },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(9.dp)) {
            // a triangle's centroid is a third of the way along, not half, so it has to be
            // nudged right to look centred in the circle
            val shift = size.width / 6f
            val p = androidx.compose.ui.graphics.Path().apply {
                moveTo(shift, 0f)
                lineTo(shift + size.width, size.height / 2f)
                lineTo(shift, size.height)
                close()
            }
            drawPath(p, Color.White)
        }
    }
}

/** The attributes, in the order tanarang.com shows them, with their labels. */
private val ATTRIBUTES = listOf(
    "aaroha" to R.string.attr_aaroha,
    "avaroha" to R.string.attr_avaroha,
    "mukhyanga" to R.string.attr_mukhyanga,
    "aarohi_nyas" to R.string.attr_aarohi_nyas,
    "avarohi_nyas" to R.string.attr_avarohi_nyas,
    "vaadi" to R.string.attr_vaadi,
    "samvaadi" to R.string.attr_samvaadi,
    "thaat" to R.string.attr_thaat,
    "prahar" to R.string.attr_prahar,
)

/**
 * Sa for sounding notes out: whatever the Raag Identifier last saved, else A3.
 *
 * These tools do not need a tonic to answer anything — the scale of a raag is the same
 * wherever Sa sits — so nobody is sent through the Sa screen to use them. It only decides
 * what pitch the play buttons make.
 */
@Composable
private fun rememberSaHz() = LocalContext.current.let { context ->
    remember { mutableStateOf(Sa.savedOrNull(context) ?: Sa.hzOf(57)) }
}

/** The Sa strip, and the keyboard it opens. */
@Composable
private fun SaSection(saHz: Double, onPick: (Double) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    SaBar(saHz) { open = !open }
    if (open) {
        Spacer(Modifier.height(14.dp))
        SaKeyboard(Sa.nearest(saHz).first) { midi ->
            val hz = Sa.hzOf(midi)
            onPick(hz)
            Sa.remember(context, hz)
            scope.launch { runCatching { Audio.playTone(hz, 0.9) } }
        }
        Spacer(Modifier.height(12.dp))
        Box(
            Modifier.fillMaxWidth().height(44.dp).clip(Card)
                .border(1.dp, Mogra.Cream.copy(alpha = 0.16f), Card)
                .clickable { open = false }
                .semantics { role = Role.Button },
            contentAlignment = Alignment.Center,
        ) {
            Text(stringResource(R.string.sa_pick_done),
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                color = Mogra.Cream.copy(alpha = 0.78f))
        }
    }
}

// ------------------------------------------------------------------ by notes

/**
 * Pick swars off a chromatic circle; the raags whose scale is exactly that set come back.
 *
 * The circle rather than a row because the twelve swars *are* a cycle — S sits at the top
 * and its octave is the same place — and because a row of twelve on a phone leaves each one
 * too narrow to hit.
 */
@Composable
fun ByNotesScreen(onBack: () -> Unit) = MograScreen {
    val db = RaagDb.load(LocalContext.current)
    val script = rememberScript()
    var saHz by rememberSaHz()
    val scope = rememberCoroutineScope()
    var picked by remember { mutableStateOf(setOf<String>()) }
    val matches = if (picked.isEmpty()) emptyList() else db.matching(picked)

    FlowHeader(stringResource(R.string.tool_by_notes_title), null, onBack)

    Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
        Spacer(Modifier.height(14.dp))
        SaSection(saHz) { saHz = it }
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.notes_title),
            style = MaterialTheme.typography.displayMedium, color = Mogra.TextPrimary)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.notes_subtitle),
            style = MaterialTheme.typography.bodyLarge, color = Mogra.Cream.copy(alpha = 0.48f))

        Spacer(Modifier.height(20.dp))
        SwarCircle(db.swarOrder, picked, script) { swar ->
            picked = if (swar in picked) picked - swar else picked + swar
            // sounding the swar as it is picked is the whole reason this screen knows Sa
            val index = Notation.LATIN.indexOf(swar)
            if (index >= 0) scope.launch {
                runCatching { Audio.playTone(Tuning.frequency(saHz, index), 0.55) }
            }
        }

        Spacer(Modifier.height(16.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (picked.isEmpty()) {
                Text(stringResource(R.string.notes_prompt),
                    style = MaterialTheme.typography.titleMedium, color = Mogra.TextMuted)
            } else {
                SwarPhrase(picked.sortedBy { db.swarOrder.indexOf(it) }.joinToString(" "),
                    script, Mogra.CrimsonSoft, 18.sp)
            }
            if (picked.isNotEmpty()) {
                Text(stringResource(R.string.notes_clear),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = Mogra.CrimsonSoft,
                    modifier = Modifier.clickable { picked = emptySet() }
                        .semantics { role = Role.Button })
            }
        }

        if (picked.isNotEmpty()) {
            Spacer(Modifier.height(18.dp))
            if (matches.isEmpty()) {
                Text(stringResource(R.string.notes_none),
                    style = MaterialTheme.typography.bodyLarge, color = Mogra.TextSecondary)
            } else {
                MicroLabel(stringResource(R.string.notes_matches), color = Mogra.TextFaint)
                Spacer(Modifier.height(10.dp))
                matches.forEach { RaagCard(it, db, script, saHz) }
            }
        }

        Spacer(Modifier.height(18.dp))
        Credit()
        Spacer(Modifier.height(16.dp))
    }
}

/**
 * The twelve swars on a clock face, S at the top and rising clockwise.
 *
 * Laid out by hand rather than with a layout: twelve items on a circle is exactly the case
 * where trigonometry is shorter and clearer than any arrangement of rows.
 */
@Composable
private fun SwarCircle(
    order: List<String>, picked: Set<String>, script: Script, onToggle: (String) -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val diameter = minOf(maxWidth.value, 320f)
        val chip = 46f
        val radius = (diameter - chip) / 2f
        Box(Modifier.size(diameter.dp), contentAlignment = Alignment.Center) {
            order.forEachIndexed { i, swar ->
                val angle = Math.toRadians(-90.0 + i * 30.0)
                val on = swar in picked
                val label = stringResource(R.string.cd_swar, script.spoken(swar))
                Box(
                    Modifier
                        .offset(
                            x = (radius * cos(angle)).toFloat().dp,
                            y = (radius * sin(angle)).toFloat().dp,
                        )
                        .size(chip.dp)
                        .clip(CircleShape)
                        .background(if (on) Mogra.Crimson else Mogra.Cream.copy(alpha = 0.045f))
                        .border(1.dp,
                            if (on) Mogra.Crimson else Mogra.Cream.copy(alpha = 0.14f),
                            CircleShape)
                        .clickable { onToggle(swar) }
                        .semantics {
                            contentDescription = label
                            role = Role.Checkbox
                            this.selected = on
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    SwarToken(
                        Notation.parse(swar, script.swars).first(), script, 21.sp,
                        if (on) Color(0xFFFFF1F3) else Mogra.Cream.copy(alpha = 0.72f),
                    )
                }
            }
        }
    }
}

// ------------------------------------------------------------------ by name

@Composable
fun ByNameScreen(onBack: () -> Unit) = MograScreen {
    val db = RaagDb.load(LocalContext.current)
    val script = rememberScript()
    var saHz by rememberSaHz()
    var chosen by remember { mutableStateOf<Int?>(null) }
    var filter by remember { mutableStateOf("") }

    FlowHeader(stringResource(R.string.tool_by_name_title), null, onBack)

    val pickedIndex = chosen
    if (pickedIndex == null) {
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.name_title),
            style = MaterialTheme.typography.displayMedium, color = Mogra.TextPrimary)
        Spacer(Modifier.height(14.dp))
        FilterField(filter) { filter = it }
        Spacer(Modifier.height(12.dp))
        // filter on both scripts, so typing "yaman" works in a Marathi build and typing
        // "यमन" works in an English one
        val needle = filter.trim()
        val shown = db.raags.indices.filter { i ->
            needle.isEmpty() ||
                db.raags[i].name.contains(needle, ignoreCase = true) ||
                script.name(i, db.raags[i].name).contains(needle, ignoreCase = true)
        }
        LazyColumn(Modifier.weight(1f)) {
            items(shown, key = { db.raags[it].key }) { i ->
                Text(
                    script.name(i, db.raags[i].name),
                    fontFamily = Tiro, fontSize = 20.sp, color = Mogra.Cream.copy(alpha = 0.88f),
                    modifier = Modifier.fillMaxWidth()
                        .clickable { chosen = i }
                        .semantics { role = Role.Button }
                        .padding(vertical = 13.dp),
                )
            }
        }
        Credit()
        Spacer(Modifier.height(12.dp))
    } else {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(14.dp))
            SaSection(saHz) { saHz = it }
            Spacer(Modifier.height(16.dp))
            val raag = db.raags[pickedIndex]
            Text(script.name(pickedIndex, raag.name),
                fontFamily = Tiro, fontSize = 33.sp, color = Mogra.TextPrimary)
            Spacer(Modifier.height(14.dp))
            RaagCard(raag, db, script, saHz, showName = false)
            Spacer(Modifier.height(18.dp))
            Credit()
            Spacer(Modifier.height(16.dp))
        }
        Box(
            Modifier.fillMaxWidth().height(50.dp).clip(Card)
                .border(1.dp, Mogra.Cream.copy(alpha = 0.16f), Card)
                .clickable { chosen = null }
                .semantics { role = Role.Button },
            contentAlignment = Alignment.Center,
        ) {
            Text(stringResource(R.string.name_pick),
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                color = Mogra.Cream.copy(alpha = 0.78f))
        }
    }
}

@Composable
private fun FilterField(value: String, onChange: (String) -> Unit) {
    Box(
        Modifier.fillMaxWidth().height(48.dp).clip(Card)
            .background(Mogra.SurfaceTint)
            .border(1.dp, Mogra.Hairline, Card)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (value.isEmpty()) {
            Text(stringResource(R.string.name_filter),
                style = MaterialTheme.typography.bodyLarge, color = Mogra.TextFaint)
        }
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = TextStyle(
                fontSize = 15.sp, color = Mogra.TextPrimary,
                fontFamily = MaterialTheme.typography.bodyLarge.fontFamily,
            ),
            cursorBrush = SolidColor(Mogra.Crimson),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// ------------------------------------------------------------------ shared

/** One raag as the attribute table tanarang.com shows, minus the tonnetz. */
@Composable
private fun RaagCard(
    raag: RaagDb.Raag, db: RaagDb, script: Script, saHz: Double, showName: Boolean = true,
) {
    Column(
        Modifier.fillMaxWidth().padding(bottom = 10.dp).clip(Card)
            .border(1.dp, Mogra.Cream.copy(alpha = 0.085f), Card)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        if (showName) {
            Text(script.name(db.raags.indexOf(raag), raag.name),
                fontFamily = Tiro, fontSize = 21.sp, color = Mogra.Cream.copy(alpha = 0.88f))
            Spacer(Modifier.height(10.dp))
        }
        val scope = rememberCoroutineScope()
        ATTRIBUTES.forEach { (field, label) ->
            val values = raag.fields[field] ?: return@forEach
            // the two phrases worth hearing get a play button; a vaadi is one note and a
            // thaat is a word
            val playable = field == "aaroha" || field == "avaroha"
            val labelText = stringResource(label)
            Row(
                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Text(labelText, style = MaterialTheme.typography.bodyMedium,
                    color = Mogra.TextFaint, modifier = Modifier.size(width = 92.dp, height = 22.dp))
                if (playable) {
                    val spoken = stringResource(R.string.cd_play_phrase, labelText)
                    PlayButton(spoken) {
                        scope.launch {
                            runCatching {
                                Audio.playPhrase(
                                    Tuning.frequencies(saHz, script.tokens(values.first())))
                            }
                        }
                    }
                    Spacer(Modifier.size(8.dp))
                }
                Column(Modifier.weight(1f)) {
                    when (field) {
                        "thaat" -> values.forEach {
                            Text(script.thaat(it), style = MaterialTheme.typography.bodyLarge,
                                color = Mogra.Cream.copy(alpha = 0.80f))
                        }
                        "prahar" -> values.forEach {
                            Text(script.prahar(it), style = MaterialTheme.typography.bodyLarge,
                                color = Mogra.Cream.copy(alpha = 0.80f))
                        }
                        else -> values.forEach {
                            SwarPhrase(it, script, Mogra.Cream.copy(alpha = 0.80f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Credit() {
    Text(
        stringResource(R.string.credit_tanarang),
        style = MaterialTheme.typography.bodyMedium, color = Mogra.TextFaint,
        modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
    )
}
