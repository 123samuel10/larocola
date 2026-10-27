package com.larocola.app.ui.busqueda

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import com.larocola.app.R
import com.larocola.app.core.ui.theme.*
import com.larocola.app.core.util.formatearDuracion
import com.larocola.app.data.model.Cancion
import com.larocola.app.di.AppContainer

@Composable
fun BusquedaScreen(
    container: AppContainer,
    windowSizeClass: WindowSizeClass,
    onNavegaACola: () -> Unit,
    vm: BusquedaViewModel = viewModel(factory = BusquedaViewModel.Factory(container)),
) {
    val state by vm.uiState.collectAsState()
    val sugerenciasState by vm.sugerencias.collectAsState()
    val textoBusqueda by vm.textoBusqueda.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current

    val proximoMsg = stringResource(R.string.busqueda_proximo)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(containerColor = AzulOscuro) {
                NavigationBarItem(
                    selected = true,
                    onClick = {},
                    icon = { Icon(Icons.Default.Search, null, tint = RojoPrincipal) },
                    label = { Text(stringResource(R.string.nav_buscar), color = RojoPrincipal) },
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavegaACola,
                    icon = { Icon(Icons.Default.Add, null, tint = Color.White.copy(alpha = 0.6f)) },
                    label = { Text(stringResource(R.string.nav_cola), color = Color.White.copy(alpha = 0.6f)) },
                )
            }
        },
    ) { padding ->
        val isWide = windowSizeClass.widthSizeClass >= WindowWidthSizeClass.Medium
        val contentMod = if (isWide) Modifier.widthIn(max = 600.dp) else Modifier.fillMaxWidth()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Header azul
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AzulMarino)
                    .systemBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
            ) {
                Column(modifier = contentMod.align(Alignment.Center)) {
                    Text(
                        text = stringResource(R.string.busqueda_titulo),
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                    )
                    Spacer(Modifier.height(12.dp))
                    SearchField(
                        texto = textoBusqueda,
                        onTextoCambiado = vm::onTextoCambiado,
                        onBuscar = {
                            keyboard?.hide()
                            if (textoBusqueda.isNotBlank()) vm.buscar(textoBusqueda)
                        },
                        onLimpiar = vm::limpiar,
                    )
                    // Suggestions
                    if (sugerenciasState.mostrar && sugerenciasState.sugerencias.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp),
                            colors = CardDefaults.cardColors(containerColor = AzulOscuro),
                        ) {
                            sugerenciasState.sugerencias.forEach { sug ->
                                Text(
                                    text = sug,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            keyboard?.hide()
                                            vm.buscar(sug)
                                        }
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White,
                                )
                                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                            }
                        }
                    }
                }
            }

            // Contenido principal
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(FondoClaro),
                contentAlignment = Alignment.TopCenter,
            ) {
                val innerMod = contentMod.padding(horizontal = 16.dp)
                when (val s = state) {
                    is BusquedaUiState.VacioInicial -> EstadoVacio(innerMod)
                    is BusquedaUiState.Cargando -> EstadoCargando(innerMod)
                    is BusquedaUiState.Resultados -> {
                        val cols = if (isWide) 2 else 1
                        ResultadosList(
                            canciones = s.canciones,
                            consulta = s.consulta,
                            columnas = cols,
                            modifier = contentMod,
                            onPedir = {
                                scope.launch {
                                    snackbarHostState.showSnackbar(proximoMsg)
                                }
                            },
                        )
                    }
                    is BusquedaUiState.SinResultados -> EstadoTexto(
                        stringResource(R.string.busqueda_sin_resultados), innerMod)
                    is BusquedaUiState.SinConexion -> EstadoTexto(
                        stringResource(R.string.busqueda_sin_conexion), innerMod)
                    is BusquedaUiState.CuotaAgotada -> EstadoTexto(
                        stringResource(R.string.busqueda_cuota_agotada), innerMod)
                    is BusquedaUiState.Error -> EstadoTexto(s.mensaje, innerMod)
                }
            }
        }
    }
}

@Composable
private fun SearchField(
    texto: String,
    onTextoCambiado: (String) -> Unit,
    onBuscar: () -> Unit,
    onLimpiar: () -> Unit,
) {
    OutlinedTextField(
        value = texto,
        onValueChange = onTextoCambiado,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(stringResource(R.string.busqueda_placeholder), color = Color.White.copy(alpha = 0.5f)) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = stringResource(R.string.cd_buscar), tint = Color.White) },
        trailingIcon = if (texto.isNotEmpty()) {
            { IconButton(onClick = onLimpiar) { Icon(Icons.Default.Clear, stringResource(R.string.cd_limpiar), tint = Color.White) } }
        } else null,
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onBuscar() }),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedBorderColor = RojoPrincipal,
            unfocusedBorderColor = Color.White.copy(alpha = 0.4f),
            cursorColor = RojoPrincipal,
        ),
        shape = RoundedCornerShape(12.dp),
    )
}

@Composable
private fun ResultadosList(
    canciones: List<Cancion>,
    consulta: String,
    columnas: Int,
    modifier: Modifier,
    onPedir: (String) -> Unit,
) {
    LazyColumn(modifier = modifier, contentPadding = PaddingValues(vertical = 12.dp)) {
        item {
            Text(
                text = stringResource(R.string.busqueda_resultados),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelLarge,
                color = TextoSecundario,
            )
            Text(
                text = stringResource(R.string.busqueda_n_videos, canciones.size),
                modifier = Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.bodySmall,
                color = TextoSecundario,
            )
        }
        if (columnas == 1) {
            items(canciones) { cancion -> CancionItem(cancion, onPedir) }
        } else {
            items(canciones.chunked(2)) { fila ->
                Row(modifier = Modifier.padding(horizontal = 8.dp)) {
                    fila.forEach { c -> CancionItem(c, onPedir, Modifier.weight(1f).padding(4.dp)) }
                    if (fila.size < 2) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun CancionItem(
    cancion: Cancion,
    onPedir: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val alpha = if (cancion.reproducible) 1f else 0.45f
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.alpha(alpha)) {
                AsyncImage(
                    model = cancion.miniaturaUrl,
                    contentDescription = stringResource(R.string.cd_miniatura, cancion.titulo),
                    modifier = Modifier
                        .size(80.dp, 56.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop,
                )
                // Duration chip
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(2.dp),
                    shape = RoundedCornerShape(4.dp),
                    color = Color.Black.copy(alpha = 0.7f),
                ) {
                    Text(
                        text = cancion.duracionSeg.formatearDuracion(),
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = cancion.titulo,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (cancion.reproducible) TextoPrincipal else TextoSecundario,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (cancion.reproducible) {
                    Text(
                        text = cancion.artista,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextoSecundario,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                } else {
                    Text(
                        text = stringResource(R.string.busqueda_no_reproducible),
                        style = MaterialTheme.typography.bodySmall,
                        color = RojoPrincipal,
                    )
                }
            }

            if (cancion.reproducible) {
                Spacer(Modifier.width(8.dp))
                FilledIconButton(
                    onClick = { onPedir(cancion.titulo) },
                    modifier = Modifier.size(40.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = RojoPrincipal),
                ) {
                    Icon(Icons.Default.Add, stringResource(R.string.busqueda_pedir_cancion, cancion.titulo), tint = Color.White)
                }
            }
        }
    }
}

@Composable
private fun EstadoVacio(modifier: Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(R.string.busqueda_vacio),
            style = MaterialTheme.typography.bodyLarge,
            color = TextoSecundario,
        )
    }
}

@Composable
private fun EstadoCargando(modifier: Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = RojoPrincipal)
    }
}

@Composable
private fun EstadoTexto(mensaje: String, modifier: Modifier) {
    Box(modifier = modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(
            text = mensaje,
            style = MaterialTheme.typography.bodyLarge,
            color = TextoSecundario,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewResultados() {
    LaRocolaTheme {
        // Simplified preview
        Text("BusquedaScreen Preview — abrir en emulador para ver completo")
    }
}
