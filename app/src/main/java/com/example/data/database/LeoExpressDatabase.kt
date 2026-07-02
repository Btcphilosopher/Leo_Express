package com.example.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.data.dao.LeoExpressDao
import com.example.data.model.*

@Database(
    entities = [
        StationEntity::class,
        RouteEntity::class,
        BookingEntity::class,
        PassengerEntity::class,
        PaymentMethodEntity::class,
        NotificationEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class LeoExpressDatabase : RoomDatabase() {
    abstract fun leoExpressDao(): LeoExpressDao
}
