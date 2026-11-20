package studio.vyra.app.update

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.MessageDigest

@Composable
fun UpdateCheckAction() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var checking by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<UpdateCheckResult?>(null) }
    var downloadUpdate by remember { mutableStateOf<AppUpdate?>(null) }
    var downloadId by remember { mutableStateOf<Long?>(null) }
    var downloadedBytes by remember { mutableStateOf(0L) }
    var progressPercent by remember { mutableStateOf(0) }
    var downloadStatus by remember { mutableStateOf("İndirmeye hazır") }
    var isDownloading by remember { mutableStateOf(false) }

    IconButton(
        modifier = Modifier.padding(end = 8.dp).background(Color.White.copy(alpha = 0.08f), CircleShape).border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape),
        onClick = {
            if (!checking) {
                checking = true
                result = null
                scope.launch {
                    val currentName = context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0.0.0"
                    result = withContext(Dispatchers.IO) { AppUpdateChecker().check(currentName) }
                    checking = false
                }
            }
        },
        enabled = !checking,
    ) {
        if (checking) CircularProgressIndicator()
        else Icon(Icons.Filled.SystemUpdateAlt, contentDescription = "Check for VYRA updates")
    }

    result?.let { state ->
        when (state) {
            is UpdateCheckResult.UpdateAvailable -> AlertDialog(
                onDismissRequest = { result = null },
                title = { Text("VYRA update available") },
                text = {
                    androidx.compose.foundation.layout.Column {
                        Text("Version ${state.update.version} is available.")
                        if (state.update.fileSizeBytes > 0L) {
                            Text("APK size: ${state.update.fileSizeBytes / 1024 / 1024} MB")
                        }
                        if (state.update.sha256.isNullOrBlank()) {
                            Text(
                                "This release has no SHA-256 digest. Download is blocked until its checksum is published.",
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                        if (state.update.releaseNotes.isNotBlank()) Text(state.update.releaseNotes.take(1200))
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        downloadUpdate = state.update
                        downloadedBytes = 0L
                        progressPercent = 0
                        downloadStatus = "İndirmeye hazır"
                        result = null
                    }) { Text("Download APK") }
                },
                dismissButton = { TextButton(onClick = { result = null }) { Text("Close") } },
            )
            UpdateCheckResult.UpToDate -> AlertDialog(
                onDismissRequest = { result = null },
                title = { Text("VYRA is up to date") },
                text = { Text("You are using the latest stable release.") },
                confirmButton = { TextButton(onClick = { result = null }) { Text("OK") } },
            )
            is UpdateCheckResult.Failed -> AlertDialog(
                onDismissRequest = { result = null },
                title = { Text("Update check failed") },
                text = { Text(state.reason) },
                confirmButton = { TextButton(onClick = { result = null }) { Text("OK") } },
            )
        }
    }

    val update = downloadUpdate
    if (update != null) {
        LiquidGlassDownloadDialog(
            update = update,
            progressPercent = progressPercent,
            downloadedBytes = downloadedBytes,
            status = downloadStatus,
            isDownloading = isDownloading,
            canDownload = !update.sha256.isNullOrBlank() && !isDownloading && downloadId == null,
            onStart = {
                if (!isDownloading && downloadId == null && !update.sha256.isNullOrBlank()) {
                    scope.launch {
                        try {
                            isDownloading = true
                            downloadStatus = "GitHub'dan gerçek APK indiriliyor"
                            val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                            val request = DownloadManager.Request(Uri.parse(update.apkUrl))
                                .setTitle("VYRA Store ${update.version}")
                                .setDescription("VYRA APK indiriliyor ve SHA-256 doğrulanacak")
                                .setMimeType("application/vnd.android.package-archive")
                                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                .setAllowedOverMetered(true)
                                .setAllowedOverRoaming(false)
                                .setDestinationInExternalFilesDir(
                                    context,
                                    Environment.DIRECTORY_DOWNLOADS,
                                    "vyra-store-${update.version}.apk",
                                )
                            val id = manager.enqueue(request)
                            downloadId = id
                            var finished = false
                            while (!finished) {
                                delay(400)
                                val cursor = manager.query(DownloadManager.Query().setFilterById(id))
                                cursor.use {
                                    if (!it.moveToFirst()) throw IllegalStateException("Download task disappeared")
                                    val status = it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                                    downloadedBytes = it.getLong(it.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)).coerceAtLeast(0L)
                                    val total = it.getLong(it.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                                    progressPercent = if (total > 0L) ((downloadedBytes * 100L) / total).toInt().coerceIn(0, 100) else 0
                                    when (status) {
                                        DownloadManager.STATUS_PENDING -> downloadStatus = "İndirme sıraya alındı"
                                        DownloadManager.STATUS_RUNNING -> downloadStatus = "Gerçek APK indiriliyor"
                                        DownloadManager.STATUS_PAUSED -> downloadStatus = "İndirme beklemede"
                                        DownloadManager.STATUS_SUCCESSFUL -> finished = true
                                        DownloadManager.STATUS_FAILED -> {
                                            val reason = it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
                                            throw IllegalStateException("APK indirme başarısız (kod $reason)")
                                        }
                                    }
                                }
                            }
                            downloadStatus = "SHA-256 doğrulanıyor"
                            val apkUri = manager.getUriForDownloadedFile(id)
                                ?: throw IllegalStateException("Downloaded APK could not be opened")
                            val actualDigest = withContext(Dispatchers.IO) {
                                context.contentResolver.openInputStream(apkUri)?.use { input ->
                                    val digest = MessageDigest.getInstance("SHA-256")
                                    val buffer = ByteArray(64 * 1024)
                                    while (true) {
                                        val count = input.read(buffer)
                                        if (count < 0) break
                                        digest.update(buffer, 0, count)
                                    }
                                    digest.digest().joinToString("") { "%02x".format(it) }
                                } ?: throw IllegalStateException("Cannot read downloaded APK")
                            }
                            if (!actualDigest.equals(update.sha256, ignoreCase = true)) {
                                manager.remove(id)
                                throw SecurityException("SHA-256 uyuşmadı. APK güvenlik için silindi; kurulum engellendi.")
                            }
                            progressPercent = 100
                            downloadedBytes = update.fileSizeBytes
                            downloadStatus = "APK doğrulandı · kurulum açılıyor"
                            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(apkUri, "application/vnd.android.package-archive")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(installIntent)
                        } catch (error: Exception) {
                            downloadStatus = error.message ?: "İndirme veya doğrulama başarısız"
                        } finally {
                            isDownloading = false
                            downloadId = null
                        }
                    }
                }
            },
            onDismiss = {
                if (isDownloading) {
                    downloadId?.let { id ->
                        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                        manager.remove(id)
                    }
                    isDownloading = false
                    downloadStatus = "İndirme iptal edildi"
                }
                downloadUpdate = null
                downloadId = null
            },
        )
    }
}
