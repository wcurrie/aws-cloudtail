package io.github.binaryfoo.cloudtail.parser

import com.amazonaws.services.cloudtrail.processinglibrary.model.CloudTrailEvent
import com.amazonaws.services.cloudtrail.processinglibrary.model.CloudTrailLog
import com.amazonaws.services.cloudtrail.processinglibrary.serializer.RawLogDeliveryEventSerializer
import com.fasterxml.jackson.databind.ObjectMapper
import java.util.ArrayList

private val mapper = ObjectMapper()
fun parseEvents(fullLogText: String, hasHeader: Boolean): ArrayList<CloudTrailEvent> {
    val events = ArrayList<CloudTrailEvent>()
    val jsonParser = mapper.factory.createParser(fullLogText)
    val serializer = if (hasHeader) {
        RawLogDeliveryEventSerializer(fullLogText, CloudTrailLog("", ""), jsonParser)
    } else {
        HeaderlessCloudTrailSerializer(fullLogText, CloudTrailLog("", ""), jsonParser)
    }
    while (serializer.hasNextEvent()) {
        events.add(serializer.nextEvent)
    }
    return events
}
