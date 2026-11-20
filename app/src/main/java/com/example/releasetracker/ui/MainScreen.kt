package com.example.releasetracker.ui

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import com.example.releasetracker.data.GitHubService
import com.example.releasetracker.data.Repository
import com.example.releasetracker.data.RepositoryStore
import com.example.releasetracker.data.Release
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember { RepositoryStore(context) }

    val repositories by store.repositories.collectAsState(initial = emptyList())
    val language by store.language.collectAsState(initial = "ru")
    val strings = remember(language) { Strings(language) }

    var showAddDialog by remember { mutableStateOf(false) }
    var checking by remember { mutableStateOf(false) }
    var expandedRepoId by remember { mutableStateOf<String?>(null) }
    var releasesCache by remember { mutableStateOf<Map<String, List<Release>>>(emptyMap()) }

    LaunchedEffect(Unit) {
        createNotificationChannel(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.appName) },
                actions = {
                    IconButton(onClick = {
                        scope.launch {
                            store.setLanguage(if (language == "ru") "en" else "ru")
                        }
                    }) {
                        Icon(Icons.Default.Language, contentDescription = strings.language)
                    }
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = strings.add)
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            Button(
                onClick = {
                    scope.launch {
                        checking = true
                        val newCache = mutableMapOf<String, List<Release>>()
                        repositories.forEach { repo ->
                            val releases = GitHubService.fetchReleases(repo.owner, repo.name)
                            if (releases.isNotEmpty()) {
                                newCache[repo.id] = releases
                                val latest = releases.first()
                                if (repo.lastSeenRelease != null && repo.lastSeenRelease != latest.version) {
                                    sendNotification(context, strings.newRelease, "${repo.id}: ${latest.version}")
                                }
                                store.updateLastSeen(repo.id, latest.version)
                            }
                        }
                        releasesCache = newCache
                        checking = false
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                enabled = !checking && repositories.isNotEmpty()
            ) {
                if (checking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(strings.checking)
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(strings.checkUpdates)
                }
            }

            if (repositories.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(strings.noRepos, style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(16.dp)) {
                    items(repositories) { repo ->
                        RepoCard(
                            repo = repo,
                            releases = releasesCache[repo.id] ?: emptyList(),
                            strings = strings,
                            expanded = expandedRepoId == repo.id,
                            onToggleExpand = {
                                expandedRepoId = if (expandedRepoId == repo.id) null else repo.id
                            },
                            onDelete = {
                                scope.launch { store.removeRepository(repo.id) }
                            },
                            onDownload = { url ->
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddRepoDialog(
            strings = strings,
            onDismiss = { showAddDialog = false },
            onAdd = { owner, name ->
                scope.launch {
                    store.addRepository(owner, name)
                    showAddDialog = false
                }
            }
        )
    }
}

@Composable
fun RepoCard(
    repo: Repository,
    releases: List<Release>,
    strings: Strings,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    onDelete: () -> Unit,
    onDownload: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onToggleExpand() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(repo.id, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (releases.isNotEmpty()) {
                        Text(
                            strings.latestVersion.format(releases.first().version),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = strings.delete)
                }
            }

            if (expanded && releases.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                releases.take(3).forEach { release ->
                    Text(release.title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                    if (release.changelog.isNotBlank()) {
                        Text(
                            release.changelog,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 4,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    if (release.downloadUrl != null) {
                        Button(
                            onClick = { onDownload(release.downloadUrl) },
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Text(strings.download)
                        }
                    } else {
                        TextButton(onClick = { onDownload(release.pageUrl) }) {
                            Text(strings.download)
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }
            }
        }
    }
}

@Composable
fun AddRepoDialog(strings: Strings, onDismiss: () -> Unit, onAdd: (String, String) -> Unit) {
    var input by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.addRepo) },
        text = {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                label = { Text(strings.repoHint) },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val parts = input.trim().split("/")
                    if (parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
                        onAdd(parts[0].trim(), parts[1].trim())
                    }
                }
            ) { Text(strings.add) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        }
    )
}

data class Strings(val lang: String) {
    val appName = if (lang == "ru") "Release Tracker" else "Release Tracker"
    val addRepo = if (lang == "ru") "Добавить репозиторий" else "Add Repository"
    val repoHint = if (lang == "ru") "владелец/репозиторий" else "owner/repo"
    val checkUpdates = if (lang == "ru") "Проверить обновления" else "Check for Updates"
    val download = if (lang == "ru") "Скачать" else "Download"
    val noRepos = if (lang == "ru") "Репозитории ещё не добавлены" else "No repositories added yet"
    val newRelease = if (lang == "ru") "Доступен новый релиз!" else "New release available!"
    val language = if (lang == "ru") "Язык" else "Language"
    val delete = if (lang == "ru") "Удалить" else "Delete"
    val cancel = if (lang == "ru") "Отмена" else "Cancel"
    val add = if (lang == "ru") "Добавить" else "Add"
    val checking = if (lang == "ru") "Проверка…" else "Checking…"
    val latestVersion = if (lang == "ru") "Последняя: %s" else "Latest: %s"
}

fun createNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            "release_updates",
            "Release Updates",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }
}

fun sendNotification(context: Context, title: String, message: String) {
    val notification = NotificationCompat.Builder(context, "release_updates")
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle(title)
        .setContentText(message)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .build()

    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    manager.notify(System.currentTimeMillis().toInt(), notification)
}
