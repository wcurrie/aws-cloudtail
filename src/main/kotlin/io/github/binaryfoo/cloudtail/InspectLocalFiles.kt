package io.github.binaryfoo.cloudtail

import com.github.salomonbrys.kotson.fromJson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import io.github.binaryfoo.cloudtail.writer.readRawMsgsJson
import java.io.File

fun main(args: Array<String>) {
    val gson = GsonBuilder().setPrettyPrinting().create()

    readRawMsgsJson(File("tmp/recent.json"))
            .groupBy { it.eventData.userIdentity.identityType + "-" + it.eventData.eventName }
            .flatMap { g -> g.toList().toObservable().filter { it.size < 5 }.map { Pair(g.key, it) } }
            .forEach {
                println(it.first)
                it.second.forEach { e ->
                    val json = gson.fromJson<JsonObject>(e.rawEvent)
                    println(gson.toJson(json))
                }
                println()
            }

}