package fr.whitytoes.badgemoi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dagger.hilt.android.AndroidEntryPoint
import fr.whitytoes.badgemoi.ui.BadgeMoiApp
import fr.whitytoes.badgemoi.ui.theme.BadgeMoiTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // État de thème temporaire, en mémoire, par défaut nuit comme le POC.
            // Il est remplacé par la préférence persistée (SettingsRepository, lot 1)
            // à l'issue suivante du lot 2 — la bascule ne survit donc pas encore à
            // la fermeture de l'application.
            var isDarkTheme by rememberSaveable { mutableStateOf(true) }

            BadgeMoiTheme(darkTheme = isDarkTheme) {
                BadgeMoiApp(
                    isDarkTheme = isDarkTheme,
                    onToggleTheme = { isDarkTheme = !isDarkTheme },
                )
            }
        }
    }
}
