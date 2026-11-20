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

fun OverviewScreen(
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
            Text("Your apps,\nin one place.", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("Browse apps, discover VYRA builds, and manage updates from one place.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
