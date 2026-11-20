package studio.vyra.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import studio.vyra.app.apk.ApkInspection
import studio.vyra.app.apk.ApkInspector
import studio.vyra.app.catalog.StoreCatalogClient
import studio.vyra.app.catalog.StoreCatalogEntry
import studio.vyra.app.repository.PatchArtifactDownloader
import studio.vyra.app.repository.PatchCompatibility
import studio.vyra.app.repository.MppBundleInspector
import studio.vyra.app.repository.PatchEntry
import studio.vyra.app.repository.PatchRepository
import studio.vyra.app.repository.RepositoryClient
import studio.vyra.app.repository.RepositorySourceStore
import studio.vyra.app.ui.theme.VyraTheme

fun PatcherScreen(padding: PaddingValues, selectedApk: Uri?, onChooseApk: () -> Unit) {
    val context = LocalContext.current
    var inspection by remember(selectedApk) { mutableStateOf<ApkInspection?>(null) }
    var inspectionError by remember(selectedApk) { mutableStateOf<String?>(null) }
    var isInspecting by remember(selectedApk) { mutableStateOf(false) }
    var patchMatches by remember(selectedApk) { mutableStateOf<List<Pair<String, PatchEntry>>>(emptyList()) }
    var isCheckingPatches by remember(selectedApk) { mutableStateOf(false) }
    var patchLookupMessage by remember(selectedApk) { mutableStateOf<String?>(null) }

    LaunchedEffect(selectedApk) {
        val uri = selectedApk ?: return@LaunchedEffect
        isInspecting = true
        inspectionError = null
        try {
            inspection = withContext(Dispatchers.IO) { ApkInspector(context).inspect(uri) }
        } catch (error: Exception) {
            inspectionError = error.message ?: "APK inspection failed."
        } finally {
            isInspecting = false
        }
    }


    LaunchedEffect(inspection) {
        val apk = inspection ?: return@LaunchedEffect
        isCheckingPatches = true
        patchLookupMessage = null
        try {
            val store = RepositorySourceStore(context)
            val client = RepositoryClient()
            val urls = withContext(Dispatchers.IO) { store.getUrls() }
            val matches = mutableListOf<Pair<String, PatchEntry>>()
            for (sourceUrl in urls) {
                try {
                    val repository = withContext(Dispatchers.IO) { client.fetch(sourceUrl) }
                    repository.patches.filter { PatchCompatibility.matches(it, apk) }
                        .forEach { matches += repository.name to it }
                } catch (_: Exception) {
                    // A failed source is reported in the Sources screen; other sources remain usable.
                }
            }
            patchMatches = matches
            patchLookupMessage = if (matches.isEmpty()) {
                "No compatible patches found in your saved sources."
            } else {
                "${matches.size} compatible patch(es) found."
            }
        } finally {
            isCheckingPatches = false
        }
    }

    PageColumn(padding) {
        PageHeading("APK Patcher", "Inspect an APK before choosing compatible patches.")
        InfoCard("1. Choose your APK", selectedApk?.lastPathSegment?.substringAfterLast('/') ?: "Select an APK from your device storage. The file stays on your device.")
        Button(onClick = onChooseApk, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
            Text(if (selectedApk == null) "Choose APK file" else "Choose another APK")
        }
        when {
            isInspecting -> InfoCard("Inspecting APK", "Reading package metadata and calculating SHA-256 fingerprints…")
            inspectionError != null -> InfoCard("Inspection failed", inspectionError ?: "Unknown inspection error.")
            inspection != null -> ApkDetailsCard(inspection!!)
        }
        when {
            isCheckingPatches -> InfoCard("Checking compatibility", "Comparing this APK with saved repository indexes…")
            patchLookupMessage != null -> InfoCard("Compatibility", patchLookupMessage!!)
        }
        patchMatches.forEach { (repositoryName, patch) ->
            InfoCard(
                title = patch.name,
                body = "Source: ${repositoryName}\nTarget: ${patch.targetPackage}\nFormat: ${patch.format.uppercase()}\nArtifact SHA-256: ${patch.artifactSha256}",
            )
        }
        InfoCard("2. Compatibility", "Only package and version-range matches are listed. Review the patch source and requirements before proceeding.")
        InfoCard("3. Build and export", "The patch engine is not connected yet. This version does not modify or generate APK files.")
    }
}

@Composable
fun ApkDetailsCard(info: ApkInspection) {
    InfoCard(
        title = "APK inspection complete",
        body = buildString {
            appendLine("App: ${info.appLabel}")
            appendLine("Package: ${info.packageName}")
            appendLine("Version: ${info.versionName ?: "Unknown"} (${info.versionCode})")
            appendLine("Minimum SDK: ${info.minSdk ?: "Unknown"}")
            appendLine("Target SDK: ${info.targetSdk ?: "Unknown"}")
            appendLine("APK SHA-256: ${info.apkSha256}")
            append("Signer SHA-256: ")
            append(if (info.signerSha256.isEmpty()) "Not available" else info.signerSha256.joinToString())
        },
    )
}

@Composable
