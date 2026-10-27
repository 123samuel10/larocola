package com.larocola.app.ui.busqueda

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.larocola.app.core.util.AppResult
import com.larocola.app.data.model.Cancion
import com.larocola.app.di.AppContainer
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

sealed interface BusquedaUiState {
    data object VacioInicial : BusquedaUiState
    data object Cargando : BusquedaUiState
    data class Resultados(val canciones: List<Cancion>, val consulta: String) : BusquedaUiState
    data object SinResultados : BusquedaUiState
    data object SinConexion : BusquedaUiState
    data object CuotaAgotada : BusquedaUiState
    data class Error(val mensaje: String) : BusquedaUiState
}

data class SugerenciasState(
    val sugerencias: List<String> = emptyList(),
    val mostrar: Boolean = false,
)

@OptIn(FlowPreview::class)
class BusquedaViewModel(private val container: AppContainer) : ViewModel() {

    private val _uiState = MutableStateFlow<BusquedaUiState>(BusquedaUiState.VacioInicial)
    val uiState: StateFlow<BusquedaUiState> = _uiState.asStateFlow()

    private val _sugerencias = MutableStateFlow(SugerenciasState())
    val sugerencias: StateFlow<SugerenciasState> = _sugerencias.asStateFlow()

    private val _textoBusqueda = MutableStateFlow("")
    val textoBusqueda: StateFlow<String> = _textoBusqueda.asStateFlow()

    private var searchJob: Job? = null
    // Prevents the debounce from re-showing suggestions after buscar() is called
    @Volatile private var buscando = false

    init {
        viewModelScope.launch {
            _textoBusqueda
                .debounce(400L)
                .distinctUntilChanged()
                .collect { texto ->
                    if (buscando) return@collect
                    if (texto.length >= 3) {
                        val sugs = container.sugerenciasRepository.obtenerSugerencias(texto)
                        if (!buscando) {
                            _sugerencias.value = SugerenciasState(sugerencias = sugs, mostrar = true)
                        }
                    } else {
                        _sugerencias.value = SugerenciasState()
                    }
                }
        }
    }

    fun onTextoCambiado(texto: String) {
        buscando = false
        _textoBusqueda.value = texto
        if (texto.length < 3) _sugerencias.value = SugerenciasState()
    }

    fun buscar(consulta: String) {
        buscando = true
        _sugerencias.value = SugerenciasState()
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            try {
                _uiState.value = BusquedaUiState.Cargando

                if (!container.connectivityObserver.isConnected.value) {
                    _uiState.value = BusquedaUiState.SinConexion
                    return@launch
                }

                when (val r = container.busquedaRepository.buscar(consulta)) {
                    is AppResult.Error -> {
                        _uiState.value = when (r.code) {
                            "cuota_agotada", "rate_limit" -> BusquedaUiState.CuotaAgotada
                            else -> BusquedaUiState.Error(r.message)
                        }
                    }
                    is AppResult.Success -> {
                        _uiState.value = if (r.data.resultados.isEmpty()) {
                            BusquedaUiState.SinResultados
                        } else {
                            BusquedaUiState.Resultados(r.data.resultados, consulta)
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.value = BusquedaUiState.Error(e.message ?: "Error inesperado")
            }
        }
    }

    fun limpiar() {
        buscando = false
        searchJob?.cancel()
        _textoBusqueda.value = ""
        _uiState.value = BusquedaUiState.VacioInicial
        _sugerencias.value = SugerenciasState()
    }

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            BusquedaViewModel(container) as T
    }
}
