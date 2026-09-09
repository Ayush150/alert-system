package com.sih26001.mobilealert.data.local

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.sih26001.mobilealert.domain.model.AffectedAsset
import com.sih26001.mobilealert.domain.model.AlertSeverity
import com.sih26001.mobilealert.domain.model.AlertStatus
import java.time.Instant

class Converters {

    private val gson = Gson()

    @TypeConverter
    fun fromInstant(instant: Instant?): Long? {
        return instant?.toEpochMilli()
    }

    @TypeConverter
    fun toInstant(millis: Long?): Instant? {
        return millis?.let { Instant.ofEpochMilli(it) }
    }

    @TypeConverter
    fun fromAlertSeverity(severity: AlertSeverity): String {
        return severity.name
    }

    @TypeConverter
    fun toAlertSeverity(severityString: String): AlertSeverity {
        return try {
            AlertSeverity.valueOf(severityString)
        } catch (e: IllegalArgumentException) {
            AlertSeverity.NORMAL
        }
    }

    @TypeConverter
    fun fromAlertStatus(status: AlertStatus): String {
        return status.name
    }

    @TypeConverter
    fun toAlertStatus(statusString: String): AlertStatus {
        return try {
            AlertStatus.valueOf(statusString)
        } catch (e: IllegalArgumentException) {
            AlertStatus.RECEIVED
        }
    }

    @TypeConverter
    fun fromStringList(list: List<String>?): String? {
        if (list == null) return null
        return gson.toJson(list)
    }

    @TypeConverter
    fun toStringList(json: String?): List<String>? {
        if (json == null) return null
        val type = object : TypeToken<List<String>>() {}.type
        return try {
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromAffectedAssetList(list: List<AffectedAsset>?): String? {
        if (list == null) return null
        return gson.toJson(list)
    }

    @TypeConverter
    fun toAffectedAssetList(json: String?): List<AffectedAsset>? {
        if (json == null) return null
        val type = object : TypeToken<List<AffectedAsset>>() {}.type
        return try {
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }
}
