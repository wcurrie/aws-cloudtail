package io.github.binaryfoo.cloudtail

import io.github.binaryfoo.cloudtail.writer.readRawMsgsJson
import java.io.File

fun main(args: Array<String>) {

    var lastArn: String = ""

    // Finds IAM permissions used by each role in recent events

    readRawMsgsJson(File("tmp/recent.json"))
            .groupBy { it.invokerArn }
            .flatMap { byArn -> byArn.groupBy { it.eventData.eventName } }
            .forEach { g ->
                g.toList().toObservable().forEach { g ->
                    val event = g[0]
                    if (lastArn != event.invokerArn) {
                        println(event.invokerArn)
                        lastArn = event.invokerArn
                    }
                    val service = event.eventData.eventSource.replace(Regex("\\..*"), "")
                    println("  " + service + ":" + event.eventData.eventName + " " + g.size)
                }
            }

}