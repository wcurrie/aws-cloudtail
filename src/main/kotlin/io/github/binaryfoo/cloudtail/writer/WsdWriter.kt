package io.github.binaryfoo.cloudtail.writer

import com.amazonaws.services.cloudtrail.processinglibrary.model.CloudTrailEvent
import com.github.salomonbrys.kotson.contains
import com.github.salomonbrys.kotson.fromJson
import com.github.salomonbrys.kotson.set
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import io.github.binaryfoo.cloudtail.time
import io.reactivex.Observable
import java.io.File
import java.time.format.DateTimeFormatter

private val TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss")

private typealias EventFilter = (CloudTrailEvent) -> Boolean

fun writeWebSequenceDiagram(events: Observable<CloudTrailEvent>, wsdFile: File, include: EventFilter) {
    wsdFile.printWriter().use { out ->
        out.println("@startuml")
        events.subscribe { event ->
            val eventName = event.eventData.eventName
            val server = quote(event.eventData.eventSource)
            val client = quote(event.eventData.sourceIPAddress)
            val userName = event.eventData.userIdentity.userName
            val request = formatJson(event.eventData.requestParameters)
            val response = formatJson(event.eventData.responseElements)

            if (include(event)) {
                val optionalUser = userName?.let { " ($it)" } ?: ""
                out.println("$client -> $server: ${TIME_FORMAT.format(event.time)} $eventName$optionalUser $request")
                if (response != "") {
                    out.println("$client <-- $server: $response")
                }
            }
        }
        out.println("@enduml")
    }
}

private val Sensitive = Regex("[ -]")
fun quote(s: String): String {
    // try to reduce noise by only quoting when required
    return if (Sensitive.containsMatchIn(s)) {
        '"' + s + '"'
    } else {
        s
    }
}

private val gson = GsonBuilder().setPrettyPrinting().create()
fun formatJson(s: String?): String {
    return (s?.let {
        val json = gson.fromJson<JsonObject>(it)
        if (json.contains("credentials")) {
            val credentials = json["credentials"].asJsonObject
            if (credentials.contains("sessionToken")) {
                credentials["sessionToken"] = "SNIPPED"
            }
        }
        gson.toJson(json)
    } ?: "").replace("\n", "\\n") // plantuml wants one line with \n for newline
}