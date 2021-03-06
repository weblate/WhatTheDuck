package net.ducksmanager.activity

import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import net.ducksmanager.adapter.CountryAdapter
import net.ducksmanager.adapter.ItemAdapter
import net.ducksmanager.api.DmServer
import net.ducksmanager.persistence.models.coa.InducksCountryName
import net.ducksmanager.persistence.models.composite.InducksCountryNameWithPossession
import net.ducksmanager.util.ReleaseNotes
import net.ducksmanager.util.Settings
import net.ducksmanager.whattheduck.R
import net.ducksmanager.whattheduck.WhatTheDuck
import net.ducksmanager.whattheduck.WhatTheDuck.Companion.appDB
import retrofit2.Response
import java.lang.ref.WeakReference
import java.util.*

class CountryList : ItemList<InducksCountryNameWithPossession>() {

    override val itemAdapter: ItemAdapter<InducksCountryNameWithPossession>
        get() = CountryAdapter(this, data)

    override fun downloadList(currentActivity: Activity) {
        DmServer.api.getCountries(WhatTheDuck.locale).enqueue(object : DmServer.Callback<HashMap<String, String>>("getInducksCountries", currentActivity) {
            override val isFailureAllowed = true

            override fun onSuccessfulResponse(response: Response<HashMap<String, String>>) {
                val countries: List<InducksCountryName> = response.body()!!.keys.map { countryCode ->
                    InducksCountryName(countryCode, response.body()!![countryCode]!!)
                }
                appDB!!.inducksCountryDao().deleteAll()
                appDB!!.inducksCountryDao().insertList(countries)
                setData()
            }

            override fun onFailureFailover() {
                isOfflineMode = true
                findViewById<LinearLayout>(R.id.action_logout).visibility = View.GONE
                setData()
            }
        })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Settings.shouldShowMessage(Settings.MESSAGE_KEY_WELCOME)) {
            val builder = AlertDialog.Builder(this@CountryList)
            builder.setTitle(getString(R.string.welcomeTitle))
            builder.setMessage(getString(R.string.welcomeMessage))
            builder.setPositiveButton(R.string.ok) { dialogInterface: DialogInterface, _: Int ->
                ReleaseNotes.current.showOnVersionUpdate(WeakReference(this@CountryList))
                dialogInterface.dismiss()
            }
            Settings.addToMessagesAlreadyShown(Settings.MESSAGE_KEY_WELCOME)
            builder.create().show()
        } else {
            ReleaseNotes.current.showOnVersionUpdate(WeakReference(this))
        }
        WhatTheDuck.selectedCountry = null
        WhatTheDuck.selectedPublication = null
        show()
    }

    override fun isPossessedByUser() = true

    override fun getList() = appDB!!.inducksCountryDao().findAllWithPossession()

    override fun shouldShow() = true

    override fun shouldShowNavigationCountry() = false

    override fun shouldShowNavigationPublication() =  false

    override fun shouldShowToolbar() = true

    override fun shouldShowAddToCollectionButton() = !isOfflineMode

    override fun shouldShowFilter(items: List<InducksCountryNameWithPossession>) = items.size > MIN_ITEM_NUMBER_FOR_FILTER

    override fun hasDividers() = true

    override fun onBackPressed() {
        if (isCoaList()) {
            onBackFromAddIssueActivity()
        }
    }

    override fun shouldShowItemSelectionTip() = false

    override fun shouldShowSelectionValidation() = false
}