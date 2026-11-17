package studio.vyra.app

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
import studio.vyra.app.catalog.RemoteVyraCatalog
import studio.vyra.app.catalog.VyraRepositoryCatalogClient
import studio.vyra.app.apk.ApkInspector
import studio.vyra.app.repository.PatchArtifactDownloader
import studio.vyra.app.repository.PatchCompatibility
import studio.vyra.app.repository.MppBundleInspector
import studio.vyra.app.repository.PatchEntry
import studio.vyra.app.repository.PatchRepository
import studio.vyra.app.repository.RepositoryClient
import studio.vyra.app.repository.RepositorySourceStore
import studio.vyra.app.ui.theme.VyraTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VyraTheme {
                VyraApp()
            }
        }
    }
}

private enum class VyraTab(
    val label: String,
    val icon: ImageVector,
) {
    OVERVIEW("Overview", Icons.Filled.Home),
    STORE("Store", Icons.Filled.Store),
    PATCHER("Patcher", Icons.Filled.Build),
    CREATOR("Creator", Icons.Filled.Edit),
    SOURCES("Sources", Icons.Filled.Folder),
    BUILDS("Builds", Icons.Filled.History),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VyraApp() {
    var selectedTab by remember { mutableStateOf(VyraTab.OVERVIEW) }
    var selectedApk by remember { mutableStateOf<Uri?>(null) }

    val apkPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { selectedApk = it },
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "VYRA STUDIO",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp,
                        )
                        Text(
                            text = "ANDROID PATCH WORKSPACE",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.2.sp,
                        )
                    }
                },
                actions = { studio.vyra.app.update.UpdateCheckAction() },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                VyraTab.entries.filter { it != VyraTab.CREATOR }.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                        alwaysShowLabel = false,
                    )
                }
            }
        },
    ) { innerPadding ->
        when (selectedTab) {
            VyraTab.OVERVIEW -> OverviewScreen(
                padding = innerPadding,
                onOpenStore = { selectedTab = VyraTab.STORE },
                onOpenPatcher = { selectedTab = VyraTab.PATCHER },
                onOpenCreator = { selectedTab = VyraTab.CREATOR },
                onOpenSources = { selectedTab = VyraTab.SOURCES },
            )
            VyraTab.STORE -> StoreScreen(innerPadding)
            VyraTab.CREATOR -> CreatorScreen(innerPadding)
            VyraTab.PATCHER -> PatcherScreen(
                padding = innerPadding,
                selectedApk = selectedApk,
                onChooseApk = {
                    apkPicker.launch(arrayOf("application/vnd.android.package-archive"))
                },
            )
            VyraTab.SOURCES -> SourcesScreen(innerPadding)
            VyraTab.BUILDS -> BuildsScreen(innerPadding)
        }
    }
}

@Composable
private fun OverviewScreen(
    padding: PaddingValues,
    onOpenStore: () -> Unit,
    onOpenPatcher: () -> Unit,
    onOpenCreator: () -> Unit,
    onOpenSources: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = padding.calculateTopPadding() + 18.dp,
            bottom = padding.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text("Your workspace,\nwithout the clutter.", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("Discover patches, prepare builds, and keep every source in one place.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Surface(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), shape = RoundedCornerShape(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Surface(modifier = Modifier.size(42.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primary) {
                        BoxCenter { Text("V", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold) }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Early access", fontWeight = FontWeight.SemiBold)
                        Text("The workspace is taking shape.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        item { Text("Quick actions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
        item { ActionCard("Explore the Vyra Store", "Browse patch collections and release information.", "Open store", onOpenStore) }
        item { ActionCard("Start a patching session", "Choose an APK you are authorized to modify.", "Open patcher", onOpenPatcher) }
        item { ActionCard("Create a patch collection", "Prepare your own patch pack and metadata.", "Open creator", onOpenCreator) }
        item { ActionCard("Manage patch sources", "Review repositories and their provenance.", "View sources", onOpenSources) }
        item { Text("No builds yet", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
private fun StoreScreen(padding: PaddingValues) {
    val client = remember { VyraRepositoryCatalogClient() }
    val scope = rememberCoroutineScope()
    var catalog by remember { mutableStateOf<RemoteVyraCatalog?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun refreshCatalog() {
        scope.launch {
            isLoading = true
            error = null
            try {
                catalog = withContext(Dispatchers.IO) { client.fetch() }
            } catch (failure: Exception) {
                error = failure.message ?: "Could not load the VYRA catalog."
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) { refreshCatalog() }

    PageColumn(padding) {
        PageHeading("Vyra Store", "Live app and patch-source catalog from VYRA Repository.")
        Button(
            onClick = { refreshCatalog() },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
        ) { Text(if (isLoading) "Refreshing catalog…" else "Refresh catalog") }

        if (isLoading && catalog == null) {
            InfoCard("Connecting to VYRA Repository", "Loading the app list, source records, and version compatibility data.")
        }
        error?.let { InfoCard("Catalog unavailable", "$it\nThe last successfully loaded catalog remains visible, if available.") }
        val loaded = catalog
        if (loaded == null && !isLoading && error == null) {
            InfoCard("Catalog not loaded", "VYRA will retry when you refresh this screen.")
        }
        loaded?.let { remote ->
            InfoCard(
                "Repository connected",
                "Loaded ${remote.apps.size} apps and ${remote.sources.size} patch sources.\nCatalog timestamp: ${remote.generatedAt.ifBlank { "Not provided" }}",
            )
            remote.apps.forEach { app ->
                val statusLabel = when (app.status) {
                    "verified" -> "Verified"
                    "candidate" -> "Candidate — not tested end to end"
                    "experimental" -> "Experimental — community source"
                    "unsupported" -> "Unsupported"
                    else -> app.status
                }
                val sourceDetails = app.patchSourceIds.joinToString("\n") { sourceId ->
                    val source = remote.sources.firstOrNull { it.id == sourceId }
                    if (source == null) sourceId else {
                        val version = source.releaseVersion?.let { " • bundle $it" }.orEmpty()
                        "${source.publisher} ($sourceId)$version"
                    }
                }
                val versionDetails = app.targetVersions.entries.joinToString("\n") { (sourceId, versions) ->
                    "$sourceId versions: ${versions.joinToString(", ").ifBlank { "Not listed" }}"
                }
                InfoCard(
                    app.name,
                    buildString {
                        appendLine(app.packageName)
                        appendLine("Status: $statusLabel")
                        appendLine("Category: ${app.category}")
                        appendLine("Patch sources: ${app.patchSourceIds.size}")
                        if (sourceDetails.isNotBlank()) appendLine(sourceDetails)
                        if (versionDetails.isNotBlank()) {
                            appendLine()
                            append(versionDetails)
                        }
                    },
                )
            }
            InfoCard(
                "Safe patch handling",
                "This screen reads catalog metadata only. It does not download or execute patch bundles, fetch APKs, or build modified APKs. Experimental sources remain untrusted and require review.",
            )
        }
    }
}
@Composable
private fun PatcherScreen(padding: PaddingValues, selectedApk: Uri?, onChooseApk: () -> Unit) {
    val context = LocalContext.current
    var inspection by remember(selectedApk) { mutableStateOf<ApkInspection?>(null) }
    var inspectionError by remember(selectedApk) { mutableStateOf<String?>(null) }
    var isInspecting by remember(selectedApk) { mutableStateOf(false) }
    var patchMatches by remember(selectedApk) { mutableStateOf<List<Pair<String, PatchEntry>>>(emptyList()) }
    var isCheckingPatches by remember(selectedApk) { mutableStateOf(false) }
    var patchLookupMessage by remember(selectedApk) { mutableStateOf<String?>(null) }
    var catalogPatchMatches by remember(selectedApk) { mutableStateOf<List<Triple<String, String, PatchEntry>>>(emptyList()) }
    var catalogLookupMessage by remember(selectedApk) { mutableStateOf<String?>(null) }
    var downloadingPatchId by remember(selectedApk) { mutableStateOf<String?>(null) }
    var bundleInspectionMessage by remember(selectedApk) { mutableStateOf<String?>(null) }
    val patcherScope = rememberCoroutineScope()

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

            try {
                val remoteCatalog = withContext(Dispatchers.IO) { VyraRepositoryCatalogClient().fetch() }
                val remoteMatches = buildList {
                    remoteCatalog.apps.filter { it.packageName == apk.packageName }.forEach { app ->
                        app.patchSourceIds.forEach sourceLoop@{ sourceId ->
                            val patch = remoteCatalog.patchFor(app.id, sourceId) ?: return@sourceLoop
                            if (PatchCompatibility.matches(patch, apk)) {
                                val source = remoteCatalog.sources.firstOrNull { it.id == sourceId }
                                if (source != null) {
                                    val trustLabel = if (source.trust == "official-publisher") "Official publisher" else "Unverified community source"
                                    add(Triple(source.publisher, "${app.status} • $trustLabel • execution ${source.execution}", patch))
                                }
                            }
                        }
                    }
                }
                catalogPatchMatches = remoteMatches
                catalogLookupMessage = if (remoteMatches.isEmpty()) {
                    "No exact-version match in the VYRA catalog for this APK."
                } else {
                    "${remoteMatches.size} VYRA catalog match(es) found. These are compatibility candidates, not applied patches."
                }
            } catch (catalogError: Exception) {
                catalogPatchMatches = emptyList()
                catalogLookupMessage = "VYRA catalog could not be checked: ${catalogError.message ?: "unknown error"}"
            }
        } finally {
            isCheckingPatches = false
        }
    }

    // Prepare official, hash-pinned bundles automatically after an exact APK/version match.
    // Community bundles remain manual; no downloaded patch code is executed here.
    LaunchedEffect(catalogPatchMatches) {
        val officialCandidates = catalogPatchMatches.filter { (_, status, _) ->
            status.contains("Official publisher") &&
                status.contains("execution disabled-until-reviewed")
        }
        for ((publisher, _, patch) in officialCandidates) {
            downloadingPatchId = patch.id
            bundleInspectionMessage = "Automatically preparing the official patch bundle from $publisher…"
            try {
                val inspected = withContext(Dispatchers.IO) {
                    val file = PatchArtifactDownloader().downloadAndVerify(patch, context.cacheDir)
                    val metadata = MppBundleInspector().inspect(file)
                    file to metadata
                }
                val metadata = inspected.second
                bundleInspectionMessage = buildString {
                    appendLine("Automatic preparation complete: ${inspected.first.name}")
                    appendLine("Publisher: $publisher")
                    appendLine("SHA-256: ${patch.artifactSha256} (verified)")
                    appendLine("MPP name: ${metadata.name ?: "Not declared"}")
                    appendLine("MPP version: ${metadata.version ?: "Not declared"}")
                    appendLine("Author: ${metadata.author ?: "Not declared"}")
                    appendLine("License: ${metadata.license ?: "Not declared"}")
                    appendLine("DEX entries: ${metadata.dexEntryCount}")
                    append("Inspection only — patch execution and APK rebuilding are not enabled yet.")
                }
            } catch (failure: Exception) {
                bundleInspectionMessage = "Automatic bundle preparation failed: ${failure.message ?: "unknown error"}"
            } finally {
                downloadingPatchId = null
            }
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
        catalogLookupMessage?.let { InfoCard("VYRA Repository compatibility", it) }
        catalogPatchMatches.forEach { (publisher, sourceStatus, patch) ->
            InfoCard(
                title = patch.name,
                body = "Publisher: $publisher\nTarget: ${patch.targetPackage}\nExact app versions: ${patch.targetVersions.sorted().joinToString(", ")}\nSource status: $sourceStatus\nSHA-256: ${patch.artifactSha256}\nThe bundle can be downloaded for inspection only; patch execution is disabled.",
            )
            Button(
                onClick = {
                    patcherScope.launch {
                        downloadingPatchId = patch.id
                        bundleInspectionMessage = null
                        try {
                            val inspected = withContext(Dispatchers.IO) {
                                val file = PatchArtifactDownloader().downloadAndVerify(patch, context.cacheDir)
                                val metadata = MppBundleInspector().inspect(file)
                                file to metadata
                            }
                            val metadata = inspected.second
                            bundleInspectionMessage = buildString {
                                appendLine("Verified download: ${inspected.first.name}")
                                appendLine("SHA-256: ${patch.artifactSha256}")
                                appendLine("MPP name: ${metadata.name ?: "Not declared"}")
                                appendLine("MPP version: ${metadata.version ?: "Not declared"}")
                                appendLine("Author: ${metadata.author ?: "Not declared"}")
                                appendLine("License: ${metadata.license ?: "Not declared"}")
                                appendLine("DEX entries: ${metadata.dexEntryCount}")
                                append("Inspection only — no patch code was executed.")
                            }
                        } catch (failure: Exception) {
                            bundleInspectionMessage = "Download or inspection failed: ${failure.message ?: "unknown error"}"
                        } finally {
                            downloadingPatchId = null
                        }
                    }
                },
                enabled = downloadingPatchId == null,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text(if (downloadingPatchId == patch.id) "Downloading and checking…" else "Download and inspect MPP")
            }
        }
        bundleInspectionMessage?.let { InfoCard("MPP bundle inspection", it) }
        InfoCard("2. Compatibility", "VYRA checks the package name and exact version name against the central catalog and saved sources. A match only confirms listed compatibility; it does not mean the patch was applied or tested on a device.")
        InfoCard("3. Build and export", "The patch engine is not connected yet. This version does not modify or generate APK files.")
    }
}

@Composable
private fun ApkDetailsCard(info: ApkInspection) {
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
private fun CreatorScreen(padding: PaddingValues) {
    PageColumn(padding) {
        PageHeading("Patch Creator", "Design and validate your own patch collection.")
        InfoCard("Manifest workspace", "Create a collection name, version, description, target apps, and compatibility rules.")
        InfoCard("Validation", "Manifest checks and patch packaging will be added before the creator can export a release.")
        InfoCard("No code execution yet", "This screen is a product shell. It does not compile or execute patch code.")
    }
}

@Composable
private fun SourcesScreen(padding: PaddingValues) {
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
private fun RepositoryCard(
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
private fun BuildsScreen(padding: PaddingValues) {
    PageColumn(padding) {
        PageHeading("Build center", "Follow every build from start to exported artifact.")
        InfoCard("No build history", "Your completed builds, validation results, and logs will be listed here.")
    }
}

@Composable
private fun PageColumn(padding: PaddingValues, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(
            start = 20.dp, end = 20.dp,
            top = padding.calculateTopPadding() + 20.dp,
            bottom = padding.calculateBottomPadding() + 20.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) { content() }
}

@Composable
private fun PageHeading(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun InfoCard(title: String, body: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(18.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ActionCard(title: String, description: String, action: String, onClick: () -> Unit) {
    Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(18.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(action.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun BoxCenter(content: @Composable () -> Unit) {
    androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        content()
    }
}
