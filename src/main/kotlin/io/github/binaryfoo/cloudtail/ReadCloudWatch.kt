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
    val tenMinutesAgo = System.currentTimeMillis() - (30 * 60 * 1000)
    val observable = eventsSince(awsLogs, tenMinutesAgo).doAfterNext { fullEvents.println(it.rawEvent) }
    val exclude = Regex(propertiesFrom("config.properties").getProperty("exclusion_regex"))

    val wsdFile = File("tmp/recent.wsd")
    writeWebSequenceDiagram(observable, wsdFile) { !exclude.containsMatchIn(it.rawEvent) }
    fullEvents.close()

    drawSvgOfWsd(wsdFile)
}

private fun eventsSince(awsLogs: AWSLogs, tenMinutesAgo: Long): Observable<CloudTrailEvent> {
    return Observable.create { subscriber ->
        val response = awsLogs.filterLogEvents(FilterLogEventsRequest().withLogGroupName("CloudTrail/logs").withStartTime(tenMinutesAgo))
        println("Received ${response.events.size} events")
        if (response.nextToken != null) {
            println("Response truncated because pagination not implemented. Next token: ${response.nextToken}")
        }
        response.events.forEach { cloudWatchEvent ->
            parseEvents(cloudWatchEvent.message).forEach { cloudTrailEvent ->
                subscriber.onNext(cloudTrailEvent)
            }
        }
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