package app.areada.ui.reader

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay
import karacken.curl.DeckRejectionReason
import karacken.curl.GestureRejectionReason
import karacken.curl.PageChange
import karacken.curl.PageImage
import karacken.curl.PageSurfaceListener
import karacken.curl.PageSurfaceView
import karacken.curl.PortraitPageDeck
import karacken.curl.RenderCapabilities
import karacken.curl.RenderFailure
import java.util.concurrent.atomic.AtomicBoolean

/**
 * One captured page turn: the page we leave, the page we land on, and the direction.
 * Both bitmaps stay immutable for the whole animation.
 */
internal data class CurlTurnFrames(
    val generationId: Long,
    val from: Bitmap,
    val to: Bitmap,
    val forward: Boolean,
)

/** Snapshots a view (the reader WebView) into an ARGB_8888 bitmap the curl renderer can upload. */
internal fun captureReaderBitmap(view: View): Bitmap? = runCatching {
    val width = view.width
    val height = view.height
    if (width <= 0 || height <= 0) return null
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    bitmap.setHasAlpha(false)
    view.draw(Canvas(bitmap))
    bitmap
}.getOrNull()

/**
 * Plays a single PlayLikeCurl page turn over the reader.
 *
 * The overlay is only composed while a turn is running and always reports back through
 * [onFinished] — on success, rejection, render failure or timeout — so the underlying
 * WebView is never left hidden.
 */
@Composable
internal fun CurlPageTurnOverlay(
    frames: CurlTurnFrames,
    onFinished: (failed: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val latestOnFinished by rememberUpdatedState(onFinished)
    val completed = remember(frames.generationId) { AtomicBoolean(false) }

    LaunchedEffect(frames.generationId) {
        delay(CurlTurnTimeoutMs)
        if (completed.compareAndSet(false, true)) {
            latestOnFinished(true)
        }
    }

    DisposableEffect(frames.generationId) {
        onDispose {
            completed.set(true)
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            PageSurfaceView(context).apply {
                setZOrderOnTop(true)
                var attempts = 0

                fun finish(failed: Boolean) {
                    if (completed.compareAndSet(false, true)) {
                        post { latestOnFinished(failed) }
                    }
                }

                fun submitFrames() {
                    val viewportWidth = if (width > 0) width else frames.from.width
                    val viewportHeight = if (height > 0) height else frames.from.height
                    if (viewportWidth <= 0 || viewportHeight <= 0) {
                        attempts += 1
                        if (attempts > 20) {
                            finish(true)
                        } else {
                            postDelayed({ submitFrames() }, 16L)
                        }
                        return
                    }
                    runCatching {
                        setViewport(viewportWidth, viewportHeight)
                        val leaving = pageImage(frames.generationId, "leaving", 1, frames.from)
                        val landing = pageImage(frames.generationId, "landing", if (frames.forward) 2 else 0, frames.to)
                        val filler = pageImage(frames.generationId, "filler", if (frames.forward) 0 else 2, frames.from)
                        val deck = if (frames.forward) {
                            PortraitPageDeck(filler, leaving, landing)
                        } else {
                            PortraitPageDeck(landing, leaving, filler)
                        }
                        submitDeck(deck)
                    }.onFailure { finish(true) }
                }

                setPageSurfaceListener(object : PageSurfaceListener {
                    override fun onCapabilitiesAvailable(capabilities: RenderCapabilities) {
                        post { submitFrames() }
                    }

                    override fun onDeckPrepared(generationId: Long) {
                        post {
                            val change = if (frames.forward) PageChange.NEXT else PageChange.PREVIOUS
                            if (!turn(change)) {
                                finish(false)
                            }
                        }
                    }

                    override fun onDeckRejected(generationId: Long, reason: DeckRejectionReason) {
                        finish(true)
                    }

                    override fun onRenderFailure(failure: RenderFailure) {
                        finish(true)
                    }

                    override fun onGestureRejected(
                        gestureId: Long,
                        generationId: Long,
                        reason: GestureRejectionReason,
                    ) {
                        finish(false)
                    }

                    override fun onSettlementCancelled(generationId: Long, currentLogicalPageId: String) {
                        finish(false)
                    }

                    override fun onSettlementCompleted(
                        generationId: Long,
                        currentLogicalPageId: String,
                        currentOrdinal: Int,
                        pageChange: PageChange,
                    ) {
                        finish(false)
                    }
                })

                attach()
                setVisible(true)
                if (getRenderCapabilities() != null) {
                    post { submitFrames() }
                }
            }
        },
        onRelease = { surface ->
            runCatching {
                surface.setPageSurfaceListener(null)
                surface.detach()
                surface.dispose()
            }
        },
    )
}

private const val CurlTurnTimeoutMs = 2600L

private fun pageImage(
    generationId: Long,
    id: String,
    ordinal: Int,
    bitmap: Bitmap,
): PageImage<Bitmap> = PageImage(
    generationId,
    id,
    ordinal,
    bitmap.width,
    bitmap.height,
    bitmap,
)
