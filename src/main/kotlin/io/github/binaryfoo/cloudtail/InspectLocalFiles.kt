package io.github.binaryfoo.cloudtail

import io.github.binaryfoo.cloudtail.aggregations.groupByRoleAndEventName
import io.github.binaryfoo.cloudtail.writer.readRawMsgsJson
import java.io.File

fun main(args: Array<String>) {
    groupByRoleAndEventName(readRawMsgsJson(File("tmp/recent.json")))
}