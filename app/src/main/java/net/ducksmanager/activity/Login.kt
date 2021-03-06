package net.ducksmanager.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import com.pusher.pushnotifications.auth.AuthData
import com.pusher.pushnotifications.auth.AuthDataGetter
import com.pusher.pushnotifications.auth.BeamsTokenProvider
import net.ducksmanager.api.DmServer
import net.ducksmanager.api.DmServer.getRequestHeaders
import net.ducksmanager.persistence.models.composite.SuggestionList
import net.ducksmanager.persistence.models.dm.Issue
import net.ducksmanager.persistence.models.dm.Purchase
import net.ducksmanager.persistence.models.dm.User
import net.ducksmanager.util.Settings.toSHA1
import net.ducksmanager.whattheduck.R
import net.ducksmanager.whattheduck.WhatTheDuck
import net.ducksmanager.whattheduck.databinding.LoginBinding
import retrofit2.Call
import retrofit2.Response
import java.lang.ref.WeakReference
import java.util.*

class Login : AppCompatActivity() {
    private lateinit var binding: LoginBinding

    companion object {
        fun fetchCollection(activityRef: WeakReference<Activity>, targetClass: Class<*>, alertIfError: Boolean?) {
            ItemList.type = WhatTheDuck.CollectionType.USER.toString()

            DmServer.api.userIssues.enqueue(object : DmServer.Callback<List<Issue>>("retrieveCollection", activityRef.get()!!, alertIfError!!) {
                override fun onFailure(call: Call<List<Issue>>, t: Throwable) {
                    activityRef.get()!!.startActivity(Intent(activityRef.get()!!, targetClass))
                }
                override fun onSuccessfulResponse(response: Response<List<Issue>>) {
                    val user = User(DmServer.apiDmUser!!, DmServer.apiDmPassword!!)
                    WhatTheDuck.appDB!!.userDao().insert(user)

                    WhatTheDuck.appDB!!.issueDao().deleteAll()
                    WhatTheDuck.appDB!!.issueDao().insertList(response.body()!!)

                    val apiEndpointUrl: String = WhatTheDuck.config.getProperty(WhatTheDuck.CONFIG_KEY_API_ENDPOINT_URL)

                    WhatTheDuck.tokenProvider = BeamsTokenProvider(
                        "$apiEndpointUrl/collection/notification_token",
                        object : AuthDataGetter {
                            override fun getAuthData(): AuthData {
                                return AuthData(getRequestHeaders(true), HashMap())
                            }
                        }
                    )
                    WhatTheDuck.registerForNotifications(activityRef, false)

                    activityRef.get()!!.startActivity(Intent(activityRef.get()!!, targetClass))

                    DmServer.api.userPurchases.enqueue(object : DmServer.Callback<List<Purchase>>("getPurchases", activityRef.get()!!, true) {
                        override fun onSuccessfulResponse(response: Response<List<Purchase>>) {
                            WhatTheDuck.appDB!!.purchaseDao().deleteAll()
                            WhatTheDuck.appDB!!.purchaseDao().insertList(response.body()!!)
                        }
                    })

                    DmServer.api.suggestedIssues.enqueue(object : DmServer.Callback<SuggestionList>("getSuggestedIssues", activityRef.get()!!) {
                        override fun onSuccessfulResponse(response: Response<SuggestionList>) {
                            Suggestions.loadSuggestions(response.body()!!)
                        }
                    })
                }

                override fun onErrorResponse(response: Response<List<Issue>>?) {
                    if (!alertIfError!!) {
                        activityRef.get()!!.startActivity(Intent(activityRef.get(), Login::class.java))
                    }
                }
            })
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (application as WhatTheDuck).setup()

        val user: User? = WhatTheDuck.appDB!!.userDao().currentUser
        if (user != null) {
            DmServer.apiDmUser = user.username
            DmServer.apiDmPassword = user.password
            fetchCollection(WeakReference(this@Login), CountryList::class.java, false)
        } else {
            binding = LoginBinding.inflate(layoutInflater)
            setContentView(binding.root)
            showLoginForm()
        }
    }

    private fun showLoginForm() {
        binding.endSignup.setOnClickListener {
            startActivity(Intent(this, Signup::class.java)
                .putExtra("username", binding.username.text.toString())
            )
        }

        binding.login.setOnClickListener { view: View? ->
            hideKeyboard(view)
            loginAndFetchCollection()
        }

        binding.linkToDM.setOnClickListener { view: View? ->
            hideKeyboard(view)
            this@Login.startActivity(
                Intent(Intent.ACTION_VIEW).setData(Uri.parse(WhatTheDuck.dmUrl))
            )
        }
    }

    private fun loginAndFetchCollection() {
        val username = binding.username.text.toString()
        val password = binding.password.text.toString()

        DmServer.apiDmUser = username
        DmServer.apiDmPassword = toSHA1(password, WeakReference(this))

        val activityRef = WeakReference<Activity>(this)

        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
            WhatTheDuck.alert(activityRef, R.string.input_error, R.string.input_error__empty_credentials)
            binding.progressBar.visibility = View.INVISIBLE
        } else {
            fetchCollection(activityRef, CountryList::class.java, true)
        }
    }

    private fun hideKeyboard(view: View?) {
        if (view != null) {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }
}