package io.github.binaryfoo.cloudtail.aggregations

import com.amazonaws.services.cloudtrail.processinglibrary.model.CloudTrailEvent
import io.github.binaryfoo.cloudtail.invokerArn
import io.reactivex.Observable

// IAM permissions used by each role in these events
fun groupByRoleAndEventName(events: Observable<CloudTrailEvent>) {
    var lastArn: String = ""

    events.groupBy { it.invokerArn }
            .flatMap { byArn -> byArn.groupBy { it.eventData.eventName } }
            .forEach { g ->
                g.toList().toObservable().forEach { g ->
                    val event = g[0]
                    if (lastArn != event.invokerArn) {
                        println(event.invokerArn)
                        lastArn = event.invokerArn
                    }
                    val service = event.eventData.eventSource.replace(Regex("\\..*"), "")
                    println("  " + service + ":" + event.eventData.eventName + " " + g.size)
                }
            }
}