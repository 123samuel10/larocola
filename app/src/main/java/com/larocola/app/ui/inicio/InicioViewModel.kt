package com.larocola.app.ui.inicio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.larocola.app.core.util.AppResult
import com.larocola.app.data.model.ProximoHorario
import com.larocola.app.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface InicioUiState {
    data object Cargando : InicioUiState
    data class SesionActiva(val nombreEstablecimiento: String, val slug: String) : InicioUiState
    data class SinSesion(
        val nombreEstablecimiento: String,
        val proximosHorarios: List<ProximoHorario>,
    ) : InicioUiState
    data class Error(val mensaje: String) : InicioUiState
    data object EstablecimientoNoEncontrado : InicioUiState
}

class InicioViewModel(
    private val container: AppContainer,
    private val slug: String,
) : ViewModel() {

    private val _uiState = MutableStateFlow<InicioUiState>(InicioUiState.Cargando)
    val uiState: StateFlow<InicioUiState> = _uiState.asStateFlow()

    init {
        registrar()
    }

    fun registrar() {
        viewModelScope.launch {
            _uiState.value = InicioUiState.Cargando

            when (val auth = container.authRepository.asegurarSesionAnonima()) {
                is AppResult.Error -> {
                    _uiState.value = InicioUiState.Error(auth.message)
                    return@launch
                }
                is AppResult.Success -> {}
            }

            container.preferenciasDataStore.guardarSlug(slug)

            when (val sesion = container.sesionRepository.registrar(slug)) {
                is AppResult.Error -> {
                    _uiState.value = when (sesion.code) {
                        "establecimiento_no_encontrado" -> InicioUiState.EstablecimientoNoEncontrado
                        else -> InicioUiState.Error(sesion.message)
                    }
                }
                is AppResult.Success -> {
                    val resp = sesion.data
                    _uiState.value = when (resp.estado) {
                        "activa" -> InicioUiState.SesionActiva(
                            nombreEstablecimiento = resp.establecimiento.nombre,
                            slug = slug,
                        )
                        else -> InicioUiState.SinSesion(
                            nombreEstablecimiento = resp.establecimiento.nombre,
                            proximosHorarios = resp.proximosHorarios,
                        )
                    }
                }
            }
        }
    }

    class Factory(private val container: AppContainer, private val slug: String) :
        ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            InicioViewModel(container, slug) as T
    }
}
