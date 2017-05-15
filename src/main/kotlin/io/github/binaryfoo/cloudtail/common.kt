package io.github.binaryfoo.cloudtail

import com.amazonaws.services.cloudtrail.processinglibrary.model.CloudTrailEvent
import com.amazonaws.services.cloudtrail.processinglibrary.model.LogDeliveryInfo
import com.github.salomonbrys.kotson.fromJson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import java.io.File
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.*

val CloudTrailEvent.rawEvent: String
    get() = (this.eventMetadata as LogDeliveryInfo).rawEvent

val CloudTrailEvent.time: LocalDateTime
    get() = LocalDateTime.ofInstant(this.eventData.eventTime.toInstant(), ZoneId.of("UTC"))

fun CloudTrailEvent.timeInZone(zoneId: ZoneId): LocalDateTime {
    return LocalDateTime.ofInstant(this.eventData.eventTime.toInstant(), zoneId)
}

private val gson = GsonBuilder().setPrettyPrinting().create()
val CloudTrailEvent.prettyJson: String
    get() = gson.toJson(rawJson)

val CloudTrailEvent.rawJson: JsonObject
    get() = gson.fromJson<JsonObject>(rawEvent)

val CloudTrailEvent.userIdentity: String?
    get() {
        return if (eventData.userIdentity.userName != null) {
            eventData.userIdentity.userName
        } else {
            invokerArn.let {
                val slash = it.indexOf('/')
                if (slash != -1) it.substring(slash + 1) else it
            }
        }
    }

val CloudTrailEvent.requestParametersJson: JsonObject
    get() = gson.fromJson(eventData.requestParameters)

val CloudTrailEvent.invokerArn: String
    get() {
        val identityType = eventData.userIdentity?.identityType ?: eventData.userIdentity.get("type")
        return when (identityType) {
            "AssumedRole" -> eventData.userIdentity?.sessionContext?.sessionIssuer?.arn ?: eventData.userIdentity.arn
            "IAMUser" -> eventData.userIdentity.arn
            "AWSService" -> {
                // AWS service calling AssumeRole
                requestParametersJson["roleArn"]?.asString ?: eventData.userIdentity.invokedBy
            }
            "AWSAccount" -> {
                // Another account calling AssumeRole or s3 data event
                requestParametersJson["roleArn"]?.asString ?: eventData.userIdentity.accountId + "/" + eventData.userIdentity.principalId
            }
            else -> throw Exception("Unknown invoker arn in: $prettyJson")
        }
    }

fun CloudTrailEvent.involves(actor: String): Boolean {
    return eventData.sourceIPAddress == actor || eventData.eventSource == actor
}

fun compareEventsByTimestamp(e1: CloudTrailEvent, e2: CloudTrailEvent): Int {
    return e1.eventData.eventTime.compareTo(e2.eventData.eventTime)
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

fun Long.asUTC() = LocalDateTime.ofEpochSecond(this / 1000, 0, ZoneOffset.UTC)