package com.larocola.app.core.config

import com.larocola.app.BuildConfig

object AppConfig {
    val supabaseUrl: String get() = BuildConfig.SUPABASE_URL
    val supabaseAnonKey: String get() = BuildConfig.SUPABASE_ANON_KEY
    val landingHost: String get() = BuildConfig.LANDING_HOST
    val defaultSlug: String get() = BuildConfig.DEFAULT_SLUG.trim()
}
