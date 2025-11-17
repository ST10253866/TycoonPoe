package vcmsa.projects.tycoonpoe

import androidx.appcompat.app.AppCompatActivity
import GameViewModel
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import androidx.activity.viewModels
import vcmsa.projects.tycoonpoe.databinding.ActivityOnlineGameBinding

class OnlineGameActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnlineGameBinding
    private lateinit var uiManager: OnlineGameUIManager
    private val viewModel: GameViewModel by viewModels()

    // 👇 Add this
    private var panelOpen = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnlineGameBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        uiManager = OnlineGameUIManager(this, binding, this, viewModel)
        uiManager.setupUI()
        uiManager.setupObservers()

        //  Call setupPanel to initialize the side panel
        setupPanel()
    }

    private fun setupPanel() {
        val panel = findViewById<LinearLayout>(R.id.sidePanel)
        val dimOverlay = findViewById<View>(R.id.dimOverlay)
        val btnPanel = findViewById<Button>(R.id.btnPanel)

        btnPanel.setOnClickListener {
            togglePanel(panel, dimOverlay)
        }

        dimOverlay.setOnClickListener {
            togglePanel(panel, dimOverlay)
        }
    }

    private fun togglePanel(panel: LinearLayout, dimOverlay: View) {
        val translationX = if (panelOpen) -panel.width.toFloat() else 0f
        panel.animate().translationX(translationX).setDuration(300).start()

        if (panelOpen) {
            dimOverlay.animate().alpha(0f).setDuration(300).withEndAction {
                dimOverlay.visibility = View.GONE
            }.start()
        } else {
            dimOverlay.visibility = View.VISIBLE
            dimOverlay.animate().alpha(1f).setDuration(300).start()
        }

        panelOpen = !panelOpen
    }

    override fun onResume() {
        super.onResume()
        uiManager.onResume()
    }

    override fun onPause() {
        super.onPause()
        uiManager.onPause()
    }
}
