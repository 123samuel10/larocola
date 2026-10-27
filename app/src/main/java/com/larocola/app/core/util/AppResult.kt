package com.larocola.app.core.util

sealed class AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>()
    data class Error(val message: String, val code: String? = null) : AppResult<Nothing>()
}
