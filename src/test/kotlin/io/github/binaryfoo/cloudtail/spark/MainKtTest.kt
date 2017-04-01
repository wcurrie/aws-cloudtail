package io.github.binaryfoo.cloudtail.spark

import org.junit.Assert.*
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class MainKtTest {
    private val Sydney = ZoneId.of("Australia/Sydney")

    @Test
    fun parsesDateRange() {
        val (from, to) = parseDateRange("2017/03/31 12:00 AM - 2017/03/31 11:59 PM", Sydney)

        assertEquals(ZonedDateTime.of(2017, 3, 31,  0,  0, 0, 0, Sydney).toEpochSecond() * 1000, from)
        assertEquals(ZonedDateTime.of(2017, 3, 31, 23, 59, 0, 0, Sydney).toEpochSecond() * 1000, to)
    }
}