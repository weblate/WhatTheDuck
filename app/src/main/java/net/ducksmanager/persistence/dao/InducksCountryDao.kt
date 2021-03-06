package net.ducksmanager.persistence.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import net.ducksmanager.persistence.models.coa.InducksCountryName
import net.ducksmanager.persistence.models.composite.InducksCountryNameWithPossession

@Dao
interface InducksCountryDao {
    @Query("SELECT DISTINCT inducks_countryname.*, CASE WHEN issues.country IS NULL THEN 0 ELSE 1 END AS isPossessed FROM inducks_countryname LEFT JOIN issues ON inducks_countryname.countryCode = issues.country ORDER BY inducks_countryname.countryName")
    fun findAllWithPossession(): LiveData<List<InducksCountryNameWithPossession>>

    @Query("SELECT * FROM inducks_countryname WHERE countryCode = :countryCode")
    fun findByCountryCode(countryCode: String?): LiveData<InducksCountryName>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertList(issueList: List<InducksCountryName>)

    @Query("DELETE FROM inducks_countryname")
    fun deleteAll()
}