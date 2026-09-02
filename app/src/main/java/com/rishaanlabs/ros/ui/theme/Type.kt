package com.rishaanlabs.ros.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.rishaanlabs.ros.R

/**
 * The two typefaces the design is built on.
 *
 * Instrument Sans carries the prose — titles, labels, anything read as language. IBM Plex Mono
 * carries anything that is a value rather than a sentence: money, dates, counts, section
 * headings. That split is the whole reason the screens read as an instrument panel instead of a
 * generic list app, so it is worth being strict about which one a piece of text belongs to.
 */
val RosSans = FontFamily(
    Font(R.font.instrument_sans_400, FontWeight.Normal),
    Font(R.font.instrument_sans_500, FontWeight.Medium),
    Font(R.font.instrument_sans_600, FontWeight.SemiBold),
    Font(R.font.instrument_sans_700, FontWeight.Bold)
)

val RosMono = FontFamily(
    Font(R.font.ibm_plex_mono_400, FontWeight.Normal),
    Font(R.font.ibm_plex_mono_500, FontWeight.Medium)
)

/**
 * Monospaced small caps with wide tracking — the design's section rule ("TOP 3 · CHOSEN",
 * "NEEDS ATTENTION"). Text passed to this should already be upper case; it is a label, not a
 * sentence, and capitalising it in the layout keeps that decision in one place.
 */
val RosLabel = TextStyle(
    fontFamily = RosMono,
    fontWeight = FontWeight.SemiBold,
    fontSize = 10.5.sp,
    letterSpacing = 1.4.sp
)

/** Metadata under a title: project, due date, account. Monospaced, quieter, slightly tracked. */
val RosMeta = TextStyle(
    fontFamily = RosMono,
    fontWeight = FontWeight.Normal,
    fontSize = 11.sp,
    letterSpacing = 0.44.sp
)

/** A figure meant to be compared with other figures — balances, amounts, counts. */
val RosNumeric = TextStyle(
    fontFamily = RosMono,
    fontWeight = FontWeight.Medium,
    fontSize = 13.sp
)

/** The one big number on a screen, as on Home's finance line. */
val RosDisplayNumeric = TextStyle(
    fontFamily = RosMono,
    fontWeight = FontWeight.SemiBold,
    fontSize = 30.sp,
    letterSpacing = (-0.9).sp
)

val RosTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = RosSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 30.sp,
        lineHeight = 33.sp,
        letterSpacing = (-0.9).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = RosSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 27.sp,
        letterSpacing = (-0.4).sp
    ),
    titleLarge = TextStyle(
        fontFamily = RosSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 22.sp
    ),
    titleMedium = TextStyle(
        fontFamily = RosSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.5.sp,
        lineHeight = 20.sp
    ),
    titleSmall = TextStyle(
        fontFamily = RosSans,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 18.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = RosSans,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 21.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = RosSans,
        fontWeight = FontWeight.Normal,
        fontSize = 13.5.sp,
        lineHeight = 19.sp
    ),
    bodySmall = TextStyle(
        fontFamily = RosSans,
        fontWeight = FontWeight.Normal,
        fontSize = 12.5.sp,
        lineHeight = 17.sp
    ),
    labelLarge = TextStyle(
        fontFamily = RosSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp
    ),
    labelMedium = RosLabel,
    labelSmall = RosMeta
)
