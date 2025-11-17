package vcmsa.projects.tycoonpoe

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    // Language chosen from spinner (e.g. "English", "Japanese")
    val language: String,

    // The resource ID or name of the selected card back (e.g. "card_back1")
    val selectedCardBack: String,

    // Whether biometrics are disabled (true = disabled, false = enabled)
    val biometricsDisabled: Boolean
)