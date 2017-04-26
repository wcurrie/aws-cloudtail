package io.github.binaryfoo.cloudtail

import com.amazonaws.services.cloudtrail.processinglibrary.model.CloudTrailEvent
import com.amazonaws.services.cloudtrail.processinglibrary.model.LogDeliveryInfo
import com.github.salomonbrys.kotson.fromJson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import java.io.File
import java.time.LocalDateTime
import java.time.ZoneId
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

val CloudTrailEvent.requestParametersJson: JsonObject
    get() = gson.fromJson(eventData.requestParameters)

val CloudTrailEvent.invokerArn: String
    get() = when (eventData.userIdentity.identityType) {
            "AssumedRole" -> eventData.userIdentity.sessionContext.sessionIssuer.arn
            "IAMUser" -> eventData.userIdentity.arn
            "AWSService" -> requestParametersJson["roleArn"].asString // AWS service calling AssumeRole
            "AWSAccount" -> requestParametersJson["roleArn"].asString // Another account calling AssumeRole
            else -> throw Exception("Unknown invoker arn in: $prettyJson")
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