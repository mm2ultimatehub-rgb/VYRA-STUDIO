package studio.vyra.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val VyraColors = darkColorScheme(
    primary = Color(0xFFB6A0FF),
    onPrimary = Color(0xFF24104F),
    secondary = Color(0xFF8CD8D0),
    background = Color(0xFF0B0B10),
    surface = Color(0xFF15151D),
    surfaceVariant = Color(0xFF242431),
    onBackground = Color(0xFFF4F1FA),
    onSurface = Color(0xFFF4F1FA),
    onSurfaceVariant = Color(0xFFB4B0C2),
)

@Composable
fun VyraTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = VyraColors,
        content = content,
    )
}
