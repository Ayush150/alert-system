package com.sih26001.mobilealert.data.remote

import com.sih26001.mobilealert.core.util.Environment
import com.sih26001.mobilealert.core.util.EnvironmentConfig
import com.sih26001.mobilealert.data.remote.api.AlertApiService
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.Proxy
import java.util.concurrent.TimeUnit

object RetrofitProvider {

    private fun provideOkHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)

        if (EnvironmentConfig.CURRENT_ENV == Environment.DEBUG) {
            builder.proxy(Proxy.NO_PROXY)

            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            builder.addInterceptor(loggingInterceptor)
        }

        return builder.build()
    }

    private fun provideRetrofit(baseUrl: String, okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    fun provideAlertApiService(): AlertApiService {
        val okHttpClient = provideOkHttpClient()
        val retrofit = provideRetrofit(EnvironmentConfig.BASE_URL, okHttpClient)
        return retrofit.create(AlertApiService::class.java)
    }
}
