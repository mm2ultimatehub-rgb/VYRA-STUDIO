package studio.vyra.app.update

import android.content.Intent
import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun UpdateCheckAction() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var checking by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<UpdateCheckResult?>(null) }

    IconButton(
        onClick = {
            if (!checking) {
                checking = true
                result = null
                scope.launch {
                    val currentName = context.packageManager.getPackageInfo(
                        context.packageName, 0
                    ).versionName ?: "0.0.0"
                    result = withContext(Dispatchers.IO) {
                        AppUpdateChecker().check(currentName)
                    }
                    checking = false
                }
            }
        },
        enabled = !checking,
    ) {
        if (checking) {
            CircularProgressIndicator()
        } else {
            Icon(Icons.Filled.SystemUpdateAlt, contentDescription = "Check for VYRA updates")
        }
    }

    result?.let { state ->
        when (state) {
            is UpdateCheckResult.UpdateAvailable -> AlertDialog(
                onDismissRequest = { result = null },
                title = { Text("VYRA update available") },
                text = {
                    androidx.compose.foundation.layout.Column {
                        Text("Version ${state.update.version} is available.")
                        if (state.update.fileSizeBytes > 0) {
                            Text("APK size: ${state.update.fileSizeBytes / 1024 / 1024} MB")
                        }
                        if (state.update.sha256 == null) {
                            Text(
                                "This release has no SHA-256 digest in GitHub metadata. " +
                                    "Do not install until its checksum can be verified.",
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                        if (state.update.releaseNotes.isNotBlank()) {
                            Text(state.update.releaseNotes.take(1200))
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(state.update.releasePageUrl))
                            )
                            result = null
                        },
                    ) { Text("Open release details") }
                },
                dismissButton = {
                    TextButton(onClick = { result = null }) { Text("Close") }
                },
            )
            UpdateCheckResult.UpToDate -> AlertDialog(
                onDismissRequest = { result = null },
                title = { Text("VYRA is up to date") },
                text = { Text("You are using the latest stable release.") },
                confirmButton = {
                    TextButton(onClick = { result = null }) { Text("OK") }
                },
            )
            is UpdateCheckResult.Failed -> AlertDialog(
                onDismissRequest = { result = null },
                title = { Text("Update check failed") },
                text = { Text(state.reason) },
                confirmButton = {
                    TextButton(onClick = { result = null }) { Text("OK") }
                },
            )
        }
    }
}
