package com.larocola.app.ui.inicio

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.larocola.app.R
import com.larocola.app.core.ui.theme.*
import com.larocola.app.data.model.ProximoHorario
import com.larocola.app.di.AppContainer

@Composable
fun InicioScreen(
    slug: String,
    container: AppContainer,
    onEntrar: () -> Unit,
    onServicioNoDisponible: () -> Unit,
    vm: InicioViewModel = viewModel(factory = InicioViewModel.Factory(container, slug)),
) {
    val state by vm.uiState.collectAsState()
    LaRocolaTheme(oscuro = true) {
        when (val s = state) {
            is InicioUiState.Cargando -> InicioLoading()
            is InicioUiState.SesionActiva -> InicioContenido(
                nombre = s.nombreEstablecimiento,
                onEntrar = onEntrar,
            )
            is InicioUiState.SinSesion -> {
                // navigate to Servicio No Disponible
                onServicioNoDisponible()
            }
            is InicioUiState.Error -> InicioError(mensaje = s.mensaje, onReintentar = vm::registrar)
            is InicioUiState.EstablecimientoNoEncontrado -> InicioError(
                mensaje = stringResource(R.string.inicio_no_encontrado),
                onReintentar = vm::registrar,
            )
        }
    }
}

@Composable
private fun InicioContenido(nombre: String, onEntrar: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AzulMarino)
            .systemBarsPadding()
    ) {
        // Decorative circles
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width * 0.85f
            val cy = size.height * 0.15f
            for (r in listOf(80f, 160f, 250f, 360f)) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.05f),
                    radius = r.dp.toPx(),
                    center = Offset(cx, cy),
                )
            }
            drawCircle(
                color = RojoPrincipal,
                radius = 10.dp.toPx(),
                center = Offset(cx, cy),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(64.dp))

            // Logo
            Text(
                text = "LA ROCOLA",
                style = MaterialTheme.typography.displayLarge,
                color = Color.White,
                letterSpacing = MaterialTheme.typography.displayLarge.letterSpacing,
            )

            Spacer(Modifier.height(40.dp))

            // Headline
            Text(
                text = buildAnnotatedString {
                    append("Tú eliges la música de esta ")
                    withStyle(SpanStyle(color = Coral)) { append("noche.") }
                },
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.inicio_subtitulo),
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(32.dp))

            // Connected card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = AzulOscuro),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.inicio_conectado_a),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.6f),
                        )
                        Text(
                            text = nombre,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = VerdeActivo.copy(alpha = 0.15f),
                    ) {
                        Text(
                            text = stringResource(R.string.inicio_sesion_activa),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = VerdeActivo,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            // CTA button
            Button(
                onClick = onEntrar,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RojoPrincipal),
            ) {
                Text(
                    text = "${stringResource(R.string.inicio_entrar)} →",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                )
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.inicio_privacidad),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun InicioLoading() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AzulMarino),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            CircularProgressIndicator(color = RojoPrincipal)
            Text(
                text = stringResource(R.string.inicio_cargando),
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.7f),
            )
        }
    }
}

@Composable
private fun InicioError(mensaje: String, onReintentar: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AzulMarino)
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = mensaje,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
                textAlign = TextAlign.Center,
            )
            OutlinedButton(
                onClick = onReintentar,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
            ) {
                Text(stringResource(R.string.accion_reintentar))
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun PreviewSesionActiva() {
    LaRocolaTheme(oscuro = true) {
        InicioContenido(nombre = "La Esquina Bar", onEntrar = {})
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun PreviewCargando() {
    LaRocolaTheme(oscuro = true) { InicioLoading() }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun PreviewError() {
    LaRocolaTheme(oscuro = true) {
        InicioError(
            mensaje = stringResource(R.string.inicio_error_red),
            onReintentar = {},
        )
    }
}
