package app.areada.data.preview

import android.content.Context
import android.net.Uri
import app.areada.data.reader.DocumentType
import app.areada.data.reader.ReadingProgress
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.URLDecoder
import java.util.zip.ZipInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.parser.Parser

/**
 * Plain-text snippet around the saved reading position, used for the two pages
 * of the open-book hero card on Reading Now.
 */
object BookPreviewText {
    private const val MAX_CHARS = 1400
    private val cache = mutableMapOf<String, String>()

    suspend fun load(
        context: Context,
        uriString: String,
        type: DocumentType,
        progress: ReadingProgress?,
    ): String? {
        val key = "$uriString#${progress?.epubChapterIndex ?: 0}#${progress?.txtScrollFraction ?: 0f}"
        synchronized(cache) { cache[key] }?.let { return it }

        val text = withContext(Dispatchers.IO) {
            runCatching {
                val appContext = context.applicationContext
                val uri = Uri.parse(uriString)
                when (type) {
                    DocumentType.EPUB -> epubText(appContext, uri, progress?.epubChapterIndex ?: 0)
                    DocumentType.TXT, DocumentType.MARKDOWN -> plainText(
                        appContext,
                        uri,
                        progress?.txtScrollFraction ?: 0f,
                    )

                    else -> null
                }
            }.getOrNull()
        }?.trim()?.takeIf { it.isNotEmpty() }

        if (text != null) {
            synchronized(cache) { cache[key] = text }
        }
        return text
    }

    private fun plainText(context: Context, uri: Uri, fraction: Float): String? {
        val raw = context.contentResolver.openInputStream(uri)?.use { stream ->
            stream.bufferedReader().readText()
        } ?: return null
        if (raw.isBlank()) return null

        val start = (raw.length * fraction.coerceIn(0f, 0.97f)).toInt()
        return raw.substring(start).take(MAX_CHARS)
    }

    private fun epubText(context: Context, uri: Uri, chapterIndex: Int): String? {
        val containerXml = readEntry(context, uri) { it == "META-INF/container.xml" }
            ?.toString(Charsets.UTF_8) ?: return null
        val opfPath = Jsoup.parse(containerXml, "", Parser.xmlParser())
            .select("rootfile").firstOrNull()?.attr("full-path")
            ?.takeIf { it.isNotBlank() } ?: return null

        val opfXml = readEntry(context, uri) { it.equals(opfPath, ignoreCase = true) }
            ?.toString(Charsets.UTF_8) ?: return null
        val opf = Jsoup.parse(opfXml, "", Parser.xmlParser())
        val directory = opfPath.substringBeforeLast('/', "")

        val hrefById = opf.select("manifest > item").associate { item ->
            item.attr("id") to item.attr("href")
        }
        val spine = opf.select("spine > itemref").mapNotNull { ref ->
            hrefById[ref.attr("idref")]?.takeIf { it.isNotBlank() }
        }
        if (spine.isEmpty()) return null

        val href = spine.getOrNull(chapterIndex.coerceIn(0, spine.lastIndex)) ?: return null
        val path = resolve(directory, href) ?: return null

        val bytes = readEntry(context, uri) { name -> name.equals(path, ignoreCase = true) }
            ?: return null

        return Jsoup.parse(bytes.toString(Charsets.UTF_8))
            .text()
            .replace(Regex("\\s+"), " ")
            .take(MAX_CHARS)
    }

    private fun resolve(directory: String, rawHref: String): String? {
        val href = runCatching { URLDecoder.decode(rawHref, "UTF-8") }.getOrDefault(rawHref)
            .substringBefore('#')
            .trim()
        if (href.isEmpty()) return null

        val combined = if (directory.isEmpty()) href else "$directory/$href"
        val segments = mutableListOf<String>()
        combined.split('/').forEach { segment ->
            when (segment) {
                "", "." -> Unit
                ".." -> if (segments.isNotEmpty()) segments.removeAt(segments.lastIndex)
                else -> segments.add(segment)
            }
        }
        return segments.joinToString("/").takeIf { it.isNotEmpty() }
    }

    private fun readEntry(
        context: Context,
        uri: Uri,
        match: (String) -> Boolean,
    ): ByteArray? {
        val stream: InputStream = context.contentResolver.openInputStream(uri) ?: return null
        stream.use { input ->
            ZipInputStream(input.buffered()).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: return null
                    if (entry.isDirectory || !match(entry.name)) {
                        zip.closeEntry()
                        continue
                    }
                    val out = ByteArrayOutputStream()
                    val buffer = ByteArray(16 * 1024)
                    while (true) {
                        val read = zip.read(buffer)
                        if (read <= 0) break
                        out.write(buffer, 0, read)
                    }
                    return out.toByteArray()
                }
            }
        }
        @Suppress("UNREACHABLE_CODE")
        return null
    }
}
