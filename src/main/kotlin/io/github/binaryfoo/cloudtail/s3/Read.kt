package io.github.binaryfoo.cloudtail.s3

import com.amazonaws.services.cloudtrail.processinglibrary.model.CloudTrailLog
import com.amazonaws.services.cloudtrail.processinglibrary.serializer.DefaultEventSerializer
import com.fasterxml.jackson.databind.ObjectMapper
import java.io.File
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.zip.GZIPInputStream

private val TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss")

fun main(args: Array<String>) {
    val mapper = ObjectMapper()
    File("tmp").listFiles().sorted().forEach { logFile ->
        GZIPInputStream(logFile.inputStream()).use { inputStream ->
            val jsonParser = mapper.factory.createParser(inputStream)
            val serializer = DefaultEventSerializer(CloudTrailLog("", ""), jsonParser)
            while (serializer.hasNextEvent()) {
                val event = serializer.nextEvent
                val server = event.eventData.eventSource
                val client = event.eventData.sourceIPAddress
                val time = LocalDateTime.ofInstant(event.eventData.eventTime.toInstant(), ZoneId.of("UTC"))

                println("${TIME_FORMAT.format(time)}: $client -> $server")
            }
        }
    }
}
