package io.github.binaryfoo.cloudtail.writer

import com.amazonaws.services.cloudtrail.processinglibrary.model.CloudTrailEvent
import com.github.salomonbrys.kotson.contains
import com.github.salomonbrys.kotson.fromJson
import com.github.salomonbrys.kotson.keys
import com.github.salomonbrys.kotson.set
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import io.github.binaryfoo.cloudtail.rawEvent
import io.github.binaryfoo.cloudtail.time
import io.github.binaryfoo.cloudtail.userIdentity
import io.reactivex.Observable
import net.sourceforge.plantuml.FileFormat
import net.sourceforge.plantuml.FileFormatOption
import net.sourceforge.plantuml.SourceStringReader
import java.io.File
import java.io.PrintWriter
import java.time.format.DateTimeFormatter

private val TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss")

private typealias EventFilter = (CloudTrailEvent) -> Boolean

data class Diagram(val wsdFile: File) {
    val events = File(wsdFile.parent, wsdFile.name.replace(".wsd", ".json"))
    val html   = File(wsdFile.parent, wsdFile.name.replace(".wsd", ".html"))

    fun delete() {
        wsdFile.delete()
        events.delete()
        html.delete()
    }
}

fun writeWebSequenceDiagram(events: Observable<CloudTrailEvent>, diagram: Diagram, labelArrows: Boolean = false, include: EventFilter) {
    val rawMsgsFile = diagram.events
    val rawMsgWriter = rawMsgsFile.printWriter()
    rawMsgWriter.print("var rawMsgs = [")
    var rawIndex = 0

    diagram.wsdFile.printWriter().use { out ->
        out.println("@startuml")
        events.subscribe { event ->
            val eventName = event.eventData.eventName
            val server = quote(event.eventData.eventSource)
            val client = quote(event.eventData.sourceIPAddress)
            val userName = event.userIdentity
            val request = if (labelArrows) formatJson(event.eventData.requestParameters) else ""
            val response = if (labelArrows) formatJson(event.eventData.responseElements) else ""

            if (include(event)) {
                val optionalUser = userName?.let { " ($it)" } ?: ""
                val formattedTime = TIME_FORMAT.format(event.time)
                val linkToRawMsg = "[[javascript:showRawMsg($rawIndex) $formattedTime]]"
                out.println("$client -> $server: $linkToRawMsg $eventName$optionalUser $request")
                if (labelArrows && response != "") {
                    out.println("$client <-- $server: $response")
                }
                rawMsgWriter.print(event.rawEvent)
                rawMsgWriter.println(",")
                rawIndex += 1
            }
        }
        out.println("@enduml")
    }

    rawMsgWriter.println("];")
    rawMsgWriter.close()
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
                credentials["sessionToken"] = "..."
            }
        }
        json.keys().forEach { key ->
            if (json[key].isJsonPrimitive && json[key].asString.length > 100) {
                json[key] = json[key].asString.substring(0, 100) + "..."
            }
        }
        gson.toJson(json)
    } ?: "").replace("\n", "\\n") // plantuml wants one line with \n for newline
}


/**
 * Render a web (plantuml) sequence diagram as SVG embedded in html.
 */
fun drawSvgOfWsd(diagram: Diagram) {
    val plantUml = SourceStringReader(diagram.wsdFile.readText())
    val outputFile = diagram.html
    val rawMsgsFile = diagram.events
    println("Rendering ${diagram.wsdFile} to $outputFile")
    outputFile.outputStream().use { out ->
        val writer = PrintWriter(out)
        writer.println("""<html>
    <head>
        <style>${readResource("main.css")}</style>
        <style>${readResource("highlightjs.default.min.css")}</style>
        <script src="https://cdnjs.cloudflare.com/ajax/libs/highlight.js/9.10.0/highlight.min.js"></script>
        <script type="text/javascript">${readResource("scroll-header.js")}</script>
        <script type="text/javascript">${rawMsgsFile.readText()}</script>
    <head>
    <body>
""")
        writer.flush()
        plantUml.generateImage(out, FileFormatOption(FileFormat.SVG))
        out.flush()
        writer.println("""
        <div id="popup">
            <span id="popupclose">X</span>
            <code id="popupcontent" class="hljs json">
            </code>
        </div>
        <script type="text/javascript">${readResource("show-raw-msg.js")}</script>
    </body>
</html>
""")
        writer.close()
    }
}

fun readResource(fileName: String): String {
    return Thread.currentThread().contextClassLoader.getResourceAsStream(fileName).reader().readText()
}