package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LodgistOrangeButton
import com.example.ui.theme.accessibleGrey
import com.example.ui.theme.accessibleOrange
import com.example.ui.theme.accessibleTeal

/** Payment channel recorded for every request made from this sheet. The app never takes payment itself. */
const val PAYMENT_METHOD_WHATSAPP_MANUAL = "WHATSAPP_MANUAL"

/** Where the user is in the WhatsApp handoff. Saved across rotation/process death as a String. */
private object RequestStage {
    const val FORM = "FORM"
    const val OPENED = "OPENED"            // WhatsApp opened; we don't know yet whether the user sent it
    const val OPEN_FAILED = "OPEN_FAILED"  // WhatsApp/browser could not open; show number + reference
    const val SENT = "SENT"                // user confirmed they sent the message
}

/**
 * Prefilled WhatsApp text for a payment request. It carries only the listing reference and the
 * request reference. It never contains card, bank or account details: Lodgist staff send the
 * approved payment instructions on WhatsApp and confirm payment manually. Untrusted text (the
 * server listing title and id) is sanitized so it cannot add lines to the user's message.
 */
fun buildWhatsAppPaymentRequestMessage(
    title: String,
    listingTitle: String,
    propertyId: String,
    amountNaira: Long,
    requestRef: String
): String {
    return "Hello Lodgist, I would like payment instructions for ${sanitizeForWhatsApp(title)}.\n" +
        "Listing: ${sanitizeForWhatsApp(listingTitle)}\n" +
        "Listing reference: ${sanitizeForWhatsApp(propertyId, maxLength = 64)}\n" +
        "Amount shown in app: ${formatNaira(amountNaira)}\n" +
        "Request reference: ${sanitizeForWhatsApp(requestRef, maxLength = 32)}\n" +
        "I understand nothing is paid or confirmed until Lodgist staff confirm it."
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentCheckoutSheet(
    title: String,
    subtitle: String,
    amountNaira: Long,
    purpose: String, // "INSPECTION_TOKEN", "ANNUAL_RENT_ESCROW", "EXPRESS_VERIFICATION"
    propertyId: String,
    payerName: String,
    payerPhone: String,
    /** Stable reference for this listing's request; reused when the sheet is reopened. */
    requestRef: String,
    /** True when the user already confirmed sending this request (sheet reopens on the pending state). */
    alreadySent: Boolean = false,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    onDismiss: () -> Unit,
    /** Called only after the user confirms they sent the WhatsApp message. Records a pending request. */
    onRequestSent: (method: String, ref: String) -> Unit
) {
    var stage by rememberSaveable(propertyId) { mutableStateOf(if (alreadySent) RequestStage.SENT else RequestStage.FORM) }
    val ref by rememberSaveable(propertyId) { mutableStateOf(requestRef) }
    val context = LocalContext.current

    fun openWhatsApp() {
        val launched = openWhatsAppSupport(
            context,
            buildWhatsAppPaymentRequestMessage(
                title = title,
                listingTitle = subtitle,
                propertyId = propertyId,
                amountNaira = amountNaira,
                requestRef = ref
            ),
            fallbackToWebsite = false
        )
        stage = if (launched) RequestStage.OPENED else RequestStage.OPEN_FAILED
    }

    fun confirmSent() {
        if (stage != RequestStage.SENT) {
            stage = RequestStage.SENT
            onRequestSent(PAYMENT_METHOD_WHATSAPP_MANUAL, ref)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(bottom = 36.dp)
        ) {
            when (stage) {
                RequestStage.FORM -> RequestForm(
                    title = title,
                    subtitle = subtitle,
                    amountNaira = amountNaira,
                    onOpenWhatsApp = { openWhatsApp() }
                )
                RequestStage.OPENED -> HandoffPending(
                    headingText = "Opened WhatsApp: did you send the message?",
                    bodyText = "Nothing is saved or paid until you confirm you sent it. If you didn't, open WhatsApp again; your reference stays the same.",
                    requestRef = ref,
                    onConfirmSent = { confirmSent() },
                    onOpenAgain = { openWhatsApp() },
                    onDismiss = onDismiss
                )
                RequestStage.OPEN_FAILED -> HandoffPending(
                    headingText = "Couldn't open WhatsApp",
                    bodyText = "Message Lodgist on WhatsApp at $LODGIST_WHATSAPP_DISPLAY and include your request reference. Nothing has been saved or paid.",
                    requestRef = ref,
                    onConfirmSent = { confirmSent() },
                    onOpenAgain = { openWhatsApp() },
                    onDismiss = onDismiss
                )
                else -> RequestSent(
                    title = title,
                    amountNaira = amountNaira,
                    payerName = payerName,
                    requestRef = ref,
                    onOpenAgain = { openWhatsAppSupport(context, "Hello Lodgist, I have a question about my request. Request reference: ${sanitizeForWhatsApp(ref, 32)}") },
                    onDismiss = onDismiss
                )
            }
        }
    }
}

@Composable
private fun RequestForm(
    title: String,
    subtitle: String,
    amountNaira: Long,
    onOpenWhatsApp: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Pay via Lodgist WhatsApp",
                modifier = Modifier.semantics { heading() },
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        // Describes the process, not a status: nothing is confirmed yet.
        Text(
            text = "Manual confirmation",
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(accessibleGrey().copy(alpha = 0.15f))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = accessibleGrey()
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Pay only to the details Lodgist staff send you on WhatsApp",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = formatNaira(amountNaira),
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = accessibleOrange()
            )
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = "How payment works",
        modifier = Modifier.semantics { heading() },
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
    )

    Spacer(modifier = Modifier.height(10.dp))

    // Information only (not a selectable option), so it has no click action for TalkBack to announce.
    InfoRow(
        title = "WhatsApp Lodgist staff",
        subtitle = "Staff send approved payment instructions and confirm payment manually"
    )

    Spacer(modifier = Modifier.height(14.dp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Your payment counts only once Lodgist staff confirm they received it; this app cannot confirm payments. $REFUND_COPY",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface,
            lineHeight = 15.sp
        )
    }

    Spacer(modifier = Modifier.height(20.dp))

    // WhatsApp handoff: opens wa.me with a prefilled request. No payment happens here.
    Button(
        onClick = onOpenWhatsApp,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .testTag("submit_escrow_payment_btn"),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = LodgistOrangeButton, contentColor = Color.White)
    ) {
        Icon(
            imageVector = Icons.Default.Forum,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Open WhatsApp to request payment details",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun HandoffPending(
    headingText: String,
    bodyText: String,
    requestRef: String,
    onConfirmSent: () -> Unit,
    onOpenAgain: () -> Unit,
    onDismiss: () -> Unit
) {
    Text(
        text = headingText,
        modifier = Modifier.semantics {
            heading()
            liveRegion = LiveRegionMode.Polite
        },
        fontSize = 17.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
    )
    Spacer(modifier = Modifier.height(6.dp))
    Text(
        text = bodyText,
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        lineHeight = 16.sp
    )
    Spacer(modifier = Modifier.height(12.dp))
    ReferenceRow(requestRef = requestRef)
    Spacer(modifier = Modifier.height(16.dp))
    Button(
        onClick = onConfirmSent,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .testTag("confirm_whatsapp_sent_btn"),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(containerColor = LodgistOrangeButton, contentColor = Color.White)
    ) {
        Text("Yes, I sent the message", fontWeight = FontWeight.Bold)
    }
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedButton(
        onClick = onOpenAgain,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .testTag("reopen_whatsapp_btn"),
        shape = RoundedCornerShape(10.dp)
    ) {
        Text("Open WhatsApp again", fontWeight = FontWeight.Bold)
    }
    TextButton(
        onClick = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
    ) {
        Text("Not now")
    }
}

@Composable
private fun RequestSent(
    title: String,
    amountNaira: Long,
    payerName: String,
    requestRef: String,
    onOpenAgain: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(accessibleOrange().copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.HourglassTop,
                contentDescription = null, // the heading below already says "awaiting"
                tint = accessibleOrange(),
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Request sent, awaiting Lodgist staff confirmation",
            modifier = Modifier.semantics {
                heading()
                liveRegion = LiveRegionMode.Polite
            },
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(10.dp))

        ReferenceRow(requestRef = requestRef)

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                ReceiptItem(label = "Request for", value = title)
                ReceiptItem(label = "Amount shown", value = formatNaira(amountNaira))
                ReceiptItem(label = "Channel", value = "WhatsApp (manual)")
                ReceiptItem(label = "Payer", value = payerName.ifBlank { "Not provided" })
                ReceiptItem(label = "Status", value = "Awaiting staff confirmation")
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "This screen doesn't update automatically. Lodgist staff will confirm on WhatsApp. $REFUND_COPY",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 15.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = onOpenAgain,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text("Ask about this request on WhatsApp", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onDismiss,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .testTag("dismiss_receipt_btn"),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = LodgistOrangeButton, contentColor = Color.White)
        ) {
            Text("Done", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ReferenceRow(requestRef: String) {
    val clipboard = LocalClipboardManager.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Request reference: $requestRef",
                fontSize = 13.sp,
                color = accessibleOrange(),
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Lodgist WhatsApp: $LODGIST_WHATSAPP_DISPLAY",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        TextButton(
            onClick = { clipboard.setText(AnnotatedString(requestRef)) },
            modifier = Modifier
                .heightIn(min = 48.dp)
                .testTag("copy_request_ref_btn")
        ) {
            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = accessibleTeal()
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Copy reference", color = accessibleTeal(), fontSize = 12.sp)
        }
    }
}

@Composable
private fun InfoRow(
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Forum,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 13.sp,
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
}

@Composable
private fun ReceiptItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}
