package com.rishaanlabs.ros.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * The design's colour tokens, verbatim.
 *
 * Material's scheme has nowhere to put most of these. The design leans on a four-step ink ramp
 * and two divider weights to build hierarchy without boxes or colour — that ramp *is* the visual
 * language, and collapsing it into `onSurface` / `onSurfaceVariant` would flatten exactly the
 * distinction the screens rely on. So the ramp lives here alongside the Material scheme rather
 * than being squeezed into it.
 *
 * Names match the design's CSS custom properties so the two can be diffed by eye.
 */
@Immutable
data class RosColors(
    /** Page background. */
    val bg: Color,
    /** Raised surface: cards, the search field, the capture bar. */
    val surf: Color,
    /** Recessed surface: progress troughs, inactive chips. */
    val surf2: Color,
    /** Structural divider — section rules, card borders. */
    val line: Color,
    /** Hairline divider between rows inside a card. */
    val line2: Color,
    /** Primary text. */
    val ink: Color,
    /** Secondary text: still prose, deliberately quieter. */
    val ink2: Color,
    /** Metadata and section labels. */
    val ink3: Color,
    /** Furthest back: placeholders, inactive tabs, checkbox outlines. */
    val ink4: Color,
    /** Something is wrong now — overdue, over budget. */
    val danger: Color,
    /** Something needs a decision soon — near a limit, uncategorised. */
    val warn: Color,
    /** Accent fill. Near-black in light, near-white in dark: the design has no brand hue. */
    val acc: Color,
    /** Text on [acc]. */
    val onAcc: Color,
    /** Scrim behind sheets and dialogs. */
    val shade: Color
)

internal val RosLightColors = RosColors(
    bg = Color(0xFFFAFAFC),
    surf = Color(0xFFFFFFFF),
    surf2 = Color(0xFFF0F0F5),
    line = Color(0xFFE2E2EC),
    line2 = Color(0xFFEEEEF4),
    ink = Color(0xFF1A1A2E),
    ink2 = Color(0xFF4A4A6A),
    ink3 = Color(0xFF8A8AA5),
    ink4 = Color(0xFFB6B6C9),
    danger = Color(0xFFB00020),
    warn = Color(0xFFC8A23A),
    acc = Color(0xFF1A1A2E),
    onAcc = Color(0xFFFFFFFF),
    shade = Color(0x521A1A2E)
)

internal val RosDarkColors = RosColors(
    bg = Color(0xFF0F0F1A),
    surf = Color(0xFF1A1A2A),
    surf2 = Color(0xFF252535),
    line = Color(0xFF2E2E46),
    line2 = Color(0xFF26263A),
    ink = Color(0xFFE0E0F0),
    ink2 = Color(0xFFB0B0D0),
    ink3 = Color(0xFF8383A3),
    ink4 = Color(0xFF5C5C7D),
    danger = Color(0xFFCF6679),
    warn = Color(0xFFD0B05F),
    acc = Color(0xFFB0B0D0),
    onAcc = Color(0xFF0F0F1A),
    shade = Color(0x8C000000)
)

val LocalRosColors = staticCompositionLocalOf { RosLightColors }

/**
 * The design's tokens for the current theme. Read as `Ros.colors.ink3` at a call site.
 *
 * Deliberately not defaulted anywhere: a screen that reads this outside [RosTheme] gets the light
 * ramp, which is wrong in dark mode and visibly so, rather than silently correct-looking.
 */
object Ros {
    val colors: RosColors
        @androidx.compose.runtime.Composable
        @androidx.compose.runtime.ReadOnlyComposable
        get() = LocalRosColors.current
}
