package com.dettle.app.orchestrator.tools

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "WebBrowserTool"

/**
 * Gives the AI direct access to the live internet.
 * This is exposed to the LLM via Function Calling (Tools).
 * When the AI encounters a topic it doesn't know, it calls these functions automatically.
 */
@Singleton
class WebBrowserTool @Inject constructor() {

    /**
     * Executes a live web search using DuckDuckGo's HTML-only version 
     * (Requires no API keys and is extremely lightweight for the 250MB RAM limit).
     */
    suspend fun searchWeb(query: String): String = withContext(Dispatchers.IO) {
        Log.d(TAG, "AI initiated web search for: $query")
        try {
            val urlString = "https://html.duckduckgo.com/html/?q=${java.net.URLEncoder.encode(query, "UTF-8")}"
            val connection = URL(urlString).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            
            val html = connection.inputStream.bufferedReader().use { it.readText() }
            
            // Extract the snippet results (a crude but effective regex for DDG HTML)
            val snippetRegex = Regex("<a class=\"result__snippet[^>]*>(.*?)</a>", RegexOption.DOT_MATCHES_ALL)
            val urlRegex = Regex("<a class=\"result__url\" href=\"(.*?)\"", RegexOption.DOT_MATCHES_ALL)
            
            val snippets = snippetRegex.findAll(html).map { it.groupValues[1] }.toList()
            val urls = urlRegex.findAll(html).map { it.groupValues[1] }.toList()

            buildString {
                appendLine("Search Results for '$query':")
                for (i in 0 until minOf(5, snippets.size)) {
                    val cleanSnippet = snippets[i].replace(Regex("<[^>]*>"), "").trim()
                    appendLine("${i+1}. $cleanSnippet")
                    if (i < urls.size) appendLine("   URL: ${urls[i]}")
                    appendLine()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Web search failed", e)
            "Error performing search: ${e.message}"
        }
    }

    /**
     * Directly fetches and reads the content of a specific URL.
     * Strips HTML tags to provide clean, token-efficient text back to the AI.
     */
    suspend fun readUrl(urlString: String): String = withContext(Dispatchers.IO) {
        Log.d(TAG, "AI reading URL: $urlString")
        try {
            val connection = URL(urlString).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "Mozilla/5.0")
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            val html = connection.inputStream.bufferedReader().use { it.readText() }
            
            // Basic HTML to Markdown/Text extraction to save LLM context window limits
            var text = html
            // Remove scripts and styles completely
            text = text.replace(Regex("<script\\b[^<]*(?:(?!</script>)<[^<]*)*</script>", RegexOption.IGNORE_CASE), "")
            text = text.replace(Regex("<style\\b[^<]*(?:(?!</style>)<[^<]*)*</style>", RegexOption.IGNORE_CASE), "")
            // Strip remaining HTML tags
            text = text.replace(Regex("<[^>]*>"), " ")
            // Compress whitespace
            text = text.replace(Regex("\\s+"), " ").trim()

            // Return the first 10,000 characters to prevent crashing the memory limit
            text.take(10000) 
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read URL", e)
            "Error reading URL: ${e.message}"
        }
    }
}
