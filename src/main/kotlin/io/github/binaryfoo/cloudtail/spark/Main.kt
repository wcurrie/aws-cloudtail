package io.github.binaryfoo.cloudtail.spark

import io.github.binaryfoo.cloudtail.drawEvents
import io.github.binaryfoo.cloudtail.writer.Diagram
import spark.Spark.get
import java.io.File

fun main(args: Array<String>) {
    get("/draw") { req, res ->
        val from = req.queryParams("from")?.let(String::toLong)?:(System.currentTimeMillis()-(10*60*1000))
        val to = req.queryParams("to")?.let(String::toLong)?:(System.currentTimeMillis())
        val diagram = Diagram(tempFile("events", ".wsd"))

        drawEvents(diagram, from, to)
        val html = diagram.html.readText()
        diagram.delete()

        res.type("text/html")
        html
    }
}

private fun tempFile(prefix: String, suffix: String): File {
    return File.createTempFile(prefix, suffix).apply {
        deleteOnExit()
    }
}