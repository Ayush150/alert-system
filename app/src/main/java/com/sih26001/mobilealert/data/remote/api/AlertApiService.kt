package com.sih26001.mobilealert.data.remote.api

import com.sih26001.mobilealert.data.remote.dto.AlertDto
import retrofit2.http.GET

interface AlertApiService {
    @GET("api/v1/alerts")
    suspend fun getAlerts(): List<AlertDto>
}
