package com.example.data.repository

import com.example.data.dao.LeoExpressDao
import com.example.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class LeoExpressRepository(
    private val dao: LeoExpressDao,
    private val scope: CoroutineScope
) {
    // Reactive Settings / User Profile in memory (resets on app close, but highly reactive and modifiable)
    private val _userProfile = MutableStateFlow(UserProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    init {
        // Automatically seed database with stations and routes on startup
        scope.launch(Dispatchers.IO) {
            seedDatabaseIfEmpty()
        }
    }

    // --- Profile & Settings Methods ---
    fun updateLanguage(langCode: String) {
        _userProfile.value = _userProfile.value.copy(preferredLanguage = langCode)
    }

    fun updateCurrency(currencyCode: String) {
        _userProfile.value = _userProfile.value.copy(currency = currencyCode)
    }

    fun updateProfile(name: String, email: String) {
        _userProfile.value = _userProfile.value.copy(fullName = name, email = email)
    }

    fun addLoyaltyPoints(points: Int) {
        val currentPoints = _userProfile.value.loyaltyPoints
        val newPoints = currentPoints + points
        val nextTier = when {
            newPoints >= 1500 -> "Gold Tier"
            newPoints >= 800 -> "Silver Tier"
            newPoints >= 300 -> "Bronze Tier"
            else -> "Standard"
        }
        _userProfile.value = _userProfile.value.copy(
            loyaltyPoints = newPoints,
            loyaltyTier = nextTier
        )
    }

    // --- Station Methods ---
    val allStations: Flow<List<StationEntity>> = dao.getAllStations()
    suspend fun insertStations(stations: List<StationEntity>) = dao.insertStations(stations)

    // --- Route Methods ---
    val allRoutes: Flow<List<RouteEntity>> = dao.getAllRoutes()
    fun findRoutes(fromId: String, toId: String): Flow<List<RouteEntity>> = dao.findRoutes(fromId, toId)
    suspend fun insertRoutes(routes: List<RouteEntity>) = dao.insertRoutes(routes)
    suspend fun deleteRoute(routeId: Int) = dao.deleteRoute(routeId)

    // --- Booking Methods ---
    val allBookings: Flow<List<BookingEntity>> = dao.getAllBookings()
    suspend fun insertBooking(booking: BookingEntity) {
        dao.insertBooking(booking)
        // Add 10% of Czech Crowns price as loyalty points (100 CZK = 10 Leo Crowns)
        val crownsEarned = (booking.pricePaidCzk * 0.1).toInt()
        addLoyaltyPoints(crownsEarned)

        // Generate immediate trip confirmation notification
        val notification = NotificationEntity(
            title = "Ticket Booked: ${booking.id}",
            message = "Your ticket from ${booking.fromStationId} to ${booking.toStationId} is confirmed. Carriage ${booking.carriageNumber}, Seat ${booking.seatNumber}.",
            type = "REMINDER"
        )
        dao.insertNotification(notification)
    }
    suspend fun cancelBooking(booking: BookingEntity) {
        val updated = booking.copy(status = "CANCELLED")
        dao.updateBooking(updated)
        // Refund half points
        val pointsDeducted = (booking.pricePaidCzk * 0.1).toInt()
        addLoyaltyPoints(-pointsDeducted / 2)

        // Create cancellation notification
        val notification = NotificationEntity(
            title = "Trip Cancelled: ${booking.id}",
            message = "Your ticket from ${booking.fromStationId} to ${booking.toStationId} has been successfully cancelled & refunded.",
            type = "INFO"
        )
        dao.insertNotification(notification)
    }

    // --- Passenger Methods ---
    val allPassengers: Flow<List<PassengerEntity>> = dao.getAllPassengers()
    suspend fun insertPassenger(passenger: PassengerEntity) = dao.insertPassenger(passenger)
    suspend fun deletePassenger(passenger: PassengerEntity) = dao.deletePassenger(passenger)

    // --- Payment Methods ---
    val allPaymentMethods: Flow<List<PaymentMethodEntity>> = dao.getAllPaymentMethods()
    suspend fun insertPaymentMethod(paymentMethod: PaymentMethodEntity) = dao.insertPaymentMethod(paymentMethod)
    suspend fun deletePaymentMethod(paymentMethod: PaymentMethodEntity) = dao.deletePaymentMethod(paymentMethod)

    // --- Notification Methods ---
    val allNotifications: Flow<List<NotificationEntity>> = dao.getAllNotifications()
    suspend fun insertNotification(notification: NotificationEntity) = dao.insertNotification(notification)
    suspend fun markNotificationAsRead(id: Int) = dao.markNotificationAsRead(id)
    suspend fun deleteNotification(notification: NotificationEntity) = dao.deleteNotification(notification)

    // --- Seeding Routine ---
    private suspend fun seedDatabaseIfEmpty() {
        val currentStations = dao.getAllStations().first()
        if (currentStations.isEmpty()) {
            val stations = listOf(
                StationEntity("PRG", "Praha hl.n.", "Prague", "Czech Republic", 50.083, 14.435, false),
                StationEntity("OSV", "Ostrava hl.n.", "Ostrava", "Czech Republic", 49.851, 18.268, false),
                StationEntity("BRN", "Brno hl.n.", "Brno", "Czech Republic", 49.190, 16.613, false),
                StationEntity("PLZ", "Plzeň hl.n.", "Pilsen", "Czech Republic", 49.743, 13.389, false),
                StationEntity("BTS", "Bratislava hl.st.", "Bratislava", "Slovakia", 48.158, 17.106, false),
                StationEntity("KSC", "Košice hl.st.", "Kosice", "Slovakia", 48.728, 21.267, false),
                StationEntity("KRK", "Kraków Główny", "Krakow", "Poland", 50.068, 19.947, false),
                StationEntity("VIE", "Wien Hauptbahnhof", "Vienna", "Austria", 48.185, 16.376, false),
                StationEntity("BER", "Berlin Hbf", "Berlin", "Germany", 52.525, 13.369, false),
                StationEntity("MUC", "München Hbf", "Munich", "Germany", 48.140, 11.558, false)
            )
            dao.insertStations(stations)

            val routes = listOf(
                RouteEntity(fromStationId = "PRG", toStationId = "OSV", departureTime = "06:12", arrivalTime = "09:32", type = "TRAIN", priceCzk = 329.0, stops = "Pardubice, Olomouc"),
                RouteEntity(fromStationId = "PRG", toStationId = "OSV", departureTime = "10:12", arrivalTime = "13:32", type = "TRAIN", priceCzk = 359.0, stops = "Pardubice, Olomouc"),
                RouteEntity(fromStationId = "PRG", toStationId = "OSV", departureTime = "14:12", arrivalTime = "17:32", type = "TRAIN", priceCzk = 389.0, stops = "Pardubice, Olomouc"),
                RouteEntity(fromStationId = "PRG", toStationId = "KRK", departureTime = "16:10", arrivalTime = "22:45", type = "TRAIN", priceCzk = 590.0, stops = "Olomouc, Ostrava, Katowice"),
                RouteEntity(fromStationId = "PRG", toStationId = "VIE", departureTime = "08:00", arrivalTime = "13:15", type = "COACH", priceCzk = 420.0, stops = "Brno"),
                RouteEntity(fromStationId = "PRG", toStationId = "BTS", departureTime = "09:30", arrivalTime = "14:00", type = "COACH", priceCzk = 350.0, stops = "Brno"),
                RouteEntity(fromStationId = "BTS", toStationId = "VIE", departureTime = "10:00", arrivalTime = "11:15", type = "COACH", priceCzk = 149.0, stops = "Bratislava Airport"),
                RouteEntity(fromStationId = "KRK", toStationId = "OSV", departureTime = "11:30", arrivalTime = "13:45", type = "COACH", priceCzk = 199.0, stops = "Katowice"),
                RouteEntity(fromStationId = "OSV", toStationId = "KSC", departureTime = "09:40", arrivalTime = "12:55", type = "TRAIN", priceCzk = 280.0, stops = "Žilina, Poprad-Tatry"),
                RouteEntity(fromStationId = "PRG", toStationId = "PLZ", departureTime = "07:15", arrivalTime = "08:45", type = "TRAIN", priceCzk = 120.0, stops = "Karlštejn, Beroun"),
                RouteEntity(fromStationId = "VIE", toStationId = "MUC", departureTime = "13:00", arrivalTime = "18:30", type = "COACH", priceCzk = 450.0, stops = "Linz, Salzburg"),
                RouteEntity(fromStationId = "BER", toStationId = "PRG", departureTime = "07:00", arrivalTime = "11:45", type = "COACH", priceCzk = 490.0, stops = "Dresden")
            )
            dao.insertRoutes(routes)

            // Seed sample passengers
            dao.insertPassenger(PassengerEntity(firstName = "Jan", lastName = "Novák", birthDate = "1990-05-12", discountType = "NONE", loyaltyNumber = "LE-1250"))
            dao.insertPassenger(PassengerEntity(firstName = "Marie", lastName = "Nováková", birthDate = "1993-09-24", discountType = "NONE", loyaltyNumber = ""))

            // Seed sample payment method
            dao.insertPaymentMethod(PaymentMethodEntity(cardHolder = "Jan Novák", maskedNumber = "**** **** **** 5183", expiryDate = "08/28", cardType = "Visa"))

            // Seed sample active and past bookings
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }.time
            val tomorrowStr = sdf.format(tomorrow)

            val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -2) }.time
            val yesterdayStr = sdf.format(yesterday)

            dao.insertBooking(BookingEntity(
                id = "LE-482103-CZ",
                fromStationId = "PRG",
                toStationId = "OSV",
                departureDate = tomorrowStr,
                departureTime = "06:12",
                arrivalTime = "09:32",
                type = "TRAIN",
                seatNumber = "24B",
                carriageNumber = "Car 3 (Premium)",
                passengerName = "Jan Novák",
                pricePaidCzk = 329.0,
                status = "ACTIVE"
            ))

            dao.insertBooking(BookingEntity(
                id = "LE-104924-CZ",
                fromStationId = "PRG",
                toStationId = "VIE",
                departureDate = yesterdayStr,
                departureTime = "08:00",
                arrivalTime = "13:15",
                type = "COACH",
                seatNumber = "14A",
                carriageNumber = "Bus 1",
                passengerName = "Jan Novák",
                pricePaidCzk = 420.0,
                status = "ACTIVE" // Completed in the past but kept active for history
            ))

            // Seed notifications
            dao.insertNotification(NotificationEntity(
                title = "Welcome to Leo Express!",
                message = "Earn 10% cash back in Leo Crowns on every ticket you purchase. Safe travels through Central Europe!",
                type = "INFO"
            ))
            dao.insertNotification(NotificationEntity(
                title = "Platform Assigned: Train LE 1352",
                message = "Your train from Prague hl.n. to Ostrava hl.n. departs from Platform 3, Track 4.",
                type = "PLATFORM"
            ))
        }
    }
}
