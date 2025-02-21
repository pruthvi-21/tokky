package com.jw.preferences

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jw.preferences.utils.PreferenceDefaults
import com.jw.preferences.utils.copy

@Composable
fun Preference(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: @Composable (() -> Unit)? = null,
    summary: @Composable (() -> Unit)? = null,
    widgetContainer: @Composable (() -> Unit)? = null,
    showDivider: Boolean? = null,
    onClick: (() -> Unit)? = null,
) {
    val theme = LocalPreferenceTheme.current
    BasicPreference(
        textContainer = {
            Column(
                modifier = Modifier.padding(theme.preferencePadding)
            ) {
                PreferenceDefaults.TitleContainer(title = title, enabled = enabled)
                PreferenceDefaults.SummaryContainer(summary = summary, enabled = enabled)
            }
        },
        modifier = modifier,
        enabled = enabled,
        iconContainer = { PreferenceDefaults.IconContainer(icon = icon, enabled = enabled) },
        widgetContainer = {
            Box(
                modifier = Modifier.padding(
                    theme.preferencePadding.copy(
                        start = 10.dp,
                    )
                ),
            ) {
                widgetContainer?.invoke()
            }
        },
        showDivider = showDivider ?: theme.showPreferenceDivider,
        onClick = onClick,
    )
}