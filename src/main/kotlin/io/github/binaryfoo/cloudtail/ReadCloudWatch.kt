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

/**
 * Pull recent logs cloudtrail from cloudwatch and sequence diagram them.
 * Requires cloudtrail to be forwarding logs to cloudwatch.
 */
fun main(args: Array<String>) {
    val wsdFile = File("tmp/recent.wsd")
    val since = System.currentTimeMillis() - (60 * 60 * 1000)
    val until = since + (60 * 60 * 1000)
    val exclude = Regex(propertiesFrom("config.properties").getProperty("exclusion_regex"))

    drawEvents(Diagram(wsdFile), since, until) { !exclude.containsMatchIn(it.rawEvent) }
}

fun drawEvents(diagram: Diagram, since: Long, until: Long, include: EventFilter) {
    val awsLogs = AWSLogsClientBuilder.defaultClient()
    val observable = eventsSince(awsLogs, since, until)

    writeWebSequenceDiagram(observable, diagram, include = include)
    drawSvgOfWsd(diagram)
}

private fun eventsSince(awsLogs: AWSLogs, fromTime: Long, untilTime: Long? = null): Observable<CloudTrailEvent> {
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

