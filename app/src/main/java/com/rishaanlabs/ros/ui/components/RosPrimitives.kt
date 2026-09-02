package com.rishaanlabs.ros.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rishaanlabs.ros.ui.theme.Ros
import com.rishaanlabs.ros.ui.theme.RosLabel
import com.rishaanlabs.ros.ui.theme.RosMeta

/**
 * The design's structural pieces, in one place.
 *
 * These are what make the screens read as one system: a monospaced rule that names each section, a
 * bordered card that holds rows, hairline-separated rows inside it. They are deliberately dumb —
 * no state, no data, no decisions — so the same section rule cannot drift between Home and
 * Finance the way the hand-rolled headers did.
 */

/**
 * A section rule: an upper-case monospaced label, a hairline that fills the width, and an
 * optional count or action on the right.
 *
 * The rule is what carries the hierarchy in this design. There are no card titles and no elevation
 * steps — a section is announced by its label and separated by its line, which is why the label is
 * always upper case and always monospaced.
 */
@Composable
fun RosSectionRule(
    label: String,
    modifier: Modifier = Modifier,
    trailing: String? = null,
    onTrailingClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(label.uppercase(), style = RosLabel, color = Ros.colors.ink3)
        Box(
            Modifier
                .weight(1f)
                .height(1.dp)
                .background(Ros.colors.line)
        )
        if (trailing != null) {
            val trailingModifier = if (onTrailingClick != null) {
                Modifier.clickable(onClick = onTrailingClick)
            } else {
                Modifier
            }
            Text(
                text = trailing,
                style = RosMeta,
                color = Ros.colors.ink3,
                modifier = trailingModifier
            )
        }
    }
}

/** The bordered container the design uses for grouped rows. Square-ish corners, no elevation. */
@Composable
fun RosCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(5.dp))
            .background(Ros.colors.surf)
            .border(1.dp, Ros.colors.line, RoundedCornerShape(5.dp))
    ) { content() }
}

/** The hairline between rows inside a card or under a list row. */
@Composable
fun RosHairline(modifier: Modifier = Modifier, color: Color = Ros.colors.line2) {
    Box(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(color)
    )
}

/**
 * A list row: a small severity dot, a line of prose, and a monospaced trailing value.
 *
 * The dot is the only place colour is allowed to signal urgency. The design keeps whole rows
 * neutral so that a screen with four warnings still reads as a briefing rather than an alarm —
 * the same reasoning that put severity on a dot rather than a coloured card in V0.1.1.
 */
@Composable
fun RosDotRow(
    text: String,
    dotColor: Color,
    modifier: Modifier = Modifier,
    trailing: String = "",
    onClick: (() -> Unit)? = null
) {
    val rowModifier = if (onClick != null) modifier.clickable(onClick = onClick) else modifier
    Row(
        rowModifier.fillMaxWidth().padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            Modifier
                .size(5.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Text(
            text = text,
            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
            color = Ros.colors.ink,
            modifier = Modifier.weight(1f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        if (trailing.isNotBlank()) {
            Text(trailing, style = RosMeta, color = Ros.colors.ink3)
        }
    }
}

/**
 * The design's progress bar: a flat 6dp trough with a squared-off fill, never a Material
 * indicator. [fraction] is clamped, so a plan that is 140% spent still draws a full bar rather
 * than overflowing its container.
 */
@Composable
fun RosMeter(
    fraction: Float,
    modifier: Modifier = Modifier,
    fillColor: Color = Ros.colors.acc
) {
    Box(
        modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(Ros.colors.surf2)
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(fillColor)
        )
    }
}

/** A fixed-width monospaced index, as used down the left of the Top 3 card. */
@Composable
fun RosIndex(n: Int, modifier: Modifier = Modifier) {
    Text(
        text = n.toString(),
        style = RosMeta,
        color = Ros.colors.ink4,
        modifier = modifier.width(11.dp)
    )
}
