package com.example.colorblend.network

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.network.okHttpClient
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

object ApolloClientProvider {

    val apolloClient: ApolloClient by lazy {
        val okHttp = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()

        ApolloClient.Builder()
            .serverUrl("https://graphql.anilist.co")
            .okHttpClient(okHttp)
            .build()
    }
}