package com.larocola.app.ui.navigation

sealed class Destino(val ruta: String) {
    data object Inicio : Destino("inicio/{slug}") {
        fun crear(slug: String) = "inicio/$slug"
    }
    data object Busqueda : Destino("busqueda")
    data object Cola : Destino("cola")
    data object ServicioNoDisponible : Destino("servicio/{slug}") {
        fun crear(slug: String) = "servicio/$slug"
    }
}
