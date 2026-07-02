package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "stations")
data class StationEntity(
    @PrimaryKey val id: String, // e.g., "PRG", "VIE"
    val name: String,
    val city: String,
    val country: String,
    val latitude: Double,
    val longitude: Double,
    val isCoachStop: Boolean
)

@Entity(tableName = "routes")
data class RouteEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val fromStationId: String,
    val toStationId: String,
    val departureTime: String, // e.g., "08:30"
    val arrivalTime: String,   // e.g., "12:45"
    val type: String,          // "TRAIN" or "COACH"
    val priceCzk: Double,
    val carrierCode: String = "LE", // "LE" for Leo Express
    val stops: String          // Comma-separated station names, e.g., "Pardubice, Olomouc"
)

@Entity(tableName = "bookings")
data class BookingEntity(
    @PrimaryKey val id: String = "LE-" + (100000..999999).random().toString() + "-CZ",
    val fromStationId: String,
    val toStationId: String,
    val departureDate: String, // e.g., "2026-07-03"
    val departureTime: String,
    val arrivalTime: String,
    val type: String,          // "TRAIN" or "COACH"
    val seatNumber: String,
    val carriageNumber: String,
    val passengerName: String,
    val ticketType: String = "Adult", // "Adult", "Student", "Child"
    val pricePaidCzk: Double,
    val qrCodeData: String = "LEOEXPRESS|$id|${fromStationId}|${toStationId}|$departureDate|$seatNumber",
    val status: String = "ACTIVE", // "ACTIVE", "CANCELLED", "REFUNDED"
    val bookingTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "passengers")
data class PassengerEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val firstName: String,
    val lastName: String,
    val birthDate: String,
    val discountType: String = "NONE", // "NONE", "STUDENT", "SENIOR", "CHILD"
    val loyaltyNumber: String = ""
)

@Entity(tableName = "payment_methods")
data class PaymentMethodEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val cardHolder: String,
    val maskedNumber: String, // e.g., "**** **** **** 4242"
    val expiryDate: String,   // e.g., "12/28"
    val cardType: String      // e.g., "Visa", "Mastercard"
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val type: String = "INFO" // "DELAY", "PLATFORM", "REMINDER", "PROMO"
)

data class UserProfile(
    val fullName: String = "Jan Novák",
    val email: String = "jan.novak@leoexpress.cz",
    val loyaltyTier: String = "Gold Tier", // "Standard", "Bronze", "Silver", "Gold"
    val loyaltyPoints: Int = 1250, // Leo Crowns (LÉO)
    val preferredLanguage: String = "en", // "cs", "sk", "pl", "de", "en"
    val currency: String = "CZK" // "CZK", "EUR"
)
