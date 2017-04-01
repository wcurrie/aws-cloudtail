package io.github.binaryfoo.cloudtail.scrub

import com.github.salomonbrys.kotson.fromJson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import java.util.*

private val gson = GsonBuilder().setPrettyPrinting().create()

/**
 * Intent: remove any data tying a CloudTrail event back to an actual account.
 *
 * Warning: Heuristic based so surely leaves some sensitive data in the message!
 */
fun scrubEvent(raw: String): String {
    val json = gson.fromJson<JsonObject>(raw)
    scrub(json)
    return gson.toJson(json)
}

private val AccountId = Regex("(?<=\\D|^)\\d{12}(?=\\D|$)")
private val MOCK_ACCOUNT_ID = "123456789012"

private val UserARN = Regex(":user/(.*)$")
private val MOCK_USERNAME = "some_user"

private val AccessKeyId = Regex("(A[A-Z0-9]*)([A-Z0-9]{7})")

private val TrackingUUID = Regex("[-a-z0-9]{36}")

private fun scrub(json: JsonObject) {
    json.entrySet().forEach { e ->
        if (e.value is JsonPrimitive) {
            if ((e.value as JsonPrimitive).isString) {
                e.setValue(JsonPrimitive(e.value.asString.replace(AccountId, MOCK_ACCOUNT_ID)))
            }
            if (e.key.contains("user", ignoreCase = true)) {
                e.setValue(JsonPrimitive(MOCK_USERNAME))
            }
            if (e.key == "arn") {
                e.setValue(JsonPrimitive(e.value.asString.replace(UserARN, ":user/$MOCK_USERNAME")))
            }
            if (e.key == "principalId" || e.key == "accessKeyId") {
                e.setValue(JsonPrimitive(e.value.asString.replace(AccessKeyId, "$1EXAMPLE")))
            }
            if (TrackingUUID.matches(e.value.asString)) {
                e.setValue(JsonPrimitive(UUID.randomUUID().toString()))
            }
            if (e.key == "sourceIPAddress") {
                e.setValue(JsonPrimitive(fakeIpAddress()))
            }
        }
        if (e.value is JsonObject) {
            scrub(e.value as JsonObject)
        }
    }
}

private fun fakeIpAddress(): String {
    val rand = Random()
    return (1..4).map { rand.nextInt(253) + 1 }.joinToString(separator = ".")
}
