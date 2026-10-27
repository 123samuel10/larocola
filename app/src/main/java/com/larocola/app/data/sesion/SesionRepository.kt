package com.larocola.app.data.sesion

import com.larocola.app.core.util.AppResult
import com.larocola.app.data.model.SesionRegistroResponse
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.functions.functions
import io.ktor.client.call.body
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class SesionRepository(private val supabase: SupabaseClient) {

    suspend fun registrar(slug: String): AppResult<SesionRegistroResponse> = try {
        val response = supabase.functions.invoke(
            function = "registrar-dispositivo",
            body = buildJsonObject { put("slug", slug) },
        )
        val result = response.body<SesionRegistroResponse>()
        AppResult.Success(result)
    } catch (e: Exception) {
        val code = when {
            e.message?.contains("establecimiento_no_encontrado") == true -> "establecimiento_no_encontrado"
            else -> "network_error"
        }
        AppResult.Error(e.message ?: "Error al registrar dispositivo", code)
    }
}
