package io.github.binaryfoo.cloudtail.s3

import com.amazonaws.services.cloudtrail.processinglibrary.model.CloudTrailEvent
import com.amazonaws.services.cloudtrail.processinglibrary.model.CloudTrailLog
import com.amazonaws.services.cloudtrail.processinglibrary.model.LogDeliveryInfo
import com.amazonaws.services.cloudtrail.processinglibrary.serializer.RawLogDeliveryEventSerializer
import com.fasterxml.jackson.databind.ObjectMapper
import java.io.File
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.zip.GZIPInputStream

private val TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss")

fun main(args: Array<String>) {
    File("tmp/all.wsd").printWriter().use { out ->
        processEvents("tmp") { event ->
            val server = event.eventData.eventSource
            val client = event.eventData.sourceIPAddress
            val time = LocalDateTime.ofInstant(event.eventData.eventTime.toInstant(), ZoneId.of("UTC"))
            val userName = event.eventData.userIdentity.userName
            val principalId = event.eventData.userIdentity.principalId
            val eventMetadata = event.eventMetadata as LogDeliveryInfo

            out.println("${TIME_FORMAT.format(time)}: $client -> $server ($userName/$principalId)")
        }
    }
}

private val mapper = ObjectMapper()

fun processEvents(directory: String, f: (CloudTrailEvent) -> Unit) {
    File(directory).listFiles().filter { it.name.endsWith(".gz") }.sorted().forEach { logFile ->
        println(logFile.name)
        val events = parseEventsFrom(logFile)
        // events within a file are not ordered
        events.sortBy { it.eventData.eventTime }
        events.forEach(f)
    }
}

private fun parseEventsFrom(logFile: File): ArrayList<CloudTrailEvent> {
    val events = ArrayList<CloudTrailEvent>()
    GZIPInputStream(logFile.inputStream()).use { inputStream ->
        val fullLogText = inputStream.reader().readText()
        val jsonParser = mapper.factory.createParser(fullLogText)
        val serializer = RawLogDeliveryEventSerializer(fullLogText, CloudTrailLog("", ""), jsonParser)
        while (serializer.hasNextEvent()) {
            events.add(serializer.nextEvent)
        }
    }
    return events
}
