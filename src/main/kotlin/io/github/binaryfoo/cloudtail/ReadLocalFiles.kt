package io.github.binaryfoo.cloudtail

import com.amazonaws.services.cloudtrail.processinglibrary.model.CloudTrailEvent
import io.github.binaryfoo.cloudtail.parser.parseEvents
import io.github.binaryfoo.cloudtail.writer.Diagram
import io.github.binaryfoo.cloudtail.writer.drawSvgOfWsd
import io.github.binaryfoo.cloudtail.writer.writeWebSequenceDiagram
import io.reactivex.Observable
import java.io.File
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*
import java.util.zip.GZIPInputStream

/**
 * Read cloudtrail logs from a set of downloaded .gz files.
 */
fun main(args: Array<String>) {
    val exclude = Regex(propertiesFrom("config.properties").getProperty("exclusion_regex"))
    val diagram = Diagram(File("tmp/all.wsd"), displayTimeZone = ZoneId.systemDefault())

    val start = LocalDateTime.of(2017, 3, 24, 3, 0, 0, 0)
    val end = start.plusMinutes(60)
    println("From $start until $end")

    val events = processEvents("tmp").filter {
        !exclude.containsMatchIn(it.rawEvent)
                && it.eventData.userAgent != "signin.amazonaws.com"
//        && it.time >= start && it.time <= end
    }
    writeWebSequenceDiagram(events, diagram)
    drawSvgOfWsd(diagram)
}

fun processEvents(directory: String): Observable<CloudTrailEvent> {
    return Observable.create { subscriber ->
        File(directory).listFiles().filter { it.name.endsWith(".gz") }.sorted().forEach { logFile ->
            println(logFile.name)
            val events = parseEventsFrom(logFile)
            // events within a file are not ordered
            events.sortBy { it.eventData.eventTime }
            events.forEach {
                subscriber.onNext(it)
            }
        }
        subscriber.onComplete()
    }
}

private fun parseEventsFrom(logFile: File): ArrayList<CloudTrailEvent> {
    GZIPInputStream(logFile.inputStream()).use { inputStream ->
        val fullLogText = inputStream.reader().readText()
        return parseEvents(fullLogText, hasHeader = true)
    }
}
