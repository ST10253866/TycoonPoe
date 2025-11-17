package vcmsa.projects.tycoonpoe

import androidx.room.*

@Dao
interface SettingsDao {

    @Query("SELECT * FROM settings LIMIT 1")
    suspend fun getSettings(): SettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: SettingsEntity)

    @Query("DELETE FROM settings")
    suspend fun clearSettings()
}