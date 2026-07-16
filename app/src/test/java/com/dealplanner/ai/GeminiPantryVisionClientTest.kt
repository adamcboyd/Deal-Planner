package com.dealplanner.ai

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

class GeminiPantryVisionClientTest {

    @Test
    fun `blank key is not configured`() {
        val client = GeminiPantryVisionClient(apiKey = "", model = "gemini-3.5-flash")

        assertThat(client.isConfigured()).isFalse()
    }

    @Test
    fun `placeholder key is not configured`() {
        val client = GeminiPantryVisionClient(apiKey = "YOUR_GEMINI_API_KEY", model = "gemini-3.5-flash")

        assertThat(client.isConfigured()).isFalse()
    }

    @Test
    fun `real-looking key is configured`() {
        val client = GeminiPantryVisionClient(apiKey = "test-real-key-for-unit-tests", model = "gemini-3.5-flash")

        assertThat(client.isConfigured()).isTrue()
    }

    @Test
    fun `blank model falls back to default model name`() {
        val client = GeminiPantryVisionClient(apiKey = "test-real-key-for-unit-tests", model = "")

        assertThat(client.modelName).isEqualTo("gemini-3.5-flash")
    }

    @Test
    fun `connection test reports missing key without network`() = runTest {
        val client = GeminiPantryVisionClient(apiKey = "", model = "gemini-3.5-flash")

        val result = client.testConnection()

        assertThat(result.success).isFalse()
        assertThat(result.message).contains("not configured")
    }
}
