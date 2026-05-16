package br.com.CapitularIA.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource

@Composable
fun AppBackground(
    @DrawableRes backgroundResId: Int,
    content: @Composable () -> Unit
) {
    val backgroundColor = MaterialTheme.colorScheme.background
    val isDarkPalette = backgroundColor.luminance() < 0.5f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        Image(
            painter = painterResource(id = backgroundResId),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = if (isDarkPalette) 0.12f else 0.35f
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (isDarkPalette) {
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)
                    } else {
                        Color.Transparent
                    }
                )
        )

        content()
    }
}
