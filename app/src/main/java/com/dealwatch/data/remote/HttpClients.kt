package com.dealwatch.data.remote

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/** A single shared OkHttpClient so finders reuse the connection pool. */
object HttpClients {
    val shared: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }
}
