package io.github.binaryfoo.cloudtail.s3

import com.amazonaws.services.cloudtrail.processinglibrary.model.CloudTrailEvent
import com.amazonaws.services.cloudtrail.processinglibrary.model.CloudTrailLog
import com.amazonaws.services.cloudtrail.processinglibrary.model.LogDeliveryInfo
import com.amazonaws.services.cloudtrail.processinglibrary.serializer.RawLogDeliveryEventSerializer
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.binaryfoo.cloudtail.propertiesFrom
import java.io.File
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.zip.GZIPInputStream

private val TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss")

fun main(args: Array<String>) {
    val exclude = Regex(propertiesFrom("config.properties").getProperty("exclusion_regex"))
    File("tmp/all.wsd").printWriter().use { out ->
        processEvents("tmp") { event ->
            val eventName = event.eventData.eventName
            val server = event.eventData.eventSource
            val client = event.eventData.sourceIPAddress
            val time = LocalDateTime.ofInstant(event.eventData.eventTime.toInstant(), ZoneId.of("UTC"))
            val userName = event.eventData.userIdentity.userName
            val principalId = event.eventData.userIdentity.principalId
            val request = compress(event.eventData.requestParameters)
            val response = compress(event.eventData.responseElements)
            val rawEvent = (event.eventMetadata as LogDeliveryInfo).rawEvent

            if (!exclude.containsMatchIn(rawEvent)) {
                val optionalUser = userName?.let { " ($it)" }?:""
                out.println("$client -> $server: ${TIME_FORMAT.format(time)} $eventName$optionalUser $request")
                if (response != "") {
                    out.println("$client <-- $server: $response")
                }
            }

        }
    }
}

val SessionToken = Regex("\"sessionToken\":\"[^\"]+\"")

fun compress(requestParameters: String?): String {
    val request = requestParameters?:""
    return request.replace(SessionToken, "\"sessionToken\":\"<token>\"")
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
