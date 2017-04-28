package io.github.binaryfoo.cloudtail

import com.amazonaws.services.cloudtrail.processinglibrary.model.CloudTrailEvent
import com.amazonaws.services.logs.AWSLogs
import com.amazonaws.services.logs.AWSLogsClientBuilder
import com.amazonaws.services.logs.model.FilterLogEventsRequest
import io.github.binaryfoo.cloudtail.parser.parseEvents
import io.github.binaryfoo.cloudtail.writer.Diagram
import io.github.binaryfoo.cloudtail.writer.EventFilter
import io.github.binaryfoo.cloudtail.writer.drawSvgOfWsd
import io.github.binaryfoo.cloudtail.writer.writeWebSequenceDiagram
import io.reactivex.Observable
import java.io.File
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.concurrent.TimeUnit

/**
 * Pull recent logs cloudtrail from cloudwatch and sequence diagram them.
 * Requires cloudtrail to be forwarding logs to cloudwatch.
 */
fun main(args: Array<String>) {
    val diagram = Diagram(File("tmp/recent.wsd"), displayTimeZone = ZoneId.systemDefault())
    val until = System.currentTimeMillis()
    val since = until - TimeUnit.MINUTES.toMillis(30)
    val exclude = Regex(propertiesFrom("config.properties").getProperty("exclusion_regex"))

    drawEvents(diagram, since, until) { !exclude.containsMatchIn(it.rawEvent) }
}

fun drawEvents(diagram: Diagram, since: Long, until: Long, include: EventFilter) {
    val awsLogs = AWSLogsClientBuilder.defaultClient()
    val observable = eventsViaCloudWatchSince(awsLogs, since, until)
            .filter(include)
            .sorted(::compareEventsByTimestamp)

    println("Querying from ${asUTC(since)} to ${asUTC(until)}")

    writeWebSequenceDiagram(observable, diagram)
    drawSvgOfWsd(diagram)
}

private fun asUTC(since: Long) = LocalDateTime.ofEpochSecond(since / 1000, 0, ZoneOffset.UTC)

fun eventsViaCloudWatchSince(awsLogs: AWSLogs, fromTime: Long, untilTime: Long? = null): Observable<CloudTrailEvent> {
    return Observable.create { subscriber ->
        val request = FilterLogEventsRequest()
                .withLogGroupName("CloudTrail/logs")
                .withStartTime(fromTime)
                .withEndTime(untilTime)
        do {
            val response = awsLogs.filterLogEvents(request)
            println("Received ${response.events.size} events nextToken ${response.nextToken}")
            response.events.forEach { cloudWatchEvent ->
                parseEvents(cloudWatchEvent.message, hasHeader = false).forEach { cloudTrailEvent ->
                    if (!subscriber.isDisposed) {
                        subscriber.onNext(cloudTrailEvent)
                    }
                }
            }
            request.nextToken = response.nextToken
        } while (response.nextToken != null && !subscriber.isDisposed)
        subscriber.onComplete()
    }
}

