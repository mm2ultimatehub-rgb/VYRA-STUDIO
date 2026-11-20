package studio.vyra.app.update

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
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** Monochrome liquid-glass download surface driven only by real DownloadManager progress. */
@Composable
fun LiquidGlassDownloadDialog(
    update: AppUpdate,
    progressPercent: Int,
    downloadedBytes: Long,
    status: String,
    isDownloading: Boolean,
    canDownload: Boolean,
    onStart: () -> Unit,
    onDismiss: () -> Unit,
) {
    val totalBytes = update.fileSizeBytes
    val fraction = if (totalBytes > 0L) {
        (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
    } else 0f
    val percent = progressPercent.coerceIn(0, 100)
    val complete = status == "APK doğrulandı · kurulum açılıyor"

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
                    .background(Brush.verticalGradient(listOf(Color(0xFF292C32), Color(0xFF101115), Color(0xFF050506))))
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
                        Text("SIVI CAM · GÜVENLİ İNDİRME", color = Color(0xFFB8BBC2), fontSize = 9.sp, letterSpacing = 1.5.sp)
                    }
                    Text(
                        "v${update.version}",
                        color = Color.White,
                        fontSize = 10.sp,
                        modifier = Modifier.background(Color.White.copy(alpha = 0.10f), RoundedCornerShape(50)).padding(horizontal = 11.dp, vertical = 7.dp),
                    )
                }
                Spacer(Modifier.height(2.dp))
                Box(contentAlignment = Alignment.Center) {
                    Canvas(Modifier.size(252.dp)) {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val radius = min(size.width, size.height) * 0.385f
                        drawCircle(
                            brush = Brush.radialGradient(
                                listOf(Color.White.copy(alpha = 0.16f), Color(0xFF9DA3AF).copy(alpha = 0.06f), Color.Transparent),
                                center = Offset(center.x - radius * 0.25f, center.y - radius * 0.52f),
                                radius = radius * 2.1f,
                            ),
                            radius = radius * 1.28f,
                            center = center,
                        )
                        drawCircle(Color.White.copy(alpha = 0.24f), radius, center, style = Stroke(width = 1.2.dp.toPx()))
                        drawCircle(Color.White.copy(alpha = 0.13f), radius * 0.91f, center, style = Stroke(width = 3.dp.toPx()))
                        if (fraction > 0f) {
                            val sweep = fraction * (2f * PI.toFloat())
                            val path = Path()
                            val samples = maxOf(2, (160 * fraction).toInt())
                            for (i in 0..samples) {
                                val t = i.toFloat() / samples
                                val angle = -PI.toFloat() / 2f + sweep * t
                                val waveRadius = radius * 0.91f + sin(angle * 7f) * 2.2f
                                val point = Offset(center.x + cos(angle) * waveRadius, center.y + sin(angle) * waveRadius)
                                if (i == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
                            }
                            drawPath(path, Color.White.copy(alpha = 0.20f), style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round))
                            drawPath(path, Color.White, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
                        }
                        val v = Path().apply {
                            moveTo(center.x - radius * 0.32f, center.y - radius * 0.24f)
                            lineTo(center.x - radius * 0.04f, center.y + radius * 0.30f)
                            quadraticTo(center.x, center.y + radius * 0.40f, center.x + radius * 0.06f, center.y + radius * 0.28f)
                            lineTo(center.x + radius * 0.34f, center.y - radius * 0.24f)
                        }
                        drawPath(v, Color.White.copy(alpha = 0.15f), style = Stroke(width = 19.dp.toPx(), cap = StrokeCap.Round))
                        drawPath(v, Brush.linearGradient(listOf(Color.White, Color(0xFF8E929B), Color.White)), style = Stroke(width = 11.dp.toPx(), cap = StrokeCap.Round))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$percent%", color = Color.White, fontSize = 48.sp, fontWeight = FontWeight.Light, letterSpacing = (-2).sp)
                        Text(if (complete) "HAZIR" else "VYRA APK", color = Color.White.copy(alpha = 0.68f), fontSize = 9.sp, letterSpacing = 2.sp)
                    }
                }
                Text(status, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text(
                    "${formatBytes(downloadedBytes)} / ${formatBytes(totalBytes)}",
                    color = Color.White.copy(alpha = 0.70f),
                    fontSize = 12.sp,
                )
                if (update.sha256.isNullOrBlank()) {
                    Text(
                        "Bu sürümde SHA-256 doğrulaması yok. Güvenlik için indirme devre dışı.",
                        color = Color(0xFFFFB4AB),
                        fontSize = 11.sp,
                    )
                }
                Button(
                    onClick = onStart,
                    enabled = canDownload && !isDownloading && !complete,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (isDownloading) "İndiriliyor…" else if (complete) "Kurulum açılıyor" else "Gerçek APK'yı indir ve doğrula")
                }
                TextButton(onClick = onDismiss) { Text("Kapat", color = Color.White.copy(alpha = 0.76f)) }
            }
        }
    }
}

private fun formatBytes(value: Long): String {
    if (value <= 0L) return "—"
    val mb = value / 1024.0 / 1024.0
    return String.format(java.util.Locale.US, "%.1f MB", mb)
}
