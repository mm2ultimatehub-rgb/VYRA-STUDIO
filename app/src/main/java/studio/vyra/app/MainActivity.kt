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
import studio.vyra.app.apk.ApkInspector
import studio.vyra.app.apk.InstalledAppApkProvider
import studio.vyra.app.apk.InstalledApkResult
import studio.vyra.app.repository.PatchArtifactDownloader
import studio.vyra.app.repository.PatchCompatibility
import studio.vyra.app.repository.MppBundleInspector
import studio.vyra.app.repository.PatchEntry
import studio.vyra.app.repository.PatchRepository
import studio.vyra.app.repository.RepositoryClient
import studio.vyra.app.repository.RepositorySourceStore
import studio.vyra.app.catalog.VyraCatalogPatchProvider
import studio.vyra.app.catalog.CatalogApp
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
    OVERVIEW("Ana sayfa", Icons.Filled.Home),
    STORE("Mağaza", Icons.Filled.Store),
    PATCHER("Yama", Icons.Filled.Build),
    CREATOR("Üretici", Icons.Filled.Edit),
    SOURCES("Kaynaklar", Icons.Filled.Folder),
    BUILDS("Derlemeler", Icons.Filled.History),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VyraApp() {
    var selectedTab by remember { mutableStateOf(VyraTab.OVERVIEW) }
    var selectedApk by remember { mutableStateOf<Uri?>(null) }
    var selectedTarget by remember { mutableStateOf<CatalogApp?>(null) }

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
                            text = "APP CUSTOMIZATION STUDIO",
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
            VyraTab.STORE -> StoreScreen(
                padding = innerPadding,
                onSelectApp = { app ->
                    selectedTarget = app
                    selectedTab = VyraTab.PATCHER
                },
            )
            VyraTab.CREATOR -> CreatorScreen(innerPadding)
            VyraTab.PATCHER -> PatcherScreen(
                padding = innerPadding,
                selectedApk = selectedApk,
                selectedTarget = selectedTarget,
                onChooseApk = {
                    apkPicker.launch(arrayOf("application/vnd.android.package-archive"))
                },
                onUseInstalledApk = { selectedApk = it },
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
            top = padding.calculateTopPadding() + 12.dp,
            bottom = padding.calculateBottomPadding() + 28.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("MERHABA, VYRA", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary, letterSpacing = 2.sp,
                    fontWeight = FontWeight.Bold)
                Text("Uygulamalarını\nyeniden tasarla.", style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold, lineHeight = 37.sp)
                Text("Yamalarını keşfet. Kendi sürümünü oluştur.", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(26.dp),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Surface(
                            modifier = Modifier.size(54.dp),
                            shape = RoundedCornerShape(17.dp),
                            color = MaterialTheme.colorScheme.primary,
                        ) {
                            BoxCenter {
                                Text("V", color = MaterialTheme.colorScheme.onPrimary,
                                    fontSize = 31.sp, fontWeight = FontWeight.Black)
                            }
                        }
                        Column(modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text("VYRA STUDIO", style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold, letterSpacing = 1.4.sp)
                            Text("YOUR APPS. YOUR RULES.", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.1.sp)
                        }
                    }
                    Text("Tek bir çalışma alanında uygulama seç, uyumlu yamaları incele ve kendi özelleştirilmiş sürümünü hazırlamaya başla.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = onOpenStore, modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)) {
                        Text("VYRA Mağazası'nı keşfet")
                    }
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("ÇALIŞMA ALANI", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary, letterSpacing = 1.5.sp,
                    fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text("VYRA STUDIO", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            ActionCard("Yama merkezini aç", "APK seç, uygulama bilgilerini incele ve uyumlu yamaları kontrol et.",
                "YAMA MERKEZİ", onOpenPatcher)
        }
        item {
            ActionCard("Uygulama mağazası", "Destek durumu ve yama kaynakları bulunan uygulamaları keşfet.",
                "MAĞAZAYI AÇ", onOpenStore)
        }
        item {
            ActionCard("Yama koleksiyonu oluştur", "Kendi yama paketlerin ve koleksiyonların için çalışma alanı.",
                "CREATOR'I AÇ", onOpenCreator)
        }
        item {
            ActionCard("Kaynaklarını yönet", "Depoları incele, yeni kaynak ekle ve güven durumlarını kontrol et.",
                "KAYNAKLARI GÖR", onOpenSources)
        }
        item {
            Text("Güvenli çalışma", style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold)
            Text("VYRA, uyumluluğu doğrulanmamış paketleri otomatik çalıştırmaz. Her yama kaynağını kontrol ederek ilerle.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun StoreScreen(padding: PaddingValues, onSelectApp: (CatalogApp) -> Unit) {
    var query by remember { mutableStateOf("") }
    val apps = studio.vyra.app.catalog.AppCatalog.apps.filter {
        it.name.contains(query, ignoreCase = true) || it.packageName.contains(query, ignoreCase = true)
    }
    PageColumn(padding) {
        PageHeading("VYRA Mağazası", "Uygulamanı seç. Destek ve uyumluluk durumunu gör.")
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(22.dp),
        ) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("UYGULAMA KATALOĞU", color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelMedium, letterSpacing = 1.5.sp,
                    fontWeight = FontWeight.Bold)
                Text("Bir sonraki düzenlemen burada başlıyor.",
                    style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Listelenen her uygulama henüz yamalanabilir anlamına gelmez. VYRA, uyumluluğu doğrulanmamış yamaları hazır gibi göstermez.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Uygulama ara") },
            placeholder = { Text("YouTube, Spotify, Discord…") },
            singleLine = true,
            shape = RoundedCornerShape(15.dp),
        )
        Text("\${apps.size} UYGULAMA", style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary, letterSpacing = 1.3.sp,
            fontWeight = FontWeight.Bold)
        apps.forEach { app ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(19.dp),
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(44.dp),
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                        ) {
                            BoxCenter {
                                Text(app.name.take(1).uppercase(), color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            }
                        }
                        Column(Modifier.weight(1f).padding(start = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(app.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(app.packageName, style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Text(app.availability.label, style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    Text(app.availability.detail, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(
                        onClick = { onSelectApp(app) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(13.dp),
                    ) { Text("Bu uygulamayı seç") }
                }
            }
        }
        if (apps.isEmpty()) {
            InfoCard("Sonuç bulunamadı", "Arama kelimesini değiştirip tekrar dene.")
        }
    }
}

@Composable
private fun PatcherScreen(padding: PaddingValues, selectedApk: Uri?, selectedTarget: CatalogApp?, onChooseApk: () -> Unit, onUseInstalledApk: (Uri) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var inspection by remember(selectedApk) { mutableStateOf<ApkInspection?>(null) }
    var inspectionError by remember(selectedApk) { mutableStateOf<String?>(null) }
    var isInspecting by remember(selectedApk) { mutableStateOf(false) }
    var patchMatches by remember(selectedApk) { mutableStateOf<List<Pair<String, PatchEntry>>>(emptyList()) }
    var isCheckingPatches by remember(selectedApk) { mutableStateOf(false) }
    var patchLookupMessage by remember(selectedApk) { mutableStateOf<String?>(null) }
    var downloadingArtifactId by remember(selectedApk) { mutableStateOf<String?>(null) }
    var artifactStatuses by remember(selectedApk) { mutableStateOf<Map<String, String>>(emptyMap()) }
    var installedApkStatus by remember(selectedTarget) { mutableStateOf<String?>(null) }
    var isCopyingInstalledApk by remember { mutableStateOf(false) }

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


    LaunchedEffect(inspection, selectedTarget) {
        val apk = inspection ?: run {
            patchMatches = emptyList()
            patchLookupMessage = null
            return@LaunchedEffect
        }
        patchMatches = emptyList()
        if (selectedTarget != null && selectedTarget.packageName != apk.packageName) {
            isCheckingPatches = false
            patchLookupMessage = "Selected app does not match this APK. Choose an APK for ${selectedTarget.name} before checking patches."
            return@LaunchedEffect
        }
        isCheckingPatches = true
        patchLookupMessage = null
        try {
            val store = RepositorySourceStore(context)
            val client = RepositoryClient()
            val urls = withContext(Dispatchers.IO) { store.getUrls() }
            val matches = mutableListOf<Pair<String, PatchEntry>>()
            try {
                val centralMatches = withContext(Dispatchers.IO) {
                    VyraCatalogPatchProvider().fetchCompatible(apk)
                }
                matches += centralMatches
            } catch (cancel: kotlinx.coroutines.CancellationException) {
                throw cancel
            } catch (_: Exception) {
                // A central catalog outage must not prevent locally configured sources from working.
            }
            for (sourceUrl in urls) {
                try {
                    val repository = withContext(Dispatchers.IO) { client.fetch(sourceUrl) }
                    repository.patches.filter { PatchCompatibility.matches(it, apk) }
                        .forEach { matches += repository.name to it }
                } catch (cancel: kotlinx.coroutines.CancellationException) {
                throw cancel
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
        PageHeading("Yama Merkezi", "APK dosyanı incele ve uyumlu yamaları kontrol et.")
        selectedTarget?.let { target ->
            InfoCard("Selected app: ${target.name}", "${target.packageName}\n${target.availability.label}")
        }
        InfoCard("1. APK dosyan",  selectedApk?.lastPathSegment?.substringAfterLast('/') ?: "Select an APK from your device storage. The file stays on your device.")
        Button(onClick = onChooseApk, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
            Text(if (selectedApk == null) "APK seç" else "Başka APK seç")
        }
        if (selectedTarget != null) {
            Button(
                onClick = {
                    val target = selectedTarget
                    if (target != null) {
                        scope.launch {
                            isCopyingInstalledApk = true
                            installedApkStatus = null
                            val result = withContext(Dispatchers.IO) {
                                InstalledAppApkProvider(context).copyBaseApk(target.packageName)
                            }
                            when (result) {
                                is InstalledApkResult.Available -> {
                                    installedApkStatus = "Copied installed ${result.label} APK into VYRA's private cache."
                                    onUseInstalledApk(result.uri)
                                }
                                is InstalledApkResult.Unavailable -> installedApkStatus = result.reason
                            }
                            isCopyingInstalledApk = false
                        }
                    }
                },
                enabled = !isCopyingInstalledApk,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text(if (isCopyingInstalledApk) "Hazırlanıyor…" else "Telefondaki uygulamayı kullan")
            }
            installedApkStatus?.let { InfoCard("Kurulu uygulama", it) }
        }
        when {
            isInspecting -> InfoCard("APK inceleniyor", "Paket bilgileri okunuyor ve SHA-256 özeti hesaplanıyor…")
            inspectionError != null -> InfoCard("İnceleme başarısız", inspectionError ?: "Bilinmeyen hata.")
            inspection != null -> {
                ApkDetailsCard(inspection!!)
                if (selectedTarget != null && inspection!!.packageName != selectedTarget.packageName) {
                    InfoCard("App mismatch", "You selected ${selectedTarget.name}, but this APK is ${inspection!!.appLabel}. Choose the correct APK before continuing.")
                }
            }
        }
        when {
            isCheckingPatches -> InfoCard("Uyumluluk kontrol ediliyor", "APK, kayıtlı yama kaynaklarıyla karşılaştırılıyor…")
            patchLookupMessage != null -> InfoCard("Compatibility", patchLookupMessage!!)
        }
        patchMatches.forEach { (repositoryName, patch) ->
            InfoCard(
                title = patch.name,
                body = "Source: ${repositoryName}\nTarget: ${patch.targetPackage}\nFormat: ${patch.format.uppercase()}\nArtifact SHA-256: ${patch.artifactSha256}",
            )
            Button(
                onClick = {
                    downloadingArtifactId = patch.id
                    scope.launch {
                        try {
                            val (file, metadata) = withContext(Dispatchers.IO) {
                                val verified = PatchArtifactDownloader().downloadAndVerify(patch, context.cacheDir)
                                verified to MppBundleInspector().inspect(verified, patch.expectedArtifactVersion)
                            }
                            val label = metadata.name ?: file.name
                            val version = metadata.version?.let { " v$it" }.orEmpty()
                            artifactStatuses = artifactStatuses + (
                                patch.id to "SHA-256 verified · MPP inspected: $label$version · ${metadata.dexEntryCount} DEX entr(y/ies). Verified locally; engine import is not connected yet."
                            )
                        } catch (error: Exception) {
                            artifactStatuses = artifactStatuses + (patch.id to (error.message ?: "Artifact verification failed."))
                        } finally {
                            downloadingArtifactId = null
                        }
                    }
                },
                enabled = downloadingArtifactId == null,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(if (downloadingArtifactId == patch.id) "Downloading and verifying…" else "Download and verify patch")
            }
            artifactStatuses[patch.id]?.let { InfoCard("Patch package status", it) }
        }
        InfoCard("2. Yama uyumluluğu", "Yalnızca paket ve sürüm koşulları eşleşen yamalar listelenir.")
        InfoCard("3. Oluştur ve dışa aktar", "Yama motorunun bu ekrandaki işlem akışına bağlantısı henüz tamamlanmadı. Bu sürüm APK değiştirmez veya üretmez.")
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
        PageHeading("Yama Atölyesi", "Kendi yama koleksiyonunu tasarla ve doğrula.")
        InfoCard("Koleksiyon çalışma alanı", "Koleksiyon adı, sürüm, açıklama, hedef uygulama ve uyumluluk kuralları.")
        InfoCard("Doğrulama", "Dışa aktarma açılmadan önce manifest kontrolleri ve paketleme tamamlanacak.")
        InfoCard("Henüz kod çalıştırmaz", "Bu ekran şimdilik çalışma alanı taslağıdır; yama kodu derlemez veya çalıştırmaz.")
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
