package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddHome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LandlordListingEntity
import com.example.ui.components.formatNaira
import com.example.ui.theme.LodgistOrange
import com.example.ui.theme.LodgistTeal

@Composable
fun ListPropertyScreen(
    submissions: List<LandlordListingEntity>,
    onSubmitProperty: (
        title: String,
        type: String,
        state: String,
        city: String,
        neighborhood: String,
        rent: Long,
        beds: Int,
        baths: Int,
        ownerName: String,
        ownerPhone: String,
        hasTitle: Boolean,
        hasNIN: Boolean,
        hasWater: Boolean,
        hasMeter: Boolean
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    var title by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("2 Bedroom Flat") }
    var selectedCity by remember { mutableStateOf("Owerri") }
    var neighborhood by remember { mutableStateOf("") }
    var annualRentText by remember { mutableStateOf("") }
    var cautionDepositText by remember { mutableStateOf("") }
    var agencyFeeText by remember { mutableStateOf("") }
    var legalFeeText by remember { mutableStateOf("") }
    var inspectionFeeText by remember { mutableStateOf("") }
    var consentToLeadReview by remember { mutableStateOf(false) }
    var isSubmittedSuccess by remember { mutableStateOf(false) }
    var bedrooms by remember { mutableStateOf("2") }
    var bathrooms by remember { mutableStateOf("2") }
    var ownerName by remember { mutableStateOf("") }
    var ownerPhone by remember { mutableStateOf("") }

    var hasTitle by remember { mutableStateOf(true) }
    var hasNIN by remember { mutableStateOf(true) }
    var hasWater by remember { mutableStateOf(true) }
    var hasMeter by remember { mutableStateOf(true) }

    var hasAttemptedSubmit by remember { mutableStateOf(false) }

    val types = listOf("Self-Contain", "1 Bedroom Flat", "2 Bedroom Flat", "3 Bedroom Flat", "Studio Apartment")
    val cities = listOf("Owerri", "Lagos", "Port Harcourt", "Abuja")

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("list_property_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Header
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(LodgistOrange.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddHome,
                                contentDescription = null,
                                tint = LodgistOrange,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "List Your Property on Lodgist",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Zero tenant scam. Direct verified renters.",
                                fontSize = 11.sp,
                                color = LodgistTeal,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Landlords who list on Lodgist only receive inquiries from pre-verified users. Our field officer will schedule a physical inspection to verify water, light, and title before listing goes live.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // The Listing Form
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "1. Property Overview",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Property Title") },
                        placeholder = { Text("e.g. Spacious 2-Bedroom Flat with POP") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("landlord_input_title"),
                        singleLine = true,
                        isError = hasAttemptedSubmit && title.isBlank()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Property Type",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        types.take(3).forEach { type ->
                            val isSel = selectedType == type
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSel) LodgistOrange else MaterialTheme.colorScheme.surface
                                    )
                                    .border(
                                        1.dp,
                                        if (isSel) LodgistOrange else MaterialTheme.colorScheme.outlineVariant,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { selectedType = type }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = type.replace(" Flat", ""),
                                    fontSize = 11.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "City",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        cities.forEach { city ->
                            val isSel = selectedCity == city
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSel) LodgistOrange else MaterialTheme.colorScheme.surface
                                    )
                                    .border(
                                        1.dp,
                                        if (isSel) LodgistOrange else MaterialTheme.colorScheme.outlineVariant,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { selectedCity = city }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = city,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = neighborhood,
                        onValueChange = { neighborhood = it },
                        label = { Text("Neighborhood & Specific Address") },
                        placeholder = { Text("e.g. Nekede Old Poly Rd or Lekki Phase 1") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("landlord_input_neighborhood"),
                        singleLine = true,
                        isError = hasAttemptedSubmit && neighborhood.isBlank()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = annualRentText,
                        onValueChange = { annualRentText = it.filter { char -> char.isDigit() } },
                        label = { Text("Annual Rent in Naira (₦)") },
                        placeholder = { Text("e.g. 1200000") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Payment, contentDescription = null)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("landlord_input_rent"),
                        singleLine = true,
                        isError = hasAttemptedSubmit && (annualRentText.toLongOrNull() ?: 0L) <= 0
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = cautionDepositText,
                            onValueChange = { cautionDepositText = it.filter { char -> char.isDigit() } },
                            label = { Text("Caution Deposit (₦)") },
                            placeholder = { Text("e.g. 100000") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("landlord_input_caution"),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = agencyFeeText,
                            onValueChange = { agencyFeeText = it.filter { char -> char.isDigit() } },
                            label = { Text("Agency / Legal Fee (₦)") },
                            placeholder = { Text("0 for Zero Fee") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("landlord_input_agency"),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = "2. Landlord / Manager Information",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = ownerName,
                        onValueChange = { ownerName = it },
                        label = { Text("Your Full Name") },
                        placeholder = { Text("e.g. Chief Emeka N.") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Person, contentDescription = null)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("landlord_input_owner_name"),
                        singleLine = true,
                        isError = hasAttemptedSubmit && ownerName.isBlank()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = ownerPhone,
                        onValueChange = { ownerPhone = it },
                        label = { Text("Phone Number for Verification Call") },
                        placeholder = { Text("e.g. 08032001199") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Phone, contentDescription = null)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("landlord_input_owner_phone"),
                        singleLine = true,
                        isError = hasAttemptedSubmit && ownerPhone.length < 8
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = "3. Lodgist TruthScore Verification Checklist",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    CheckItem(
                        text = "I possess genuine Title Documents (Deed / Consent / Root)",
                        checked = hasTitle,
                        onCheckedChange = { hasTitle = it }
                    )

                    CheckItem(
                        text = "I agree to verify my National Identity (NIN/BVN)",
                        checked = hasNIN,
                        onCheckedChange = { hasNIN = it }
                    )

                    CheckItem(
                        text = "Property has functional borehole running water",
                        checked = hasWater,
                        onCheckedChange = { hasWater = it }
                    )

                    CheckItem(
                        text = "Property has functional prepaid electricity meter",
                        checked = hasMeter,
                        onCheckedChange = { hasMeter = it }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    CheckItem(
                        text = "I confirm this is an advertiser lead for Lodgist review. Review-for-display is not verification; payment never buys a verification badge.",
                        checked = consentToLeadReview,
                        onCheckedChange = { consentToLeadReview = it }
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    if (isSubmittedSuccess) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = LodgistTeal.copy(alpha = 0.15f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "Lead Submitted Successfully",
                                    fontWeight = FontWeight.Bold,
                                    color = LodgistTeal,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Our Owerri field inspector will review your property details. Note: A submitted lead is not yet a published home. For quick assistance, contact WhatsApp +2349136094315.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    Button(
                        onClick = {
                            hasAttemptedSubmit = true
                            val rent = annualRentText.toLongOrNull() ?: 0L
                            if (title.isNotBlank() && neighborhood.isNotBlank() && rent > 0 &&
                                ownerName.isNotBlank() && ownerPhone.length >= 8 && consentToLeadReview
                            ) {
                                isSubmittedSuccess = true
                                onSubmitProperty(
                                    title,
                                    selectedType,
                                    when (selectedCity) {
                                        "Owerri" -> "Imo State"
                                        "Lagos" -> "Lagos State"
                                        "Port Harcourt" -> "Rivers State"
                                        else -> "Abuja FCT"
                                    },
                                    selectedCity,
                                    neighborhood,
                                    rent,
                                    bedrooms.toIntOrNull() ?: 2,
                                    bathrooms.toIntOrNull() ?: 2,
                                    ownerName,
                                    ownerPhone,
                                    hasTitle,
                                    hasNIN,
                                    hasWater,
                                    hasMeter
                                )
                            }
                        },
                        enabled = !isSubmittedSuccess,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("submit_landlord_listing_btn"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LodgistOrange)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Submit for TruthScore Audit",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Past Submissions Section
        if (submissions.isNotEmpty()) {
            item {
                Text(
                    text = "My Submitted Properties (${submissions.size})",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            items(submissions, key = { it.id }) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
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
                                text = item.title,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(LodgistOrange.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.HourglassTop,
                                    contentDescription = null,
                                    tint = LodgistOrange,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Audit In Progress",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = LodgistOrange
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "${item.propertyType} • ${item.neighborhood}, ${item.city}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Listed Rent: ${formatNaira(item.annualRent)}/yr",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = LodgistOrange
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CheckItem(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(checkedColor = LodgistOrange)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
