package io.github.binaryfoo.cloudtail.s3

import io.github.binaryfoo.cloudtail.quote
import org.junit.Assert.*
import org.junit.Test

class ReadTest {
    @Test
    fun escapeActorName() {
        assertEquals("\"ecs-tasks.amazonaws.com\"", quote("ecs-tasks.amazonaws.com"))
        assertEquals("\"AWS Internal\"", quote("AWS Internal"))
    }
}