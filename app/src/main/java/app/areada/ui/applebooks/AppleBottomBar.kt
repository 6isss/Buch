package app.areada.ui.applebooks

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.areada.R

internal enum class AppleTab {
    ReadingNow,
    Library,
    Search,
}

/** Apple Books style tab glyphs: outline when inactive, filled when active. */
internal fun AppleTab.iconRes(active: Boolean): Int = when (this) {
    AppleTab.ReadingNow -> if (active) R.drawable.ic_tab_reading_now_fill else R.drawable.ic_tab_reading_now
    AppleTab.Library -> if (active) R.drawable.ic_tab_library_fill else R.drawable.ic_tab_library
    AppleTab.Search -> if (active) R.drawable.ic_tab_search_fill else R.drawable.ic_tab_search
}

/** Translucent, blurred-looking three tab bar in the Apple Books style. */
@Composable
internal fun AppleBottomBar(
    selected: AppleTab,
    labels: @Composable (AppleTab) -> String,
    onSelect: (AppleTab) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(scheme.surface.copy(alpha = 0.7f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(scheme.outline.copy(alpha = 0.5f)),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(top = 8.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppleTab.entries.forEach { tab ->
                val active = tab == selected
                val tint = if (active) scheme.onBackground else scheme.onSurfaceVariant.copy(alpha = 0.8f)
                val interaction = remember { MutableInteractionSource() }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            interactionSource = interaction,
                            indication = null,
                            onClick = { onSelect(tab) },
                        )
                        .padding(vertical = 4.dp),
                ) {
                    Icon(
                        painter = painterResource(id = tab.iconRes(active)),
                        contentDescription = labels(tab),
                        tint = tint,
                        modifier = Modifier.size(if (active) 25.dp else 23.dp),
                    )
                    Text(
                        text = labels(tab),
                        color = tint,
                        fontSize = 10.5.sp,
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.dp)
                .background(Color.Transparent),
        )
    }
}
