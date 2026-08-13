package com.rishaanlabs.ros.domain.attention

/**
 * How loudly an attention item should present itself.
 *
 * Most of what the system notices is not an emergency. Rishaan OS is meant to read as a calm
 * briefing rather than an alarm dashboard, so URGENT is reserved for things that are genuinely
 * time-sensitive and is the only severity the UI is allowed to render in an error colour.
 */
enum class AttentionSeverity {
    INFORMATIONAL,
    NEEDS_ATTENTION,
    URGENT
}

/** The kinds of situations the system knows how to recognise. */
enum class AttentionKind {
    WAITING_FOLLOW_UP_OVERDUE,
    PROJECT_NO_NEXT_ACTION,
    OVERDUE_TASK,
    INBOX_UNPROCESSED,
    PROJECT_INACTIVE,
    TOO_MANY_PRIORITIES
}

/** Where tapping an attention item should take the user. */
enum class AttentionTarget {
    TASK, PROJECT, WAITING, INBOX, NONE
}

/**
 * One thing the system believes deserves the user's attention.
 *
 * These are derived from current state every time it changes; nothing here is stored. That keeps
 * the interpretation always consistent with the data and means the feature needs no schema of
 * its own.
 */
data class AttentionItem(
    val id: String,
    val kind: AttentionKind,
    val severity: AttentionSeverity,
    val title: String,
    val detail: String,
    val target: AttentionTarget = AttentionTarget.NONE,
    val targetId: String? = null
)
