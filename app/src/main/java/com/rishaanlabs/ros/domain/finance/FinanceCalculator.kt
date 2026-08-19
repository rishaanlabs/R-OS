package com.rishaanlabs.ros.domain.finance

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.ceil
import kotlin.math.roundToLong

object FinanceCalculator {

    data class GoalProjection(
        val progress: Double,
        val remainingMinor: Long,
        val requiredMonthlyMinor: Long?,
        val projectedCompletionDate: LocalDate?,
        val projectedMonthsRemaining: Int?
    )

    data class LoanProjection(
        val monthsRemaining: Int,
        val totalFutureInterestMinor: Long,
        val totalFuturePaymentsMinor: Long,
        val payoffDate: LocalDate
    )

    fun goalProjection(
        currentMinor: Long,
        targetMinor: Long,
        plannedMonthlyMinor: Long,
        today: LocalDate = LocalDate.now(),
        targetDate: LocalDate? = null
    ): GoalProjection {
        if (targetMinor <= 0L) {
            return GoalProjection(1.0, 0L, null, today, 0)
        }
        val current = currentMinor.coerceAtLeast(0L)
        val remaining = (targetMinor - current).coerceAtLeast(0L)
        val progress = (current.toDouble() / targetMinor.toDouble()).coerceIn(0.0, 1.0)

        val monthsByPlan = if (remaining == 0L) 0 else if (plannedMonthlyMinor > 0L) {
            ceil(remaining.toDouble() / plannedMonthlyMinor.toDouble()).toInt()
        } else null

        val projectedDate = monthsByPlan?.let { today.plusMonths(it.toLong()) }

        val required = targetDate?.takeIf { it.isAfter(today) && remaining > 0L }?.let { deadline ->
            val months = monthsInclusive(today, deadline).coerceAtLeast(1)
            ceil(remaining.toDouble() / months.toDouble()).toLong()
        }

        return GoalProjection(progress, remaining, required, projectedDate, monthsByPlan)
    }

    fun emergencyRunwayMonths(emergencyBalanceMinor: Long, averageEssentialMonthlyMinor: Long): Double? {
        if (averageEssentialMonthlyMinor <= 0L) return null
        return emergencyBalanceMinor.coerceAtLeast(0L).toDouble() / averageEssentialMonthlyMinor.toDouble()
    }

    /**
     * Monthly amortization estimate. This is intentionally used only for loans explicitly marked
     * AMORTIZED_MONTHLY. Daily-simple, precomputed and manual loans need lender-specific rules.
     */
    fun amortizedLoanProjection(
        balanceMinor: Long,
        annualInterestRateBps: Int,
        monthlyPaymentMinor: Long,
        extraMonthlyMinor: Long = 0L,
        today: LocalDate = LocalDate.now(),
        maxMonths: Int = 1200
    ): LoanProjection? {
        if (balanceMinor <= 0L) return LoanProjection(0, 0L, 0L, today)
        val payment = monthlyPaymentMinor + extraMonthlyMinor
        if (payment <= 0L) return null

        val monthlyRate = annualInterestRateBps.toDouble() / 10_000.0 / 12.0
        var balance = balanceMinor.toDouble()
        var interestTotal = 0.0
        var paymentsTotal = 0.0
        var months = 0

        while (balance > 0.5 && months < maxMonths) {
            val interest = balance * monthlyRate
            if (payment.toDouble() <= interest && monthlyRate > 0.0) return null
            val due = balance + interest
            val paid = minOf(payment.toDouble(), due)
            val principal = paid - interest
            if (principal <= 0.0) return null
            balance = (balance - principal).coerceAtLeast(0.0)
            interestTotal += interest
            paymentsTotal += paid
            months++
        }

        if (balance > 0.5) return null
        return LoanProjection(
            monthsRemaining = months,
            totalFutureInterestMinor = interestTotal.roundToLong(),
            totalFuturePaymentsMinor = paymentsTotal.roundToLong(),
            payoffDate = today.plusMonths(months.toLong())
        )
    }

    private fun monthsInclusive(from: LocalDate, to: LocalDate): Int {
        val wholeMonths = ChronoUnit.MONTHS.between(from.withDayOfMonth(1), to.withDayOfMonth(1)).toInt()
        return wholeMonths + 1
    }
}
