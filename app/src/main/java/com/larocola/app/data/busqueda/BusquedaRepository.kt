package com.larocola.app.data.busqueda

import com.larocola.app.core.util.AppResult
import com.larocola.app.data.model.BusquedaResponse
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.functions.functions
import io.ktor.client.call.body
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class BusquedaRepository(private val supabase: SupabaseClient) {

    suspend fun buscar(query: String, sesionId: String? = null): AppResult<BusquedaResponse> = try {
        val response = supabase.functions.invoke(
            function = "youtube-search",
            body = buildJsonObject {
                put("q", query)
                sesionId?.let { put("sesion_id", it) }
            },
        )
        AppResult.Success(response.body<BusquedaResponse>())
    } catch (e: Exception) {
        val code = if (e.message?.contains("cuota_agotada") == true) "cuota_agotada"
                   else if (e.message?.contains("rate_limit") == true) "rate_limit"
                   else "network_error"
        AppResult.Error(e.message ?: "Error de búsqueda", code)
    }
}
