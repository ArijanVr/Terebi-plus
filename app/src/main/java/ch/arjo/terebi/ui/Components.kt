package ch.arjo.terebi.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow

private val OK_KEYS = setOf(Key.DirectionCenter, Key.Enter, Key.NumPadEnter)

/**
 * OK kurz = [onClick], OK lang (ab longPressMs, wiederholte KeyDown-Ereignisse der Fernbedienung)
 * = [onLongClick]. Ohne [onLongClick] löst auch langes Drücken nur einmal [onClick] aus.
 */
@Composable
fun Modifier.tvClick(longPressMs: Long, onLongClick: (() -> Unit)? = null, onClick: () -> Unit): Modifier {
    val st = remember { TvClickState() }
    return this
    .semantics { role = Role.Button; onClick { onClick(); true } }
    .then(
        Modifier.onKeyEvent { e ->
            if (e.key !in OK_KEYS) return@onKeyEvent false
            val n = e.nativeKeyEvent
            when (e.type) {
                KeyEventType.KeyDown -> {
                    if (n.repeatCount == 0) { st.down = n.eventTime; st.longDone = false }
                    // Android setzt FLAG_LONG_PRESS nach ~500 ms Halten; Zeitdifferenz nur als Rückfall
                    if (onLongClick != null && !st.longDone && st.down > 0 && (n.isLongPress || n.eventTime - st.down >= longPressMs)) {
                        st.longDone = true
                        onLongClick()
                    }
                    true
                }
                KeyEventType.KeyUp -> {
                    val wasDown = st.down > 0
                    st.down = 0
                    if (wasDown && !st.longDone) onClick()
                    true
                }
                else -> false
            }
        },
    )
}

private class TvClickState { var down = 0L; var longDone = false }

/** Fokussierbar mit Zustand. */
@Composable
fun rememberFocus(): Pair<Boolean, Modifier> {
    var focused by remember { mutableStateOf(false) }
    return focused to Modifier.onFocusChanged { focused = it.isFocused }.focusable()
}

@Composable
fun Label(text: String, size: androidx.compose.ui.unit.TextUnit, color: Color, modifier: Modifier = Modifier, weight: FontWeight = FontWeight.Normal, maxLines: Int = 2, tnum: Boolean = false, mono: Boolean = false) {
    BasicText(text, modifier, style = TextStyle(color = color, fontSize = size, fontWeight = weight, fontFamily = if (mono) TerebiMono else TerebiFont, fontFeatureSettings = if (tnum) "tnum" else null), maxLines = maxLines, overflow = TextOverflow.Ellipsis)
}

/** Zeile im Einstellungsmenü und in Menüs: links Bezeichnung, rechts Wert oder Schalter. */
@Composable
fun SettingRow(
    label: String,
    modifier: Modifier = Modifier,
    value: String? = null,
    toggle: Boolean? = null,
    selected: Boolean = false,
    muted: Boolean = false,
    mono: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    val d = LocalDesign.current
    val (focused, focusMod) = rememberFocus()
    val bg by animateColorAsState(if (focused) d.text else Color.Transparent, tween(d.focusMs), label = "rowBg")
    val fg = when {
        focused -> d.ink
        selected -> d.text
        muted -> d.textSecondary.copy(alpha = 0.6f)
        else -> d.textSecondary
    }
    val s by animateFloatAsState(if (focused) 1.02f else 1f, tween(d.focusMs), label = "rowScale")
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = d.dp(56))
            .scale(s)
            .clip(RoundedCornerShape(d.px(14)))
            .background(bg)
            .then(focusMod)
            .tvClick(d.longPressMs, onLongClick, onClick)
            .padding(horizontal = d.px(24)),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Label(label, d.body, fg, Modifier.weight(1f, fill = false), maxLines = 1)
        if (value != null) Label(value, d.caption, fg.copy(alpha = 0.8f), Modifier.padding(start = d.px(24)), maxLines = 1, mono = mono)
        if (toggle != null) { Label(if (toggle) "Ein" else "Aus", d.caption, fg.copy(alpha = 0.8f), Modifier.padding(start = d.px(16)), maxLines = 1); Toggle(toggle, focused) }
    }
}

/** AN = warmweisse Spur (Fokus: dunkel) statt Rot, damit höchstens der Logo-Punkt rot bleibt (DECISIONS.md). */
@Composable
fun Toggle(on: Boolean, focused: Boolean) {
    val d = LocalDesign.current
    val fg = if (focused) d.ink else d.text
    val track by animateColorAsState(if (on) fg else fg.copy(alpha = 0.25f), tween(d.focusMs), label = "track")
    val knob = if (on) (if (focused) d.text else d.ink) else fg.copy(alpha = 0.72f)
    val x by animateFloatAsState(if (on) 1f else 0f, tween(d.focusMs), label = "knob")
    Box(Modifier.padding(start = d.px(24)).size(d.px(52), d.px(30)).clip(CircleShape).background(track)) {
        Box(Modifier.offset(x = d.px(3 + 22 * x), y = d.px(3)).size(d.px(24)).clip(CircleShape).background(knob))
    }
}
