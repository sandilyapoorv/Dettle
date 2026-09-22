package com.dettle.app.orchestrator

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Simple web search client using DuckDuckGo HTML scraping (no API key needed).
 * For production, swap with Brave Search API (free tier: 2,000 queries/month) or SerpAPI.
 */
@Singleton
class WebSearchClient @Inject constructor(
    private val client: OkHttpClient
) {
    suspend fun search(query: String, numResults: Int = 5): String = withContext(Dispatchers.IO) {
        try {
            val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
            val url = "https://html.duckduckgo.com/html/?q=$encodedQuery"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36")
                .build()

            val response = client.newCall(request).execute()
            val html = response.body?.string() ?: return@withContext "No search results"

            // Extract result snippets from DuckDuckGo HTML
            val results = Regex("""<a class="result__a"[^>]*href="([^"]*)"[^>]*>(.*?)</a>.*?<a class="result__snippet"[^>]*>(.*?)</a>""",
                setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
            ).findAll(html)
                .take(numResults)
                .mapIndexed { i, match ->
                    val title = match.groupValues[2].replace(Regex("<[^>]+>"), "").trim()
                    val snippet = match.groupValues[3].replace(Regex("<[^>]+>"), "").trim()
                    val link = match.groupValues[1]
                    "${i + 1}. **$title**\n   $snippet\n   URL: $link"
                }
                .joinToString("\n\n")

            if (results.isBlank()) "No results found for: $query"
            else "Search results for \"$query\":\n\n$results"

        } catch (e: Exception) {
            "Search failed: ${e.message}"
        }
    }
}
