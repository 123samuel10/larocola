package com.larocola.app.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val OscuroScheme = darkColorScheme(
    primary = RojoPrincipal,
    onPrimary = FondoClaro,
    background = AzulMarino,
    onBackground = FondoClaro,
    surface = AzulOscuro,
    onSurface = FondoClaro,
    secondary = Coral,
    onSecondary = AzulOscuro,
)

private val ClaroScheme = lightColorScheme(
    primary = RojoPrincipal,
    onPrimary = FondoClaro,
    background = FondoClaro,
    onBackground = TextoPrincipal,
    surface = FondoClaro,
    onSurface = TextoPrincipal,
    onSurfaceVariant = TextoSecundario,
    secondary = AzulMedio,
    onSecondary = FondoClaro,
)

@Composable
fun LaRocolaTheme(
    oscuro: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (oscuro) OscuroScheme else ClaroScheme,
        typography = AppTypography,
        content = content,
    )
}
