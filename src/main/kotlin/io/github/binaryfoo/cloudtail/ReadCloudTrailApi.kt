package io.github.binaryfoo.cloudtail

import com.amazonaws.services.cloudtrail.AWSCloudTrail
import com.amazonaws.services.cloudtrail.AWSCloudTrailClientBuilder
import com.amazonaws.services.cloudtrail.model.LookupEventsRequest
import com.amazonaws.services.cloudtrail.processinglibrary.model.CloudTrailEvent
import io.github.binaryfoo.cloudtail.parser.parseEvents
import io.github.binaryfoo.cloudtail.writer.Diagram
import io.github.binaryfoo.cloudtail.writer.drawSvgOfWsd
import io.github.binaryfoo.cloudtail.writer.writeWebSequenceDiagram
import io.reactivex.Observable
import java.io.File
import java.time.ZoneId
import java.util.*
import java.util.concurrent.TimeUnit

fun main(args: Array<String>) {
    val until = System.currentTimeMillis()
    val since = until - TimeUnit.HOURS.toMillis(3)

    val client = AWSCloudTrailClientBuilder.defaultClient()
    val events = client.eventsViaCloudTrailApiSince(since, until)

    println("Querying from ${since.asUTC()} to ${until.asUTC()}")

    val diagram = Diagram(File("tmp/recent.wsd"), displayTimeZone = ZoneId.systemDefault())
    writeWebSequenceDiagram(events, diagram)
    drawSvgOfWsd(diagram)
}

/**
 * Retrieve CloudTrail events directly from cloudtrail api endpoint
 */
fun AWSCloudTrail.eventsViaCloudTrailApiSince(fromTime: Long, untilTime: Long): Observable<CloudTrailEvent> {
    return Observable.create { subscriber ->
        val request = LookupEventsRequest().apply {
            withStartTime(Date(fromTime))
            withEndTime(Date(untilTime))
        }
        do {
            val response = lookupEvents(request)
            println("Received ${response.events.size} events nextToken ${response.nextToken}")
            response.events.forEach { event ->
                parseEvents(event.cloudTrailEvent, hasHeader = false).forEach { cloudTrailEvent ->
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
