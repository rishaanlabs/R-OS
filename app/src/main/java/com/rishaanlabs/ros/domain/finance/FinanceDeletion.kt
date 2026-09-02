package com.rishaanlabs.ros.domain.finance

/**
 * Whether a finance record can be removed or changed, and what it costs.
 *
 * These rules are a pure function of how many other rows point at the record, so they can be
 * tested without a database — the same approach the attention rules take. The repository looks
 * the counts up and asks; nothing here touches Room.
 *
 * The rules exist because a wrong answer is expensive in both directions. Refusing every delete
 * leaves the user wiping application data to correct a typo, which is what prompted this work.
 * Allowing every delete silently orphans rows and makes balances disagree with the transactions
 * that produced them.
 */
sealed interface DeletionVerdict {

    /** Nothing else references this record. */
    data object Allowed : DeletionVerdict

    /**
     * Deleting is safe, but something else changes with it and the user should be told before
     * confirming rather than discovering it afterwards.
     */
    data class AllowedWithEffect(val effect: String) : DeletionVerdict

    /** Deleting would corrupt or orphan data. [reason] is shown to the user, so it says what to do. */
    data class Blocked(val reason: String) : DeletionVerdict
}

/**
 * An account is the anchor for every transaction recorded against it, and that foreign key is
 * NO_ACTION — the database itself would refuse. Archiving keeps the history and the balances
 * intact while taking the account out of the way, which is what "delete" nearly always means here.
 */
fun accountDeletionVerdict(transactionCount: Int): DeletionVerdict = when {
    transactionCount == 0 -> DeletionVerdict.Allowed
    transactionCount == 1 -> DeletionVerdict.Blocked(
        "This account has 1 transaction. Delete the transaction first, or archive the account to " +
            "hide it while keeping its history."
    )
    else -> DeletionVerdict.Blocked(
        "This account has $transactionCount transactions. Delete them first, or archive the " +
            "account to hide it while keeping its history."
    )
}

/**
 * Changing an account's currency after money has moved through it would reinterpret every
 * transaction already stored against it — each one carries the currency it was recorded in.
 */
fun accountCurrencyChangeVerdict(transactionCount: Int, currencyChanged: Boolean): DeletionVerdict = when {
    !currencyChanged || transactionCount == 0 -> DeletionVerdict.Allowed
    else -> DeletionVerdict.Blocked(
        "This account already has $transactionCount transactions recorded in its current " +
            "currency. Changing the currency now would misstate them."
    )
}

/**
 * A goal's allocations are virtual: they earmark money that is still sitting in a real account.
 * Removing the goal releases the earmark and no account balance moves, so this is always safe —
 * the schema cascades the allocations away deliberately.
 */
fun goalDeletionVerdict(allocationCount: Int): DeletionVerdict = when (allocationCount) {
    0 -> DeletionVerdict.Allowed
    1 -> DeletionVerdict.AllowedWithEffect(
        "Its 1 allocation is released back to unallocated cash. No account balance changes."
    )
    else -> DeletionVerdict.AllowedWithEffect(
        "Its $allocationCount allocations are released back to unallocated cash. " +
            "No account balance changes."
    )
}

fun goalCurrencyChangeVerdict(allocationCount: Int, currencyChanged: Boolean): DeletionVerdict = when {
    !currencyChanged || allocationCount == 0 -> DeletionVerdict.Allowed
    else -> DeletionVerdict.Blocked(
        "This goal already holds $allocationCount allocations in its current currency. " +
            "Remove them before changing it."
    )
}

/**
 * Deleting a loan drops its payment breakdown, but the transactions that moved the money stay:
 * the money genuinely left the account, and rewriting that would make the account balance wrong.
 * They keep their amounts and simply stop pointing at a loan.
 */
fun loanDeletionVerdict(paymentCount: Int): DeletionVerdict = when (paymentCount) {
    0 -> DeletionVerdict.Allowed
    1 -> DeletionVerdict.AllowedWithEffect(
        "Its 1 recorded payment is removed. The transaction that paid it stays, so no account " +
            "balance changes."
    )
    else -> DeletionVerdict.AllowedWithEffect(
        "Its $paymentCount recorded payments are removed. The transactions that paid them stay, " +
            "so no account balance changes."
    )
}

/**
 * A debt payment is two rows written together — the money leaving the account, and the
 * principal/interest split against the loan. Deleting one alone would leave the loan showing
 * principal repaid that the account never paid, so they go together.
 */
fun transactionDeletionVerdict(pairedWithLoanPayment: Boolean): DeletionVerdict =
    if (pairedWithLoanPayment) {
        DeletionVerdict.AllowedWithEffect(
            "This is a loan payment. Its principal and interest are removed from the loan too, " +
                "so the loan and the account stay in agreement."
        )
    } else {
        DeletionVerdict.Allowed
    }

/**
 * A debt payment's amount is split into principal, interest and fees on the loan side. Editing
 * the transaction alone would break that split, so correcting one means removing it and
 * recording the payment again.
 */
fun transactionEditVerdict(pairedWithLoanPayment: Boolean): DeletionVerdict =
    if (pairedWithLoanPayment) {
        DeletionVerdict.Blocked(
            "This is a loan payment, and its principal and interest split lives on the loan. " +
                "Delete it and record the payment again to correct it."
        )
    } else {
        DeletionVerdict.Allowed
    }
