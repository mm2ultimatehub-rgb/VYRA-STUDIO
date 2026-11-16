package studio.vyra.app

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
            ) {
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
            Text(
                text = "Your workspace,\nwithout the clutter.",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Discover patches, prepare builds, and keep every source in one place.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                shape = RoundedCornerShape(18.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Surface(
                        modifier = Modifier.size(42.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                    ) {
                        BoxCenter {
                            Text("V", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Early access", fontWeight = FontWeight.SemiBold)
                        Text(
                            "The workspace is taking shape.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        item {
            Text("Quick actions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
        item {
            ActionCard(
                title = "Explore the Vyra Store",
                description = "Browse patch collections and release information.",
                action = "Open store",
                onClick = onOpenStore,
            )
        }
        item {
            ActionCard(
                title = "Start a patching session",
                description = "Choose an APK you are authorized to modify.",
                action = "Open patcher",
                onClick = onOpenPatcher,
            )
        }
        item {
            ActionCard(
                title = "Create a patch collection",
                description = "Prepare your own patch pack and metadata.",
                action = "Open creator",
                onClick = onOpenCreator,
            )
        }
        item {
            ActionCard(
                title = "Manage patch sources",
                description = "Review repositories and their provenance.",
                action = "View sources",
                onClick = onOpenSources,
            )
        }
        item {
            Text(
                text = "No builds yet",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StoreScreen(padding: PaddingValues) {
    PageColumn(padding) {
        PageHeading("Vyra Store", "A home for community patch collections.")
        InfoCard(
            title = "Patch catalog",
            body = "Patch collections will appear here with supported apps, versions, changelogs, and requirements.",
        )
        InfoCard(
            title = "Compatibility first",
            body = "Each patch will show its target app and supported versions before you start a build.",
        )
        InfoCard(
            title = "Sources you control",
            body = "Only add repositories you trust. External sources will always require your confirmation.",
        )
    }
}

@Composable
private fun PatcherScreen(
    padding: PaddingValues,
    selectedApk: Uri?,
    onChooseApk: () -> Unit,
) {
    PageColumn(padding) {
        PageHeading("APK Patcher", "Prepare an app package for a guided patching workflow.")
        InfoCard(
            title = "1. Choose your APK",
            body = selectedApk?.lastPathSegment?.substringAfterLast('/') ?: "Select an APK from your device storage. The file stays on your device.",
        )
        Button(
            onClick = onChooseApk,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
        ) {
            Text(if (selectedApk == null) "Choose APK file" else "Choose another APK")
        }
        InfoCard(
            title = "2. Inspect and match",
            body = "Package details, signing certificate, and compatible patches will be checked in a later milestone.",
        )
        InfoCard(
            title = "3. Build and export",
            body = "The patch engine is not connected yet. No APK is modified or generated in this preview.",
        )
    }
}

@Composable
private fun CreatorScreen(padding: PaddingValues) {
    PageColumn(padding) {
        PageHeading("Patch Creator", "Design and validate your own patch collection.")
        InfoCard(
            title = "Manifest workspace",
            body = "Create a collection name, version, description, target apps, and compatibility rules.",
        )
        InfoCard(
            title = "Validation",
            body = "Manifest checks and patch packaging will be added before the creator can export a release.",
        )
        InfoCard(
            title = "No code execution yet",
            body = "This screen is a product shell. It does not compile or execute patch code.",
        )
    }
}

@Composable
private fun SourcesScreen(padding: PaddingValues) {
    PageColumn(padding) {
        PageHeading("Repositories", "Keep your patch sources organized and transparent.")
        InfoCard(
            title = "No sources added",
            body = "Add a repository when the source format and trust checks are ready. New sources will never run silently.",
        )
    }
}

@Composable
private fun BuildsScreen(padding: PaddingValues) {
    PageColumn(padding) {
        PageHeading("Build center", "Follow every build from start to exported artifact.")
        InfoCard(
            title = "No build history",
            body = "Your completed builds, validation results, and logs will be listed here.",
        )
    }
}

@Composable
private fun PageColumn(
    padding: PaddingValues,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(
            start = 20.dp,
            end = 20.dp,
            top = padding.calculateTopPadding() + 20.dp,
            bottom = padding.calculateBottomPadding() + 20.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        content()
    }
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
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ActionCard(
    title: String,
    description: String,
    action: String,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp),
    ) {
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
    androidx.compose.foundation.layout.Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
        content = { content() },
    )
}
