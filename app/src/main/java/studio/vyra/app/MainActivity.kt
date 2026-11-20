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
    var selectedTab by remember { mutableStateOf(VyraTab.STORE) }
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
                            text = "VYRA STORE",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp,
                        )
                        Text(
                            text = "APP CATALOG & UPDATE HUB",
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
