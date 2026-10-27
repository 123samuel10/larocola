package com.larocola.app.ui.servicio

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.larocola.app.R
import com.larocola.app.core.ui.theme.*
import com.larocola.app.core.util.AppResult
import com.larocola.app.data.model.ProximoHorario
import com.larocola.app.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private class ServicioViewModel(
    private val container: AppContainer,
    private val slug: String,
) : ViewModel() {

    data class UiState(
        val nombreEstablecimiento: String = "",
        val proximosHorarios: List<ProximoHorario> = emptyList(),
        val cargando: Boolean = false,
    )

    private val _uiState = MutableStateFlow(
        UiState(
            nombreEstablecimiento = "La Esquina Bar",
            proximosHorarios = listOf(
                ProximoHorario("Hoy, viernes", "20:00", "02:00"),
                ProximoHorario("Sábado", "20:00", "03:00"),
            ),
        )
    )
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun reintentar(onSesionActiva: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(cargando = true)

            when (container.authRepository.asegurarSesionAnonima()) {
                is AppResult.Error -> {
                    _uiState.value = _uiState.value.copy(cargando = false)
                    return@launch
                }
                is AppResult.Success -> {}
            }

            when (val r = container.sesionRepository.registrar(slug)) {
                is AppResult.Error -> {
                    _uiState.value = _uiState.value.copy(cargando = false)
                }
                is AppResult.Success -> {
                    if (r.data.estado == "activa") {
                        onSesionActiva()
                    } else {
                        _uiState.value = UiState(
                            nombreEstablecimiento = r.data.establecimiento.nombre,
                            proximosHorarios = r.data.proximosHorarios,
                            cargando = false,
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
            ServicioViewModel(container, slug) as T
    }
}

@Composable
fun ServicioNoDisponibleScreen(
    container: AppContainer,
    slug: String,
    onServicioActivo: () -> Unit,
) {
    val vm: ServicioViewModel = viewModel(factory = ServicioViewModel.Factory(container, slug))
    val state by vm.uiState.collectAsState()
    LaRocolaTheme(oscuro = true) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AzulMarino)
                .systemBarsPadding(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(64.dp))

                Text(
                    text = "LA ROCOLA",
                    style = MaterialTheme.typography.displayLarge,
                    color = Color.White,
                )

                Spacer(Modifier.height(40.dp))

                // Clock icon placeholder
                Surface(
                    shape = RoundedCornerShape(50),
                    color = AzulMedio,
                    modifier = Modifier.size(80.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("⏸", style = MaterialTheme.typography.headlineLarge)
                    }
                }

                Spacer(Modifier.height(24.dp))

                Text(
                    text = stringResource(R.string.servicio_titulo),
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.servicio_descripcion),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(32.dp))

                // Schedule card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = AzulOscuro),
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = stringResource(R.string.servicio_proxima_apertura),
                            style = MaterialTheme.typography.labelLarge,
                            color = Coral,
                        )
                        Spacer(Modifier.height(12.dp))
                        state.proximosHorarios.forEach { horario ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = horario.etiqueta,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White,
                                )
                                Text(
                                    text = "${horario.inicio} – ${horario.fin}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.75f),
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(32.dp))

                if (state.cargando) {
                    CircularProgressIndicator(color = RojoPrincipal)
                } else {
                    OutlinedButton(
                        onClick = { vm.reintentar(onServicioActivo) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    ) {
                        Text(stringResource(R.string.accion_reintentar))
                    }
                }

                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun PreviewServicioNoDisponible() {
    LaRocolaTheme(oscuro = true) {
        ServicioContenido(
            nombre = "La Esquina Bar",
            horarios = listOf(
                ProximoHorario("Hoy, viernes", "20:00", "02:00"),
                ProximoHorario("Sábado", "20:00", "03:00"),
            ),
            cargando = false,
            onReintentar = {},
        )
    }
}

@Composable
private fun ServicioContenido(
    nombre: String,
    horarios: List<ProximoHorario>,
    cargando: Boolean,
    onReintentar: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AzulMarino)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text("Preview: $nombre — ${horarios.size} horarios", color = Color.White)
    }
}
