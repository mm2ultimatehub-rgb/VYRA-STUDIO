package studio.vyra.app.update

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** Visual-only adaptation of the V Sıvı Cam Akıcı Yükleme HTML. */
@Composable
fun LiquidGlassDownloadPreview(onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    val progress = remember { Animatable(0f) }
    val infinite = rememberInfiniteTransition(label = "liquid-glass")
    val phase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (PI * 2).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "wave-phase",
    )
    val percent = (progress.value * 100f).toInt()
    val complete = progress.value >= 0.999f
    val downloaded = String.format(Locale("tr", "TR"), "%.1f", 48.0 * progress.value)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
            shape = RoundedCornerShape(30.dp),
            color = Color(0xFF08090B),
            tonalElevation = 0.dp,
        ) {
            Column(
                modifier = Modifier
                    .background(Brush.verticalGradient(listOf(Color(0xFF202228), Color(0xFF0B0C0F), Color(0xFF050506))))
                    .padding(horizontal = 22.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("VYRA", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                        Text("SIVI CAM İNDİRME", color = Color(0xFF9FA3AC), fontSize = 9.sp, letterSpacing = 1.8.sp)
                    }
                    Text(
                        "ÖNİZLEME",
                        color = Color(0xFFD7D9DE),
                        fontSize = 10.sp,
                        letterSpacing = 1.2.sp,
                        modifier = Modifier.background(Color.White.copy(alpha = 0.09f), RoundedCornerShape(50)).padding(horizontal = 11.dp, vertical = 7.dp),
                    )
                }

                Spacer(Modifier.height(2.dp))
                Box(contentAlignment = Alignment.Center) {
                    Canvas(Modifier.size(276.dp)) {
                        val c = Offset(size.width / 2f, size.height / 2f)
                        val radius = min(size.width, size.height) * 0.385f
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color.White.copy(alpha = 0.13f), Color(0xFF9DA3AF).copy(alpha = 0.055f), Color.Transparent),
                                center = Offset(c.x - radius * 0.25f, c.y - radius * 0.55f),
                                radius = radius * 2.1f,
                            ),
                            radius = radius * 1.28f,
                            center = c,
                        )
                        drawCircle(Color.White.copy(alpha = 0.22f), radius, c, style = Stroke(width = 1.2.dp.toPx()))
                        drawCircle(Color.White.copy(alpha = 0.10f), radius * 0.91f, c, style = Stroke(width = 3.dp.toPx()))

                        if (progress.value > 0f) {
                            val sweep = progress.value * (2f * PI.toFloat())
                            val path = Path()
                            val samples = maxOf(2, (150 * progress.value).toInt())
                            for (i in 0..samples) {
                                val t = i.toFloat() / samples
                                val angle = -PI.toFloat() / 2f + sweep * t
                                val waveRadius = radius * 0.91f + sin(angle * 7f + phase) * 2.4f
                                val point = Offset(c.x + cos(angle) * waveRadius, c.y + sin(angle) * waveRadius)
                                if (i == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
                            }
                            drawPath(path, Color.White.copy(alpha = 0.22f), style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round))
                            drawPath(path, Color.White.copy(alpha = 0.94f), style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
                            val endAngle = -PI.toFloat() / 2f + sweep
                            val endRadius = radius * 0.91f + sin(endAngle * 7f + phase) * 2.4f
                            val bead = Offset(c.x + cos(endAngle) * endRadius, c.y + sin(endAngle) * endRadius)
                            drawCircle(Color.White.copy(alpha = 0.22f), 10.dp.toPx(), bead)
                            drawCircle(Color.White, 4.dp.toPx(), bead)
                        }

                        val v = Path().apply {
                            moveTo(c.x - radius * 0.32f, c.y - radius * 0.24f)
                            lineTo(c.x - radius * 0.04f, c.y + radius * 0.30f)
                            quadraticTo(c.x, c.y + radius * 0.40f, c.x + radius * 0.06f, c.y + radius * 0.28f)
                            lineTo(c.x + radius * 0.34f, c.y - radius * 0.24f)
                        }
                        drawPath(v, Color.White.copy(alpha = 0.13f), style = Stroke(width = 19.dp.toPx(), cap = StrokeCap.Round))
                        drawPath(
                            v,
                            Brush.linearGradient(listOf(Color.White, Color(0xFF8E929B), Color.White), start = Offset(c.x - radius * 0.3f, c.y - radius * 0.3f), end = Offset(c.x + radius * 0.3f, c.y + radius * 0.3f)),
                            style = Stroke(width = 11.dp.toPx(), cap = StrokeCap.Round),
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$percent%", color = Color.White, fontSize = 48.sp, fontWeight = FontWeight.Light, letterSpacing = (-2).sp)
                        Text(if (complete) "TAMAMLANDI" else "VYRA APK", color = Color.White.copy(alpha = 0.62f), fontSize = 9.sp, letterSpacing = 2.sp)
                    }
                }

                Text(if (complete) "Önizleme tamamlandı" else "İndirme animasyonu önizlemesi", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                Text("$downloaded / 48,0 MB", color = Color.White.copy(alpha = 0.66f), fontSize = 12.sp)
                Text("Örnek animasyon · Gerçek APK indirmesi bağlı değil", color = Color.White.copy(alpha = 0.42f), fontSize = 10.sp)

                Button(
                    onClick = {
                        scope.launch {
                            progress.snapTo(0f)
                            progress.animateTo(1f, animationSpec = tween(7000, easing = LinearEasing))
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (complete) "Önizlemeyi yeniden oynat" else "Önizlemeyi başlat")
                }
                TextButton(onClick = onDismiss) { Text("Kapat", color = Color.White.copy(alpha = 0.72f)) }
            }
        }
    }
}
