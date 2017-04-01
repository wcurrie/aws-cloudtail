package io.github.binaryfoo.cloudtail

import io.github.binaryfoo.cloudtail.parser.parseEvents
import io.github.binaryfoo.cloudtail.writer.HH_MM_SS
import io.github.binaryfoo.cloudtail.writer.readResource
import org.junit.Test
import java.time.ZoneId
import kotlin.test.assertEquals

class EventTest {

    @Test
    fun timeInZone() {
        val sample = parseEvents(readResource("sample-event.json"), hasHeader = false)[0]

        assertEquals("23:00:01", sample.timeInZone(ZoneId.of("UTC")).format(HH_MM_SS))
        assertEquals("10:00:01", sample.timeInZone(ZoneId.of("Australia/Sydney")).format(HH_MM_SS))
    }
}