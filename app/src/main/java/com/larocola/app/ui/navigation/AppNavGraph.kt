package com.larocola.app.ui.navigation

import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.larocola.app.core.config.AppConfig
import com.larocola.app.di.AppContainer
import com.larocola.app.ui.busqueda.BusquedaScreen
import com.larocola.app.ui.cola.ColaPlaceholderScreen
import com.larocola.app.ui.inicio.InicioScreen
import com.larocola.app.ui.servicio.ServicioNoDisponibleScreen

@Composable
fun AppNavGraph(
    container: AppContainer,
    slugInicial: String?,
    windowSizeClass: WindowSizeClass,
) {
    val slug = slugInicial ?: AppConfig.defaultSlug
    key(slug) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Destino.Inicio.crear(slug),
    ) {
        composable(
            route = Destino.Inicio.ruta,
            arguments = listOf(navArgument("slug") { type = NavType.StringType }),
        ) { back ->
            val s = back.arguments?.getString("slug") ?: slug
            InicioScreen(
                slug = s,
                container = container,
                onEntrar = { navController.navigate(Destino.Busqueda.ruta) },
                onServicioNoDisponible = {
                    navController.navigate(Destino.ServicioNoDisponible.crear(s)) {
                        popUpTo(Destino.Inicio.crear(s)) { inclusive = true }
                    }
                },
            )
        }

        composable(Destino.Busqueda.ruta) {
            BusquedaScreen(
                container = container,
                windowSizeClass = windowSizeClass,
                onNavegaACola = { navController.navigate(Destino.Cola.ruta) },
            )
        }

        composable(Destino.Cola.ruta) {
            ColaPlaceholderScreen(
                onNavegaABusqueda = { navController.navigate(Destino.Busqueda.ruta) },
            )
        }

        composable(
            route = Destino.ServicioNoDisponible.ruta,
            arguments = listOf(navArgument("slug") { type = NavType.StringType }),
        ) { back ->
            val s = back.arguments?.getString("slug") ?: slug
            ServicioNoDisponibleScreen(
                container = container,
                slug = s,
                onServicioActivo = {
                    navController.navigate(Destino.Inicio.crear(s)) {
                        popUpTo(Destino.ServicioNoDisponible.crear(s)) { inclusive = true }
                    }
                },
            )
        }
    }
    } // key(slug)
}
