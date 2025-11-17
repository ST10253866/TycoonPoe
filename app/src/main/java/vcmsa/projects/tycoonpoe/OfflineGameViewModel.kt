package vcmsa.projects.tycoonpoe
import androidx.lifecycle.ViewModel

class OfflineGameViewModel : ViewModel() {
    // create your game rules and bot instances
    private val gameRules = GameRules()
    val botPlayer = BotPlayer(gameRules)

    // pass them to the engine
    val engine = OfflineGameEngine(gameRules, botPlayer)
}
