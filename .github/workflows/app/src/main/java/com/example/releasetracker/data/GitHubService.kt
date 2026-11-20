package com.example.releasetracker.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

object GitHubService {
    suspend fun fetchReleases(owner: String, repo: String): List<Release> = withContext(Dispatchers.IO) {
        val url = URL("https://github.com/$owner/$repo/releases.atom")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty("User-Agent", "ReleaseTracker")
        connection.connectTimeout = 10000
        connection.readTimeout = 10000

        try {
            val inputStream = connection.inputStream
            val xml = inputStream.bufferedReader().use { it.readText() }
            ReleaseParser.parseAtom(xml)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        } finally {
            connection.disconnect()
        }
    }
}
