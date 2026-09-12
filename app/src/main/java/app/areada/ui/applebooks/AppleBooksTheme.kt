package app.areada.ui.applebooks

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Off-white paper, near-black ink, warm orange accent. */
private val AppleLightColors = lightColorScheme(
    primary = Color(0xFF1C1C1E),
    onPrimary = Color(0xFFF9F9FB),
    primaryContainer = Color(0xFFECECEF),
    onPrimaryContainer = Color(0xFF1C1C1E),
    secondary = Color(0xFFD2763A),
    onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFFF9F9FB),
    onBackground = Color(0xFF15151A),
    surface = Color(0xFFFDFDFF),
    onSurface = Color(0xFF15151A),
    surfaceVariant = Color(0xFFEDEDF1),
    onSurfaceVariant = Color(0xFF6C6C74),
    outline = Color(0xFFDDDDE3),
)

private val AppleDarkColors = darkColorScheme(
    primary = Color(0xFFF2F2F5),
    onPrimary = Color(0xFF121214),
    primaryContainer = Color(0xFF2A2A2E),
    onPrimaryContainer = Color(0xFFF2F2F5),
    secondary = Color(0xFFE08C4C),
    onSecondary = Color(0xFF121214),
    background = Color(0xFF121214),
    onBackground = Color(0xFFF2F2F5),
    surface = Color(0xFF19191C),
    onSurface = Color(0xFFF2F2F5),
    surfaceVariant = Color(0xFF242428),
    onSurfaceVariant = Color(0xFFA5A5AD),
    outline = Color(0xFF2F2F35),
)

private val AppleShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

private val AppleTypography: Typography
    @Composable get() {
        val base = MaterialTheme.typography
        return base.copy(
            displaySmall = base.displaySmall.copy(
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 34.sp,
                letterSpacing = (-0.6).sp,
            ),
            headlineLarge = base.headlineLarge.copy(
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp,
            ),
            headlineMedium = base.headlineMedium.copy(
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.4).sp,
            ),
            titleLarge = base.titleLarge.copy(
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
            ),
            titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        )
    }

/**
 * Apple Books skin applied to the three home tabs only; the reader keeps its own
 * sepia / sage / blush / dark themes.
 */
@Composable
fun AppleBooksTheme(content: @Composable () -> Unit) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    MaterialTheme(
        colorScheme = if (dark) AppleDarkColors else AppleLightColors,
        shapes = AppleShapes,
        typography = AppleTypography,
        content = content,
    )
}
