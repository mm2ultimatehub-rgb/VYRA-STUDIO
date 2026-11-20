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

fun SourcesScreen(padding: PaddingValues) {
    val context = LocalContext.current
    val store = remember(context) { RepositorySourceStore(context) }
    val client = remember { RepositoryClient() }
    val scope = rememberCoroutineScope()
    var url by remember { mutableStateOf("") }
    var sources by remember { mutableStateOf<List<Pair<String, PatchRepository>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    var pendingSource by remember { mutableStateOf<Pair<String, PatchRepository>?>(null) }

    fun refreshAll() {
        scope.launch {
            isLoading = true
            status = null
            val urls = withContext(Dispatchers.IO) { store.getUrls() }
            val loaded = mutableListOf<Pair<String, PatchRepository>>()
            val failures = mutableListOf<String>()
            for (sourceUrl in urls) {
                try {
                    val repository = withContext(Dispatchers.IO) { client.fetch(sourceUrl) }
                    loaded += sourceUrl to repository
                } catch (error: Exception) {
                    failures += "${sourceUrl}: ${error.message ?: "Refresh failed"}"
                }
            }
            sources = loaded
            status = if (failures.isEmpty()) {
                if (loaded.isEmpty()) "No saved repositories yet." else "Updated ${loaded.size} source(s)."
            } else {
                failures.joinToString("\n")
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { refreshAll() }

    PageColumn(padding) {
        PageHeading("Repositories", "Add an HTTPS repository index and inspect its patch catalog.")
        InfoCard("Source safety", "Only repository metadata is fetched. Patch files are not downloaded or executed. Adding a source does not mean it is trusted.")
        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Repository index URL") },
            placeholder = { Text("https://example.com/vyra/index.json") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
        )
        Button(
            onClick = {
                val candidate = url.trim()
                scope.launch {
                    isLoading = true
                    status = null
                    try {
                        val repository = withContext(Dispatchers.IO) { client.fetch(candidate) }
                        pendingSource = candidate to repository
                        status = "Repository validated. Review its origin before adding it."
                    } catch (error: Exception) {
                        status = error.message ?: "Could not add repository."
                    } finally {
                        isLoading = false
                    }
                }
            },
            enabled = !isLoading && url.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
        ) { Text("Validate and add source") }
        Button(
            onClick = { refreshAll() },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
        ) { Text("Refresh saved sources") }
        if (isLoading) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                Text("Fetching repository indexes…", style = MaterialTheme.typography.bodySmall)
            }
        }
        status?.let { InfoCard("Source status", it) }
        if (sources.isEmpty() && !isLoading) {
            InfoCard("No sources added", "Paste a direct HTTPS URL to a repository index that follows the Vyra Repository Index v1 format.")
        }
        sources.forEach { (sourceUrl, repository) ->
            RepositoryCard(
                sourceUrl = sourceUrl,
                repository = repository,
                onRemove = {
                    scope.launch {
                        withContext(Dispatchers.IO) { store.removeUrl(sourceUrl) }
                        sources = sources.filterNot { it.first == sourceUrl }
                        status = "Removed source: ${repository.name}"
                    }
                },
            )
        }
    }

    pendingSource?.let { (candidate, repository) ->
        AlertDialog(
            onDismissRequest = { pendingSource = null },
            title = { Text("Review patch source") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(repository.name, fontWeight = FontWeight.SemiBold)
                    Text(repository.description.ifBlank { "No description provided." })
                    Text(candidate, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    Text("${repository.patches.size} patch(es) listed.")
                    Text("Only add sources you trust. A future patch engine may execute code supplied by a source. VYRA currently validates and downloads artifacts but does not execute patches.")
                }
            },
            confirmButton = {
                Button(onClick = {
                    val accepted = pendingSource
                    if (accepted != null) {
                        scope.launch {
                            withContext(Dispatchers.IO) { store.addUrl(accepted.first) }
                            sources = sources.filterNot { it.first == accepted.first } + accepted
                            url = ""
                            status = "Added ${accepted.second.name} — ${accepted.second.patches.size} patch(es)."
                            pendingSource = null
                        }
                    }
                }) { Text("I trust this source") }
            },
            dismissButton = {
                Button(onClick = { pendingSource = null }) { Text("Cancel") }
            },
        )
    }
}

@Composable
fun RepositoryCard(
    sourceUrl: String,
    repository: PatchRepository,
    onRemove: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var downloadingPatchId by remember { mutableStateOf<String?>(null) }
    var downloadStatuses by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(18.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(repository.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(repository.description.ifBlank { "No repository description provided." }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(sourceUrl, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            Text("${repository.patches.size} patch(es)", style = MaterialTheme.typography.labelMedium)
            Button(onClick = onRemove, shape = RoundedCornerShape(12.dp)) {
                Text("Remove source")
            }
            repository.patches.forEach { patch ->
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(patch.name, fontWeight = FontWeight.Medium)
                    Text("${patch.targetPackage} · ${patch.format.uppercase()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("SHA-256: ${patch.artifactSha256}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(
                        onClick = {
                            downloadingPatchId = patch.id
                            scope.launch {
                                try {
                                    val (file, metadata) = withContext(Dispatchers.IO) {
                                        val verifiedFile = PatchArtifactDownloader().downloadAndVerify(patch, context.cacheDir)
                                        val bundleMetadata = MppBundleInspector().inspect(verifiedFile)
                                        verifiedFile to bundleMetadata
                                    }
                                    val bundleName = metadata.name ?: file.name
                                    val bundleVersion = metadata.version?.let { " v$it" }.orEmpty()
                                    val patcherVersion = metadata.patcherVersion?.let { " · Patcher $it" }.orEmpty()
                                    downloadStatuses = downloadStatuses + (
                                        patch.id to "SHA-256 verified · MPP structure valid: $bundleName$bundleVersion · ${metadata.dexEntryCount} DEX entr(y/ies)$patcherVersion"
                                    )
                                } catch (error: Exception) {
                                    downloadStatuses = downloadStatuses + (patch.id to (error.message ?: "Patch download failed."))
                                } finally {
                                    downloadingPatchId = null
                                }
                            }
                        },
                        enabled = downloadingPatchId == null,
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text(if (downloadingPatchId == patch.id) "Downloading…" else "Download & verify")
                    }
                    downloadStatuses[patch.id]?.let { message ->
                        Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
