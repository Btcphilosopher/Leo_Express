package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.data.database.LeoExpressDatabase
import com.example.data.model.*
import com.example.data.repository.LeoExpressRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class LeoExpressViewModel(application: Application) : AndroidViewModel(application) {

    // Database & Repository Initialization
    private val database: LeoExpressDatabase = Room.databaseBuilder(
        application,
        LeoExpressDatabase::class.java,
        "leo_express_db"
    ).fallbackToDestructiveMigration()
    .build()

    private val repository = LeoExpressRepository(database.leoExpressDao(), viewModelScope)

    // Expose Data from Repository
    val userProfile: StateFlow<UserProfile> = repository.userProfile
    val stations: StateFlow<List<StationEntity>> = repository.allStations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val routes: StateFlow<List<RouteEntity>> = repository.allRoutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bookings: StateFlow<List<BookingEntity>> = repository.allBookings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val passengers: StateFlow<List<PassengerEntity>> = repository.allPassengers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val paymentMethods: StateFlow<List<PaymentMethodEntity>> = repository.allPaymentMethods
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notifications: StateFlow<List<NotificationEntity>> = repository.allNotifications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Search & Journey Planner State ---
    private val _searchFrom = MutableStateFlow("PRG")
    val searchFrom: StateFlow<String> = _searchFrom.asStateFlow()

    private val _searchTo = MutableStateFlow("OSV")
    val searchTo: StateFlow<String> = _searchTo.asStateFlow()

    private val _searchDate = MutableStateFlow("")
    val searchDate: StateFlow<String> = _searchDate.asStateFlow()

    private val _passengerCount = MutableStateFlow(1)
    val passengerCount: StateFlow<Int> = _passengerCount.asStateFlow()

    // Results of timetables / routes
    private val _searchResultRoutes = MutableStateFlow<List<RouteEntity>>(emptyList())
    val searchResultRoutes: StateFlow<List<RouteEntity>> = _searchResultRoutes.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    // Active trip booking flow state
    private val _selectedRouteForBooking = MutableStateFlow<RouteEntity?>(null)
    val selectedRouteForBooking: StateFlow<RouteEntity?> = _selectedRouteForBooking.asStateFlow()

    private val _selectedSeat = MutableStateFlow<String?>(null)
    val selectedSeat: StateFlow<String?> = _selectedSeat.asStateFlow()

    private val _selectedCarriage = MutableStateFlow("Car 3 (Premium)")
    val selectedCarriage: StateFlow<String> = _selectedCarriage.asStateFlow()

    init {
        // Set default date as tomorrow
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, 1)
        _searchDate.value = sdf.format(cal.time)
    }

    // --- Actions ---
    fun setFromStation(stationId: String) {
        _searchFrom.value = stationId
    }

    fun setToStation(stationId: String) {
        _searchTo.value = stationId
    }

    fun setDate(dateStr: String) {
        _searchDate.value = dateStr
    }

    fun updatePassengerCount(count: Int) {
        if (count in 1..9) {
            _passengerCount.value = count
        }
    }

    fun swapStations() {
        val temp = _searchFrom.value
        _searchFrom.value = _searchTo.value
        _searchTo.value = temp
    }

    fun performSearch() {
        viewModelScope.launch {
            _isSearching.value = true
            // Find routes linking these stations
            val fromId = _searchFrom.value
            val toId = _searchTo.value
            
            // Get all routes and filter locally for maximum responsiveness
            val allRoutesList = repository.allRoutes.first()
            val filtered = allRoutesList.filter { 
                it.fromStationId == fromId && it.toStationId == toId 
            }
            _searchResultRoutes.value = filtered
            _isSearching.value = false
        }
    }

    fun selectRouteForBooking(route: RouteEntity) {
        _selectedRouteForBooking.value = route
        // Pick a default seat
        _selectedSeat.value = "${(1..15).random()}${listOf("A", "B", "C", "D").random()}"
        _selectedCarriage.value = if (route.type == "TRAIN") "Car 3 (Premium)" else "Bus 1"
    }

    fun setSeat(seat: String, carriage: String) {
        _selectedSeat.value = seat
        _selectedCarriage.value = carriage
    }

    fun completeBooking(passengerName: String) {
        val route = _selectedRouteForBooking.value ?: return
        val seat = _selectedSeat.value ?: "12A"
        val carriage = _selectedCarriage.value

        viewModelScope.launch {
            val booking = BookingEntity(
                fromStationId = route.fromStationId,
                toStationId = route.toStationId,
                departureDate = _searchDate.value,
                departureTime = route.departureTime,
                arrivalTime = route.arrivalTime,
                type = route.type,
                seatNumber = seat,
                carriageNumber = carriage,
                passengerName = passengerName,
                pricePaidCzk = route.priceCzk * _passengerCount.value,
                status = "ACTIVE"
            )
            repository.insertBooking(booking)
            // Reset booking flow state
            _selectedRouteForBooking.value = null
            _selectedSeat.value = null
        }
    }

    fun cancelTicket(booking: BookingEntity) {
        viewModelScope.launch {
            repository.cancelBooking(booking)
        }
    }

    // --- Profile & Settings Actions ---
    fun updateLanguage(langCode: String) {
        repository.updateLanguage(langCode)
    }

    fun updateCurrency(currencyCode: String) {
        repository.updateCurrency(currencyCode)
    }

    fun updateProfile(name: String, email: String) {
        repository.updateProfile(name, email)
    }

    fun addPassenger(firstName: String, lastName: String, discountType: String) {
        viewModelScope.launch {
            val passenger = PassengerEntity(
                firstName = firstName,
                lastName = lastName,
                birthDate = "1995-01-01",
                discountType = discountType
            )
            repository.insertPassenger(passenger)
        }
    }

    fun deletePassenger(passenger: PassengerEntity) {
        viewModelScope.launch {
            repository.deletePassenger(passenger)
        }
    }

    fun addPaymentMethod(holder: String, number: String, expiry: String, type: String) {
        viewModelScope.launch {
            val card = PaymentMethodEntity(
                cardHolder = holder,
                maskedNumber = "**** **** **** " + number.takeLast(4),
                expiryDate = expiry,
                cardType = type
            )
            repository.insertPaymentMethod(card)
        }
    }

    fun deletePaymentMethod(paymentMethod: PaymentMethodEntity) {
        viewModelScope.launch {
            repository.deletePaymentMethod(paymentMethod)
        }
    }

    // --- Admin Operations ---
    fun addRoute(from: String, to: String, depTime: String, arrTime: String, type: String, price: Double, stops: String) {
        viewModelScope.launch {
            val route = RouteEntity(
                fromStationId = from,
                toStationId = to,
                departureTime = depTime,
                arrivalTime = arrTime,
                type = type,
                priceCzk = price,
                stops = stops
            )
            repository.insertRoutes(listOf(route))
            // Refresh results
            performSearch()
        }
    }

    fun removeRoute(routeId: Int) {
        viewModelScope.launch {
            repository.deleteRoute(routeId)
            performSearch()
        }
    }

    fun sendSystemNotification(title: String, message: String, type: String) {
        viewModelScope.launch {
            val notif = NotificationEntity(
                title = title,
                message = message,
                type = type
            )
            repository.insertNotification(notif)
        }
    }

    fun deleteNotification(notification: NotificationEntity) {
        viewModelScope.launch {
            repository.deleteNotification(notification)
        }
    }

    fun markNotificationAsRead(id: Int) {
        viewModelScope.launch {
            repository.markNotificationAsRead(id)
        }
    }
}
