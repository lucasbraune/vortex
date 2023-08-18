package vortex.protocol.v3

import com.google.gson.JsonParser
import org.junit.jupiter.api.Assertions.assertEquals

internal fun assertJsonEquals(expected: String, actual: String) {
    assertEquals(JsonParser.parseString(expected), JsonParser.parseString(actual))
}
