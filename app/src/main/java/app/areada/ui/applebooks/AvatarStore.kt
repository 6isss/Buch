package app.areada.ui.applebooks

import android.content.Context
import android.content.Intent
import android.net.Uri

/** Stores the user-picked profile photo for the Reading Now avatar. */
internal object AvatarStore {
    private const val PREFS = "apple_books_profile"
    private const val KEY_URI = "avatar_uri"

    fun get(context: Context): Uri? {
        val value = context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_URI, null)
        return value?.takeIf { it.isNotBlank() }?.let(Uri::parse)
    }

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
        app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_URI, uri?.toString())
            .apply()
    }
}
