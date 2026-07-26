package fr.whitytoes.badgemoi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import fr.whitytoes.badgemoi.ui.BadgeMoiApp
import fr.whitytoes.badgemoi.ui.ThemeViewModel
import fr.whitytoes.badgemoi.ui.theme.BadgeMoiTheme
import fr.whitytoes.badgemoi.ui.theme.isDark

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: ThemeViewModel = hiltViewModel()
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val systemInDarkTheme = isSystemInDarkTheme()

            // Rien n'est composé tant que la préférence n'a pas été lue du DataStore :
            // afficher un thème par défaut puis le corriger produirait un flash au
            // lancement.
            themeMode?.let { mode ->
                val darkTheme = mode.isDark(systemInDarkTheme)
                BadgeMoiTheme(darkTheme = darkTheme) {
                    BadgeMoiApp(
                        isDarkTheme = darkTheme,
                        onToggleTheme = { viewModel.toggleTheme(systemInDarkTheme) },
                    )
                }
            }
        }
    }
}
