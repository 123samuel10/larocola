package com.larocola.app.data.auth

import com.larocola.app.core.util.AppResult
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus

class AuthRepository(private val supabase: SupabaseClient) {

    suspend fun asegurarSesionAnonima(): AppResult<String> = try {
        val session = supabase.auth.currentSessionOrNull()
        if (session == null) {
            supabase.auth.signInAnonymously()
        }
        val uid = supabase.auth.currentUserOrNull()?.id
            ?: return AppResult.Error("No se pudo obtener ID de usuario")
        AppResult.Success(uid)
    } catch (e: Exception) {
        AppResult.Error(e.message ?: "Error de autenticación")
    }

    val sessionStatus get() = supabase.auth.sessionStatus
}
