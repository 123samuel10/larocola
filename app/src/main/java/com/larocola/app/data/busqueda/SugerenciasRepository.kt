package com.larocola.app.data.busqueda

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

class SugerenciasRepository(private val httpClient: HttpClient) {

    suspend fun obtenerSugerencias(texto: String): List<String> = try {
        val raw = httpClient.get("https://suggestqueries.google.com/complete/search") {
            parameter("client", "firefox")
            parameter("ds", "yt")
            parameter("hl", "es")
            parameter("q", texto)
        }.bodyAsText()
        val arr = Json.parseToJsonElement(raw).jsonArray
        if (arr.size < 2) return emptyList()
        (arr[1] as JsonArray)
            .take(5)
            .mapNotNull { runCatching { it.jsonPrimitive.content }.getOrNull() }
    } catch (_: Exception) {
        emptyList()
    }
}
