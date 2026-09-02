package com.rishaanlabs.ros.domain.finance

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FinanceDeletionTest {

    @Test
    fun `an unused account can be deleted`() {
        assertEquals(DeletionVerdict.Allowed, accountDeletionVerdict(transactionCount = 0))
    }

    @Test
    fun `an account with transactions is refused rather than left to the database`() {
        val verdict = accountDeletionVerdict(transactionCount = 4)
        assertTrue(verdict is DeletionVerdict.Blocked)
        val reason = (verdict as DeletionVerdict.Blocked).reason
        assertTrue("the count belongs in the message", reason.contains("4"))
        assertTrue("the user needs the way out, not just the refusal", reason.contains("archive"))
    }

    @Test
    fun `the refusal reads correctly for a single transaction`() {
        val reason = (accountDeletionVerdict(1) as DeletionVerdict.Blocked).reason
        assertTrue(reason.contains("1 transaction"))
        assertTrue("no stray plural", !reason.contains("1 transactions"))
    }

    @Test
    fun `an account currency may change only while nothing has been recorded in it`() {
        assertEquals(
            DeletionVerdict.Allowed,
            accountCurrencyChangeVerdict(transactionCount = 0, currencyChanged = true)
        )
        assertTrue(
            accountCurrencyChangeVerdict(transactionCount = 1, currencyChanged = true)
                is DeletionVerdict.Blocked
        )
    }

    @Test
    fun `leaving the currency alone is always fine`() {
        assertEquals(
            DeletionVerdict.Allowed,
            accountCurrencyChangeVerdict(transactionCount = 99, currencyChanged = false)
        )
    }

    @Test
    fun `deleting a goal is allowed because allocations are virtual`() {
        assertEquals(DeletionVerdict.Allowed, goalDeletionVerdict(allocationCount = 0))

        val verdict = goalDeletionVerdict(allocationCount = 3)
        assertTrue(verdict is DeletionVerdict.AllowedWithEffect)
        val effect = (verdict as DeletionVerdict.AllowedWithEffect).effect
        assertTrue("the user must know no real money moves", effect.contains("No account balance changes"))
    }

    @Test
    fun `deleting a loan keeps the transactions that actually moved money`() {
        val verdict = loanDeletionVerdict(paymentCount = 2)
        assertTrue(verdict is DeletionVerdict.AllowedWithEffect)
        val effect = (verdict as DeletionVerdict.AllowedWithEffect).effect
        assertTrue(effect.contains("transactions that paid them stay"))
        assertTrue(effect.contains("no account balance changes"))
    }

    @Test
    fun `an unpaid loan deletes without any warning`() {
        assertEquals(DeletionVerdict.Allowed, loanDeletionVerdict(paymentCount = 0))
    }

    @Test
    fun `an ordinary transaction deletes cleanly`() {
        assertEquals(
            DeletionVerdict.Allowed,
            transactionDeletionVerdict(pairedWithLoanPayment = false)
        )
    }

    @Test
    fun `deleting a debt payment warns that the loan changes with it`() {
        val verdict = transactionDeletionVerdict(pairedWithLoanPayment = true)
        assertTrue(verdict is DeletionVerdict.AllowedWithEffect)
        assertTrue(
            (verdict as DeletionVerdict.AllowedWithEffect).effect.contains("loan and the account")
        )
    }

    @Test
    fun `a debt payment cannot be edited in place because its split lives on the loan`() {
        val verdict = transactionEditVerdict(pairedWithLoanPayment = true)
        assertTrue(verdict is DeletionVerdict.Blocked)
        assertTrue(
            "the message must say how to correct it",
            (verdict as DeletionVerdict.Blocked).reason.contains("record the payment again")
        )
    }

    @Test
    fun `an ordinary transaction can be edited in place`() {
        assertEquals(DeletionVerdict.Allowed, transactionEditVerdict(pairedWithLoanPayment = false))
    }

    @Test
    fun `a goal currency may change only while it holds nothing`() {
        assertEquals(DeletionVerdict.Allowed, goalCurrencyChangeVerdict(0, currencyChanged = true))
        assertTrue(goalCurrencyChangeVerdict(1, currencyChanged = true) is DeletionVerdict.Blocked)
    }
}
