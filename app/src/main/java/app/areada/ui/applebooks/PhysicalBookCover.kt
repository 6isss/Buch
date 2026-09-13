package app.areada.ui.applebooks

import android.graphics.Bitmap
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import app.areada.data.cover.BookCoverRepository
import app.areada.data.reader.DocumentType

/** Shared hardcover shape: 1 : 1.45 board with softly rounded 4dp corners. */
internal val HardcoverShape = RoundedCornerShape(4.dp)
internal const val HARDCOVER_RATIO = 1f / 1.45f

/**
 * Realistic hardcover treatment used for every cover in the app, modelled on a
 * photographed blank hardback: rounded board, a hinge groove with a bright
 * highlight strip either side of it, fine bevels on all four edges, a soft
 * top-left sheen and a floating drop shadow tinted by the artwork.
 */
@Composable
fun PhysicalBookCover(
    uriString: String,
    title: String,
    type: DocumentType,
    modifier: Modifier = Modifier,
    elevation: Dp = 16.dp,
) {
    val context = LocalContext.current
    var bitmap by remember(uriString) { mutableStateOf<Bitmap?>(BookCoverRepository.cached(uriString)) }

    LaunchedEffect(uriString, type) {
        if (bitmap == null) {
            bitmap = BookCoverRepository.load(context, uriString, type)
        }
    }

    val fallbackTint = remember(title) { generatedTint(title) }
    val tint = remember(bitmap, fallbackTint) { bitmap?.let(::dominantColor) ?: fallbackTint }
    val appeared by animateFloatAsState(
        targetValue = if (bitmap != null) 1f else 0.85f,
        animationSpec = tween(durationMillis = 260),
        label = "coverAppear",
    )

    Box(modifier = modifier.aspectRatio(HARDCOVER_RATIO)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .shadow(
                    elevation = elevation,
                    shape = HardcoverShape,
                    ambientColor = tint.copy(alpha = 0.55f),
                    spotColor = tint.copy(alpha = 0.85f),
                )
                .clip(HardcoverShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            val image = bitmap
            if (image != null) {
                Image(
                    bitmap = image.asImageBitmap(),
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(appeared),
                )
            } else {
                GeneratedCover(title = title, tint = fallbackTint)
            }

            // spine band: narrow dark hinge groove with a bright highlight on each side
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.14f)
                    .background(
                        Brush.horizontalGradient(
                            0.00f to Color.Black.copy(alpha = 0.22f),
                            0.14f to Color.White.copy(alpha = 0.30f),
                            0.34f to Color.Black.copy(alpha = 0.26f),
                            0.48f to Color.White.copy(alpha = 0.26f),
                            0.72f to Color.Black.copy(alpha = 0.06f),
                            1.00f to Color.Transparent,
                        ),
                    ),
            )

            // fore-edge page sliver on the right
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(2.5.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.10f),
                                Color.White.copy(alpha = 0.34f),
                                Color(0x40786E64),
                            ),
                        ),
                    ),
            )

            // fine bevel along the top edge
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(1.5.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.White.copy(alpha = 0.28f), Color.Transparent),
                        ),
                    ),
            )

            // fine bevel along the bottom edge
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.22f)),
                        ),
                    ),
            )

            // soft sheen across the board, brightest at the top left
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.16f),
                                Color.White.copy(alpha = 0.04f),
                                Color.Transparent,
                            ),
                            start = Offset.Zero,
                            end = Offset.Infinite,
                        ),
                    ),
            )
        }
    }
}

@Composable
private fun GeneratedCover(title: String, tint: Color) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    listOf(tint.copy(alpha = 0.92f), tint.copy(alpha = 0.62f)),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = title.substringBeforeLast('.'),
            style = MaterialTheme.typography.titleMedium,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = 0.94f),
            textAlign = TextAlign.Center,
            maxLines = 4,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
    }
}

private fun dominantColor(bitmap: Bitmap): Color {
    return runCatching {
        val small = Bitmap.createScaledBitmap(bitmap, 8, 8, true)
        var red = 0L
        var green = 0L
        var blue = 0L
        var count = 0

        for (x in 0 until small.width) {
            for (y in 0 until small.height) {
                val pixel = small.getPixel(x, y)
                red += (pixel shr 16) and 0xFF
                green += (pixel shr 8) and 0xFF
                blue += pixel and 0xFF
                count += 1
            }
        }

        if (count == 0) return Color(0xFF6B6B72)
        Color(
            red = (red / count).toInt() / 255f,
            green = (green / count).toInt() / 255f,
            blue = (blue / count).toInt() / 255f,
        )
    }.getOrDefault(Color(0xFF6B6B72))
}

private val generatedPalette = listOf(
    Color(0xFF7B5E57),
    Color(0xFF395B64),
    Color(0xFF6D597A),
    Color(0xFF3F5E45),
    Color(0xFF8C5A3C),
    Color(0xFF2F4858),
)

private fun generatedTint(title: String): Color {
    val index = (title.hashCode().toUInt() % generatedPalette.size.toUInt()).toInt()
    return generatedPalette[index]
}
