package vcmsa.projects.tycoonpoe
import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [SettingsEntity::class], // add more later if needed
    version = 1,
    exportSchema = false
)
abstract class TycoonDatabase : RoomDatabase() {

    abstract fun settingsDao(): SettingsDao

    companion object {
        @Volatile
        private var INSTANCE: TycoonDatabase? = null

        fun getDatabase(context: Context): TycoonDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TycoonDatabase::class.java,
                    "tycoonpoe_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}