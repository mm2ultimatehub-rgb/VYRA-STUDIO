package studio.vyra.app.repository

import android.content.Context
import org.json.JSONArray

class RepositorySourceStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun getUrls(): List<String> {
        val encoded = preferences.getString(KEY_URLS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(encoded)
            buildList {
                for (index in 0 until array.length()) {
                    val value = array.optString(index).trim()
                    if (value.isNotEmpty()) add(value)
                }
            }
        }.getOrDefault(emptyList())
    }

    fun addUrl(url: String) {
        val normalized = url.trim()
        require(normalized.isNotEmpty()) { "Repository URL cannot be empty." }
        val updated = (getUrls().filterNot { it == normalized } + normalized).takeLast(MAX_SOURCES)
        val array = JSONArray()
        updated.forEach(array::put)
        preferences.edit().putString(KEY_URLS, array.toString()).apply()
    }

    fun removeUrl(url: String) {
        val array = JSONArray()
        getUrls().filterNot { it == url }.forEach(array::put)
        preferences.edit().putString(KEY_URLS, array.toString()).apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "vyra_repository_sources"
        const val KEY_URLS = "source_urls"
        const val MAX_SOURCES = 30
    }
}
