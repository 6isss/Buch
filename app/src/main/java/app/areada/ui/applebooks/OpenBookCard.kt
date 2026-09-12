package app.areada.ui.applebooks

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The 3D open-book hero: two white pages side by side, a central spine crease,
 * curved outer page edges, a thin page stack and a warm shadow underneath.
 */
@Composable
fun OpenBookCard(
    previewText: String?,
    loading: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val (left, right) = remember(previewText) { splitPages(previewText) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1.38f)
            .shadow(
                elevation = 26.dp,
                shape = RoundedCornerShape(14.dp),
                ambientColor = Color(0xFF8A6A4A),
                spotColor = Color(0xFF6E5237),
            )
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF2B2B30), Color(0xFF17171A)),
                ),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            BookPage(
                text = if (loading) null else left,
                leftHand = true,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
            Spacer(modifier = Modifier.width(1.dp))
            BookPage(
                text = if (loading) null else right,
                leftHand = false,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
        }

        // central spine crease
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxHeight()
                .width(22.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.20f),
                            Color.Black.copy(alpha = 0.38f),
                            Color.Black.copy(alpha = 0.20f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )
    }
}

@Composable
private fun BookPage(
    text: String?,
    leftHand: Boolean,
    modifier: Modifier = Modifier,
) {
    val shape = if (leftHand) {
        RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp, topEnd = 1.dp, bottomEnd = 1.dp)
    } else {
        RoundedCornerShape(topStart = 1.dp, bottomStart = 1.dp, topEnd = 10.dp, bottomEnd = 10.dp)
    }

    Box(modifier = modifier) {
        // page stack behind the visible sheet
        Box(
            modifier = Modifier
                .align(if (leftHand) Alignment.CenterStart else Alignment.CenterEnd)
                .fillMaxHeight(0.97f)
                .width(6.dp)
                .clip(shape)
                .background(
                    Brush.horizontalGradient(
                        if (leftHand) {
                            listOf(Color(0xFFBFB6A8), Color(0xFFF3EFE7))
                        } else {
                            listOf(Color(0xFFF3EFE7), Color(0xFFBFB6A8))
                        },
                    ),
                ),
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = if (leftHand) 4.dp else 0.dp, end = if (leftHand) 0.dp else 4.dp)
                .clip(shape)
                .background(Color(0xFFFFFDF9)),
        ) {
            if (text != null) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Serif,
                    fontSize = 8.sp,
                    lineHeight = 13.sp,
                    color = Color(0xFF2B2B2F),
                    overflow = TextOverflow.Clip,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            start = if (leftHand) 14.dp else 18.dp,
                            end = if (leftHand) 18.dp else 14.dp,
                            top = 16.dp,
                            bottom = 14.dp,
                        ),
                )
            } else {
                SkeletonLines()
            }
        }

        // inner shadow towards the spine
        Box(
            modifier = Modifier
                .align(if (leftHand) Alignment.CenterEnd else Alignment.CenterStart)
                .fillMaxHeight()
                .width(26.dp)
                .background(
                    Brush.horizontalGradient(
                        if (leftHand) {
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.16f))
                        } else {
                            listOf(Color.Black.copy(alpha = 0.16f), Color.Transparent)
                        },
                    ),
                ),
        )
    }
}

@Composable
private fun SkeletonLines() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        repeat(9) { index ->
            Box(
                modifier = Modifier
                    .fillMaxWidth(if (index % 4 == 3) 0.62f else 0.95f)
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFFE7E3DA)),
            )
        }
    }
}

private fun splitPages(text: String?): Pair<String?, String?> {
    if (text.isNullOrBlank()) return null to null
    val clean = text.trim()
    val half = (clean.length / 2).coerceAtMost(700)
    val breakPoint = clean.lastIndexOf(' ', half).takeIf { it > 40 } ?: half
    return clean.substring(0, breakPoint) to clean.substring(breakPoint).trim().take(700)
}
