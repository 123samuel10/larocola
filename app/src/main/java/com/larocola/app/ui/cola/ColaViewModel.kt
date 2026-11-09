package com.larocola.app.ui.cola

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.larocola.app.core.util.AppResult
import com.larocola.app.data.model.SolicitudEnCola
import com.larocola.app.di.AppContainer
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

sealed interface ColaUiState {
    data object Cargando : ColaUiState
    data class Resultados(val items: List<SolicitudEnCola>) : ColaUiState
    data object Vacia : ColaUiState
    data object SinSesion : ColaUiState
    data class Error(val mensaje: String) : ColaUiState
}

sealed interface VotoEvento {
    data class Exito(val totalVotos: Int, val eliminado: Boolean) : VotoEvento
    data class Error(val mensaje: String) : VotoEvento
}

class ColaViewModel(private val container: AppContainer) : ViewModel() {

    private val _uiState = MutableStateFlow<ColaUiState>(ColaUiState.Cargando)
    val uiState: StateFlow<ColaUiState> = _uiState.asStateFlow()

    private val _votoEventos = MutableSharedFlow<VotoEvento>(extraBufferCapacity = 1)
    val votoEventos: SharedFlow<VotoEvento> = _votoEventos.asSharedFlow()

    init {
        viewModelScope.launch {
            while (isActive) {
                fetchCola()
                delay(10_000L)
            }
        }
    }

    fun recargar() {
        viewModelScope.launch { fetchCola() }
    }

    fun votar(solicitudId: String) {
        viewModelScope.launch {
            when (val r = container.solicitudRepository.votar(solicitudId)) {
                is AppResult.Success -> {
                    _votoEventos.emit(
                        VotoEvento.Exito(r.data.totalVotos, eliminado = !r.data.yaVotado)
                    )
                    fetchCola()
                }
                is AppResult.Error -> _votoEventos.emit(VotoEvento.Error(r.message))
            }
        }
    }

    private suspend fun fetchCola() {
        val sesionId = container.sesionActivaId
        if (sesionId == null) {
            _uiState.value = ColaUiState.SinSesion
            return
        }
        if (_uiState.value !is ColaUiState.Resultados) {
            _uiState.value = ColaUiState.Cargando
        }
        when (val r = container.solicitudRepository.obtenerCola(sesionId)) {
            is AppResult.Success -> _uiState.value =
                if (r.data.items.isEmpty()) ColaUiState.Vacia
                else ColaUiState.Resultados(r.data.items)
            is AppResult.Error -> _uiState.value = ColaUiState.Error(r.message)
        }
    }

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ColaViewModel(container) as T
    }
}
