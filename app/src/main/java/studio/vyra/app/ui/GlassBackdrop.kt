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

/** Layered monochrome light fields behind translucent VYRA surfaces. */
@Composable
fun GlassBackdrop() {
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier.offset(x = (-118).dp, y = 72.dp).size(330.dp).blur(86.dp)
                .background(
                    Brush.radialGradient(
                        listOf(Color.White.copy(alpha = 0.19f), Color(0xFFB8C0CE).copy(alpha = 0.075f), Color.Transparent)
                    ),
                    CircleShape,
                )
        )
        Box(
            Modifier.offset(x = 175.dp, y = 330.dp).size(290.dp).blur(92.dp)
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xFFDCE3EF).copy(alpha = 0.12f), Color.White.copy(alpha = 0.035f), Color.Transparent)
                    ),
                    CircleShape,
                )
        )
        Box(
            Modifier.offset(x = (-35).dp, y = 650.dp).size(270.dp).blur(78.dp)
                .background(
                    Brush.radialGradient(
                        listOf(Color.White.copy(alpha = 0.11f), Color.Transparent)
                    ),
                    CircleShape,
                )
        )
    }
}
