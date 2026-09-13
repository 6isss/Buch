package app.areada.ui.applebooks

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri

/**
 * Stores the user-picked profile photo and keeps the decoded bitmap in memory so
 * the avatar does not flicker when switching tabs.
 */
internal object AvatarStore {
    private const val PREFS = "apple_books_profile"
    private const val KEY_URI = "avatar_uri"

    private var cachedKey: String? = null
    private var cachedBitmap: Bitmap? = null

    fun getUriString(context: Context): String? {
        return context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_URI, null)
            ?.takeIf { it.isNotBlank() }
    }

    fun get(context: Context): Uri? = getUriString(context)?.let(Uri::parse)

    fun set(context: Context, uri: Uri?) {
        val app = context.applicationContext
        if (uri != null) {
            runCatching {
                app.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
        }
        cachedKey = null
        cachedBitmap = null
        app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_URI, uri?.toString())
            .apply()
    }

    /** Already-decoded avatar, if any. Safe to read during composition. */
    fun cached(uriString: String?): Bitmap? {
        if (uriString == null) return null
        return if (cachedKey == uriString) cachedBitmap else null
    }

    /** Decodes and caches the avatar. Call from a background dispatcher. */
    fun load(context: Context, uriString: String?): Bitmap? {
        if (uriString == null) return null
        cached(uriString)?.let { return it }

        val bitmap = runCatching {
            context.applicationContext.contentResolver
                .openInputStream(Uri.parse(uriString))
                ?.use { input ->
                    val options = BitmapFactory.Options().apply { inSampleSize = 2 }
                    BitmapFactory.decodeStream(input, null, options)
                }
        }.getOrNull()

        if (bitmap != null) {
            cachedKey = uriString
            cachedBitmap = bitmap
        }
        return bitmap
    }
}
