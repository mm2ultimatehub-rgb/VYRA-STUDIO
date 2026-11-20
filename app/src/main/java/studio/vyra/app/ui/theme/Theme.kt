package studio.vyra.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

private val VyraColors = darkColorScheme(
    primary = Color(0xFFF4F5F7),
    onPrimary = Color(0xFF101216),
    primaryContainer = Color(0xFF343941),
    onPrimaryContainer = Color(0xFFF7F8FA),
    secondary = Color(0xFFCACDD3),
    onSecondary = Color(0xFF15171B),
    secondaryContainer = Color(0xFF292D34),
    onSecondaryContainer = Color(0xFFE8EAF0),
    background = Color(0xFF07090D),
    surface = Color(0x8F171A20),
    surfaceDim = Color(0xFF090B0F),
    surfaceBright = Color(0xFF30343C),
    surfaceVariant = Color(0x7A252A32),
    onBackground = Color(0xFFF7F8FA),
    onSurface = Color(0xFFF4F5F7),
    onSurfaceVariant = Color(0xFFB8BEC8),
    outline = Color(0xFF626A76),
    outlineVariant = Color(0xFF3A404A),
    error = Color(0xFFFFB4AB),
)

private val VyraShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(34.dp),
)

@Composable
fun VyraTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = VyraColors,
        shapes = VyraShapes,
        content = content,
    )
}
