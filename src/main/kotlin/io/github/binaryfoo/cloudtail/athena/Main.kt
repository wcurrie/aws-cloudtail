package io.github.binaryfoo.cloudtail.athena

import com.github.salomonbrys.kotson.set
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import io.github.binaryfoo.cloudtail.propertiesFrom
import java.io.File
import java.sql.DriverManager
import java.sql.ResultSet
import java.util.zip.GZIPOutputStream

private val gson = GsonBuilder().setPrettyPrinting().create()

fun main(args: Array<String>) {
    val properties = propertiesFrom("connection.properties")
    val connection = DriverManager.getConnection(properties.getProperty("jdbc.url"), properties)
//    val query = "select * from sampledb.cloudtrail_logs where eventtime > '2017-03-24T03:30:00Z' and eventtime < '2017-03-24T03:40:00Z'"
    val query = "select * from sampledb.cloudtrail_logs limit 1"
    val resultSet = timed("Query") { connection.createStatement().executeQuery(query) }
    val rowsRead = timed("Save") { save(resultSet) }
    println("wrote $rowsRead rows")
}

private fun save(resultSet: ResultSet): Int {
    var rowsRead = 0
    GZIPOutputStream(File("tmp/temp.json.gz").outputStream()).bufferedWriter().use { out ->
        val metaData = resultSet.metaData
        val jsonObject = JsonObject()
        while (resultSet.next()) {
            (1..metaData.columnCount).forEach { col ->
                jsonObject[metaData.getColumnName(col)] = resultSet.getString(col)
            }
            gson.toJson(jsonObject, out)
            rowsRead += 1
        }
    }
    return rowsRead
}

private fun <T> timed(name: String, block: () -> T): T {
    println("Started $name")
    val start = System.currentTimeMillis()
    val result = block()
    val elapsed = System.currentTimeMillis() - start
    println("$name took ${elapsed}ms")
    return result
}