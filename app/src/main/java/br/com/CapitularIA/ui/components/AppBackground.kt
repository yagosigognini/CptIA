package br.com.CapitularIA.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import br.com.CapitularIA.R

@Composable
fun AppBackground(
    @DrawableRes backgroundResId: Int,
    content: @Composable () -> Unit
) {
    val isDarkPalette = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val resolvedBackground = resolveBackgroundForTheme(
        requestedBackground = backgroundResId,
        isDarkPalette = isDarkPalette
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = resolvedBackground),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        content()
    }
}

@DrawableRes
private fun resolveBackgroundForTheme(
    @DrawableRes requestedBackground: Int,
    isDarkPalette: Boolean
): Int {
    if (!isDarkPalette) return requestedBackground
    return when (requestedBackground) {
        R.drawable.background -> R.drawable.background2
        R.drawable.app_background -> R.drawable.app_background2
        else -> requestedBackground
    }
}
