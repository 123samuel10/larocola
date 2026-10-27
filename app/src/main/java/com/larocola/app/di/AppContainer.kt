package com.larocola.app.di

import android.content.Context
import com.larocola.app.core.network.ConnectivityObserver
import com.larocola.app.core.network.createAppSupabaseClient
import com.larocola.app.data.auth.AuthRepository
import com.larocola.app.data.busqueda.BusquedaRepository
import com.larocola.app.data.busqueda.SugerenciasRepository
import com.larocola.app.data.local.PreferenciasDataStore
import com.larocola.app.data.sesion.SesionRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp

class AppContainer(context: Context) {
    private val supabase = createAppSupabaseClient()
    private val httpClient = HttpClient(OkHttp)

    val authRepository = AuthRepository(supabase)
    val sesionRepository = SesionRepository(supabase)
    val busquedaRepository = BusquedaRepository(supabase)
    val sugerenciasRepository = SugerenciasRepository(httpClient)
    val preferenciasDataStore = PreferenciasDataStore(context)
    val connectivityObserver = ConnectivityObserver(context)
}
