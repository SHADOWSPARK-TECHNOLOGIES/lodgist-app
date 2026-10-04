package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.TruthScoreGreen
import com.example.ui.theme.TruthScoreRed
import com.example.ui.theme.TruthScoreYellow

fun getScoreColor(score: Int): Color {
    return when {
        score >= 85 -> TruthScoreGreen
        score >= 70 -> TruthScoreYellow
        else -> TruthScoreRed
    }
}

@Composable
fun TruthScorePill(
    score: Int,
    modifier: Modifier = Modifier
) {
    val color = getScoreColor(score)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Shield,
            contentDescription = "TruthScore",
            tint = color,
            modifier = Modifier.size(13.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "$score% TruthScore",
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun TruthScoreCircle(
    score: Int,
    modifier: Modifier = Modifier,
    size: Int = 56
) {
    val color = getScoreColor(score)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.12f))
            .border(2.dp, color, CircleShape)
    ) {
        Text(
            text = "$score%",
            color = color,
            fontSize = (size * 0.32).sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
fun VerifiedBadge(
    modifier: Modifier = Modifier,
    isVerified: Boolean = true
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1ABC9C).copy(alpha = 0.15f))
            .border(1.dp, Color(0xFF1ABC9C).copy(alpha = 0.35f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = "Verified by Lodgist",
            tint = Color(0xFF1ABC9C),
            modifier = Modifier.size(13.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = if (isVerified) "Verified by Lodgist" else "Review Pending",
            color = Color(0xFF1ABC9C),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
