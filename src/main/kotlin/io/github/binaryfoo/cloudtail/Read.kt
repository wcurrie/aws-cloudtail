package io.github.binaryfoo.cloudtail

import com.amazonaws.services.cloudtrail.processinglibrary.model.CloudTrailEvent
import com.amazonaws.services.cloudtrail.processinglibrary.model.CloudTrailLog
import com.amazonaws.services.cloudtrail.processinglibrary.model.LogDeliveryInfo
import com.amazonaws.services.cloudtrail.processinglibrary.serializer.RawLogDeliveryEventSerializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.salomonbrys.kotson.*
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import io.github.binaryfoo.cloudtail.propertiesFrom
import net.sourceforge.plantuml.FileFormat
import net.sourceforge.plantuml.FileFormatOption
import net.sourceforge.plantuml.SourceStringReader
import java.io.File
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.zip.GZIPInputStream

private val TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss")

private typealias EventFilter = (CloudTrailEvent) -> Boolean

private val CloudTrailEvent.rawEvent: String
    get() = (this.eventMetadata as LogDeliveryInfo).rawEvent

private val CloudTrailEvent.time: LocalDateTime
    get() = LocalDateTime.ofInstant(this.eventData.eventTime.toInstant(), ZoneId.of("UTC"))

fun main(args: Array<String>) {
    val exclude = Regex(propertiesFrom("config.properties").getProperty("exclusion_regex"))
    val wsdFile = File("tmp/all.wsd")

    val start = LocalDateTime.of(2017, 3, 24, 3, 30, 0, 0)
    val end = start.plusMinutes(30)

    writeWebSequenceDiagram(wsdFile) { !exclude.containsMatchIn(it.rawEvent) && it.time >= start && it.time <= end}

    drawSvgOfWsd(wsdFile)
}

private fun writeWebSequenceDiagram(wsdFile: File, include: EventFilter) {
    wsdFile.printWriter().use { out ->
        out.println("@startuml")
        processEvents("tmp") { event ->
            val eventName = event.eventData.eventName
            val server = quote(event.eventData.eventSource)
            val client = quote(event.eventData.sourceIPAddress)
            val userName = event.eventData.userIdentity.userName
            val request = formatJson(event.eventData.requestParameters)
            val response = formatJson(event.eventData.responseElements)

            if (include(event)) {
                val optionalUser = userName?.let { " ($it)" } ?: ""
                out.println("$client -> $server: ${TIME_FORMAT.format(event.time)} $eventName$optionalUser $request")
                if (response != "") {
                    out.println("$client <-- $server: $response")
                }
            }
        }
        out.println("@enduml")
    }
}

private val Sensitive = Regex("[ -]")
fun quote(s: String): String {
    // try to reduce noise by only quoting when required
    return if (Sensitive.containsMatchIn(s)) {
        '"' + s + '"'
    } else {
        s
    }
}

private val gson = GsonBuilder().setPrettyPrinting().create()
fun formatJson(s: String?): String {
    return (s?.let {
        val json = gson.fromJson<JsonObject>(it)
        if (json.contains("credentials")) {
            val credentials = json["credentials"].asJsonObject
            if (credentials.contains("sessionToken")) {
                credentials["sessionToken"] = "<token>"
            }
        }
        gson.toJson(json)
    }?:"").replace("\n", "\\n") // plantuml wants one line with \n for newline
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
