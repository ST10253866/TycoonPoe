package vcmsa.projects.tycoonpoe

import android.content.Context
import android.view.LayoutInflater
import android.widget.Button
import android.widget.FrameLayout
import android.widget.Toast

class OfflineGameUIManager(
    private val context: Context,
    private val offlineGameEngine: OfflineGameEngine
) {

    private lateinit var gameView: OfflineGameView

    fun createContentView(): FrameLayout {
        // Inflate XML layout
        val inflater = LayoutInflater.from(context)
        val rootView = inflater.inflate(R.layout.offline_game_ui_manager, null) as FrameLayout

        // Add GameView to container inside XML
        val gameViewContainer = rootView.findViewById<FrameLayout>(R.id.gameViewContainer)
        gameView = OfflineGameView(context, null, offlineGameEngine)
        gameViewContainer.addView(gameView)

        // Hook up Play button
        val playButton = rootView.findViewById<Button>(R.id.playButton)
        playButton.setOnClickListener {
            val success = gameView.playSelectedCards()
            if (!success) {
                Toast.makeText(context, "Invalid play. Try again.", Toast.LENGTH_SHORT).show()
            }
        }

        // Hook up Pass button
        val passButton = rootView.findViewById<Button>(R.id.passButton)
        passButton.setOnClickListener {
            val passed = gameView.engine.playerPass()
            if (passed) {
                Toast.makeText(context, "You passed!", Toast.LENGTH_SHORT).show()
                gameView.invalidate()
            } else {
                Toast.makeText(context, "Cannot pass right now!", Toast.LENGTH_SHORT).show()
            }
        }

        return rootView
    }

    fun onResume() { gameView.startGame() }
    fun onPause() { gameView.stopGame() }
}
