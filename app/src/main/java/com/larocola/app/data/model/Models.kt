package com.larocola.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Establecimiento(
    val id: String,
    val nombre: String,
    val slug: String,
)

@Serializable
data class ProximoHorario(
    val etiqueta: String,
    val inicio: String,
    val fin: String,
)

@Serializable
data class SesionRegistroResponse(
    val estado: String,
    @SerialName("sesion_id") val sesionId: String?,
    val establecimiento: Establecimiento,
    @SerialName("proximos_horarios") val proximosHorarios: List<ProximoHorario>,
)

@Serializable
data class Cancion(
    @SerialName("cancion_id") val cancionId: String?,
    @SerialName("youtube_id") val youtubeId: String,
    val titulo: String,
    val artista: String,
    @SerialName("duracion_seg") val duracionSeg: Int,
    @SerialName("miniatura_url") val miniaturaUrl: String,
    val reproducible: Boolean,
    @SerialName("motivo_no_reproducible") val motivoNoReproducible: String?,
)

@Serializable
data class BusquedaResponse(
    val cache: Boolean,
    val total: Int,
    val resultados: List<Cancion>,
)

@Serializable
data class RegistrarDispositivoRequest(val slug: String)

@Serializable
data class BusquedaRequest(
    val q: String,
    @SerialName("sesion_id") val sesionId: String? = null,
)
