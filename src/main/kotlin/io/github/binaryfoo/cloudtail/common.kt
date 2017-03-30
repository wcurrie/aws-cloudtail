package io.github.binaryfoo.cloudtail

import com.amazonaws.services.cloudtrail.processinglibrary.model.CloudTrailEvent
import com.amazonaws.services.cloudtrail.processinglibrary.model.LogDeliveryInfo
import java.io.File
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*

val CloudTrailEvent.rawEvent: String
    get() = (this.eventMetadata as LogDeliveryInfo).rawEvent

val CloudTrailEvent.time: LocalDateTime
    get() = LocalDateTime.ofInstant(this.eventData.eventTime.toInstant(), ZoneId.of("UTC"))

val CloudTrailEvent.userIdentity: String?
    get() {
        if (eventData.userIdentity.userName != null) {
            return eventData.userIdentity.userName
        }
        if (eventData.userIdentity.principalId != null) {
            // drop prefix from AROABLAHBLAHMEXAMPLE:AWSCodeBuild
            val colonIndex = eventData.userIdentity.principalId.indexOf(':')
            return if (colonIndex != -1) eventData.userIdentity.principalId.substring(colonIndex + 1) else eventData.userIdentity.principalId
        }
        return null
    }

fun CloudTrailEvent.involves(actor: String): Boolean {
    return eventData.sourceIPAddress == actor || eventData.eventSource == actor
}

fun propertiesFrom(fileName: String): Properties {
    val file = File(fileName)
    val fileReader = if (file.exists())
        file.reader()
    else
        Thread.currentThread().contextClassLoader.getResourceAsStream(fileName).reader()

    return fileReader.use { reader ->
        Properties().apply { load(reader) }
    }
}