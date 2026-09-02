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
}
