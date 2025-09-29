package org.dylanneve1.aicorechat.data.search

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * WebSearchService encapsulates connectivity checks and retrieval of high-signal
 * search context (titles, snippets, dates, and scraped page content) suitable for LLM prompts.
 */
class WebSearchService(private val app: Application) {
    fun isOnline(): Boolean {
        return try {
            val cm = app.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                (
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                        caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                    )
        } catch (e: Exception) {
            false
        }
    }

    suspend fun search(query: String): String = withContext(Dispatchers.IO) {
        runCatching {
            val html = fetchSearchHtml(query)
            val blocks = parseSearchBlocks(html)
            if (blocks.isEmpty()) return@runCatching "No results"

            val entries = coroutineScope {
                blocks.mapIndexed { index, block ->
                    async { processSearchBlock(index, block) }
                }.awaitAll()
            }.filterNotNull()

            if (entries.isEmpty()) "No results" else entries.joinToString("\n\n")
        }.getOrElse { error ->
            if (error is CancellationException) throw error
            "No results (error: ${error.message})"
        }
    }

    private fun fetchSearchHtml(query: String): String {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val url = URL("https://duckduckgo.com/html/?q=$encoded&kl=us-en")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("User-Agent", USER_AGENT)
            connectTimeout = SEARCH_TIMEOUT_MS
            readTimeout = SEARCH_TIMEOUT_MS
        }
        return try {
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private fun parseSearchBlocks(html: String): List<String> =
        RESULT_BLOCK_REGEX.findAll(html).take(SEARCH_RESULT_LIMIT).map { it.value }.toList()

    private suspend fun processSearchBlock(index: Int, block: String): String? {
        val fields = extractResultFields(block)
        val realUrl = decodeDuckRedirect(fields.href)
        if (!realUrl.startsWith("http")) return null

        val host = hostname(realUrl)
        var title = fields.title
        var summary = fields.summary
        var dateIso = fields.timestamp
            .takeIf { it.isNotBlank() && !it.contains("ago", ignoreCase = true) }
            ?.let { parseDateToIso(it) }

        val snapshot = scrapePage(realUrl)
        if (snapshot != null) {
            if (title.isBlank() && !snapshot.title.isNullOrBlank()) {
                title = snapshot.title
            }
            val snapshotSummary = snapshot.summary
                ?.let { collapseWhitespace(it) }
                ?.takeIf { it.isNotBlank() }
                ?.let(::clampSummary)
            if ((summary.isBlank() || summary.length < MIN_SNIPPET_LENGTH) && snapshotSummary != null) {
                summary = snapshotSummary
            }
            if (dateIso.isNullOrBlank() && !snapshot.published.isNullOrBlank()) {
                dateIso = snapshot.published
            }
        }

        val content = snapshot?.content
        if (title.isBlank() && summary.isBlank() && content.isNullOrBlank()) return null

        val header = buildHeader(host, title, dateIso)

        return buildString {
            append("${index + 1}. ")
            append(header)
            if (summary.isNotBlank()) {
                append("\n- Summary: ")
                append(summary)
            }
            if (!content.isNullOrBlank()) {
                append("\n- Content:\n")
                append(content)
            }
            append("\n- URL: ")
            append(realUrl)
        }.trimEnd()
    }

    private fun extractResultFields(block: String): ResultFields {
        val title = extractTitle(block)
        val href = htmlDecode(RESULT_LINK_REGEX.find(block)?.groupValues?.get(1).orEmpty())
        val summary = extractSnippet(block)
        val timestamp = extractTimestamp(block)
        return ResultFields(title, href, summary, timestamp)
    }

    private fun extractTitle(block: String): String {
        val rawTitle = RESULT_TITLE_REGEX.find(block)?.groupValues?.get(1).orEmpty()
        if (rawTitle.isBlank()) return ""
        return htmlDecode(rawTitle.replace(TAG_REGEX, "").trim())
    }

    private fun extractSnippet(block: String): String {
        val rawSnippet = RESULT_SNIPPET_REGEX.find(block)?.groupValues?.get(1).orEmpty()
        if (rawSnippet.isBlank()) return ""
        val sanitized = rawSnippet
            .replace(TAG_REGEX, " ")
            .replace("\n", " ")
            .replace(MULTISPACE_REGEX, " ")
            .trim()
        if (sanitized.isBlank()) return ""
        val decoded = htmlDecode(sanitized)
        return clampSummary(decoded)
    }

    private fun extractTimestamp(block: String): String {
        val rawTimestamp = RESULT_TIMESTAMP_REGEX.find(block)?.groupValues?.get(1).orEmpty()
        if (rawTimestamp.isBlank()) return ""
        return htmlDecode(rawTimestamp).trim()
    }

    private fun buildHeader(host: String, title: String, dateIso: String?): String {
        return buildString {
            if (!dateIso.isNullOrBlank()) {
                append(dateIso)
                append(" — ")
            }
            append(host)
            if (title.isNotBlank()) {
                append(" — ")
                append(title)
            }
        }.ifBlank { host }
    }

    private fun htmlDecode(input: String): String {
        var s = input
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&nbsp;", " ")
        val dec = Regex("""&#(\d+);""")
        s = dec.replace(s) { m ->
            val code = m.groupValues[1].toIntOrNull() ?: return@replace m.value
            code.toChar().toString()
        }
        val hex = Regex("""&#x([0-9a-fA-F]+);""")
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
                } else {
                    h
                }
            } else {
                h
            }
        } catch (e: Exception) {
            href
        }
    }

    private fun hostname(url: String): String {
        return try {
            val u = URL(if (url.startsWith("http")) url else "https://$url")
            u.host.removePrefix("www.")
        } catch (e: Exception) {
            url
        }
    }

    private fun parseDateToIso(dateRaw: String): String? {
        val candidates = listOf(
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "EEE, dd MMM yyyy HH:mm:ss zzz",
            "EEE, d MMM yyyy HH:mm:ss zzz",
            "EEE, d MMM yyyy HH:mm zzz",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd",
            "dd MMM yyyy",
            "d MMM yyyy",
            "MMM d, yyyy",
            "MMMM d, yyyy",
            "yyyy/MM/dd",
            "MM/dd/yyyy",
        )
        for (p in candidates) {
            try {
                val fmt = SimpleDateFormat(p, Locale.US)
                fmt.isLenient = true
                if (p.contains("'Z'")) fmt.timeZone = TimeZone.getTimeZone("UTC")
                val d = fmt.parse(dateRaw)
                if (d != null) {
                    val out = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    out.timeZone = TimeZone.getTimeZone("UTC")
                    return out.format(d)
                }
            } catch (_: Exception) {
            }
        }
        return null
    }

    private fun scrapePage(targetUrl: String): PageSnapshot? {
        val document = fetchDocument(targetUrl) ?: return null
        val title = document.title().trim().takeIf { it.isNotEmpty() }
        val summary = sequenceOf(
            document.selectFirst("meta[name=description]")?.attr("content"),
            document.selectFirst("meta[property=og:description]")?.attr("content"),
            document.selectFirst("meta[name=twitter:description]")?.attr("content"),
        ).mapNotNull { it?.trim()?.takeIf { value -> value.isNotEmpty() } }
            .firstOrNull()
        val published = extractPublishedDate(document)
        val content = extractReadableContent(document)?.let { clampContent(it) }
        return if (title == null && summary == null && published == null && content == null) {
            null
        } else {
            PageSnapshot(title, summary, published, content)
        }
    }

    private fun fetchDocument(targetUrl: String): Document? {
        return try {
            Jsoup.connect(targetUrl)
                .userAgent(USER_AGENT)
                .timeout(PAGE_TIMEOUT_MS)
                .followRedirects(true)
                .ignoreContentType(true)
                .maxBodySize(0)
                .get()
        } catch (e: Exception) {
            null
        }
    }

    private fun extractPublishedDate(doc: Document): String? {
        val candidates = mutableListOf<String>()
        val selectors = listOf(
            "meta[property=article:published_time]",
            "meta[name=pubdate]",
            "meta[name=date]",
            "meta[itemprop=datePublished]",
            "meta[property=og:updated_time]",
            "meta[name=dc.date]",
            "meta[name=dc.date.issued]",
            "meta[name=DC.date.issued]",
            "meta[name=OriginalPublicationDate]",
        )
        for (selector in selectors) {
            doc.select(selector).forEach { element ->
                val value = element.attr("content").trim()
                if (value.isNotEmpty()) candidates.add(value)
            }
        }
        doc.select("time[datetime]").forEach { element ->
            val value = element.attr("datetime").trim()
            if (value.isNotEmpty()) candidates.add(value)
        }
        doc.select("time").forEach { element ->
            val value = element.text().trim()
            if (value.isNotEmpty()) candidates.add(value)
        }
        return candidates.asSequence()
            .mapNotNull { parseDateToIso(it) }
            .firstOrNull()
    }

    private fun extractReadableContent(doc: Document): String? {
        val selectors = listOf(
            "article",
            "main",
            "[role=main]",
            "div[itemprop=articleBody]",
            "section[itemprop=articleBody]",
            "div[data-component=\"articleBody\"]",
            "div[data-testid=\"article-body\"]",
            "div[class*=article-body]",
            "section[class*=article-body]",
            "div[class*=post-content]",
            "#content",
        )
        for (selector in selectors) {
            val element = doc.selectFirst(selector) ?: continue
            val text = elementParagraphText(element)
            if (!text.isNullOrBlank() && text.length >= MIN_CONTENT_CHARS) {
                return text
            }
        }
        val paragraphs = doc.select("p")
            .mapNotNull { p ->
                val text = collapseWhitespace(p.text())
                if (text.length >= MIN_PARAGRAPH_CHARS) text else null
            }
            .take(MAX_PARAGRAPHS)
        return sanitizeParagraphs(paragraphs)
    }

    private fun elementParagraphText(element: Element): String? {
        val paragraphs = element.select("p")
            .mapNotNull { p ->
                val text = collapseWhitespace(p.text())
                if (text.length >= MIN_PARAGRAPH_CHARS) text else null
            }
            .take(MAX_PARAGRAPHS)
        val joined = sanitizeParagraphs(paragraphs)
        if (!joined.isNullOrBlank()) return joined
        val text = collapseWhitespace(element.text())
        return if (text.length >= MIN_CONTENT_CHARS) text else null
    }

    private fun sanitizeParagraphs(paragraphs: List<String>): String? {
        if (paragraphs.isEmpty()) return null
        val cleaned = paragraphs.map { it.trim() }.filter { it.isNotEmpty() }
        if (cleaned.isEmpty()) return null
        return cleaned.joinToString("\n\n")
    }

    private fun clampSummary(text: String, maxChars: Int = SUMMARY_CHAR_LIMIT): String {
        if (text.length <= maxChars) return text
        val cut = text.substring(0, maxChars)
        val split = cut.lastIndexOf(' ')
        val candidate = if (split >= maxChars / 2) cut.substring(0, split) else cut
        val normalized = candidate.trimEnd()
        val base = if (normalized.isNotEmpty()) normalized else cut.trimEnd()
        return if (base.endsWith("...")) base else "$base..."
    }

    private fun clampContent(text: String, maxChars: Int = PAGE_CONTENT_LIMIT): String {
        if (text.length <= maxChars) return text
        val cut = text.substring(0, maxChars)
        val candidate = sequenceOf(
            cut.lastIndexOf(". "),
            cut.lastIndexOf('\n'),
        ).filter { it >= maxChars / 2 }.maxOrNull() ?: cut.length
        val safe = cut.substring(0, candidate).trimEnd()
        val fallback = if (safe.isNotEmpty()) safe else cut.trimEnd()
        return "$fallback..."
    }

    private fun collapseWhitespace(value: String): String {
        return MULTISPACE_REGEX.replace(value.replace('\u00A0', ' '), " ").trim()
    }

    private data class ResultFields(
        val title: String,
        val href: String,
        val summary: String,
        val timestamp: String,
    )

    private data class PageSnapshot(
        val title: String?,
        val summary: String?,
        val published: String?,
        val content: String?,
    )

    private companion object {
        private const val SEARCH_RESULT_LIMIT = 5
        private const val SEARCH_TIMEOUT_MS = 8000
        private const val PAGE_TIMEOUT_MS = 12_000
        private const val PAGE_CONTENT_LIMIT = 2400
        private const val MIN_PARAGRAPH_CHARS = 40
        private const val MIN_CONTENT_CHARS = 180
        private const val MAX_PARAGRAPHS = 24
        private const val MIN_SNIPPET_LENGTH = 80
        private const val SUMMARY_CHAR_LIMIT = 320
        private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Mobile Safari/537.36"
        private val RESULT_BLOCK_REGEX = Regex(
            """<div class="result.*?</a>.*?</div>""",
            setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE),
        )
        private val RESULT_TITLE_REGEX =
            Regex("""<a[^>]*class="result__a[^"]*"[^>]*>(.*?)</a>""", RegexOption.IGNORE_CASE)
        private val RESULT_LINK_REGEX =
            Regex("""<a[^>]*class="result__a[^"]*"[^>]*href="(.*?)""", RegexOption.IGNORE_CASE)
        private val RESULT_SNIPPET_REGEX = Regex(
            """(?:class="result__snippet[^"]*"[^>]*>(.*?)</(?:a|span|div)>)""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
        )
        private val RESULT_TIMESTAMP_REGEX = Regex("""class="result__timestamp"[^>]*>(.*?)<""", RegexOption.IGNORE_CASE)
        private val TAG_REGEX = Regex("<.*?>")
        private val MULTISPACE_REGEX = Regex("\\s+")
    }
}
