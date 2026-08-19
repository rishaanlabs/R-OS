package com.rishaanlabs.ros.domain.finance

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class FinanceCalculatorTest {

    @Test
    fun goalProjection_calculatesProgressAndCompletion() {
        val result = FinanceCalculator.goalProjection(
            currentMinor = 10_000_00,
            targetMinor = 40_000_00,
            plannedMonthlyMinor = 5_000_00,
            today = LocalDate.of(2026, 8, 1)
        )
        assertEquals(0.25, result.progress, 0.0001)
        assertEquals(6, result.projectedMonthsRemaining)
        assertEquals(LocalDate.of(2027, 2, 1), result.projectedCompletionDate)
    }

    @Test
    fun emergencyRunway_dividesFundByEssentialSpend() {
        assertEquals(2.0, FinanceCalculator.emergencyRunwayMonths(24_000_00, 12_000_00)!!, 0.0001)
    }

    @Test
    fun amortizedProjection_extraPaymentReducesTimeAndInterest() {
        val base = FinanceCalculator.amortizedLoanProjection(
            balanceMinor = 100_000_00,
            annualInterestRateBps = 900,
            monthlyPaymentMinor = 3_000_00,
            today = LocalDate.of(2026, 8, 1)
        )
        val extra = FinanceCalculator.amortizedLoanProjection(
            balanceMinor = 100_000_00,
            annualInterestRateBps = 900,
            monthlyPaymentMinor = 3_000_00,
            extraMonthlyMinor = 500_00,
            today = LocalDate.of(2026, 8, 1)
        )
        assertNotNull(base)
        assertNotNull(extra)
        assertTrue(extra!!.monthsRemaining < base!!.monthsRemaining)
        assertTrue(extra.totalFutureInterestMinor < base.totalFutureInterestMinor)
    }
}
