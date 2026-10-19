package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.EscrowPaymentEntity
import com.example.ui.components.REFUND_COPY
import com.example.ui.components.formatNaira
import com.example.ui.components.openWhatsAppSupport
import com.example.ui.components.paymentPurposeLabel
import com.example.ui.components.paymentStatusLabel
import com.example.ui.components.displayPayerName
import com.example.ui.components.sanitizeForWhatsApp
import com.example.ui.theme.accessibleGrey
import com.example.ui.theme.accessibleOrange
import com.example.ui.theme.accessibleTeal
import com.example.ui.theme.LodgistOrange
import com.example.ui.theme.LodgistTeal
import com.example.ui.theme.TruthScoreGreen

@Composable
fun EscrowRevenueScreen(
    payments: List<EscrowPaymentEntity>,
    totalEscrow: Long,
    totalRevenue: Long,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("escrow_revenue_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Escrow Protection Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(LodgistOrange.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = LodgistOrange,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Lodgist Payment Requests",
                                    modifier = Modifier.semantics { heading() },
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Confirmed manually by Lodgist staff on WhatsApp",
                                    fontSize = 11.sp,
                                    color = accessibleTeal(),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(accessibleGrey().copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            // Describes the process; no lock/shield because nothing here is secured or held.
                            Text(
                                text = "Manual confirmation",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = accessibleGrey()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Metrics Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MetricCard(
                            label = "Awaiting Staff Confirmation",
                            amount = formatNaira(totalEscrow),
                            icon = Icons.Default.HourglassTop,
                            tint = accessibleTeal(),
                            modifier = Modifier.weight(1f)
                        )

                        MetricCard(
                            label = "Staff-Confirmed Fees",
                            amount = formatNaira(totalRevenue),
                            icon = Icons.Default.MonetizationOn,
                            tint = accessibleOrange(),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // How manual payments work
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = LodgistTeal.copy(alpha = 0.10f)
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, LodgistTeal.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = LodgistTeal,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "How Lodgist payments work",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "1. Request payment details from Lodgist staff on WhatsApp.\n2. Pay only to the details Lodgist staff send you on WhatsApp. Your payment counts only once Lodgist staff confirm they received it.\n3. This screen doesn't update automatically; Lodgist staff confirm on WhatsApp.\n4. $REFUND_COPY",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }

        // Transactions List Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Payment Requests (${payments.size})",
                    modifier = Modifier.semantics { heading() },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        if (payments.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = LodgistOrange,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No Payment Requests Yet",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Payment requests you confirm sending on WhatsApp appear here while they await Lodgist staff confirmation. Inspection and listing leads are not payments and are not shown here.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        } else {
            items(payments, key = { it.id }) { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("escrow_payment_card_${item.id}"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = paymentPurposeLabel(item.purpose),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = accessibleOrange()
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        accessibleTeal().copy(alpha = 0.15f)
                                    )
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                            ) {
                                // Display label only: legacy escrow statuses show "Status unknown" (stored value untouched).
                                Text(
                                    text = paymentStatusLabel(item.status),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = accessibleTeal()
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = item.propertyTitle,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Ref: ${item.transactionRef} • ${displayPayerName(item.payerName)}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Text(
                                text = formatNaira(item.amount),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = accessibleOrange()
                            )
                        }

                        // Release/refund of funds is handled only by Lodgist staff, never from the app.
                        val rowContext = LocalContext.current
                        TextButton(
                            onClick = {
                                openWhatsAppSupport(
                                    rowContext,
                                    "Hello Lodgist, I have a question about my request.\n" +
                                        "Request reference: ${sanitizeForWhatsApp(item.transactionRef, 32)}\n" +
                                        "Listing: ${sanitizeForWhatsApp(item.propertyTitle)}"
                                )
                            },
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Text("Ask about this request on WhatsApp", color = accessibleTeal(), fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricCard(
    label: String,
    amount: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = label,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = amount,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = tint
            )
        }
    }
}
