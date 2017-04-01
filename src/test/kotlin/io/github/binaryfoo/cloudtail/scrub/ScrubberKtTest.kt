package io.github.binaryfoo.cloudtail.scrub

import com.github.salomonbrys.kotson.fromJson
import com.github.salomonbrys.kotson.get
import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import com.google.gson.JsonObject
import io.github.binaryfoo.cloudtail.writer.readResource
import org.junit.Test

class ScrubberKtTest {

    private val gson = Gson()

    @Test
    fun scrubsAwsAccountId() {
        val clean = scrubEvent(readResource("iam-user-event.json"))

        assertThat(clean).doesNotContain("004212846312")
        assertThat(fromJson(clean)["userIdentity"]["accountId"].asString).isEqualTo("123456789012")
    }

    @Test
    fun scrubsUserName() {
        val clean = scrubEvent(readResource("iam-user-event.json"))

        assertThat(clean).doesNotContain("actual_user")
        assertThat(fromJson(clean)["userIdentity"]["userName"].asString).isEqualTo("some_user")
    }

    @Test
    fun scrubsAccessKeyIds() {
        val clean = scrubEvent(readResource("iam-user-event.json"))

        assertThat(clean).doesNotContain("AIDAIFKABCDTMZ123ABCD")
        assertThat(clean).doesNotContain("AKIAIRABCDKBIABCD123")
        assertThat(fromJson(clean)["userIdentity"]["principalId"].asString).isEqualTo("AIDAIFKABCDTMZEXAMPLE")
        assertThat(fromJson(clean)["userIdentity"]["accessKeyId"].asString).isEqualTo("AKIAIRABCDKBIEXAMPLE")
    }

    @Test
    fun scrubsUUIDs() {
        val clean = scrubEvent(readResource("iam-user-event.json"))

        assertThat(clean).doesNotContain("7fc1146c-8da6-4217-991b-232cd3ad991f")
        assertThat(clean).doesNotContain("0f81451a-cefe-4d2d-adb7-73bdececb993")
        assertThat(fromJson(clean)["requestID"].asString).matches("[-a-z0-9]{36}")
        assertThat(fromJson(clean)["eventID"].asString).matches("[-a-z0-9]{36}")
    }

    @Test
    fun scrubsIpAddress() {
        val clean = scrubEvent(readResource("iam-user-event.json"))

        assertThat(clean).doesNotContain("101.12.13.123")
        assertThat(fromJson(clean)["sourceIPAddress"].asString).matches("[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}")
    }

    private fun fromJson(clean: String) = gson.fromJson<JsonObject>(clean)
}