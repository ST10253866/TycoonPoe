package vcmsa.projects.tycoonpoe

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity

class OfflineGameActivity : AppCompatActivity() {

    // Get the ViewModel (will survive rotation)
    private val viewModel: OfflineGameViewModel by viewModels()
    private lateinit var offlineGameUIManager: OfflineGameUIManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Optional: hide the ActionBar
        supportActionBar?.hide()

        // Pass the engine from ViewModel to your UIManager
        offlineGameUIManager = OfflineGameUIManager(this, viewModel.engine)
        setContentView(offlineGameUIManager.createContentView())
    }

    override fun onResume() {
        super.onResume()
        offlineGameUIManager.onResume()
    }

    override fun onPause() {
        super.onPause()
        offlineGameUIManager.onPause()
    }
}
