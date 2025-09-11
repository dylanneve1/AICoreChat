package org.dylanneve1.aicorechat.data.search

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * WebSearchService encapsulates connectivity checks and retrieval of high-signal
 * search context (titles, snippets, dates) suitable for LLM prompts.
 */
class WebSearchService(private val app: Application) {
    fun isOnline(): Boolean {
        return try {
            val cm = app.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                 caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                 caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))
        } catch (e: Exception) { false }
    }

    private fun htmlDecode(input: String): String {
        var s = input
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&nbsp;", " ")
        val dec = Regex("&#(\\d+);")
        s = dec.replace(s) { m ->
            val code = m.groupValues[1].toIntOrNull() ?: return@replace m.value
            code.toChar().toString()
        }
        val hex = Regex("&#x([0-9a-fA-F]+);")
        s = hex.replace(s) { m ->
            val code = m.groupValues[1].toIntOrNull(16) ?: return@replace m.value
            code.toChar().toString()
        }
        return s
    }

    private fun decodeDuckRedirect(href: String): String {
        return try {
            val h = if (href.startsWith("//")) "https:$href" else href
            if (h.startsWith("http")) {
                val idx = h.indexOf("uddg=")
                if (idx != -1) {
                    val start = idx + 5
                    val end = h.indexOf('&', start).let { if (it == -1) h.length else it }
                    val enc = h.substring(start, end)
                    URLDecoder.decode(enc, "UTF-8")
                } else h
            } else h
        } catch (e: Exception) { href }
    }

    private fun hostname(url: String): String {
        return try {
            val u = URL(if (url.startsWith("http")) url else "https://$url")
            u.host.removePrefix("www.")
        } catch (e: Exception) { url }
    }

    private fun parseDateToIso(dateRaw: String): String? {
        val candidates = listOf(
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "EEE, dd MMM yyyy HH:mm:ss zzz",
            "EEE, d MMM yyyy HH:mm:ss zzz",
            "EEE, d MMM yyyy HH:mm zzz",
            "yyyy-MM-dd",
            "dd MMM yyyy",
            "d MMM yyyy",
            "MMM d, yyyy",
            "MMMM d, yyyy"
        )
        for (p in candidates) {
            try {
                val fmt = SimpleDateFormat(p, Locale.US)
                if (p.contains("'Z'")) fmt.timeZone = TimeZone.getTimeZone("UTC")
                val d = fmt.parse(dateRaw)
                if (d != null) {
                    val out = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    out.timeZone = TimeZone.getTimeZone("UTC")
                    return out.format(d)
                }
            } catch (_: Exception) { }
        }
        return null
    }

    private suspend fun fetchMeta(targetUrl: String): Triple<String?, String?, String?> = withContext(Dispatchers.IO) {
        return@withContext try {
            val u = URL(targetUrl)
            val c = (u.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("User-Agent", "Mozilla/5.0")
                connectTimeout = 4000
                readTimeout = 4000
            }
            val sb = StringBuilder()
            c.inputStream.buffered().use { input ->
                val buf = ByteArray(8192)
                var total = 0
                while (true) {
                    val n = input.read(buf)
                    if (n <= 0) break
                    sb.append(String(buf, 0, n))
                    total += n
                    if (total > 48_000) break
                }
            }
            val html = sb.toString()
            val title = Regex("<title>(.*?)</title>", RegexOption.IGNORE_CASE).find(html)?.groupValues?.get(1)
            val metaDesc = Regex("""<meta[^>]*name="description"[^>]*content="(.*?)"[^>]*>""", RegexOption.IGNORE_CASE).find(html)?.groupValues?.get(1)
                ?: Regex("""<meta[^>]*property="og:description"[^>]*content="(.*?)"[^>]*>""", RegexOption.IGNORE_CASE).find(html)?.groupValues?.get(1)
            val metaDate = Regex("""<meta[^>]*property="article:published_time"[^>]*content="(.*?)"[^>]*>""", RegexOption.IGNORE_CASE).find(html)?.groupValues?.get(1)
                ?: Regex("""<meta[^>]*name="pubdate"[^>]*content="(.*?)"[^>]*>""", RegexOption.IGNORE_CASE).find(html)?.groupValues?.get(1)
                ?: Regex("""<meta[^>]*name="date"[^>]*content="(.*?)"[^>]*>""", RegexOption.IGNORE_CASE).find(html)?.groupValues?.get(1)
                ?: Regex("""<meta[^>]*itemprop="datePublished"[^>]*content="(.*?)"[^>]*>""", RegexOption.IGNORE_CASE).find(html)?.groupValues?.get(1)
                ?: Regex("""<meta[^>]*property="og:updated_time"[^>]*content="(.*?)"[^>]*>""", RegexOption.IGNORE_CASE).find(html)?.groupValues?.get(1)
            Triple(
                title?.let { htmlDecode(it.replace(Regex("<.*?>"), "").trim()) },
                metaDesc?.let { htmlDecode(it.trim()) },
                metaDate?.let { parseDateToIso(it.trim()) }
            )
        } catch (e: Exception) { Triple(null, null, null) }
    }

    suspend fun search(query: String): String = withContext(Dispatchers.IO) {
        try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = URL("https://duckduckgo.com/html/?q=$encoded&kl=us-en")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("User-Agent", "Mozilla/5.0")
                connectTimeout = 8000
                readTimeout = 8000
            }
            conn.inputStream.bufferedReader().use { reader ->
                val html = reader.readText()
                val itemRegex = Regex("""<div class="result.*?</a>.*?</div>""", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
                val titleRegex = Regex("""<a[^>]*class="result__a[^"]*"[^>]*>(.*?)</a>""", RegexOption.IGNORE_CASE)
                val hrefRegex = Regex("""<a[^>]*class="result__a[^"]*"[^>]*href="(.*?)"""", RegexOption.IGNORE_CASE)
                val snippetRegex = Regex("""(?:class="result__snippet[^"]*"[^>]*>(.*?)</(?:a|span|div)>)""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
                val ddgTimeRegex = Regex("""class="result__timestamp"[^>]*>(.*?)<""", RegexOption.IGNORE_CASE)
                val items = itemRegex.findAll(html).take(5).map { it.value }.toList()
                if (items.isEmpty()) return@use "No results"
                val lines = mutableListOf<String>()
                for ((idx, block) in items.withIndex()) {
                    val rawTitle = titleRegex.find(block)?.groupValues?.get(1).orEmpty()
                    val rawHref = hrefRegex.find(block)?.groupValues?.get(1).orEmpty()
                    val rawSnippet = snippetRegex.find(block)?.groupValues?.get(1).orEmpty()
                    val ddgTime = ddgTimeRegex.find(block)?.groupValues?.get(1)?.let { htmlDecode(it).trim() }.orEmpty()
                    val realUrl = decodeDuckRedirect(htmlDecode(rawHref))
                    val host = hostname(realUrl)
                    var title = htmlDecode(rawTitle.replace(Regex("<.*?>"), "").trim())
                    var snippet = htmlDecode(rawSnippet.replace(Regex("<.*?>"), " ").replace("\n", " ").replace(" +".toRegex(), " ").trim())
                    var dateIso: String? = null
                    val (t, d, metaDate) = fetchMeta(realUrl)
                    if (!d.isNullOrBlank() && (snippet.isBlank() || snippet.length < 48)) snippet = d
                    if (!t.isNullOrBlank() && title.isBlank()) title = t
                    if (!metaDate.isNullOrBlank()) dateIso = metaDate else {
                        if (!ddgTime.contains("ago", ignoreCase = true)) parseDateToIso(ddgTime)?.let { dateIso = it }
                    }
                    if (title.isBlank() && snippet.isBlank()) continue
                    val header = if (!dateIso.isNullOrBlank()) "${dateIso} — ${host} — ${title}" else "${host} — ${title}"
                    lines.add("${idx + 1}. ${header}\n- ${snippet}")
                }
                if (lines.isEmpty()) "No results" else lines.joinToString("\n\n")
            }
        } catch (e: Exception) {
            "No results (error: ${e.message})"
        }
    }
} 