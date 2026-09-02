package com.rishaanlabs.ros.domain.finance

import org.junit.Assert.assertEquals
import org.junit.Test

class MoneyTest {
    @Test fun parseMajorToMinor_handlesLaari() {
        assertEquals(123_45L, parseMajorToMinor("123.45"))
    }

    @Test fun parseMajorToMinor_handlesCommas() {
        assertEquals(1_234_56L, parseMajorToMinor("1,234.56"))
    }

    @Test fun minorToEditableMajor_roundTripsThroughTheParser() {
        // Editing a record puts the stored amount back into the field it was typed into. If these
        // two disagreed, opening a record and saving it unchanged would alter the amount.
        listOf(0L, 5L, 50L, 4500L, 123_456_789L, 1L).forEach { minor ->
            assertEquals(minor, parseMajorToMinor(minorToEditableMajor(minor)))
        }
    }

    @Test fun minorToEditableMajor_hasNoCurrencyOrGrouping() {
        assertEquals("1234.56", minorToEditableMajor(1_234_56L))
        assertEquals("45.00", minorToEditableMajor(4500L))
    }
}
