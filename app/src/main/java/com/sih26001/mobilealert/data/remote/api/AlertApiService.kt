package com.sih26001.mobilealert.data.remote.api

import com.sih26001.mobilealert.data.remote.dto.AckRequestDto
import com.sih26001.mobilealert.data.remote.dto.AckResponseDto
import com.sih26001.mobilealert.data.remote.dto.AlarmStatusDto
import com.sih26001.mobilealert.data.remote.dto.AlertDto
import com.sih26001.mobilealert.data.remote.dto.SilenceResponseDto
import com.sih26001.mobilealert.data.remote.dto.SystemStatusDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface AlertApiService {
    @GET("api/v1/alerts")
    suspend fun getAlerts(): List<AlertDto>

    @GET("api/v1/alerts/active")
    suspend fun getActiveAlerts(): List<AlertDto>

    @GET("api/v1/alerts/{alert_id}")
    suspend fun getAlertById(
        @Path("alert_id") alertId: String
    ): AlertDto

    @POST("api/v1/alerts/{alert_id}/ack")
    suspend fun acknowledgeAlert(
        @Path("alert_id") alertId: String,
        @Body request: AckRequestDto? = null
    ): AckResponseDto

    @POST("api/v1/alerts/silence")
    suspend fun silenceAlarm(): SilenceResponseDto

    @GET("api/v1/alerts/status")
    suspend fun getAlarmStatus(): AlarmStatusDto

    @GET("api/v1/system/status")
    suspend fun getSystemStatus(): SystemStatusDto
}
