package com.example.releasetracker.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "repos")

class RepositoryStore(private val context: Context) {
    companion object {
        private val REPOS_KEY = stringPreferencesKey("repos")
        private val LANG_KEY = stringPreferencesKey("lang")
    }

    val repositories: Flow<List<Repository>> = context.dataStore.data.map { prefs ->
        val raw = prefs[REPOS_KEY] ?: ""
        raw.split("\n").filter { it.isNotBlank() }.mapNotNull { line ->
            val parts = line.split("|")
            if (parts.size >= 2) {
                Repository(
                    id = "${parts[0]}/${parts[1]}",
                    owner = parts[0],
                    name = parts[1],
                    lastSeenRelease = parts.getOrNull(2)
                )
            } else null
        }
    }

    val language: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[LANG_KEY] ?: "ru"
    }

    suspend fun addRepository(owner: String, name: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[REPOS_KEY] ?: ""
            val newEntry = "$owner|$name|"
            if (!current.contains("$owner|$name")) {
                prefs[REPOS_KEY] = if (current.isBlank()) newEntry else "$current\n$newEntry"
            }
        }
    }

    suspend fun removeRepository(id: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[REPOS_KEY] ?: ""
            val updated = current.split("\n")
                .filter { !it.startsWith(id.replace("/", "|")) }
                .joinToString("\n")
            prefs[REPOS_KEY] = updated
        }
    }

    suspend fun updateLastSeen(id: String, version: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[REPOS_KEY] ?: ""
            val updated = current.split("\n").map { line ->
                if (line.startsWith(id.replace("/", "|"))) {
                    val parts = line.split("|")
                    "${parts[0]}|${parts[1]}|$version"
                } else line
            }.joinToString("\n")
            prefs[REPOS_KEY] = updated
        }
    }

    suspend fun setLanguage(lang: String) {
        context.dataStore.edit { prefs ->
            prefs[LANG_KEY] = lang
        }
    }
}
