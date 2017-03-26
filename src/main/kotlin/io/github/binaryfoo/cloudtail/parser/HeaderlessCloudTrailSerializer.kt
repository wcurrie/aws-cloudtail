package io.github.binaryfoo.cloudtail.parser

import com.amazonaws.services.cloudtrail.processinglibrary.model.CloudTrailLog
import com.amazonaws.services.cloudtrail.processinglibrary.serializer.RawLogDeliveryEventSerializer
import com.fasterxml.jackson.core.JsonParser

/**
 * Like RawLogDeliveryEventSerializer but without requiring a "Results: [] around the cloudtrail events"
 */
class HeaderlessCloudTrailSerializer(logFile: String?, ctLog: CloudTrailLog?, jsonParser: JsonParser?) : RawLogDeliveryEventSerializer(logFile, ctLog, jsonParser) {

    override fun readArrayHeader() {
        // there isn't one when reading from cloudwatch logs
    }
}
