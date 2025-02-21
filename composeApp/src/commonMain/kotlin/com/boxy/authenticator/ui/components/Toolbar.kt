package com.boxy.authenticator.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import boxy_authenticator.composeapp.generated.resources.Res
import boxy_authenticator.composeapp.generated.resources.cd_navigate_back
import com.boxy.authenticator.core.Platform
import com.boxy.authenticator.utils.copy
import com.boxy.authenticator.utils.top
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Toolbar(
    title: String,
    subtitle: String = "",
    showDefaultNavigationIcon: Boolean = false,
    navigationIcon: (@Composable () -> Unit)? = null,
    onNavigationIconClick: () -> Unit = {},
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(
        navigationIconContentColor = MaterialTheme.colorScheme.primary,
        actionIconContentColor = MaterialTheme.colorScheme.primary,
    ),
    actions: @Composable RowScope.() -> Unit = {},
    windowInsets: WindowInsets? = null,
) {
    val safeDrawing = WindowInsets.safeDrawing
    val updatedInsets = windowInsets ?: safeDrawing.copy(
        top = safeDrawing.asPaddingValues().top() + if (Platform.isAndroid) 10.dp else 0.dp,
        bottom = 0.dp,
    )

    TopAppBar(
        title = { Title(title, subtitle) },
        navigationIcon = {
            if (navigationIcon == null) {
                if (showDefaultNavigationIcon)
                    DefaultNavigationIcon(onNavigationIconClick)
            } else {
                navigationIcon.invoke()
            }
        },
        colors = colors,
        actions = actions,
        windowInsets = updatedInsets
    )
}

@Composable
private fun Title(title: String, subtitle: String) {
    Column {
        Text(
            title,
            fontSize = 20.sp,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (subtitle.isNotEmpty()) {
            Text(
                subtitle,
                fontSize = 13.sp,
                lineHeight = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DefaultNavigationIcon(onNavigationIconClick: () -> Unit) {
    IconButton(onClick = onNavigationIconClick) {
        Image(
            imageVector = Icons.Rounded.ArrowBackIosNew,
            contentDescription = stringResource(Res.string.cd_navigate_back),
            contentScale = ContentScale.Crop,
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .width(50.dp)
                .padding(start = 10.dp, end = 16.dp)
        )
    }
}