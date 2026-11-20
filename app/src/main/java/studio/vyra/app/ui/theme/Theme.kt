package studio.vyra.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val VyraColors = darkColorScheme(
    primary = Color(0xFFE7E7E7),
    onPrimary = Color(0xFF111111),
    secondary = Color(0xFFBDBDBD),
    onSecondary = Color(0xFF111111),
    background = Color(0xFF080808),
    surface = Color(0xFF121212),
    surfaceVariant = Color(0xFF202020),
    onBackground = Color(0xFFF5F5F5),
    onSurface = Color(0xFFF5F5F5),
    onSurfaceVariant = Color(0xFFB0B0B0),
    outline = Color(0xFF555555),
)

@Composable
fun VyraTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = VyraColors,
        content = content,
    )
}
