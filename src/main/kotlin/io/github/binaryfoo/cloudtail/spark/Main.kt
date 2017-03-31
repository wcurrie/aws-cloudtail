package io.github.binaryfoo.cloudtail.spark

import io.github.binaryfoo.cloudtail.drawEvents
import io.github.binaryfoo.cloudtail.writer.Diagram
import spark.Response
import spark.Spark.get
import spark.Spark.staticFileLocation
import java.io.File
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

fun main(args: Array<String>) {
    defineResources()
}

fun defineResources() {
    staticFileLocation("/public")

    get("/draw") { req, res ->
        val from = req.queryParams("from")?.let(String::toLong) ?: (System.currentTimeMillis() - (10 * 60 * 1000))
        val to = req.queryParams("to")?.let(String::toLong) ?: (System.currentTimeMillis())
        val limit = req.queryParams("limit")?.let(String::toInt) ?: (2000)
        draw(from, to, limit, res)
    }

    get("/range") { req, res ->
        val range = req.queryParams("daterange")
        val limit = req.queryParams("limit").toInt()
        val (from , to) = parseDateRange(range)
        draw(from, to, limit, res)
    }
}

fun parseDateRange(range: String): Pair<Long, Long> {
    val (from, to) = range.split(" - ")
    return Pair(parseDateTime(from), parseDateTime(to))
}

private val DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM/dd h:mm a XXX")
private fun parseDateTime(from: String) = ZonedDateTime.parse(from, DATETIME_FORMAT).toEpochSecond() * 1000

private fun draw(from: Long, to: Long, maxEvents: Int, res: Response): String {
    val diagram = Diagram(tempFile("events", ".wsd"), maxEvents)

    drawEvents(diagram, from, to)
    val html = diagram.html.readText()
    diagram.delete()

    res.type("text/html")
    return html
}

private fun tempFile(prefix: String, suffix: String): File {
    return File.createTempFile(prefix, suffix).apply {
        deleteOnExit()
    }
}