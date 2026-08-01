package com.iti.presentation.core.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iti.presentation.core.ConnectivityBanner

@Composable
fun OfflineBanner(
    banner: ConnectivityBanner,
    modifier: Modifier = Modifier,
) {
    val visible = banner != ConnectivityBanner.Hidden
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
        modifier = modifier
    ) {
        val (bg, text, icon) = when (banner) {
            ConnectivityBanner.Offline -> Triple(MaterialTheme.colorScheme.error, androidx.compose.ui.res.stringResource(com.iti.presentation.R.string.offline_banner_offline), Icons.Default.WifiOff)
            ConnectivityBanner.BackOnline -> Triple(Color(0xFF4CAF50), androidx.compose.ui.res.stringResource(com.iti.presentation.R.string.offline_banner_online), Icons.Default.Wifi)
            ConnectivityBanner.Hidden -> Triple(MaterialTheme.colorScheme.error, "", Icons.Default.WifiOff)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(bg)
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text(text, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
    }
}
