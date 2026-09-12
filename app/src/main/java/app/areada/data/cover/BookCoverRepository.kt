package app.areada.data.cover

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.LruCache
import app.areada.data.reader.DocumentType
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.URLDecoder
import java.security.MessageDigest
import java.util.zip.ZipInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.parser.Parser

/**
 * Extracts and caches artwork for library documents.
 *
 * EPUB covers are read straight out of the container, PDFs fall back to a render
 * of the first page, and everything else resolves to null so the UI can draw a
 * generated typographic cover instead. Results are cached in memory and on disk
 * so the library grid stays fast and works fully offline.
 */
object BookCoverRepository {
    private const val MAX_EDGE = 640
    private const val CACHE_DIR = "book_covers"

    private val memory = LruCache<String, Bitmap>(40)
    private val misses = java.util.Collections.synchronizedSet(mutableSetOf<String>())

    fun cached(uriString: String): Bitmap? = memory.get(uriString)

    suspend fun load(
        context: Context,
        uriString: String,
        type: DocumentType,
    ): Bitmap? {
        memory.get(uriString)?.let { return it }
        if (misses.contains(uriString)) return null

        return withContext(Dispatchers.IO) {
            val appContext = context.applicationContext
            val diskFile = diskFile(appContext, uriString)

            val fromDisk = runCatching {
                if (diskFile.exists()) BitmapFactory.decodeFile(diskFile.absolutePath) else null
            }.getOrNull()

            if (fromDisk != null) {
                memory.put(uriString, fromDisk)
                return@withContext fromDisk
            }

            val bitmap = runCatching {
                when (type) {
                    DocumentType.EPUB -> epubCover(appContext, Uri.parse(uriString))
                    DocumentType.PDF -> pdfCover(appContext, Uri.parse(uriString))
                    else -> null
                }
            }.getOrNull()

            if (bitmap == null) {
                misses.add(uriString)
                return@withContext null
            }

            runCatching {
                diskFile.parentFile?.mkdirs()
                FileOutputStream(diskFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 88, out)
                }
            }

            memory.put(uriString, bitmap)
            bitmap
        }
    }

    private fun diskFile(context: Context, uriString: String): File {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(uriString.toByteArray())
            .joinToString("") { byte -> "%02x".format(byte) }
        return File(File(context.cacheDir, CACHE_DIR), "$digest.jpg")
    }

    // region EPUB

    private fun epubCover(context: Context, uri: Uri): Bitmap? {
        val containerXml = readZipEntry(context, uri) { name -> name == "META-INF/container.xml" }
            ?.toString(Charsets.UTF_8)
            ?: return null

        val opfPath = Jsoup.parse(containerXml, "", Parser.xmlParser())
            .select("rootfile")
            .firstOrNull()
            ?.attr("full-path")
            ?.takeIf { path -> path.isNotBlank() }
            ?: return null

        val opfXml = readZipEntry(context, uri) { name -> name.equals(opfPath, ignoreCase = true) }
            ?.toString(Charsets.UTF_8)
            ?: return null

        val opf = Jsoup.parse(opfXml, "", Parser.xmlParser())
        val opfDirectory = opfPath.substringBeforeLast('/', "")

        val href = coverHref(opf) ?: return null
        val archivePath = normalize(opfDirectory, href) ?: return null

        val bytes = readZipEntry(context, uri) { name ->
            name.equals(archivePath, ignoreCase = true) ||
                name.equals(archivePath.removePrefix("/"), ignoreCase = true)
        } ?: return null

        return decodeScaled(bytes)
    }

    private fun coverHref(opf: org.jsoup.nodes.Document): String? {
        opf.select("manifest > item[properties~=(?i)cover-image]")
            .firstOrNull()
            ?.attr("href")
            ?.takeIf { it.isNotBlank() }
            ?.let { return it }

        val metaCoverId = opf.select("metadata > meta[name=cover]")
            .firstOrNull()
            ?.attr("content")
            ?.takeIf { it.isNotBlank() }

        if (metaCoverId != null) {
            opf.select("manifest > item#${cssEscape(metaCoverId)}")
                .firstOrNull()
                ?.attr("href")
                ?.takeIf { it.isNotBlank() }
                ?.let { return it }
        }

        opf.select("manifest > item")
            .firstOrNull { item ->
                item.attr("media-type").startsWith("image/") &&
                    item.attr("id").contains("cover", ignoreCase = true)
            }
            ?.attr("href")
            ?.takeIf { it.isNotBlank() }
            ?.let { return it }

        return opf.select("manifest > item")
            .firstOrNull { item -> item.attr("media-type").startsWith("image/") }
            ?.attr("href")
            ?.takeIf { it.isNotBlank() }
    }

    private fun cssEscape(value: String): String =
        value.replace(Regex("([^A-Za-z0-9_-])"), "\\\\$1")

    private fun normalize(directory: String, rawHref: String): String? {
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

    private fun readZipEntry(
        context: Context,
        uri: Uri,
        match: (String) -> Boolean,
    ): ByteArray? {
        val stream: InputStream = runCatching {
            context.contentResolver.openInputStream(uri)
        }.getOrNull() ?: return null

        return stream.use { input ->
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
                @Suppress("UNREACHABLE_CODE")
                null
            }
        }
    }

    private fun decodeScaled(bytes: ByteArray): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)

        var sample = 1
        val width = bounds.outWidth
        val height = bounds.outHeight
        while (width / sample > MAX_EDGE || height / sample > MAX_EDGE) {
            sample *= 2
        }

        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    }

    // endregion

    private fun pdfCover(context: Context, uri: Uri): Bitmap? {
        val descriptor: ParcelFileDescriptor = runCatching {
            context.contentResolver.openFileDescriptor(uri, "r")
        }.getOrNull() ?: return null

        return descriptor.use { fd ->
            runCatching {
                PdfRenderer(fd).use { renderer ->
                    if (renderer.pageCount <= 0) return@runCatching null
                    renderer.openPage(0).use { page ->
                        val ratio = page.height.toFloat() / page.width.toFloat()
                        val width = MAX_EDGE.coerceAtMost(page.width * 2)
                        val height = (width * ratio).toInt().coerceAtLeast(1)
                        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        bitmap.eraseColor(android.graphics.Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        bitmap
                    }
                }
            }.getOrNull()
        }
    }
}
