package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddHome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.AddHome
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LodgistOrange
import com.example.ui.viewmodel.ScreenDestination

@Composable
fun LodgistBottomBar(
    currentDestination: ScreenDestination,
    onNavigate: (ScreenDestination) -> Unit,
    activeInspectionCount: Int = 0,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier.testTag("lodgist_bottom_navigation"),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    ) {
        // 1. Explore
        NavigationBarItem(
            selected = currentDestination == ScreenDestination.EXPLORE,
            onClick = { onNavigate(ScreenDestination.EXPLORE) },
            icon = {
                Icon(
                    imageVector = if (currentDestination == ScreenDestination.EXPLORE) Icons.Filled.Home else Icons.Outlined.Home,
                    contentDescription = "Explore"
                )
            },
            label = { Text("Explore", fontSize = 11.sp) },
            modifier = Modifier.testTag("nav_item_explore"),
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = LodgistOrange,
                selectedTextColor = LodgistOrange,
                indicatorColor = LodgistOrange.copy(alpha = 0.15f)
            )
        )

        // 2. Inspections
        NavigationBarItem(
            selected = currentDestination == ScreenDestination.INSPECTIONS,
            onClick = { onNavigate(ScreenDestination.INSPECTIONS) },
            icon = {
                BadgedBox(
                    badge = {
                        if (activeInspectionCount > 0) {
                            Badge { Text("$activeInspectionCount") }
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (currentDestination == ScreenDestination.INSPECTIONS) Icons.Filled.CalendarMonth else Icons.Outlined.CalendarMonth,
                        contentDescription = "Inspections"
                    )
                }
            },
            label = { Text("Inspections", fontSize = 11.sp) },
            modifier = Modifier.testTag("nav_item_inspections"),
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = LodgistOrange,
                selectedTextColor = LodgistOrange,
                indicatorColor = LodgistOrange.copy(alpha = 0.15f)
            )
        )

        // 3. Payment requests (awaiting Lodgist staff confirmation)
        NavigationBarItem(
            selected = currentDestination == ScreenDestination.ESCROW_VAULT,
            onClick = { onNavigate(ScreenDestination.ESCROW_VAULT) },
            icon = {
                Icon(
                    imageVector = if (currentDestination == ScreenDestination.ESCROW_VAULT) Icons.Filled.AccountBalanceWallet else Icons.Outlined.AccountBalanceWallet,
                    contentDescription = "Payment requests"
                )
            },
            label = { Text("Payments", fontSize = 11.sp) },
            modifier = Modifier.testTag("nav_item_escrow"),
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = LodgistOrange,
                selectedTextColor = LodgistOrange,
                indicatorColor = LodgistOrange.copy(alpha = 0.15f)
            )
        )

        // 4. List House (Landlord)
        NavigationBarItem(
            selected = currentDestination == ScreenDestination.LIST_PROPERTY,
            onClick = { onNavigate(ScreenDestination.LIST_PROPERTY) },
            icon = {
                Icon(
                    imageVector = if (currentDestination == ScreenDestination.LIST_PROPERTY) Icons.Filled.AddHome else Icons.Outlined.AddHome,
                    contentDescription = "List Property"
                )
            },
            label = { Text("List House", fontSize = 11.sp) },
            modifier = Modifier.testTag("nav_item_list_property"),
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = LodgistOrange,
                selectedTextColor = LodgistOrange,
                indicatorColor = LodgistOrange.copy(alpha = 0.15f)
            )
        )

        // 5. Trust & TruthScore
        NavigationBarItem(
            selected = currentDestination == ScreenDestination.TRUST_SAFETY,
            onClick = { onNavigate(ScreenDestination.TRUST_SAFETY) },
            icon = {
                Icon(
                    imageVector = if (currentDestination == ScreenDestination.TRUST_SAFETY) Icons.Filled.Shield else Icons.Outlined.Shield,
                    contentDescription = "Trust & Safety"
                )
            },
            label = { Text("TruthScore", fontSize = 11.sp) },
            modifier = Modifier.testTag("nav_item_trust"),
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = LodgistOrange,
                selectedTextColor = LodgistOrange,
                indicatorColor = LodgistOrange.copy(alpha = 0.15f)
            )
        )
    }
}
