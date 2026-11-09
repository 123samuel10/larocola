package com.larocola.app.ui.cola

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.larocola.app.R
import com.larocola.app.core.ui.theme.*
import com.larocola.app.core.util.formatearDuracion
import com.larocola.app.data.model.SolicitudEnCola
import com.larocola.app.di.AppContainer

@Composable
fun ColaScreen(
    container: AppContainer,
    onNavegaABusqueda: () -> Unit,
    vm: ColaViewModel = viewModel(factory = ColaViewModel.Factory(container)),
) {
    val state by vm.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val votoExitoMsg    = stringResource(R.string.voto_registrado)
    val votoEliminadoMsg = stringResource(R.string.voto_eliminado)

    LaunchedEffect(vm) {
        vm.votoEventos.collect { evento ->
            val msg = when (evento) {
                is VotoEvento.Exito -> if (evento.eliminado) votoEliminadoMsg
                                       else votoExitoMsg.format(evento.totalVotos)
                is VotoEvento.Error -> evento.mensaje
            }
            snackbarHostState.showSnackbar(msg)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(containerColor = AzulOscuro) {
                NavigationBarItem(
                    selected = false,
                    onClick = onNavegaABusqueda,
                    icon = { Icon(Icons.Default.Search, null, tint = Color.White.copy(alpha = 0.6f)) },
                    label = { Text(stringResource(R.string.nav_buscar), color = Color.White.copy(alpha = 0.6f)) },
                )
                NavigationBarItem(
                    selected = true,
                    onClick = {},
                    icon = { Icon(Icons.Default.ThumbUp, null, tint = RojoPrincipal) },
                    label = { Text(stringResource(R.string.nav_cola), color = RojoPrincipal) },
                )
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AzulMarino)
                    .systemBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
            ) {
                Text(
                    text = stringResource(R.string.cola_titulo),
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(FondoClaro),
                contentAlignment = Alignment.TopCenter,
            ) {
                when (val s = state) {
                    is ColaUiState.Cargando -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = RojoPrincipal)
                        }
                    }
                    is ColaUiState.Vacia -> {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                text = stringResource(R.string.cola_vacia),
                                style = MaterialTheme.typography.titleMedium,
                                color = TextoPrincipal,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.cola_vacia_descripcion),
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextoSecundario,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                    is ColaUiState.SinSesion -> {
                        Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = stringResource(R.string.cola_sin_sesion),
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextoSecundario,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                    is ColaUiState.Error -> {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                text = s.mensaje,
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextoSecundario,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = vm::recargar,
                                colors = ButtonDefaults.buttonColors(containerColor = RojoPrincipal),
                            ) {
                                Text(stringResource(R.string.accion_reintentar))
                            }
                        }
                    }
                    is ColaUiState.Resultados -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(vertical = 12.dp),
                        ) {
                            items(s.items, key = { it.solicitudId }) { item ->
                                SolicitudItem(item = item, onVotar = { vm.votar(item.solicitudId) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SolicitudItem(
    item: SolicitudEnCola,
    onVotar: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box {
                AsyncImage(
                    model = item.miniaturaUrl,
                    contentDescription = stringResource(R.string.cd_miniatura, item.titulo),
                    modifier = Modifier
                        .size(80.dp, 56.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop,
                )
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(2.dp),
                    shape = RoundedCornerShape(4.dp),
                    color = Color.Black.copy(alpha = 0.7f),
                ) {
                    Text(
                        text = item.duracionSeg.formatearDuracion(),
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                    )
                }
                if (item.estado == "reproduciendo") {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(2.dp),
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFF3DDC84),
                    ) {
                        Text(
                            text = stringResource(R.string.cola_reproduciendo),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.titulo,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextoPrincipal,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = item.artista,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextoSecundario,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF1B4F82),
                ) {
                    Text(
                        text = stringResource(R.string.cola_n_votos, item.votos),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                    )
                }
                Spacer(Modifier.height(4.dp))
                FilledIconButton(
                    onClick = onVotar,
                    modifier = Modifier.size(36.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (item.yaVotado) RojoPrincipal else Color(0xFF1B4F82),
                    ),
                ) {
                    Icon(
                        Icons.Default.ThumbUp,
                        contentDescription = stringResource(R.string.cd_votar, item.titulo),
                        tint = Color.White,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun PreviewCola() {
    LaRocolaTheme {
        Text("ColaScreen Preview — abrir en emulador para ver completo")
    }
}
