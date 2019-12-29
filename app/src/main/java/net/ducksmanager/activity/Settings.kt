package net.ducksmanager.activity

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.row.view.*
import kotlinx.android.synthetic.main.settings.*
import net.ducksmanager.api.DmServer
import net.ducksmanager.persistence.models.coa.InducksCountryName
import net.ducksmanager.persistence.models.composite.CountryListToUpdate
import net.ducksmanager.persistence.models.composite.InducksCountryNameWithPossession
import net.ducksmanager.util.AppCompatActivityWithDrawer
import net.ducksmanager.whattheduck.R
import net.ducksmanager.whattheduck.WhatTheDuck.Companion.appDB
import net.ducksmanager.whattheduck.WhatTheDuck.Companion.applicationVersion
import net.ducksmanager.whattheduck.databinding.SettingsBinding
import retrofit2.Response

class Settings : AppCompatActivityWithDrawer() {
    private lateinit var binding: SettingsBinding
    
    override fun shouldShowToolbar() = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = SettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        showToolbarIfExists()

        val toggleCountryListVisibility: (notifySwitch: Switch) -> Unit = {
            binding.notifiedCountriesListWrapper.visibility = if (notifySwitch.isChecked)
                View.VISIBLE
            else
                View.GONE
        }

        val notifySwitch = binding.notifySwitch
        notifySwitch.isClickable = false
        notifySwitch.setOnClickListener {
            toggleCountryListVisibility(notifySwitch)
        }

        appDB.inducksCountryDao().findAllWithPossession().observe(this, Observer { countryNames ->
            DmServer.api.userNotificationCountries.enqueue(object : DmServer.Callback<List<String>>("getUserNotificationCountries", this) {
                override fun onSuccessfulResponse(response: Response<List<String>>) {
                    val countriesToNotifyTo = response.body()?.toHashSet() as MutableSet<String>
                    val recyclerView = binding.notifiedCountriesList
                    recyclerView.adapter = CountryToNotifyListAdapter(this@Settings, countryNames, countriesToNotifyTo)
                    recyclerView.layoutManager = LinearLayoutManager(this@Settings)

                    notifySwitch.isClickable = true
                    notifySwitch.isChecked = countriesToNotifyTo.isNotEmpty()
                    toggleCountryListVisibility(notifySwitch)
                }
            })
        })

        binding.version.text = getString(R.string.version, applicationVersion)

        binding.save.setOnClickListener {
            val recyclerView = binding.notifiedCountriesList
            val countriesToNotifyTo = (recyclerView.adapter as CountryToNotifyListAdapter).countriesToNotifyTo
            DmServer.api.updateUserNotificationCountries(CountryListToUpdate(countriesToNotifyTo))
                .enqueue(object: DmServer.Callback<Void>("updateUserNotificationCountries", this) {
                    override fun onSuccessfulResponse(response: Response<Void>) {
                        finish()
                    }
                })
        }
    }

    class CountryToNotifyListAdapter internal constructor(
        private val context: Context,
        private var countries: List<InducksCountryNameWithPossession>,
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
            itemView.suffiximage.visibility = View.GONE
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