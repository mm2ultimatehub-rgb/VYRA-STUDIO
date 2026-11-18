/*
 * Native VYRA Store page. This screen belongs to the VYRA product layer;
 * catalog entries are loaded from the validated VYRA repository service.
 */
package app.morphe.manager.ui.screen.vyra

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.morphe.manager.domain.repository.VyraCatalogRepository
import org.koin.compose.koinInject

@Composable
fun VyraStorePage(
    bottomBarHeight: Dp = 0.dp,
    onAppSelected: (String) -> Unit = {},
    repository: VyraCatalogRepository = koinInject()
) {
    var apps by remember { mutableStateOf<List<VyraCatalogRepository.StoreApp>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }

    LaunchedEffect(repository) {
        isLoading = true
        loadError = null
        runCatching { repository.listStoreApps() }
            .onSuccess { apps = it }
            .onFailure { loadError = it.message ?: "Katalog yüklenemedi." }
        isLoading = false
    }

    val filteredApps = apps.filter {
        it.name.contains(query, ignoreCase = true) ||
            it.packageName.contains(query, ignoreCase = true) ||
            it.category.contains(query, ignoreCase = true)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp, end = 20.dp, top = 24.dp,
            bottom = bottomBarHeight + 28.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "VYRA STUDIO  /  KEŞFET",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Uygulama mağazası",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Uygulamanı bul, destek durumunu incele ve yama merkezine geç.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(42.dp),
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            androidx.compose.foundation.layout.Box(
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "V",
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                        Column(
                            modifier = Modifier.padding(start = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text("VYRA DISCOVERY", fontWeight = FontWeight.Bold)
                            Text(
                                "YOUR APPS. YOUR RULES.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Text(
                        "Katalogda görünmek, uygulamanın her sürümünün yamalanabildiği anlamına gelmez. VYRA yalnızca doğrulanmış uyumluluğu kullanılabilir olarak sunar.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Uygulama veya kategori ara") },
                placeholder = { Text("YouTube, Reddit, sosyal medya…") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )
        }
        if (isLoading) {
            item {
                Text(
                    "Uygulama kataloğu yükleniyor…",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        loadError?.let { error ->
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text(
                        "Katalog şu anda kullanılamıyor: $error",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
        if (!isLoading && loadError == null) {
            item {
                Text(
                    "${filteredApps.size} UYGULAMA",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            items(filteredApps, key = { it.packageName }) { app ->
                val selectable = app.status != "unsupported"
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    onClick = { if (selectable) onAppSelected(app.packageName) }
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                modifier = Modifier.size(46.dp),
                                shape = RoundedCornerShape(15.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                androidx.compose.foundation.layout.Box(
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        app.name.take(1).uppercase(),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Column(
                                modifier = Modifier.weight(1f).padding(start = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(app.name, style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold)
                                Text(app.packageName, style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Text(
                            app.status.replace('-', ' ').uppercase(),
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Kategori: ${app.category}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            if (selectable) "Yama seçeneklerini aç" else "Henüz yama kaynağı bulunmuyor",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            if (filteredApps.isEmpty()) {
                item {
                    Text(
                        if (apps.isEmpty()) "Katalogda henüz uygulama yok."
                        else "Aramana uygun uygulama bulunamadı.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        item {
            Spacer(Modifier.padding(top = 2.dp))
            Text(
                "Bir uygulamayı seçtiğinde VYRA, tam APK sürümünü kontrol ederek yama akışını açar.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
