package io.github.binaryfoo.cloudtail

import java.io.File
import java.util.*

fun propertiesFrom(fileName: String): Properties {
    return File(fileName).reader().use { reader ->
        Properties().apply { load(reader) }
    }
}