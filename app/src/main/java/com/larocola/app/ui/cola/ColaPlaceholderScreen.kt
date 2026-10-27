package com.larocola.app.ui.cola

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.larocola.app.R
import com.larocola.app.core.ui.theme.*

@Composable
fun ColaPlaceholderScreen(onNavegaABusqueda: () -> Unit) {
    LaRocolaTheme {
        Scaffold(
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
                        icon = { Icon(Icons.Default.Add, null, tint = RojoPrincipal) },
                        label = { Text(stringResource(R.string.nav_cola), color = RojoPrincipal) },
                    )
                }
            },
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(FondoClaro)
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.cola_proximamente),
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextoSecundario,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(32.dp),
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun PreviewCola() {
    ColaPlaceholderScreen(onNavegaABusqueda = {})
}
