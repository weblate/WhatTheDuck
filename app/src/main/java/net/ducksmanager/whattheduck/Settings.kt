package net.ducksmanager.whattheduck

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.settings.*
import net.ducksmanager.apigateway.DmServer
import net.ducksmanager.persistence.models.coa.InducksCountryName
import net.ducksmanager.persistence.models.composite.CountryListToUpdate
import net.ducksmanager.persistence.models.composite.InducksCountryNameWithPossession
import net.ducksmanager.util.AppCompatActivityWithDrawer
import net.ducksmanager.whattheduck.WhatTheDuckApplication.appDB
import net.ducksmanager.whattheduck.WhatTheDuckApplication.applicationVersion
import retrofit2.Response

class Settings : AppCompatActivityWithDrawer() {
    override fun shouldShowToolbar() = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.settings)
        showToolbarIfExists()

        val toggleCountryListVisibility: (notifySwitch: Switch) -> Unit = {
            findViewById<View>(R.id.notifiedCountriesListWrapper).visibility = if (notifySwitch.isChecked)
                View.VISIBLE
            else
                View.GONE
        }

        val notifySwitch = findViewById<Switch>(R.id.notifySwitch)
        notifySwitch.isClickable = false
        notifySwitch.setOnClickListener {
            toggleCountryListVisibility(notifySwitch)
        }

        appDB.inducksCountryDao().findAllWithPossession().observe(this, Observer { countryNames ->
            DmServer.api.userNotificationCountries.enqueue(object : DmServer.Callback<List<String>>("getUserNotificationCountries", this) {
                override fun onSuccessfulResponse(response: Response<List<String>>?) {
                    val countriesToNotifyTo = if (response == null) HashSet() else response.body()?.toHashSet() as MutableSet<String>
                    val recyclerView = findViewById<RecyclerView>(R.id.notifiedCountriesList)
                    recyclerView.adapter = CountryToNotifyListAdapter(this@Settings, countryNames, countriesToNotifyTo)
                    recyclerView.layoutManager = LinearLayoutManager(this@Settings)

                    notifySwitch.isClickable = true
                    notifySwitch.isChecked = countriesToNotifyTo.isNotEmpty()
                    toggleCountryListVisibility(notifySwitch)
                }
            })
        })

        findViewById<TextView>(R.id.version).text = getString(R.string.version, applicationVersion)

        findViewById<Button>(R.id.save).setOnClickListener {
            val recyclerView = findViewById<RecyclerView>(R.id.notifiedCountriesList)
            val countriesToNotifyTo = (recyclerView.adapter as CountryToNotifyListAdapter).countriesToNotifyTo
            DmServer.api.updateUserNotificationCountries(CountryListToUpdate(countriesToNotifyTo))
                .enqueue(object: DmServer.Callback<Void>("updateUserNotificationCountries", this) {
                    override fun onSuccessfulResponse(response: Response<Void>?) {
                        finish()
                    }
                })
        }
    }

    class CountryToNotifyListAdapter internal constructor(
        private val context: Context,
        private var countries: MutableList<InducksCountryNameWithPossession>,
        val countriesToNotifyTo: MutableSet<String>
    ) : RecyclerView.Adapter<CountryToNotifyListAdapter.ViewHolder>() {

        private val inflater: LayoutInflater = LayoutInflater.from(context)

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val countryItemView: TextView = itemView.findViewById(R.id.itemtitle)
            val prefixImageView: ImageView = itemView.findViewById(R.id.prefiximage)
            val isNotifiedCountry: CheckBox = itemView.findViewById(R.id.isNotifiedCountry)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val itemView = inflater.inflate(R.layout.row_notified_country, parent, false)
            itemView.findViewById<ImageView>(R.id.suffiximage).visibility = View.GONE
            return ViewHolder(itemView)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val currentItem = countries[position]
            holder.countryItemView.text = currentItem.country.countryName
            holder.prefixImageView.setImageResource(getImageResourceFromCountry(currentItem.country))
            holder.isNotifiedCountry.isChecked = countriesToNotifyTo.contains(currentItem.country.countryCode)
            holder.isNotifiedCountry.setOnClickListener {
                if ((it as CheckBox).isChecked) {
                    countriesToNotifyTo.add(currentItem.country.countryCode)
                }
                else  {
                    countriesToNotifyTo.remove(currentItem.country.countryCode)
                }
            }
        }

        private fun getImageResourceFromCountry(country: InducksCountryName): Int {
            val uri = "@drawable/flags_" + country.countryCode
            var imageResource = context.resources.getIdentifier(uri, null, context.packageName)

            if (imageResource == 0) {
                imageResource = R.drawable.flags_unknown
            }
            return imageResource
        }

        override fun getItemCount() = countries.size
    }
}
