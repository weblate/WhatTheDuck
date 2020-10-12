package net.ducksmanager.api

import android.app.Activity
import android.view.View
import android.widget.ProgressBar
import com.google.gson.GsonBuilder
import net.ducksmanager.persistence.models.internal.Sync
import net.ducksmanager.whattheduck.R
import net.ducksmanager.whattheduck.WhatTheDuck
import net.ducksmanager.whattheduck.WhatTheDuck.Companion.isOfflineMode
import okhttp3.Credentials
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.lang.ref.WeakReference
import java.net.HttpURLConnection.HTTP_NOT_FOUND
import java.net.HttpURLConnection.HTTP_UNAUTHORIZED
import java.time.Instant
import java.util.*

class DmServer {
    companion object {
        const val EVENT_RETRIEVE_COLLECTION = "retrieveCollection"
        const val EVENT_RETRIEVE_ALL_PUBLICATIONS = "retrieveAllPublications"
        const val EVENT_GET_PURCHASES = "getPurchases"
        const val EVENT_GET_SUGGESTED_ISSUES = "getSuggestedIssues"
        const val EVENT_GET_USER_NOTIFICATION_COUNTRIES = "getUserNotificationCountries"
        const val EVENT_RETRIEVE_ALL_COUNTRIES = "getInducksCountries"

        lateinit var completedSyncs: HashMap<String, Boolean>

        lateinit var api: DmServerApi

        var apiDmUser: String? = null
        var apiDmPassword: String? = null

        fun getRequestHeaders(withUserCredentials: Boolean): Map<String, String> {
            val filledUserCredentials = withUserCredentials && apiDmUser != null && apiDmPassword != null
            return hashMapOf(
                "Authorization" to Credentials.basic(
                    WhatTheDuck.config.getProperty(WhatTheDuck.CONFIG_KEY_ROLE_NAME),
                    WhatTheDuck.config.getProperty(WhatTheDuck.CONFIG_KEY_ROLE_PASSWORD)
                ),
                "x-dm-version" to WhatTheDuck.applicationVersion,
                "x-dm-user" to if (filledUserCredentials) apiDmUser!! else "",
                "x-dm-pass" to if (filledUserCredentials) apiDmPassword!! else ""
            ).toMap()
        }

        fun initApi() {
            completedSyncs = hashMapOf(
                EVENT_RETRIEVE_COLLECTION to false,
                EVENT_RETRIEVE_ALL_PUBLICATIONS to false,
                EVENT_GET_PURCHASES to false,
                EVENT_GET_SUGGESTED_ISSUES to false,
                EVENT_GET_USER_NOTIFICATION_COUNTRIES to false,
                EVENT_RETRIEVE_ALL_COUNTRIES to false,
            )
            val gson = GsonBuilder()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
                .create()
            val interceptor = HttpLoggingInterceptor()
            interceptor.level = HttpLoggingInterceptor.Level.BODY
            val okHttpClient = OkHttpClient().newBuilder()
                .addInterceptor(Interceptor { chain: Interceptor.Chain ->
                    val builder = chain.request().newBuilder()
                    val headers = getRequestHeaders(apiDmUser != null)
                    for (headerKey in headers.keys) {
                        builder.header(headerKey, Objects.requireNonNull(headers[headerKey])!!)
                    }
                    val newRequest = builder.build()
                    chain.proceed(newRequest)
                })
                .addInterceptor(interceptor)
                .build()
            val retrofit = Retrofit.Builder()
                .baseUrl(WhatTheDuck.config.getProperty(WhatTheDuck.CONFIG_KEY_API_ENDPOINT_URL))
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
            api = retrofit.create(DmServerApi::class.java)
        }
    }

    abstract class Callback<T> protected constructor(
        private val eventName: String,
        originActivity: Activity,
        private val alertOnError: Boolean
    ) : retrofit2.Callback<T> {
        private val originActivityRef: WeakReference<Activity>

        init {
            println("API call start : $eventName")
            WhatTheDuck.trackEvent("$eventName/start")
            originActivityRef = WeakReference(originActivity)
            originActivityRef.get()!!.findViewById<View>(R.id.progressBar)?.visibility = ProgressBar.VISIBLE
        }

        abstract fun onSuccessfulResponse(response: Response<T>)

        override fun onResponse(call: Call<T>, response: Response<T>) {
            if (response.isSuccessful) {
                isOfflineMode = false
                onSuccessfulResponse(response)
                logSync()
            } else {
                if (alertOnError) {
                    when (response.code()) {
                        HTTP_UNAUTHORIZED -> WhatTheDuck.alert(originActivityRef, R.string.input_error__invalid_credentials)
                        HTTP_NOT_FOUND -> WhatTheDuck.alert(originActivityRef, R.string.offline_mode_cannot_login)
                        else -> WhatTheDuck.alert(originActivityRef, R.string.error)
                    }
                }
                onErrorResponse(response)
                if (response.code() == HTTP_NOT_FOUND) {
                    isOfflineMode = true
                    onFailureFailover()
                    onFinished()
                }
                originActivityRef.get()!!.findViewById<View?>(R.id.progressBar)?.visibility = ProgressBar.GONE
            }
            onFinished()
        }

        open fun onErrorResponse(response: Response<T>?) {}

        open fun onFailureFailover() {}

        override fun onFailure(call: Call<T>, t: Throwable) {
            isOfflineMode = true
            onFailureFailover()
            originActivityRef.get()!!.findViewById<View?>(R.id.progressBar)?.visibility = ProgressBar.GONE
            onFinished()
        }

        private fun onFinished() {
            println("API call end : $eventName")
            WhatTheDuck.trackEvent("$eventName/finish")
        }

        private fun logSync() {
            if (completedSyncs.containsKey(eventName)) {
                completedSyncs[eventName] = true
            }
            if (completedSyncs.values.all { it }) {
                WhatTheDuck.appDB!!.syncDao().insert(Sync(Instant.now()))
            }
        }
    }
}