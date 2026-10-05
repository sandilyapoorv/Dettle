package com.dettle.app.data.api

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StreamingToolCallAccumulatorTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun testAccumulatesFragmentedToolCallDeltas() {
        val accumulator = StreamingToolCallAccumulator(json)

        // Chunk 1: index, id, name
        val chunk1 = json.parseToJsonElement(
            """[{"index":0,"id":"call_123","type":"function","function":{"name":"cloudflare_deploy_preview","arguments":""}}]"""
        )
        accumulator.ingestDelta(chunk1)
        assertTrue(accumulator.hasToolCalls())

        // Chunk 2: first fragment of arguments
        val chunk2 = json.parseToJsonElement(
            """[{"index":0,"function":{"arguments":"{\"project_name\":\"dettle-web\""}}]"""
        )
        accumulator.ingestDelta(chunk2)

        // Chunk 3: second fragment of arguments
        val chunk3 = json.parseToJsonElement(
            """[{"index":0,"function":{"arguments":",\"branch\":\"main\"}"}}]"""
        )
        accumulator.ingestDelta(chunk3)

        val primaryJson = accumulator.buildPrimaryToolCallJson()
        assertNotNull(primaryJson)
        assertTrue(primaryJson!!.contains("\"name\":\"cloudflare_deploy_preview\""))
        assertTrue(primaryJson.contains("\"project_name\":\"dettle-web\""))
        assertTrue(primaryJson.contains("\"branch\":\"main\""))

        val arrayJson = accumulator.buildJsonArrayString()
        assertTrue(arrayJson.startsWith("["))
        assertTrue(arrayJson.endsWith("]"))
        assertTrue(arrayJson.contains("call_123"))
    }

    @Test
    fun testHandlesEmptyArgumentsGracefully() {
        val accumulator = StreamingToolCallAccumulator(json)
        val chunk = json.parseToJsonElement(
            """[{"index":0,"id":"call_999","type":"function","function":{"name":"workspace_list_files","arguments":""}}]"""
        )
        accumulator.ingestDelta(chunk)

        val primaryJson = accumulator.buildPrimaryToolCallJson()
        assertNotNull(primaryJson)
        assertTrue(primaryJson!!.contains("\"arguments\":{}"))
    }

    @Test
    fun testMultipleToolCallsAccumulatedByIndex() {
        val accumulator = StreamingToolCallAccumulator(json)

        val chunk1 = json.parseToJsonElement(
            """[
                {"index":0,"id":"call_a","function":{"name":"tool_a","arguments":"{\"a\":1}"}},
                {"index":1,"id":"call_b","function":{"name":"tool_b","arguments":"{\"b\":2}"}}
            ]"""
        )
        accumulator.ingestDelta(chunk1)

        val arrayJson = accumulator.buildJsonArrayString()
        assertTrue(arrayJson.contains("tool_a"))
        assertTrue(arrayJson.contains("tool_b"))
        assertTrue(arrayJson.contains("\"a\":1"))
        assertTrue(arrayJson.contains("\"b\":2"))
    }
}
