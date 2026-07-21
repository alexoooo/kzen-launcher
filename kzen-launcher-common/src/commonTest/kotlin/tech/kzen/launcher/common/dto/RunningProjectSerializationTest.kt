package tech.kzen.launcher.common.dto

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull


// Pins the wire contract with kzen-shell's RunningProjectStatus, which shares this shape by convention
//  rather than by code. Test names are valid JS identifiers (see CommonTest).
class RunningProjectSerializationTest {
    private val json = Json { ignoreUnknownKeys = true }


    @Test
    fun decodes_an_exited_project_with_its_detail() {
        val decoded = json.decodeFromString<RunningProject>(
            """{"name":"x","state":"exited","exitCode":3,"recentOutput":["a","b"]}""")

        assertEquals(RunningState.EXITED, decoded.state)
        assertEquals(3, decoded.exitCode)
        assertEquals(listOf("a", "b"), decoded.recentOutput)
    }


    @Test
    fun decodes_a_project_without_detail() {
        val decoded = json.decodeFromString<RunningProject>(
            """{"name":"x","state":"running"}""")

        assertEquals(RunningState.RUNNING, decoded.state)
        assertNull(decoded.exitCode)
        assertNull(decoded.recentOutput)
    }


    @Test
    fun decodes_explicitly_null_detail() {
        // Ktor's default Json encodes defaults, so the server emits the absent fields as explicit nulls.
        val decoded = json.decodeFromString<RunningProject>(
            """{"name":"x","state":"starting","exitCode":null,"recentOutput":null}""")

        assertNull(decoded.exitCode)
        assertNull(decoded.recentOutput)
    }
}
