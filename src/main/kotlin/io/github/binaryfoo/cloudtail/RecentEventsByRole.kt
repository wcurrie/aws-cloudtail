package io.github.binaryfoo.cloudtail

import com.amazonaws.services.logs.AWSLogsClientBuilder
import io.github.binaryfoo.cloudtail.aggregations.groupByRoleAndEventName
import java.util.concurrent.TimeUnit

fun main(args: Array<String>) {
    val since = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(4)
    val until = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(3)
    val events = eventsViaCloudWatchSince(AWSLogsClientBuilder.defaultClient(), since, until)

    groupByRoleAndEventName(events)
}