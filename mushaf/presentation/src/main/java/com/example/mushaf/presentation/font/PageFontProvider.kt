package com.example.mushaf.presentation.font

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.example.mushaf.domain.model.ReadingMode


object PageFontProvider {

    private const val TAG = "Mushaf"

    fun assetPath(pageNumber: Int, mode: ReadingMode): String {
        val dir = if (mode == ReadingMode.TAJWEED) "tajweed" else "standard"
        return "fonts/$dir/p$pageNumber.ttf"
    }


    fun create(context: Context, pageNumber: Int, mode: ReadingMode): FontFamily {
        val path = assetPath(pageNumber, mode)
        if (assetExists(context, path)) {
            return FontFamily(Font(path = path, assetManager = context.assets))
        }
        Log.w(TAG, "Font asset missing: $path — attempting fallback")
        val fallbackMode = if (mode == ReadingMode.TAJWEED) ReadingMode.PLAIN else ReadingMode.TAJWEED
        val fallbackPath = assetPath(pageNumber, fallbackMode)
        if (assetExists(context, fallbackPath)) {
            return FontFamily(Font(path = fallbackPath, assetManager = context.assets))
        }
        Log.e(TAG, "No font asset for page $pageNumber ($path / $fallbackPath); using default font")
        return FontFamily.Default
    }

    private fun assetExists(context: Context, path: String): Boolean = try {
        context.assets.open(path).use { true }
    } catch (t: Throwable) {
        false
    }
}


@Composable
fun rememberPageFontFamily(pageNumber: Int, mode: ReadingMode): FontFamily {
    val context = LocalContext.current
    val path = PageFontProvider.assetPath(pageNumber, mode)
    return remember(path) { PageFontProvider.create(context, pageNumber, mode) }
}
