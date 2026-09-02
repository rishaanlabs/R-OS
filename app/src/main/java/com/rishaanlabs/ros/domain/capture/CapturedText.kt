package com.rishaanlabs.ros.domain.capture

/**
 * The structure found in a captured blob of text.
 *
 * Capture deliberately takes free text and asks nothing (see the UX decision record). That means
 * structure the user typed — a heading with lines under it — arrives as one string, and something
 * has to recover it at processing time. Before this existed, the whole blob became a task title,
 * so a shopping list turned into one unreadable heading and the list itself was gone.
 *
 * The parse is deterministic and local. It never rewrites or drops the user's words: every
 * non-blank line ends up in [items], and [body] holds everything that is not the title.
 */
data class CapturedText(
    /** The title to use, or to offer the user for confirmation. Never blank. */
    val proposedTitle: String,
    /** What the body should be when [proposedTitle] is kept: everything below the first line. */
    val body: String,
    /**
     * What the body should be when the user supplies a different title.
     *
     * These differ only for a list with no heading. "Shopping / Milk / Eggs / Bread" keeps
     * "Shopping" as a title and drops it from the body; "Milk / Eggs / Bread" retitled to
     * "Shopping list" has to keep "Milk", because there the first line is an item and not a
     * heading. Which of the two a capture is cannot be decided from the text, so it is decided
     * by what the user does with the title.
     */
    val bodyWhenRetitled: String,
    /** Every non-blank line, in order. Present so a future "create multiple tasks" can use it. */
    val items: List<String>,
    /**
     * True when the title is a guess rather than something the user clearly wrote as a heading.
     * The processor should show it in an editable field instead of committing it silently.
     */
    val titleNeedsConfirmation: Boolean,
    /** A second title worth offering, when the lines look like a recognisable kind of list. */
    val suggestedAlternativeTitle: String? = null
) {
    /** The body to store alongside [title], accounting for whether the first line was consumed. */
    fun bodyFor(title: String): String =
        if (title.trim() == proposedTitle.trim()) body else bodyWhenRetitled

    /** True when this capture is a list that could become several tasks rather than one. */
    val looksLikeList: Boolean get() = items.size > 1
}

private const val SINGLE_LINE_TITLE_MAX = 80

private val BULLET = Regex("""^\s*([-*•]|\d+[.)])\s+""")

/**
 * A small local vocabulary, used only to offer a better default title than the first list item.
 * It is a convenience, not a classifier: the user always sees and can overwrite the result, and
 * nothing downstream depends on it being right. Kept deliberately short — guessing more
 * confidently than this would need semantics the app does not have, and a wrong confident title
 * is worse than an honest prompt.
 */
private val SHOPPING_WORDS = setOf(
    "milk", "eggs", "egg", "bread", "butter", "cheese", "rice", "flour", "sugar", "salt",
    "coffee", "tea", "oil", "onions", "onion", "tomatoes", "tomato", "potatoes", "potato",
    "chicken", "fish", "beef", "fruit", "apples", "bananas", "yoghurt", "yogurt", "water",
    "soap", "shampoo", "toothpaste", "detergent", "tissue", "tissues"
)

/**
 * Recovers the structure in [raw].
 *
 * Rules, in order:
 *  - A single short line is simply the title.
 *  - A single long line is split at a word boundary so the title stays readable; the remainder
 *    becomes the body rather than being thrown away.
 *  - Several lines where the first is clearly a heading — it ends with a colon, is a markdown
 *    heading, or the lines beneath it are bulleted and it is not — use that heading directly.
 *  - Several lines with no such signal keep the first line as the *proposed* title but ask for
 *    confirmation, because "Shopping / Milk / Eggs / Bread" and "Milk / Eggs / Bread" are not
 *    distinguishable without understanding the words, and silently titling the second one "Milk"
 *    is the bug this function exists to prevent.
 */
fun splitCapturedText(raw: String): CapturedText {
    val lines = raw.trim().lines().map { it.trim() }.filter { it.isNotBlank() }

    if (lines.isEmpty()) {
        return CapturedText("Untitled", "", "", emptyList(), titleNeedsConfirmation = true)
    }

    if (lines.size == 1) return singleLine(lines.first())

    val first = lines.first()
    val rest = lines.drop(1)

    headingOf(first, rest)?.let { heading ->
        // An explicit heading is consumed whatever the user retitles it to: it was never an item.
        return CapturedText(
            proposedTitle = heading,
            body = rest.joinToString("\n"),
            bodyWhenRetitled = rest.joinToString("\n"),
            items = lines,
            titleNeedsConfirmation = false
        )
    }

    // No heading signal, so the first line is either a heading or the first item and the text
    // cannot say which. Keeping it as the title treats it as a heading; replacing the title
    // treats it as an item and keeps it in the list. Either way no line is lost.
    return CapturedText(
        proposedTitle = first,
        body = rest.joinToString("\n"),
        bodyWhenRetitled = lines.joinToString("\n"),
        items = lines,
        titleNeedsConfirmation = true,
        suggestedAlternativeTitle = suggestListTitle(lines)
    )
}

private fun singleLine(line: String): CapturedText {
    if (line.length <= SINGLE_LINE_TITLE_MAX) {
        return CapturedText(line, "", "", listOf(line), titleNeedsConfirmation = false)
    }
    val cut = line.lastIndexOf(' ', SINGLE_LINE_TITLE_MAX).takeIf { it > 0 } ?: SINGLE_LINE_TITLE_MAX
    val tail = line.drop(cut).trim()
    return CapturedText(
        proposedTitle = line.take(cut).trim(),
        body = tail,
        bodyWhenRetitled = tail,
        items = listOf(line),
        titleNeedsConfirmation = false
    )
}

/** The heading the user actually wrote, or null when there is no clear signal. */
private fun headingOf(first: String, rest: List<String>): String? {
    if (BULLET.containsMatchIn(first)) return null

    if (first.endsWith(":")) return first.dropLast(1).trim().ifBlank { null }

    if (first.startsWith("#")) return first.trimStart('#').trim().ifBlank { null }

    // Bulleted lines under an unbulleted line: the unbulleted line is the heading.
    if (rest.all { BULLET.containsMatchIn(it) }) return first

    // A sentence-length first line followed by short lines reads as a heading with detail under it.
    if (wordCount(first) >= 4 && rest.all { wordCount(it) <= 3 }) return first

    return null
}

private fun suggestListTitle(lines: List<String>): String? {
    val words = lines.flatMap { line ->
        line.replace(BULLET, "").lowercase().split(' ', ',', '/')
    }.map { it.trim() }.filter { it.isNotBlank() }

    val hits = words.count { it in SHOPPING_WORDS }
    return if (hits >= 2) "Shopping list" else null
}

private fun wordCount(line: String) = line.split(' ').count { it.isNotBlank() }
