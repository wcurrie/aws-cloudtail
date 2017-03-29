package io.github.binaryfoo.cloudtail

import com.amazonaws.services.cloudtrail.processinglibrary.model.CloudTrailEvent
import com.amazonaws.services.cloudtrail.processinglibrary.model.CloudTrailLog
import com.amazonaws.services.logs.AWSLogs
import com.amazonaws.services.logs.AWSLogsClientBuilder
import com.amazonaws.services.logs.model.FilterLogEventsRequest
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.binaryfoo.cloudtail.parser.HeaderlessCloudTrailSerializer
import io.github.binaryfoo.cloudtail.writer.drawSvgOfWsd
import io.github.binaryfoo.cloudtail.writer.writeWebSequenceDiagram
import io.reactivex.Observable
import java.io.File
import java.util.*

/**
 * Pull recent logs cloudtrail from cloudwatch and sequence diagram them.
 * Requires cloudtrail to be forwarding logs to cloudwatch.
 */
fun main(args: Array<String>) {
    val awsLogs = AWSLogsClientBuilder.defaultClient()

    val fullEvents = File("tmp/events.json").printWriter()
    val since = System.currentTimeMillis() - (13 * 60 * 60 * 1000)
    val until = since + (60 * 60 * 1000)
    val observable = eventsSince(awsLogs, since, until).doAfterNext { fullEvents.println(it.rawEvent) }
    val exclude = Regex(propertiesFrom("config.properties").getProperty("exclusion_regex"))

    val wsdFile = File("tmp/recent.wsd")
    writeWebSequenceDiagram(observable, wsdFile) { !exclude.containsMatchIn(it.rawEvent) }
    fullEvents.close()

    drawSvgOfWsd(wsdFile)
}

private fun eventsSince(awsLogs: AWSLogs, tenMinutesAgo: Long, untilTime: Long? = null): Observable<CloudTrailEvent> {
    return Observable.create { subscriber ->
        val request = FilterLogEventsRequest()
                .withLogGroupName("CloudTrail/logs")
                .withFilterPattern("cloudformation")
                .withStartTime(tenMinutesAgo)
                .withEndTime(untilTime)
        do {
            val response = awsLogs.filterLogEvents(request)
            println("Received ${response.events.size} events nextToken ${response.nextToken}")
            response.events.forEach { cloudWatchEvent ->
                parseEvents(cloudWatchEvent.message).forEach { cloudTrailEvent ->
                    subscriber.onNext(cloudTrailEvent)
                }
            }
            request.nextToken = response.nextToken
        } while (response.nextToken != null)
        subscriber.onComplete()
    }
}

private val mapper = ObjectMapper()
private fun parseEvents(fullLogText: String): ArrayList<CloudTrailEvent> {
    val events = ArrayList<CloudTrailEvent>()
    val jsonParser = mapper.factory.createParser(fullLogText)
    val serializer = HeaderlessCloudTrailSerializer(fullLogText, CloudTrailLog("", ""), jsonParser)
    while (serializer.hasNextEvent()) {
        events.add(serializer.nextEvent)
    }
    return events
}