/*
 * VYRA Store landing page embedded in the Morphe patching runtime.
 * Catalog labels are discovery hints, not claims of verified patch compatibility.
 */
package app.morphe.manager.ui.screen.vyra

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private data class VyraStoreApp(
    val name: String,
    val packageName: String,
    val status: String,
    val detail: String
)

private val storeApps = listOf(
    VyraStoreApp("YouTube", "com.google.android.youtube", "Candidate", "Patch metadata exists; exact APK and patch compatibility still need validation."),
    VyraStoreApp("YouTube Music", "com.google.android.apps.youtube.music", "Candidate", "Patch metadata exists; exact APK and patch compatibility still need validation."),
    VyraStoreApp("Reddit", "com.reddit.frontpage", "Candidate", "Patch metadata exists; exact APK and patch compatibility still need validation."),
    VyraStoreApp("Instagram", "com.instagram.android", "Experimental", "Coverage is experimental and may not include a compatible patch."),
    VyraStoreApp("TikTok", "com.zhiliaoapp.musically", "Experimental", "Coverage is experimental and may not include a compatible patch."),
    VyraStoreApp("Threads", "com.instagram.barcelona", "Experimental", "Coverage is experimental and may not include a compatible patch."),
    VyraStoreApp("X", "com.twitter.android", "Experimental", "Coverage is experimental and may not include a compatible patch.")
)

@Composable
fun VyraStorePage(bottomBarHeight: androidx.compose.ui.unit.Dp = 0.dp) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = bottomBarHeight + 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                Text("VYRA Store", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Browse app coverage and discover patch sources. A status is not a guarantee that a patch can be applied to every release.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
        items(storeApps, key = { it.packageName }) { app ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(app.name, style = MaterialTheme.typography.titleMedium)
                    Text(app.packageName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(app.status, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Text(app.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            Text(
                "To patch an installed app, return to Apps and select that app. The engine checks the installed version and available patch bundles before proceeding.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
    }
}
