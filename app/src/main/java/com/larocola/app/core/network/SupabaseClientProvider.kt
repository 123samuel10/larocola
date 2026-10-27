package com.larocola.app.core.network

import com.larocola.app.core.config.AppConfig
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.functions.Functions

fun createAppSupabaseClient() = createSupabaseClient(
    supabaseUrl = AppConfig.supabaseUrl,
    supabaseKey = AppConfig.supabaseAnonKey,
) {
    install(Auth)
    install(Functions)
}
