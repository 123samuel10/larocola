package com.larocola.app.data.solicitud

import com.larocola.app.core.util.AppResult
import com.larocola.app.data.model.ColaResponse
import com.larocola.app.data.model.SolicitudResponse
import com.larocola.app.data.model.VotoResponse
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.functions.functions
import io.ktor.client.call.body
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class SolicitudRepository(private val supabase: SupabaseClient) {

    suspend fun solicitar(sesionId: String, cancionId: String): AppResult<SolicitudResponse> = try {
        val response = supabase.functions.invoke(
            function = "solicitar-cancion",
            body = buildJsonObject {
                put("sesion_id", sesionId)
                put("cancion_id", cancionId)
            },
        )
        AppResult.Success(response.body<SolicitudResponse>())
    } catch (e: Exception) {
        AppResult.Error(e.message ?: "Error al solicitar canción")
    }

    suspend fun votar(solicitudId: String): AppResult<VotoResponse> = try {
        val response = supabase.functions.invoke(
            function = "votar-cancion",
            body = buildJsonObject { put("solicitud_id", solicitudId) },
        )
        AppResult.Success(response.body<VotoResponse>())
    } catch (e: Exception) {
        AppResult.Error(e.message ?: "Error al votar")
    }

    suspend fun obtenerCola(sesionId: String): AppResult<ColaResponse> = try {
        val response = supabase.functions.invoke(
            function = "obtener-cola",
            body = buildJsonObject { put("sesion_id", sesionId) },
        )
        AppResult.Success(response.body<ColaResponse>())
    } catch (e: Exception) {
        AppResult.Error(e.message ?: "Error al cargar la cola")
    }
}
