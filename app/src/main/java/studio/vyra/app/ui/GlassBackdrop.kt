package studio.vyra.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Soft blurred monochrome light behind the store's translucent surfaces. */
@Composable
fun GlassBackdrop() {
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier.offset(x = (-105).dp, y = 105.dp).size(290.dp).blur(72.dp)
                .background(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.17f), Color(0xFF9CA3AF).copy(alpha = 0.06f), Color.Transparent)), CircleShape)
        )
        Box(
            Modifier.offset(x = 190.dp, y = 390.dp).size(250.dp).blur(80.dp)
                .background(Brush.radialGradient(listOf(Color(0xFFD9DEE7).copy(alpha = 0.12f), Color.White.copy(alpha = 0.035f), Color.Transparent)), CircleShape)
        )
        Box(
            Modifier.offset(x = 55.dp, y = 690.dp).size(220.dp).blur(64.dp)
                .background(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.10f), Color.Transparent)), CircleShape)
        )
    }
}
