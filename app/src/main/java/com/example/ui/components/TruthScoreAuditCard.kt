package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.PriceCheck
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PropertyEntity
import com.example.ui.theme.LodgistOrange
import com.example.ui.theme.TruthScoreGreen
import com.example.ui.theme.TruthScoreRed

@Composable
fun TruthScoreAuditCard(
    property: PropertyEntity,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "TruthScore Security",
                        tint = LodgistOrange,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Lodgist TruthScore™",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Audited by ShadowSpark field officers",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                TruthScoreCircle(score = property.truthScore, size = 52)
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "What Lodgist checked before you pay:",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 5 Pillars
            AuditItem(
                title = "Title Document Verified",
                subtitle = "Deed of Assignment / Governor's Consent / Family Root checked",
                isPassed = property.titleDocumentChecked,
                icon = Icons.Default.AssignmentTurnedIn
            )

            AuditItem(
                title = "Landlord Identity & Authority",
                subtitle = "NIN / BVN matched to property owner or registered mandate",
                isPassed = property.landlordIdentityChecked,
                icon = Icons.Default.AccountCircle
            )

            AuditItem(
                title = "Price Against Nearby Homes",
                subtitle = "Rent matches neighborhood benchmark — no inflated agent markup",
                isPassed = property.fairPriceIndexed,
                icon = Icons.Default.PriceCheck
            )

            AuditItem(
                title = "Disputes & Demolition Check",
                subtitle = "Zero pending litigation, community disputes, or demolition marks",
                isPassed = property.disputeFree,
                icon = Icons.Default.Gavel
            )

            AuditItem(
                title = "Physical Inspection Audit",
                subtitle = "Borehole water test, prepaid meter, wiring safety & rainy season flood risk",
                isPassed = property.physicalInspectionPassed,
                icon = Icons.Default.LocationCity
            )
        }
    }
}

@Composable
private fun AuditItem(
    title: String,
    subtitle: String,
    isPassed: Boolean,
    icon: ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(
                    if (isPassed) TruthScoreGreen.copy(alpha = 0.15f)
                    else TruthScoreRed.copy(alpha = 0.15f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPassed) Icons.Default.Check else Icons.Default.Close,
                contentDescription = if (isPassed) "Passed" else "Pending",
                tint = if (isPassed) TruthScoreGreen else TruthScoreRed,
                modifier = Modifier.size(16.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 15.sp
            )
        }
    }
}
