package com.example.ui.components

import com.example.data.model.PropertyEntity
import java.util.UUID

/**
 * LM-01 honesty helpers. Lodgist payments are manual-first: Lodgist staff confirm on WhatsApp and the
 * admin dashboard is the source of truth. Nothing in the app may claim a payment, booking or check
 * happened unless the server says so.
 */

const val LODGIST_WHATSAPP_DISPLAY = "+234 913 609 4315"

const val REFUND_COPY =
    "Refunds, if any, are reviewed case by case by Lodgist staff. " +
        "Contact Lodgist on WhatsApp with your request reference."

const val SAMPLE_EXPLANATION =
    "This is an example to show how Lodgist works. Browse live listings or ask Lodgist on WhatsApp."

const val SAVED_LOCALLY_COPY = "Saved on this phone, send via WhatsApp to submit"

/** Only API-backed VERIFIED listings can be booked or paid for. Samples and unverified listings cannot. */
fun isBookableOrPayable(verificationStatus: String): Boolean = verificationStatus == "VERIFIED"

/** Label for the disabled book/pay button when a listing is not bookable. */
fun notBookableLabel(verificationStatus: String): String =
    if (verificationStatus == "SAMPLE") "Sample listing — not available to book"
    else "Not yet verified — ask Lodgist"

/** TruthScore and audit results are shown only when backed by data (a score above 0). */
fun hasAuditData(property: PropertyEntity): Boolean = property.truthScore > 0

/**
 * Makes untrusted text (e.g. a server listing title) safe to place inside a prefilled WhatsApp
 * message: removes control characters and line breaks, collapses whitespace, caps the length.
 */
fun sanitizeForWhatsApp(text: String, maxLength: Int = 80): String {
    val cleaned = buildString {
        for (ch in text) {
            append(if (ch.isISOControl() || ch == '\u2028' || ch == '\u2029' || Character.getType(ch) == Character.FORMAT.toInt()) ' ' else ch)
        }
    }.replace(Regex("\\s+"), " ").trim()
    return if (cleaned.length <= maxLength) cleaned else cleaned.take(maxLength - 1).trimEnd() + "…"
}

/** Random, non-sequential local reference. Not proof of payment; staff confirm on WhatsApp. */
fun newRequestRef(prefix: String = "LDG-REQ"): String =
    "$prefix-" + UUID.randomUUID().toString().replace("-", "").take(8).uppercase()

/** Display label for a payment record purpose (DB codes are kept for compatibility). */
fun paymentPurposeLabel(purpose: String): String = when (purpose) {
    "ANNUAL_RENT_ESCROW" -> "Rent & move-in costs"
    "INSPECTION_TOKEN" -> "Inspection fee"
    "EXPRESS_VERIFICATION" -> "Verification fee"
    "ADVERTISER_LEAD_AUDIT" -> "Listing review (no payment taken)"
    else -> "Payment request"
}

/**
 * Display label for a payment record status. Legacy local states from older builds
 * (HELD_IN_ESCROW, RELEASED_TO_LANDLORD, REFUNDED, ...) were never server-backed, so they are shown
 * as unknown. The stored value is left untouched (display only, no data loss).
 */
fun paymentStatusLabel(status: String): String = when (status) {
    "PENDING_PAYMENT" -> "Awaiting staff confirmation"
    "AWAITING_PROOF" -> "Awaiting proof of payment"
    else -> "Status unknown — contact Lodgist"
}

/** Display label for an inspection request status. Legacy local CONFIRMED rows are shown as unknown. */
fun inspectionStatusLabel(status: String): String = when (status) {
    "INSPECTION_PENDING" -> SAVED_LOCALLY_COPY
    else -> "Status unknown — contact Lodgist"
}

/** Older builds stored an invented officer name; show the honest placeholder instead. */
fun inspectionAgentLabel(assignedAgent: String): String =
    if (assignedAgent.contains("Chidi", ignoreCase = true) || assignedAgent.isBlank())
        "Awaiting Lodgist staff assignment"
    else assignedAgent
