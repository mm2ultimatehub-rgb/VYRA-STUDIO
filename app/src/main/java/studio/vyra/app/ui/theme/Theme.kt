package studio.vyra.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val VyraColors = darkColorScheme(
    primary = Color(0xFFB9A2FF),
    onPrimary = Color(0xFF17102E),
    primaryContainer = Color(0xFF302252),
    onPrimaryContainer = Color(0xFFE9DFFF),
    secondary = Color(0xFF8ED8E8),
    onSecondary = Color(0xFF08232B),
    background = Color(0xFF080B18),
    surface = Color(0xFF111629),
    surfaceVariant = Color(0xFF1B2340),
    onBackground = Color(0xFFF4F2FF),
    onSurface = Color(0xFFF4F2FF),
    onSurfaceVariant = Color(0xFFAAB3CC),
    outline = Color(0xFF3A4564),
)

@Composable
fun VyraTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = VyraColors,
        content = content,
    )
}
