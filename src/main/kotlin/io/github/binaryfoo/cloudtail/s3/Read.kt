package io.github.binaryfoo.cloudtail.s3

import com.amazonaws.services.cloudtrail.processinglibrary.model.CloudTrailEvent
import com.amazonaws.services.cloudtrail.processinglibrary.model.CloudTrailLog
import com.amazonaws.services.cloudtrail.processinglibrary.model.LogDeliveryInfo
import com.amazonaws.services.cloudtrail.processinglibrary.serializer.RawLogDeliveryEventSerializer
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.binaryfoo.cloudtail.propertiesFrom
import net.sourceforge.plantuml.FileFormat
import net.sourceforge.plantuml.FileFormatOption
import net.sourceforge.plantuml.SourceStringReader
import org.joda.time.Interval
import java.io.File
import java.time.*
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

    val plantUml = SourceStringReader(wsdFile.readText())
    File("tmp/trail.svg").outputStream().use { out ->
        plantUml.generateImage(out, FileFormatOption(FileFormat.SVG))
    }
}

private fun writeWebSequenceDiagram(wsdFile: File, include: EventFilter) {
    wsdFile.printWriter().use { out ->
        out.println("@startuml")
        processEvents("tmp") { event ->
            val eventName = event.eventData.eventName
            val server = quote(event.eventData.eventSource)
            val client = quote(event.eventData.sourceIPAddress)
            val userName = event.eventData.userIdentity.userName
            val request = quote(compress(event.eventData.requestParameters))
            val response = quote(compress(event.eventData.responseElements))

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

private val Sensitive = Regex("[\" -]")
fun quote(json: String): String {
    // try to reduce noise by only quoting when required
    return if (Sensitive.containsMatchIn(json)) {
        '"' + json.replace("\"", "\\\"") + '"'
    } else {
        json
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
