package com.example.releasetracker.data

data class Repository(
    val id: String,          // "owner/repo"
    val owner: String,
    val name: String,
    val lastSeenRelease: String? = null
)

data class Release(
    val version: String,
    val title: String,
    val changelog: String,
    val publishedDate: String,
    val downloadUrl: String?,
    val pageUrl: String
)
