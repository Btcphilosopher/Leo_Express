package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.*
import com.example.ui.theme.LeoBlack
import com.example.ui.theme.LeoDarkGray
import com.example.ui.theme.LeoGold
import com.example.ui.theme.LeoMediumGray
import com.example.ui.theme.LeoOrange
import com.example.ui.theme.LeoOrangeLight
import com.example.ui.theme.LeoRed
import com.example.ui.theme.LeoGreen
import com.example.ui.translation.Translations
import com.example.ui.viewmodel.LeoExpressViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeoExpressApp(
    viewModel: LeoExpressViewModel,
    modifier: Modifier = Modifier
) {
    val profile by viewModel.userProfile.collectAsState()
    val stations by viewModel.stations.collectAsState()
    val routes by viewModel.routes.collectAsState()
    val bookings by viewModel.bookings.collectAsState()
    val passengers by viewModel.passengers.collectAsState()
    val payments by viewModel.paymentMethods.collectAsState()
    val notifications by viewModel.notifications.collectAsState()

    // Lang dictionary helper
    val lang = profile.preferredLanguage
    fun t(key: String): String = Translations.get(lang, key)

    // Current tab navigation (0 = Search, 1 = My Tickets, 2 = Map & Board, 3 = Leo Club, 4 = Settings & Admin)
    var selectedTab by remember { mutableIntStateOf(0) }

    // Secondary UI States (Detail views / overlays)
    var currentSubScreen by remember { mutableStateOf<SubScreen?>(null) }

    // Navigation Shell
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            if (currentSubScreen == null) {
                val isDark = MaterialTheme.colorScheme.surface != Color.White
                val headerBg = if (isDark) Color(0xFF1E1E1E) else Color.White
                val headerBorderColor = if (isDark) Color(0xFF2E2E2E) else Color(0xFFF1F5F9)
                val textColor = if (isDark) Color.White else Color(0xFF1C1B1F)

                Column(modifier = Modifier.fillMaxWidth().background(headerBg)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Logo + Text group
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(if (isDark) Color(0xFFF39200) else Color.Black, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "LE",
                                    color = if (isDark) Color.Black else Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp
                                )
                            }
                            Text(
                                text = "Leo Express",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                letterSpacing = (-0.5).sp,
                                color = textColor
                            )
                        }

                        // Two geometric badges
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Left geometric badge: 40dp circle, light bg with a circular ring
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(if (isDark) Color(0xFF2E2E2E) else Color(0xFFF1F5F9), RoundedCornerShape(50)),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .border(2.dp, if (isDark) Color(0xFF94A3B8) else Color(0xFF475569), RoundedCornerShape(50))
                                )
                            }

                            // Right geometric badge: Orange solid circle with white borders and inner dot
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color(0xFFF39200), RoundedCornerShape(50))
                                    .border(2.dp, if (isDark) Color.Black else Color.White, RoundedCornerShape(50)),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .background(if (isDark) Color.Black else Color.White, RoundedCornerShape(50))
                                )
                            }
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(headerBorderColor)
                    )
                }
            }
        },
        bottomBar = {
            val isDark = MaterialTheme.colorScheme.surface != Color.White
            val navBgColor = if (isDark) Color(0xFF1E1E1E) else Color.White
            val navBorderColor = if (isDark) Color(0xFF2E2E2E) else Color(0xFFF1F5F9)
            val selectedActiveColor = if (isDark) Color(0xFFF39200) else Color.Black
            val inactiveColor = if (isDark) Color(0xFF808080) else Color(0xFF94A3B8)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(navBgColor)
            ) {
                // Top border line
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(navBorderColor)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .navigationBarsPadding(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Item 0: Search
                    val searchSelected = selectedTab == 0 && currentSubScreen == null
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                selectedTab = 0
                                currentSubScreen = null
                            }
                            .padding(vertical = 8.dp)
                            .testTag("nav_search"),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .then(
                                    if (searchSelected) {
                                        Modifier.background(selectedActiveColor, RoundedCornerShape(6.dp))
                                    } else {
                                        Modifier.border(2.dp, inactiveColor, RoundedCornerShape(6.dp))
                                    }
                                )
                        )
                        Text(
                            text = t("journey_planner").take(10).uppercase(),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (searchSelected) selectedActiveColor else inactiveColor
                        )
                    }

                    // Item 1: Tickets
                    val ticketsSelected = selectedTab == 1 && currentSubScreen == null
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                selectedTab = 1
                                currentSubScreen = null
                            }
                            .padding(vertical = 8.dp)
                            .testTag("nav_tickets"),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .then(
                                    if (ticketsSelected) {
                                        Modifier.background(selectedActiveColor, RoundedCornerShape(4.dp))
                                    } else {
                                        Modifier.border(2.dp, inactiveColor, RoundedCornerShape(4.dp))
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(12.dp)
                                    .height(2.dp)
                                    .background(if (ticketsSelected) navBgColor else inactiveColor)
                            )
                        }
                        Text(
                            text = t("my_tickets").take(10).uppercase(),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (ticketsSelected) selectedActiveColor else inactiveColor
                        )
                    }

                    // Item 2: Timetable / Board
                    val timetableSelected = selectedTab == 2 && currentSubScreen == null
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                selectedTab = 2
                                currentSubScreen = null
                            }
                            .padding(vertical = 8.dp)
                            .testTag("nav_timetable"),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .then(
                                    if (timetableSelected) {
                                        Modifier.background(selectedActiveColor, RoundedCornerShape(12.dp))
                                    } else {
                                        Modifier.border(2.dp, inactiveColor, RoundedCornerShape(12.dp))
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(if (timetableSelected) navBgColor else inactiveColor, RoundedCornerShape(50))
                            )
                        }
                        Text(
                            text = t("timetable").take(10).uppercase(),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (timetableSelected) selectedActiveColor else inactiveColor
                        )
                    }

                    // Item 3: Rewards
                    val rewardsSelected = selectedTab == 3 && currentSubScreen == null
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                selectedTab = 3
                                currentSubScreen = null
                            }
                            .padding(vertical = 8.dp)
                            .testTag("nav_rewards"),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .border(2.dp, if (rewardsSelected) selectedActiveColor else inactiveColor, RoundedCornerShape(50)),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .border(1.5.dp, if (rewardsSelected) selectedActiveColor else inactiveColor, RoundedCornerShape(50))
                            )
                        }
                        Text(
                            text = t("loyalty_rewards").take(10).uppercase(),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (rewardsSelected) selectedActiveColor else inactiveColor
                        )
                    }

                    // Item 4: Settings / Profile
                    val settingsSelected = selectedTab == 4 && currentSubScreen == null
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                selectedTab = 4
                                currentSubScreen = null
                            }
                            .padding(vertical = 8.dp)
                            .testTag("nav_settings"),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .border(2.dp, if (settingsSelected) selectedActiveColor else inactiveColor, RoundedCornerShape(50)),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(if (settingsSelected) selectedActiveColor else inactiveColor, RoundedCornerShape(50))
                            )
                        }
                        Text(
                            text = t("settings").take(10).uppercase(),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (settingsSelected) selectedActiveColor else inactiveColor
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            AnimatedContent(
                targetState = currentSubScreen ?: selectedTab,
                transitionSpec = {
                    slideInHorizontally { width -> width } + fadeIn() togetherWith
                            slideOutHorizontally { width -> -width } + fadeOut()
                },
                label = "MainAnimation"
            ) { target ->
                when (target) {
                    // --- Base Tabs ---
                    0 -> SearchTab(viewModel, stations, t = { t(it) }, profile = profile) {
                        currentSubScreen = SubScreen.SeatSelection
                    }
                    1 -> MyTicketsTab(bookings, stations, t = { t(it) }, profile = profile) { booking ->
                        viewModel.cancelTicket(booking)
                    }
                    2 -> MapBoardTab(stations, t = { t(it) })
                    3 -> LeoClubTab(
                        profile, passengers, payments, notifications, t = { t(it) },
                        onAddPassenger = { currentSubScreen = SubScreen.AddPassenger },
                        onAddCard = { currentSubScreen = SubScreen.AddCard },
                        onDeletePassenger = { viewModel.deletePassenger(it) },
                        onDeleteCard = { viewModel.deletePaymentMethod(it) },
                        onMarkNotifRead = { viewModel.markNotificationAsRead(it) },
                        onDeleteNotif = { viewModel.deleteNotification(it) }
                    )
                    4 -> SettingsTab(
                        viewModel, profile, routes, bookings, stations, t = { t(it) },
                        onAddRoute = { from, to, dep, arr, type, price, stops ->
                            viewModel.addRoute(from, to, dep, arr, type, price, stops)
                        },
                        onDeleteRoute = { viewModel.removeRoute(it) },
                        onSendNotif = { title, msg, type ->
                            viewModel.sendSystemNotification(title, msg, type)
                        }
                    )

                    // --- Detail Sub-Screens ---
                    SubScreen.SeatSelection -> {
                        val selectedRoute by viewModel.selectedRouteForBooking.collectAsState()
                        val seat by viewModel.selectedSeat.collectAsState()
                        val carriage by viewModel.selectedCarriage.collectAsState()

                        selectedRoute?.let { route ->
                            SeatSelectionScreen(
                                route = route,
                                selectedSeat = seat,
                                selectedCarriage = carriage,
                                t = { t(it) },
                                onSeatSelected = { s, c -> viewModel.setSeat(s, c) },
                                onBack = { currentSubScreen = null },
                                onConfirm = { currentSubScreen = SubScreen.PaymentConfirm }
                            )
                        } ?: run { currentSubScreen = null }
                    }
                    SubScreen.PaymentConfirm -> {
                        val selectedRoute by viewModel.selectedRouteForBooking.collectAsState()
                        val seat by viewModel.selectedSeat.collectAsState()
                        val carriage by viewModel.selectedCarriage.collectAsState()

                        selectedRoute?.let { route ->
                            PaymentConfirmationScreen(
                                route = route,
                                seat = seat ?: "24B",
                                carriage = carriage,
                                payments = payments,
                                profile = profile,
                                t = { t(it) },
                                onBack = { currentSubScreen = SubScreen.SeatSelection },
                                onPay = { passengerName ->
                                    viewModel.completeBooking(passengerName)
                                    currentSubScreen = SubScreen.PaymentSuccess
                                }
                            )
                        } ?: run { currentSubScreen = null }
                    }
                    SubScreen.PaymentSuccess -> {
                        PaymentSuccessScreen(t = { t(it) }) {
                            currentSubScreen = null
                            selectedTab = 1 // Go to My Tickets to see the new ticket!
                        }
                    }
                    SubScreen.AddPassenger -> {
                        AddPassengerScreen(
                            t = { t(it) },
                            onBack = { currentSubScreen = null },
                            onSave = { firstName, lastName, discount ->
                                viewModel.addPassenger(firstName, lastName, discount)
                                currentSubScreen = null
                            }
                        )
                    }
                    SubScreen.AddCard -> {
                        AddCardScreen(
                            t = { t(it) },
                            onBack = { currentSubScreen = null },
                            onSave = { holder, number, expiry, type ->
                                viewModel.addPaymentMethod(holder, number, expiry, type)
                                currentSubScreen = null
                            }
                        )
                    }
                }
            }
        }
    }
}

sealed interface SubScreen {
    object SeatSelection : SubScreen
    object PaymentConfirm : SubScreen
    object PaymentSuccess : SubScreen
    object AddPassenger : SubScreen
    object AddCard : SubScreen
}

// Helper to translate station code to readable name
fun getStationName(code: String, stations: List<StationEntity>): String {
    return stations.find { it.id == code }?.name ?: code
}

fun formatPrice(priceCzk: Double, profile: UserProfile): String {
    return if (profile.currency == "EUR") {
        String.format(Locale.US, "€%.2f", priceCzk / 25.0)
    } else {
        "${priceCzk.toInt()} CZK"
    }
}

// ==========================================
// 1. SEARCH & JOURNEY PLANNER TAB
// ==========================================
@Composable
fun SearchTab(
    viewModel: LeoExpressViewModel,
    stations: List<StationEntity>,
    t: (String) -> String,
    profile: UserProfile,
    onNavigateToSeatSelection: () -> Unit
) {
    val searchFrom by viewModel.searchFrom.collectAsState()
    val searchTo by viewModel.searchTo.collectAsState()
    val searchDate by viewModel.searchDate.collectAsState()
    val passengerCount by viewModel.passengerCount.collectAsState()
    val searchResults by viewModel.searchResultRoutes.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()

    var showFromPicker by remember { mutableStateOf(false) }
    var showToPicker by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Image
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clip(RoundedCornerShape(16.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_train_coach),
                    contentDescription = "Leo Express Train & Coach",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                // Black overlay gradient
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                            )
                        )
                )
                Text(
                    text = "Travel Central Europe",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                )
            }
        }

        // Search Form Box
        item {
            val isDark = MaterialTheme.colorScheme.surface != Color.White
            val surfaceCardBg = if (isDark) Color(0xFF1E1E1E) else Color.White
            val fieldBg = if (isDark) Color(0xFF2E2E2E) else Color(0xFFF8F9FF)
            val borderColor = if (isDark) Color(0xFF3E3E3E) else Color(0xFFE2E8F0)
            val primaryTextColor = if (isDark) Color.White else Color(0xFF1C1B1F)

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = surfaceCardBg),
                shape = RoundedCornerShape(32.dp),
                border = BorderStroke(1.dp, borderColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Title
                    Text(
                        text = t("journey_planner"),
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        letterSpacing = (-0.5).sp,
                        color = primaryTextColor
                    )

                    // Capsule Switcher (Train vs Coach filter)
                    var selectedTransportType by remember { mutableStateOf("ALL") }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isDark) Color(0xFF2E2E2E) else Color(0xFFF1F5F9), RoundedCornerShape(16.dp))
                            .padding(4.dp)
                    ) {
                        listOf("ALL", "TRAIN", "COACH").forEach { type ->
                            val isSelected = selectedTransportType == type
                            val isTrain = type == "TRAIN"
                            val isCoach = type == "COACH"
                            
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) (if (isDark) Color(0xFFF39200) else Color.White) else Color.Transparent)
                                    .clickable { selectedTransportType = type }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .background(if (isDark) Color.Black else Color.Black, RoundedCornerShape(50))
                                        )
                                    }
                                    Text(
                                        text = if (isTrain) t("train") else if (isCoach) t("coach") else "All",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (isSelected) Color.Black else Color.Gray
                                    )
                                }
                            }
                        }
                    }

                    // ROUTING FIELD WITH GEOMETRIC LAYOUT
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // FROM field
                            OutlinedButton(
                                onClick = { showFromPicker = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(60.dp)
                                    .testTag("from_station_picker"),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, borderColor),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = fieldBg,
                                    contentColor = primaryTextColor
                                )
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Geometric Hollow Circle
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .border(2.dp, Color(0xFF94A3B8), RoundedCornerShape(50))
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(t("from").uppercase(), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                        Text(
                                            text = getStationName(searchFrom, stations),
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 15.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            // TO field
                            OutlinedButton(
                                onClick = { showToPicker = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(60.dp)
                                    .testTag("to_station_picker"),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, borderColor),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = fieldBg,
                                    contentColor = primaryTextColor
                                )
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Geometric Solid Circle
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .background(if (isDark) Color.White else Color.Black, RoundedCornerShape(50))
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(t("to").uppercase(), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                        Text(
                                            text = getStationName(searchTo, stations),
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 15.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }

                        // Perfect swap button in the center right
                        IconButton(
                            onClick = { viewModel.swapStations() },
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 16.dp)
                                .size(38.dp)
                                .background(if (isDark) Color(0xFF1E1E1E) else Color.White, RoundedCornerShape(50))
                                .border(1.dp, borderColor, RoundedCornerShape(50))
                        ) {
                            Icon(
                                Icons.Default.SwapVert,
                                contentDescription = "Swap",
                                tint = primaryTextColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // DATE picker & Passenger details in a clean responsive grid row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Date selector
                        OutlinedTextField(
                            value = searchDate,
                            onValueChange = { viewModel.setDate(it) },
                            label = { Text(t("select_date").uppercase(), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Gray) },
                            modifier = Modifier
                                .weight(1.3f)
                                .testTag("date_input"),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = fieldBg,
                                unfocusedContainerColor = fieldBg,
                                focusedBorderColor = borderColor,
                                unfocusedBorderColor = borderColor,
                                focusedTextColor = primaryTextColor,
                                unfocusedTextColor = primaryTextColor
                            ),
                            trailingIcon = { Icon(Icons.Default.DateRange, contentDescription = null, tint = Color.Gray) }
                        )

                        // Passenger Count inside a beautiful geometric card
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            border = BorderStroke(1.dp, borderColor),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = fieldBg)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { viewModel.updatePassengerCount(passengerCount - 1) }) {
                                    Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(16.dp))
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = passengerCount.toString(), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = primaryTextColor)
                                    Text(text = t("select_passengers").take(10), fontSize = 8.sp, color = Color.Gray)
                                }
                                IconButton(onClick = { viewModel.updatePassengerCount(passengerCount + 1) }) {
                                    Icon(Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    // SEARCH Connections Button (Sleek bold black button matching theme)
                    Button(
                        onClick = { viewModel.performSearch() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("search_connections_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDark) Color(0xFFF39200) else Color(0xFF1C1B1F),
                            contentColor = if (isDark) Color.Black else Color.White
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.DirectionsTransit, contentDescription = null)
                            Text(t("search_trips"), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
            }
        }

        // Station Picker dialog simulation
        if (showFromPicker) {
            item {
                StationPickerDialog(
                    title = t("from"),
                    stations = stations,
                    onDismiss = { showFromPicker = false },
                    onSelect = {
                        viewModel.setFromStation(it)
                        showFromPicker = false
                    }
                )
            }
        }

        if (showToPicker) {
            item {
                StationPickerDialog(
                    title = t("to"),
                    stations = stations,
                    onDismiss = { showToPicker = false },
                    onSelect = {
                        viewModel.setToStation(it)
                        showToPicker = false
                    }
                )
            }
        }

        // Search Results Section
        item {
            Text(
                text = t("timetable") + " (${searchResults.size})",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (isSearching) {
            item {
                Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = LeoOrange)
                }
            }
        } else if (searchResults.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Train, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.Gray)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No connections found.",
                            fontWeight = FontWeight.Medium,
                            color = Color.Gray
                        )
                        Text(
                            text = "Try searching Prague (PRG) to Ostrava (OSV) for trains, or Prague to Vienna (VIE) for coaches!",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        } else {
            items(searchResults) { route ->
                RouteResultCard(
                    route = route,
                    stations = stations,
                    passengerCount = passengerCount,
                    profile = profile,
                    t = { t(it) },
                    onSelect = {
                        viewModel.selectRouteForBooking(route)
                        onNavigateToSeatSelection()
                    }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun StationPickerDialog(
    title: String,
    stations: List<StationEntity>,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, LeoOrange, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = LeoDarkGray),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$title: Select Station",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = LeoOrange
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }
            Divider(color = Color.Gray.copy(alpha = 0.3f), modifier = Modifier.padding(bottom = 8.dp))
            stations.forEach { station ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(station.id) }
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = station.name, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(text = "${station.city}, ${station.country}", fontSize = 12.sp, color = Color.Gray)
                    }
                    Badge(containerColor = if (station.isCoachStop) LeoGold else LeoOrange) {
                        Text(
                            text = if (station.isCoachStop) "COACH" else "TRAIN",
                            color = LeoBlack,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }
                Divider(color = Color.Gray.copy(alpha = 0.1f))
            }
        }
    }
}

@Composable
fun RouteResultCard(
    route: RouteEntity,
    stations: List<StationEntity>,
    passengerCount: Int,
    profile: UserProfile,
    t: (String) -> String,
    onSelect: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.surface != Color.White
    val cardBg = if (isDark) Color(0xFF1E1E1E) else Color.White
    val borderColor = if (isDark) Color(0xFF2E2E2E) else Color(0xFFE2E8F0)
    val textColor = if (isDark) Color.White else Color(0xFF1C1B1F)
    val badgeBg = if (isDark) Color(0xFF2E2E2E) else Color(0xFFF1F5F9)
    val badgeText = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, borderColor),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (route.type == "TRAIN") Icons.Default.Train else Icons.Default.DirectionsBus,
                        contentDescription = null,
                        tint = Color(0xFFF39200),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = if (route.type == "TRAIN") "Leo Rail LE" else "Leo Coach LC",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = textColor
                    )
                }

                // Comfort class indicator
                Box(
                    modifier = Modifier
                        .background(badgeBg, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Standard / Premium",
                        color = badgeText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Dep / Arr Timing Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.Start) {
                    Text(text = route.departureTime, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = textColor)
                    Text(text = getStationName(route.fromStationId, stations), fontSize = 12.sp, color = Color.Gray)
                }

                // Line separator with travel duration
                Column(
                    modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "4h 20m", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Start point
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .border(1.5.dp, Color(0xFF94A3B8), RoundedCornerShape(50))
                        )
                        // Line
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(1.5.dp)
                                .background(Color(0xFF94A3B8).copy(alpha = 0.5f))
                        )
                        // End point
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(if (isDark) Color.White else Color.Black, RoundedCornerShape(50))
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(text = route.arrivalTime, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = textColor)
                    Text(text = getStationName(route.toStationId, stations), fontSize = 12.sp, color = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Stops subtext
            if (route.stops.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                    Text(
                        text = "${t("stops")}: ${route.stops}",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = borderColor
            )

            // Price & Book Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = t("price") + " (${passengerCount}x)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                    Text(
                        text = formatPrice(route.priceCzk * passengerCount, profile),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        color = Color(0xFFF39200)
                    )
                }

                Button(
                    onClick = { onSelect() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) Color(0xFFF39200) else Color.Black,
                        contentColor = if (isDark) Color.Black else Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Text(text = t("book_now"), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

// ==========================================
// 2. SEAT SELECTION COMPONENT
// ==========================================
@Composable
fun SeatSelectionScreen(
    route: RouteEntity,
    selectedSeat: String?,
    selectedCarriage: String,
    t: (String) -> String,
    onSeatSelected: (String, String) -> Unit,
    onBack: () -> Unit,
    onConfirm: () -> Unit
) {
    var activeCarriage by remember { mutableStateOf(selectedCarriage) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LeoBlack)
            .padding(16.dp)
    ) {
        // Top Back Navigation
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text(
                text = t("seat_selection"),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Carriage Selection Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val carriageOptions = if (route.type == "TRAIN") {
                listOf("Car 1 (Economy)", "Car 2 (Economy)", "Car 3 (Premium)")
            } else {
                listOf("Bus 1")
            }

            carriageOptions.forEach { car ->
                val isSelected = activeCarriage == car
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { activeCarriage = car },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) LeoOrange else LeoMediumGray
                    )
                ) {
                    Box(modifier = Modifier.padding(10.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = car,
                            color = if (isSelected) LeoBlack else Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = t("select_seat_instruction"),
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Legends (Available, Selected, Taken)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(modifier = Modifier.size(16.dp).background(LeoOrange, RoundedCornerShape(4.dp)))
                Text("Available", color = Color.White, fontSize = 11.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(modifier = Modifier.size(16.dp).background(LeoGreen, RoundedCornerShape(4.dp)))
                Text("Selected", color = Color.White, fontSize = 11.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(modifier = Modifier.size(16.dp).background(Color.Gray, RoundedCornerShape(4.dp)))
                Text("Occupied", color = Color.White, fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Visual Seat Map Carriage Layout (Interactive Box)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .border(2.dp, Color.Gray.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                .background(LeoDarkGray)
                .padding(16.dp)
        ) {
            // Simulated Coach/Train Seat Grid
            // Rows 1 to 10, Columns A, B, (Aisle), C, D
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Generates 40 seats
                items(50) { index ->
                    val row = (index / 5) + 1
                    val colIndex = index % 5
                    if (colIndex == 2) {
                        // AISLE column
                        Box(
                            modifier = Modifier.size(36.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = row.toString(), color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        val colLetter = when (colIndex) {
                            0 -> "A"
                            1 -> "B"
                            3 -> "C"
                            else -> "D"
                        }
                        val seatName = "$row$colLetter"
                        // Seed mock taken state (deterministic-looking pseudorandom taken seats)
                        val isOccupied = (row + colIndex) % 3 == 0 && seatName != selectedSeat
                        val isSelected = seatName == selectedSeat

                        val cardColor = when {
                            isSelected -> LeoGreen
                            isOccupied -> Color.Gray
                            else -> LeoOrange
                        }

                        Card(
                            modifier = Modifier
                                .size(36.dp)
                                .clickable(enabled = !isOccupied) {
                                    onSeatSelected(seatName, activeCarriage)
                                }
                                .testTag("seat_$seatName"),
                            colors = CardDefaults.cardColors(containerColor = cardColor),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = colLetter,
                                    color = if (isOccupied) Color.LightGray else LeoBlack,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Selected Seat Footer
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = "Selected Seat", color = Color.Gray, fontSize = 12.sp)
                Text(
                    text = if (selectedSeat != null) "$activeCarriage, $selectedSeat" else "None",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White
                )
            }

            Button(
                onClick = onConfirm,
                enabled = selectedSeat != null,
                colors = ButtonDefaults.buttonColors(containerColor = LeoOrange, contentColor = LeoBlack),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Confirm Seat", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}

// ==========================================
// 3. PAYMENT CONFIRMATION SCREEN
// ==========================================
@Composable
fun PaymentConfirmationScreen(
    route: RouteEntity,
    seat: String,
    carriage: String,
    payments: List<PaymentMethodEntity>,
    profile: UserProfile,
    t: (String) -> String,
    onBack: () -> Unit,
    onPay: (String) -> Unit
) {
    var passengerName by remember { mutableStateOf(profile.fullName) }
    var selectedPaymentCardId by remember { mutableStateOf(payments.firstOrNull()?.id ?: 0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LeoBlack)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Back
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text(t("confirm_booking"), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Invoice ticket summary card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = LeoDarkGray),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Journey Details", color = LeoOrange, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (route.type == "TRAIN") Icons.Default.Train else Icons.Default.DirectionsBus,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "${route.fromStationId} ➔ ${route.toStationId}",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 16.sp
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Departure Time:", color = Color.Gray, fontSize = 12.sp)
                    Text("${route.departureTime} (On Time)", color = Color.White, fontWeight = FontWeight.Medium, fontSize = 12.sp)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Seat / Carriage:", color = Color.Gray, fontSize = 12.sp)
                    Text("$carriage, Seat $seat", color = Color.White, fontWeight = FontWeight.Medium, fontSize = 12.sp)
                }

                Divider(color = Color.Gray.copy(alpha = 0.2f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Total Price:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(
                        text = formatPrice(route.priceCzk, profile),
                        color = LeoOrange,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Passenger Name edit field
        Text("Passenger Name", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = passengerName,
            onValueChange = { passengerName = it },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("passenger_name_input"),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = LeoOrange,
                unfocusedBorderColor = Color.Gray
            )
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Saved Credit Card Selection
        Text(t("saved_payments"), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(8.dp))

        if (payments.isEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { /* Fast simulate simple card addition */ },
                colors = CardDefaults.cardColors(containerColor = LeoMediumGray),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.AddCard, contentDescription = null, tint = LeoOrange)
                    Text("Please add a saved card in the Leo Club tab, or pay with simulated wallet.", color = Color.LightGray, fontSize = 12.sp)
                }
            }
        } else {
            payments.forEach { card ->
                val isSelected = selectedPaymentCardId == card.id
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { selectedPaymentCardId = card.id }
                        .border(
                            width = if (isSelected) 2.dp else 0.dp,
                            color = if (isSelected) LeoOrange else Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        ),
                    colors = CardDefaults.cardColors(containerColor = LeoDarkGray)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CreditCard, contentDescription = null, tint = LeoOrange)
                            Column {
                                Text(card.maskedNumber, color = Color.White, fontWeight = FontWeight.Bold)
                                Text(card.cardHolder, color = Color.Gray, fontSize = 12.sp)
                            }
                        }
                        RadioButton(
                            selected = isSelected,
                            onClick = { selectedPaymentCardId = card.id },
                            colors = RadioButtonDefaults.colors(selectedColor = LeoOrange, unselectedColor = Color.Gray)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        // Pay Button
        Button(
            onClick = { onPay(passengerName) },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("pay_confirm_button"),
            colors = ButtonDefaults.buttonColors(containerColor = LeoOrange, contentColor = LeoBlack),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Lock, contentDescription = null)
                Text("Secure Payment & Pay Now", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

// Payment success splash
@Composable
fun PaymentSuccessScreen(
    t: (String) -> String,
    onDone: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LeoBlack)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            tint = LeoOrange,
            modifier = Modifier.size(80.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = t("payment_success"),
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Your digital ticket is stored offline in the My Tickets tab and is ready for boarding. Safe travels!",
            color = Color.Gray,
            textAlign = TextAlign.Center,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onDone,
            colors = ButtonDefaults.buttonColors(containerColor = LeoOrange, contentColor = LeoBlack),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.width(200.dp)
        ) {
            Text("Done", fontWeight = FontWeight.Bold)
        }
    }
}

// ==========================================
// 4. MY TICKETS TAB (OFFLINE QR STORAGE)
// ==========================================
@Composable
fun MyTicketsTab(
    bookings: List<BookingEntity>,
    stations: List<StationEntity>,
    t: (String) -> String,
    profile: UserProfile,
    onCancel: (BookingEntity) -> Unit
) {
    var ticketTabState by remember { mutableIntStateOf(0) } // 0 = Active, 1 = Travel History

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Text(
            text = t("my_tickets"),
            fontWeight = FontWeight.ExtraBold,
            fontSize = 24.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Tab state (Active vs Past)
        TabRow(
            selectedTabIndex = ticketTabState,
            containerColor = Color.Transparent,
            contentColor = LeoOrange,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[ticketTabState]),
                    color = LeoOrange
                )
            }
        ) {
            Tab(
                selected = ticketTabState == 0,
                onClick = { ticketTabState = 0 },
                text = { Text(t("active_tickets"), fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = ticketTabState == 1,
                onClick = { ticketTabState = 1 },
                text = { Text(t("past_tickets"), fontWeight = FontWeight.Bold) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        val filteredBookings = bookings.filter { booking ->
            if (ticketTabState == 0) {
                booking.status == "ACTIVE" && booking.departureDate >= SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            } else {
                booking.status == "CANCELLED" || booking.departureDate < SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            }
        }

        if (filteredBookings.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No tickets in this section.", color = Color.Gray, fontWeight = FontWeight.Medium)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(filteredBookings) { booking ->
                    TicketCard(
                        booking = booking,
                        stations = stations,
                        profile = profile,
                        t = { t(it) },
                        onCancel = { onCancel(booking) }
                    )
                }
            }
        }
    }
}

@Composable
fun TicketCard(
    booking: BookingEntity,
    stations: List<StationEntity>,
    profile: UserProfile,
    t: (String) -> String,
    onCancel: () -> Unit
) {
    var showCancelDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        border = BorderStroke(1.dp, Color(0xFF2E2E2E)),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .background(Brush.linearGradient(colors = listOf(Color(0xFF1C1B1F), Color(0xFF2D2D35))))
                .padding(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (booking.type == "TRAIN") Icons.Default.DirectionsTransit else Icons.Default.DirectionsBus,
                        contentDescription = null,
                        tint = LeoOrange,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = if (booking.type == "TRAIN") "Leo Rail" else "Leo Coach",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Badge(containerColor = if (booking.status == "ACTIVE") LeoGreen else LeoRed) {
                    Text(
                        text = booking.status,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Station connection
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = booking.departureTime, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                    Text(text = getStationName(booking.fromStationId, stations), color = Color.LightGray, fontSize = 12.sp)
                }
                Icon(Icons.Default.ArrowForward, contentDescription = null, tint = LeoOrange)
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = booking.arrivalTime, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                    Text(text = getStationName(booking.toStationId, stations), color = Color.LightGray, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "Date: ${booking.departureDate}", color = Color.Gray, fontSize = 12.sp)
                Text(text = "Carrier: Leo Express s.r.o.", color = Color.Gray, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Dashed Divider & Ticket Hole Visual Effect
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
            ) {
                val pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                drawLine(
                    color = Color.Gray.copy(alpha = 0.5f),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 2f,
                    pathEffect = pathEffect
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Ticket Carriage & Seat details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = t("passenger"), color = Color.Gray, fontSize = 11.sp)
                    Text(text = booking.passengerName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Column {
                    Text(text = t("carriage"), color = Color.Gray, fontSize = 11.sp)
                    Text(text = booking.carriageNumber, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = t("seat"), color = Color.Gray, fontSize = 11.sp)
                    Text(text = booking.seatNumber, color = LeoOrange, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Simulated QR Code Graphic (Canvas Draw!)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(12.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Canvas(
                        modifier = Modifier
                            .size(140.dp)
                            .testTag("ticket_qr_${booking.id}")
                    ) {
                        // Let's draw an actual robust QR pattern!
                        val sizeBlock = 10f
                        val count = (size.width / sizeBlock).toInt()
                        val r = Random(booking.id.hashCode().toLong())

                        // Draw QR Corners (3 Finder Patterns)
                        // Top Left
                        drawRect(Color.Black, Offset(0f, 0f), Size(30f, 30f))
                        drawRect(Color.White, Offset(5f, 5f), Size(20f, 20f))
                        drawRect(Color.Black, Offset(10f, 10f), Size(10f, 10f))

                        // Top Right
                        drawRect(Color.Black, Offset(size.width - 30f, 0f), Size(30f, 30f))
                        drawRect(Color.White, Offset(size.width - 25f, 5f), Size(20f, 20f))
                        drawRect(Color.Black, Offset(size.width - 20f, 10f), Size(10f, 10f))

                        // Bottom Left
                        drawRect(Color.Black, Offset(0f, size.height - 30f), Size(30f, 30f))
                        drawRect(Color.White, Offset(5f, size.height - 25f), Size(20f, 20f))
                        drawRect(Color.Black, Offset(10f, size.height - 20f), Size(10f, 10f))

                        // Random bits everywhere else
                        for (x in 0 until count) {
                            for (y in 0 until count) {
                                // Exclude finder pattern areas
                                val isFinder = (x < 4 && y < 4) || (x >= count - 4 && y < 4) || (x < 4 && y >= count - 4)
                                if (!isFinder) {
                                    if (r.nextBoolean()) {
                                        drawRect(
                                            Color.Black,
                                            Offset(x * sizeBlock, y * sizeBlock),
                                            Size(sizeBlock, sizeBlock)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "LEO CODE: ${booking.id}",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                }
            }

            // Ticket cancel & refund button if ACTIVE and in future
            if (booking.status == "ACTIVE") {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { showCancelDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = LeoRed),
                    border = BorderStroke(1.dp, LeoRed)
                ) {
                    Text(text = t("cancel_ticket"), fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Storno Ticket Confirmation") },
            text = { Text("Are you sure you want to cancel and refund your ticket ${booking.id} to ${getStationName(booking.toStationId, stations)}? Half the Leo Crowns points earned will be deducted, and fare refunded to card.") },
            confirmButton = {
                TextButton(onClick = {
                    onCancel()
                    showCancelDialog = false
                }) {
                    Text("Yes, Cancel", color = LeoRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) {
                    Text("No, Keep")
                }
            }
        )
    }
}

// ==========================================
// 5. MAPS & TIMETABLES BOARD TAB
// ==========================================
@Composable
fun MapBoardTab(
    stations: List<StationEntity>,
    t: (String) -> String
) {
    var trackerTabState by remember { mutableIntStateOf(0) } // 0 = Live Tracker, 1 = Departures Board

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Text(
            text = "Live Services",
            fontWeight = FontWeight.ExtraBold,
            fontSize = 24.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Custom Live and Timetable Tab Bar
        TabRow(
            selectedTabIndex = trackerTabState,
            containerColor = Color.Transparent,
            contentColor = LeoOrange,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[trackerTabState]),
                    color = LeoOrange
                )
            }
        ) {
            Tab(
                selected = trackerTabState == 0,
                onClick = { trackerTabState = 0 },
                text = { Text(t("live_map"), fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = trackerTabState == 1,
                onClick = { trackerTabState = 1 },
                text = { Text(t("timetable") + " Board", fontWeight = FontWeight.Bold) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (trackerTabState) {
            0 -> LiveMapTrackerSection(stations, t = t)
            1 -> LiveBoardSection(stations, t = t)
        }
    }
}

@Composable
fun LiveMapTrackerSection(
    stations: List<StationEntity>,
    t: (String) -> String
) {
    var ticks by remember { mutableIntStateOf(0) }

    // Run custom ticker animation to simulate live vehicle GPS movement
    LaunchedEffect(Unit) {
        while (true) {
            delay(1500)
            ticks++
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Drawing Canvas holding the custom map of Central Europe routes
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .border(2.dp, Color.Gray.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .background(LeoDarkGray)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Base grid of central europe
                // Coordinates mapping Prague (PRG) as center (200, 150)
                // Ostrava is East (+120, 0)
                // Bratislava is Southeast (+60, +100)
                // Kosice is Far East (+180, +100)
                // Vienna is South (+50, +120)
                // Berlin is North (-30, -100)
                // Munich is Southwest (-100, +80)
                // Krakow is Northeast (+140, -40)

                val points = mapOf(
                    "PRG" to Offset(200f, 150f),
                    "OSV" to Offset(340f, 150f),
                    "BRN" to Offset(260f, 210f),
                    "PLZ" to Offset(140f, 160f),
                    "BTS" to Offset(280f, 280f),
                    "KSC" to Offset(440f, 280f),
                    "KRK" to Offset(400f, 110f),
                    "VIE" to Offset(260f, 310f),
                    "BER" to Offset(170f, 50f),
                    "MUC" to Offset(80f, 230f)
                )

                // Draw Rail & Coach Lines
                val lineEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 5f), 0f)
                val drawLineConnector = { from: String, to: String, color: Color, isDashed: Boolean ->
                    val start = points[from]
                    val end = points[to]
                    if (start != null && end != null) {
                        drawLine(
                            color = color,
                            start = start,
                            end = end,
                            strokeWidth = 3f,
                            pathEffect = if (isDashed) lineEffect else null
                        )
                    }
                }

                // Rail lines (solid)
                drawLineConnector("PRG", "OSV", Color.White, false)
                drawLineConnector("OSV", "KSC", Color.White, false)
                drawLineConnector("PRG", "KRK", Color.White, false)
                drawLineConnector("PRG", "PLZ", Color.White, false)

                // Coach lines (dashed)
                drawLineConnector("PRG", "VIE", LeoGold, true)
                drawLineConnector("PRG", "BTS", LeoGold, true)
                drawLineConnector("BTS", "VIE", LeoGold, true)
                drawLineConnector("VIE", "MUC", LeoGold, true)
                drawLineConnector("BER", "PRG", LeoGold, true)

                // Draw Stations
                points.forEach { (code, pos) ->
                    drawCircle(color = LeoOrange, radius = 6f, center = pos)
                    // Draw mini Station Code text outline
                    // We draw small points just as labels
                }

                // --- LIVE VEHICLE SIMULATION ---
                // Train LE 1352 moving between Prague (PRG) and Ostrava (OSV)
                val trainPercent = (ticks % 10) / 10f
                val prgPos = points["PRG"]!!
                val osvPos = points["OSV"]!!
                val trainPos = Offset(
                    prgPos.x + (osvPos.x - prgPos.x) * trainPercent,
                    prgPos.y + (osvPos.y - prgPos.y) * trainPercent
                )
                drawCircle(color = LeoGreen, radius = 10f, center = trainPos)
                drawCircle(color = Color.White, radius = 12f, center = trainPos, style = Stroke(width = 2f))

                // Coach LC 5912 moving between Bratislava (BTS) and Vienna (VIE)
                val coachPercent = ((ticks + 3) % 8) / 8f
                val btsPos = points["BTS"]!!
                val viePos = points["VIE"]!!
                val coachPos = Offset(
                    btsPos.x + (viePos.x - btsPos.x) * coachPercent,
                    btsPos.y + (viePos.y - btsPos.y) * coachPercent
                )
                drawCircle(color = LeoGold, radius = 8f, center = coachPos)
            }

            // Floating legends inside map
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(modifier = Modifier.size(10.dp).background(LeoGreen, RoundedCornerShape(50)))
                    Text("Live Train LE 1352", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(modifier = Modifier.size(10.dp).background(LeoGold, RoundedCornerShape(50)))
                    Text("Live Coach LC 5912", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Text(text = "Leo Central Europe Station Hubs", fontWeight = FontWeight.Bold, fontSize = 15.sp)

        // List stations
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                stations.take(4).forEach { station ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Place, contentDescription = null, tint = LeoOrange, modifier = Modifier.size(18.dp))
                            Column {
                                Text(station.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("${station.city}, ${station.country}", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                        Text(
                            text = if (station.isCoachStop) "COACH HUB" else "RAIL HUB",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = LeoOrange
                        )
                    }
                    Divider(color = Color.Gray.copy(alpha = 0.1f))
                }
            }
        }
    }
}

@Composable
fun LiveBoardSection(
    stations: List<StationEntity>,
    t: (String) -> String
) {
    val boardServices = listOf(
        BoardService("LE 1352", "PRG", "OSV", "06:12", "Platform 3S", "On Time", "Departed"),
        BoardService("LC 5923", "PRG", "VIE", "08:00", "Bay 12", "On Time", "Boarding"),
        BoardService("LE 1354", "PRG", "OSV", "10:12", "Platform 1S", "On Time", "Scheduled"),
        BoardService("LC 5925", "PRG", "BTS", "09:30", "Bay 8", "On Time", "Scheduled")
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Warning bar
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = LeoGreen.copy(alpha = 0.2f)),
            border = BorderStroke(1.dp, LeoGreen)
        ) {
            Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = LeoGreen)
                Text(text = t("delay_warning"), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Text(text = "Departures from Prague (Praha hl.n.)", fontWeight = FontWeight.Bold, fontSize = 15.sp)

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = LeoBlack),
            border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.3f))
        ) {
            Column {
                // Header row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(LeoMediumGray)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Service", color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1f))
                    Text("Destination", color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(2f))
                    Text("Time", color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1f))
                    Text("Plat/Bay", color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1f))
                    Text("Status", color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1.2f))
                }

                boardServices.forEach { s ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Split-flap retro look for service
                        Text(
                            text = s.service,
                            color = LeoOrange,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f)
                        )

                        Text(
                            text = getStationName(s.to, stations),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(2f)
                        )

                        Text(
                            text = s.time,
                            color = Color.White,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f)
                        )

                        Text(
                            text = s.platform,
                            color = Color.LightGray,
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f)
                        )

                        Badge(
                            containerColor = when (s.boarding) {
                                "Boarding" -> LeoGold
                                "Departed" -> Color.Gray
                                else -> LeoGreen
                            },
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Text(
                                text = s.boarding,
                                color = LeoBlack,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                modifier = Modifier.padding(2.dp)
                            )
                        }
                    }
                    Divider(color = Color.Gray.copy(alpha = 0.1f))
                }
            }
        }
    }
}

data class BoardService(
    val service: String,
    val from: String,
    val to: String,
    val time: String,
    val platform: String,
    val delay: String,
    val boarding: String
)

// ==========================================
// 6. LEO CLUB & LOYALTY ACCOUNT TAB
// ==========================================
@Composable
fun LeoClubTab(
    profile: UserProfile,
    passengers: List<PassengerEntity>,
    payments: List<PaymentMethodEntity>,
    notifications: List<NotificationEntity>,
    t: (String) -> String,
    onAddPassenger: () -> Unit,
    onAddCard: () -> Unit,
    onDeletePassenger: (PassengerEntity) -> Unit,
    onDeleteCard: (PaymentMethodEntity) -> Unit,
    onMarkNotifRead: (Int) -> Unit,
    onDeleteNotif: (NotificationEntity) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = t("loyalty_account"),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 24.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // Gold Membership Loyalty Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(185.dp),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.linearGradient(colors = listOf(Color(0xFF1C1B1F), Color(0xFF2D2D35))))
                ) {
                    // Golden wavy canvas pattern
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            color = Color(0xFFF39200).copy(alpha = 0.12f),
                            radius = 240f,
                            center = Offset(size.width, 0f)
                        )
                        drawCircle(
                            color = Color(0xFFF39200).copy(alpha = 0.04f),
                            radius = 380f,
                            center = Offset(size.width, 0f)
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = profile.fullName,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                                Text(
                                    text = "Club ID: LE-098432-CZ",
                                    color = Color.LightGray,
                                    fontSize = 12.sp
                                )
                            }
                            Icon(Icons.Default.Stars, contentDescription = null, tint = LeoOrange, modifier = Modifier.size(32.dp))
                        }

                        // Progress to next tier
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Column {
                                    Text(t("loyalty_points"), color = Color.Gray, fontSize = 11.sp)
                                    Text(
                                        text = "${profile.loyaltyPoints} LÉO",
                                        color = LeoOrange,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 24.sp
                                    )
                                }
                                Badge(containerColor = LeoGold) {
                                    Text(
                                        text = profile.loyaltyTier,
                                        color = LeoBlack,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            // Gold Tier progress bar
                            LinearProgressIndicator(
                                progress = { (profile.loyaltyPoints / 2000f).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(100)),
                                color = LeoOrange,
                                trackColor = Color.Gray.copy(alpha = 0.3f),
                            )
                        }
                    }
                }
            }
        }

        // Saved Passengers Manager
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = t("saved_passengers"),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        IconButton(onClick = onAddPassenger) {
                            Icon(Icons.Default.Add, contentDescription = "Add Passenger", tint = LeoOrange)
                        }
                    }

                    if (passengers.isEmpty()) {
                        Text("No saved passengers yet.", color = Color.Gray, fontSize = 13.sp)
                    } else {
                        passengers.forEach { p ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = LeoOrange)
                                    Column {
                                        Text("${p.firstName} ${p.lastName}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("Discount: ${p.discountType}", fontSize = 11.sp, color = Color.Gray)
                                    }
                                }
                                IconButton(onClick = { onDeletePassenger(p) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = LeoRed, modifier = Modifier.size(20.dp))
                                }
                            }
                            Divider(color = Color.Gray.copy(alpha = 0.1f))
                        }
                    }
                }
            }
        }

        // Saved Payment Cards Manager
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = t("saved_payments"),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        IconButton(onClick = onAddCard) {
                            Icon(Icons.Default.Add, contentDescription = "Add Card", tint = LeoOrange)
                        }
                    }

                    if (payments.isEmpty()) {
                        Text("No saved credit cards yet.", color = Color.Gray, fontSize = 13.sp)
                    } else {
                        payments.forEach { c ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CreditCard, contentDescription = null, tint = LeoOrange)
                                    Column {
                                        Text(c.maskedNumber, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("${c.cardType} - ${c.cardHolder}", fontSize = 11.sp, color = Color.Gray)
                                    }
                                }
                                IconButton(onClick = { onDeleteCard(c) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = LeoRed, modifier = Modifier.size(20.dp))
                                }
                            }
                            Divider(color = Color.Gray.copy(alpha = 0.1f))
                        }
                    }
                }
            }
        }

        // Push Notifications Feed
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Notifications, contentDescription = null, tint = LeoOrange)
                            Text(
                                text = t("notifications"),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (notifications.isEmpty()) {
                        Text("Your notifications feed is empty.", color = Color.Gray, fontSize = 13.sp)
                    } else {
                        notifications.forEach { n ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                                    .clickable { onMarkNotifRead(n.id) }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = n.title,
                                            fontWeight = if (n.isRead) FontWeight.Medium else FontWeight.ExtraBold,
                                            fontSize = 14.sp,
                                            color = if (n.isRead) Color.Gray else MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(text = n.message, fontSize = 12.sp, color = Color.Gray)
                                    }
                                    IconButton(onClick = { onDeleteNotif(n) }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.LightGray, modifier = Modifier.size(16.dp))
                                    }
                                }
                                Divider(color = Color.Gray.copy(alpha = 0.1f), modifier = Modifier.padding(top = 8.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

// Add Passenger Sheet Composable
@Composable
fun AddPassengerScreen(
    t: (String) -> String,
    onBack: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var fn by remember { mutableStateOf("") }
    var ln by remember { mutableStateOf("") }
    var discount by remember { mutableStateOf("NONE") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LeoBlack)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text(t("add_passenger"), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
        }

        OutlinedTextField(
            value = fn,
            onValueChange = { fn = it },
            label = { Text(t("first_name")) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = LeoOrange)
        )

        OutlinedTextField(
            value = ln,
            onValueChange = { ln = it },
            label = { Text(t("last_name")) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = LeoOrange)
        )

        Text("Discount Program Tier", color = Color.White, fontWeight = FontWeight.Bold)
        val options = listOf("NONE", "STUDENT", "SENIOR", "CHILD")
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            options.forEach { opt ->
                val active = discount == opt
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { discount = opt },
                    colors = CardDefaults.cardColors(containerColor = if (active) LeoOrange else LeoMediumGray)
                ) {
                    Box(modifier = Modifier.padding(8.dp), contentAlignment = Alignment.Center) {
                        Text(opt, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (active) LeoBlack else Color.White)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = { if (fn.isNotEmpty() && ln.isNotEmpty()) onSave(fn, ln, discount) },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = LeoOrange, contentColor = LeoBlack)
        ) {
            Text("Save Saved Passenger", fontWeight = FontWeight.Bold)
        }
    }
}

// Add Card Sheet Composable
@Composable
fun AddCardScreen(
    t: (String) -> String,
    onBack: () -> Unit,
    onSave: (String, String, String, String) -> Unit
) {
    var holder by remember { mutableStateOf("") }
    var num by remember { mutableStateOf("") }
    var exp by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("Visa") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LeoBlack)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text(t("add_card"), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
        }

        OutlinedTextField(
            value = num,
            onValueChange = { num = it },
            label = { Text(t("card_number")) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = LeoOrange)
        )

        OutlinedTextField(
            value = holder,
            onValueChange = { holder = it },
            label = { Text("Cardholder Name") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = LeoOrange)
        )

        OutlinedTextField(
            value = exp,
            onValueChange = { exp = it },
            label = { Text(t("expiry")) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = LeoOrange)
        )

        Text("Card Type", color = Color.White, fontWeight = FontWeight.Bold)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Visa", "Mastercard").forEach { cardType ->
                val active = type == cardType
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { type = cardType },
                    colors = CardDefaults.cardColors(containerColor = if (active) LeoOrange else LeoMediumGray)
                ) {
                    Box(modifier = Modifier.padding(12.dp), contentAlignment = Alignment.Center) {
                        Text(cardType, fontWeight = FontWeight.Bold, color = if (active) LeoBlack else Color.White)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = { if (num.isNotEmpty() && holder.isNotEmpty()) onSave(holder, num, exp, type) },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = LeoOrange, contentColor = LeoBlack)
        ) {
            Text("Save Payment Card", fontWeight = FontWeight.Bold)
        }
    }
}

// ==========================================
// 7. SETTINGS & INTERACTIVE ADMIN PORTAL
// ==========================================
@Composable
fun SettingsTab(
    viewModel: LeoExpressViewModel,
    profile: UserProfile,
    routes: List<RouteEntity>,
    bookings: List<BookingEntity>,
    stations: List<StationEntity>,
    t: (String) -> String,
    onAddRoute: (String, String, String, String, String, Double, String) -> Unit,
    onDeleteRoute: (Int) -> Unit,
    onSendNotif: (String, String, String) -> Unit
) {
    var showLanguageDropdown by remember { mutableStateOf(false) }
    var showAdminPanel by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = t("settings"),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 24.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // Language & Currency settings
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(text = "Preferences", fontWeight = FontWeight.Bold, fontSize = 16.sp)

                    // Language switch option
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = t("language"), fontWeight = FontWeight.Medium)
                        Box {
                            val activeLangObj = Translations.languages.find { it.code == profile.preferredLanguage }
                            Button(
                                onClick = { showLanguageDropdown = true },
                                colors = ButtonDefaults.buttonColors(containerColor = LeoOrange, contentColor = LeoBlack)
                            ) {
                                Text("${activeLangObj?.flag ?: ""} ${activeLangObj?.name ?: "English"}")
                            }

                            DropdownMenu(
                                expanded = showLanguageDropdown,
                                onDismissRequest = { showLanguageDropdown = false }
                            ) {
                                Translations.languages.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text("${option.flag} ${option.name}") },
                                        onClick = {
                                            viewModel.updateLanguage(option.code)
                                            showLanguageDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Divider(color = Color.Gray.copy(alpha = 0.1f))

                    // Currency Switch Option
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = t("currency"), fontWeight = FontWeight.Medium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("CZK", "EUR").forEach { curr ->
                                val active = profile.currency == curr
                                FilterChip(
                                    selected = active,
                                    onClick = { viewModel.updateCurrency(curr) },
                                    label = { Text(curr) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = LeoOrange,
                                        selectedLabelColor = LeoBlack
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // Personal profile editor card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(text = t("personal_profile"), fontWeight = FontWeight.Bold, fontSize = 16.sp)

                    var nameInput by remember { mutableStateOf(profile.fullName) }
                    var emailInput by remember { mutableStateOf(profile.email) }

                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Full Name") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        label = { Text("Email Address") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Button(
                        onClick = { viewModel.updateProfile(nameInput, emailInput) },
                        modifier = Modifier.align(Alignment.End),
                        colors = ButtonDefaults.buttonColors(containerColor = LeoOrange, contentColor = LeoBlack),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Save Profile")
                    }
                }
            }
        }

        // Staff Mode switch button
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = if (showAdminPanel) LeoOrange.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAdminPanel = !showAdminPanel }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = LeoOrange)
                        Column {
                            Text(text = t("admin_portal") + " (Staff Only)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(text = "Manage timetables, fares, track bookings & send push delays.", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                    Switch(
                        checked = showAdminPanel,
                        onCheckedChange = { showAdminPanel = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = LeoOrange, checkedTrackColor = LeoOrange.copy(alpha = 0.5f))
                    )
                }
            }
        }

        // ADMIN PANEL FOR STAFF (Active only if switched on)
        if (showAdminPanel) {
            item {
                AdminPortalSection(
                    routes = routes,
                    bookings = bookings,
                    stations = stations,
                    t = t,
                    onAddRoute = onAddRoute,
                    onDeleteRoute = onDeleteRoute,
                    onSendNotif = onSendNotif
                )
            }
        }
    }
}

@Composable
fun AdminPortalSection(
    routes: List<RouteEntity>,
    bookings: List<BookingEntity>,
    stations: List<StationEntity>,
    t: (String) -> String,
    onAddRoute: (String, String, String, String, String, Double, String) -> Unit,
    onDeleteRoute: (Int) -> Unit,
    onSendNotif: (String, String, String) -> Unit
) {
    var adminTabState by remember { mutableIntStateOf(0) } // 0 = Timetable Admin, 1 = Bookings monitor, 2 = Notification Center

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, LeoOrange, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = LeoDarkGray),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = "Admin Hub Actions", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = LeoOrange)

            // Mini tab row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf("Timetables", "Bookings", "Alerts").forEachIndexed { index, title ->
                    val active = adminTabState == index
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { adminTabState = index },
                        colors = CardDefaults.cardColors(containerColor = if (active) LeoOrange else LeoMediumGray)
                    ) {
                        Box(modifier = Modifier.padding(8.dp), contentAlignment = Alignment.Center) {
                            Text(
                                title,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (active) LeoBlack else Color.White
                            )
                        }
                    }
                }
            }

            Divider(color = Color.Gray.copy(alpha = 0.2f))

            when (adminTabState) {
                0 -> { // Manage Schedules & Fares
                    Text("Add Custom Connection", fontWeight = FontWeight.Bold, color = Color.White)

                    var sourceId by remember { mutableStateOf("PRG") }
                    var destId by remember { mutableStateOf("OSV") }
                    var isTrainType by remember { mutableStateOf(true) }
                    var departureT by remember { mutableStateOf("09:00") }
                    var arrivalT by remember { mutableStateOf("12:00") }
                    var priceStr by remember { mutableStateOf("300") }
                    var stopsList by remember { mutableStateOf("Pardubice") }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = sourceId,
                            onValueChange = { sourceId = it },
                            label = { Text("From Code") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                        OutlinedTextField(
                            value = destId,
                            onValueChange = { destId = it },
                            label = { Text("To Code") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Transport:", color = Color.Gray, fontSize = 12.sp)
                        Row {
                            FilterChip(
                                selected = isTrainType,
                                onClick = { isTrainType = true },
                                label = { Text("Train") }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            FilterChip(
                                selected = !isTrainType,
                                onClick = { isTrainType = false },
                                label = { Text("Coach") }
                            )
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = departureT,
                            onValueChange = { departureT = it },
                            label = { Text("Dep Time") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                        OutlinedTextField(
                            value = arrivalT,
                            onValueChange = { arrivalT = it },
                            label = { Text("Arr Time") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                    }

                    OutlinedTextField(
                        value = priceStr,
                        onValueChange = { priceStr = it },
                        label = { Text("Fare Price (CZK)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )

                    OutlinedTextField(
                        value = stopsList,
                        onValueChange = { stopsList = it },
                        label = { Text("Stops (comma separated)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )

                    Button(
                        onClick = {
                            val priceDouble = priceStr.toDoubleOrNull() ?: 200.0
                            val typeStr = if (isTrainType) "TRAIN" else "COACH"
                            onAddRoute(sourceId, destId, departureT, arrivalT, typeStr, priceDouble, stopsList)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LeoOrange, contentColor = LeoBlack),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Inject Route to DB", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Existing System Connections", fontWeight = FontWeight.Bold, color = Color.White)

                    routes.take(5).forEach { r ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(LeoMediumGray, RoundedCornerShape(8.dp))
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("${r.fromStationId} ➔ ${r.toStationId} (${r.type})", color = Color.White, fontWeight = FontWeight.Bold)
                                Text("${r.departureTime} - ${r.arrivalTime} | ${r.priceCzk.toInt()} CZK", color = Color.Gray, fontSize = 11.sp)
                            }
                            IconButton(onClick = { onDeleteRoute(r.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = LeoRed, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
                1 -> { // Bookings Monitor
                    Text("Real-time Passenger Bookings", fontWeight = FontWeight.Bold, color = Color.White)
                    if (bookings.isEmpty()) {
                        Text("No tickets have been booked yet.", color = Color.Gray, fontSize = 12.sp)
                    } else {
                        bookings.forEach { b ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = LeoMediumGray)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(b.passengerName, color = Color.White, fontWeight = FontWeight.Bold)
                                        Text(b.status, color = if (b.status == "ACTIVE") LeoGreen else LeoRed, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                    Text("ID: ${b.id} | ${b.fromStationId} to ${b.toStationId}", color = Color.LightGray, fontSize = 11.sp)
                                    Text("Seat: ${b.seatNumber} | Paid: ${b.pricePaidCzk.toInt()} CZK", color = Color.Gray, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
                2 -> { // Notification dispatcher
                    Text("Dispatch Push Delay Alerts", fontWeight = FontWeight.Bold, color = Color.White)

                    var notifTitle by remember { mutableStateOf("Delay Alert: Train LE 1352") }
                    var notifMsg by remember { mutableStateOf("Train LE 1352 from Prague to Ostrava is delayed by 15 mins due to adverse weather.") }
                    var notifType by remember { mutableStateOf("DELAY") }

                    OutlinedTextField(
                        value = notifTitle,
                        onValueChange = { notifTitle = it },
                        label = { Text("Title") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )

                    OutlinedTextField(
                        value = notifMsg,
                        onValueChange = { notifMsg = it },
                        label = { Text("Message Body") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("DELAY", "PLATFORM", "PROMO").forEach { alertType ->
                            val active = notifType == alertType
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { notifType = alertType },
                                colors = CardDefaults.cardColors(containerColor = if (active) LeoOrange else LeoMediumGray)
                            ) {
                                Box(modifier = Modifier.padding(8.dp), contentAlignment = Alignment.Center) {
                                    Text(alertType, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (active) LeoBlack else Color.White)
                                }
                            }
                        }
                    }

                    Button(
                        onClick = {
                            onSendNotif(notifTitle, notifMsg, notifType)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LeoOrange, contentColor = LeoBlack),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Send Delay Push Alert", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
