package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.LodgistOrange
import com.example.ui.theme.LodgistTeal

fun openWhatsAppSupport(context: Context, customMessage: String? = null) {
    val message = customMessage ?: "Hello Lodgist Support, I am browsing rentals on the Lodgist Android App and need assistance."
    val url = "https://wa.me/2349136094315?text=" + Uri.encode(message)
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    try {
        context.startActivity(intent)
    } catch (_: Exception) {
        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://lodgist.online"))
        context.startActivity(webIntent)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LodgistTopBar(
    title: String = "Lodgist",
    subtitle: String = "Nigeria Rentals",
    canNavigateBack: Boolean = false,
    onNavigateBack: () -> Unit = {},
    showWhatsAppButton: Boolean = true,
    savedCount: Int = 0,
    onSavedClick: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    TopAppBar(
        modifier = modifier,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!canNavigateBack) {
                    Image(
                        painter = painterResource(id = R.drawable.img_lodgist_icon),
                        contentDescription = "Lodgist Logo",
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                }
                Column {
                    Text(
                        text = title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        navigationIcon = {
            if (canNavigateBack) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.testTag("top_bar_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        },
        actions = {
            if (!canNavigateBack) {
                IconButton(
                    onClick = onSearchClick,
                    modifier = Modifier.testTag("top_bar_search_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search Properties",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(
                    onClick = onSavedClick,
                    modifier = Modifier.testTag("top_bar_saved_button")
                ) {
                    BadgedBox(
                        badge = {
                            if (savedCount > 0) {
                                Badge { Text("$savedCount") }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bookmark,
                            contentDescription = "Saved Homes",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            if (showWhatsAppButton) {
                FilledTonalButton(
                    onClick = { openWhatsAppSupport(context) },
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .testTag("support_whatsapp_button"),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = LodgistTeal.copy(alpha = 0.15f),
                        contentColor = LodgistTeal
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.SupportAgent,
                        contentDescription = "Support",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Help",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}
