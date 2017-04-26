package io.github.binaryfoo.cloudtail

import com.amazonaws.services.logs.AWSLogsClientBuilder
import io.github.binaryfoo.cloudtail.aggregations.groupByRoleAndEventName
import io.github.binaryfoo.cloudtail.writer.*
import java.io.File
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/**
 * Grab recent CloudTrail events from CloudWatch Logs
 * Summarise IAM permissions by Role
 * Plot detail as sequence diagram
 */
fun main(args: Array<String>) {
    val since = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(4)
    val until = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(3)
    val events = eventsViaCloudWatchSince(AWSLogsClientBuilder.defaultClient(), since, until)
    val destination = File("tmp/local.json").apply { parentFile.mkdir(); deleteOnExit() }

    // group and dump table to console
    groupByRoleAndEventName(withRawEventsSaved(events, destination))

    // plot the events for drill down
    val diagram = Diagram(File("tmp/cloudwatch.wsd"), displayTimeZone = ZoneId.systemDefault())
    writeWebSequenceDiagram(readRawMsgsJson(destination), diagram)
    drawSvgOfWsd(diagram)
}