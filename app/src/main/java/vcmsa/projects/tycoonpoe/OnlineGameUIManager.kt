package vcmsa.projects.tycoonpoe

import GameViewModel
import android.content.Context
import android.view.View
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.*
import vcmsa.projects.tycoonpoe.databinding.ActivityOnlineGameBinding

class OnlineGameUIManager(
    private val context: Context,
    private val binding: ActivityOnlineGameBinding,
    private val viewLifecycleOwner: LifecycleOwner,
    private val viewModel: GameViewModel
) {

    private var rules = GameController { log(it) }
    private var exchangeTimerJob: Job? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    /** -------------------------
     *  Setup UI & Button Listeners
     * ------------------------- */
    fun setupUI() {
        rules = GameController()

        binding.gameView.selectionListener = object : GameView.SelectionListener {
            override fun onSelectionChanged(selectedCodes: List<String>) {
                log("Selected: ${selectedCodes.joinToString(",")}")
            }
        }

        binding.btnConnect.setOnClickListener {
            val sessionId = binding.edtRoomId.text.toString().toIntOrNull()
            if (sessionId != null) viewModel.connectSse(sessionId)
            else log("⚠️ Invalid session ID")
        }

        binding.btnSendMessage.setOnClickListener {
            val msg = binding.edtMessage.text.toString()
            val sessionId = binding.edtRoomId.text.toString().toIntOrNull()
            if (sessionId != null) viewModel.sendMessage(sessionId, msg)
            else log("⚠️ Invalid session ID")
        }

        binding.btnPass.setOnClickListener { handlePassButtonClick() }
        binding.btnPlay.setOnClickListener { handlePlayButtonClick() }

        binding.btnPanel.setOnClickListener {
            val isOpen = binding.sidePanel.translationX == 0f
            val targetX = if (isOpen) -binding.sidePanel.width.toFloat() else 0f
            binding.sidePanel.animate().translationX(targetX).setDuration(300).start()
        }
    }

    /** -------------------------
     *  Setup LiveData Observers
     * ------------------------- */
    fun setupObservers() {
        viewModel.hand.observe(viewLifecycleOwner) { hand ->
            val sortedHand = rules.sortHand(hand)
            binding.gameView.setPlayerHandFromCodes(sortedHand)
            if (hand != sortedHand) viewModel.updateHand(sortedHand)
        }

        viewModel.pot.observe(viewLifecycleOwner) { binding.gameView.setPotFromCodes(it) }
        viewModel.turnOrder.observe(viewLifecycleOwner) { order ->
            val pid = viewModel.playerId.value
            val counts = viewModel.counts.value ?: emptyMap()
            binding.gameView.setPlayersFromTurnOrder(order, pid, counts)
        }

        viewModel.counts.observe(viewLifecycleOwner) { counts ->
            val pid = viewModel.playerId.value
            val order = viewModel.turnOrder.value ?: emptyList()
            binding.gameView.setPlayersFromTurnOrder(order, pid, counts)
            counts.forEach { (playerId, count) ->
                if (playerId != pid) binding.gameView.setOtherPlayerCount(playerId, count)
            }
        }

        viewModel.currentTurn.observe(viewLifecycleOwner) { updateTurnUI() }

        // FIXED: handle nullable strings safely
        viewModel.log.observe(viewLifecycleOwner) { msg ->
            binding.txtLog.append((msg ?: "") + "\n")
        }

        viewModel.lastMessage.observe(viewLifecycleOwner) { msg ->
            binding.txtMessages.text = msg ?: ""
        }

        viewModel.roundMessage.observe(viewLifecycleOwner) { msg ->
            if (msg != null) handleRoundMessage(msg)
        }
    }

    /** -------------------------
     *  Handle Play / Pass Buttons
     * ------------------------- */
    private fun handlePlayButtonClick() {
        val selected = binding.gameView.getSelectedCardCodes()
        if (selected.isEmpty()) {
            log("⚠️ No cards selected to play")
            return
        }

        val sessionId = binding.edtRoomId.text.toString().toIntOrNull()
        if (sessionId == null) {
            log("⚠️ Need session")
            return
        }

        val pid = viewModel.playerId.value
        if (pid == null) {
            log("⚠️ Waiting for your playerId from server...")
            return
        }

        val potState = viewModel.pot.value ?: emptyList()
        if (rules.isValidPlayAgainstPot(selected, potState)) {
            val currentHand = viewModel.hand.value ?: emptyList()
            val newHand = currentHand.toMutableList().apply { removeAll(selected.toSet()) }
            viewModel.updateHand(newHand)
            viewModel.tryPlay(sessionId, selected, rules)
            binding.gameView.setPlayerHandFromCodes(newHand)

            binding.btnPlay.visibility = View.INVISIBLE
            binding.btnPass.visibility = View.INVISIBLE
        } else {
            log("Denied by controller")
        }

        binding.gameView.clearSelection()
    }

    private fun handlePassButtonClick() {
        val sessionId = binding.edtRoomId.text.toString().toIntOrNull()
        if (sessionId == null) {
            log("⚠️ Need session")
            return
        }
        viewModel.sendPass(sessionId)
    }

    /** -------------------------
     *  Round Message Handler
     * ------------------------- */
    private fun handleRoundMessage(msg: String) {
        when (msg) {
            "Select cards to exchange" -> startExchangePhase()
            "Exchange complete" -> endExchangePhase()
        }
    }

    private fun startExchangePhase() {
        binding.btnPlay.text = "Exchange"
        binding.btnPass.visibility = View.GONE
        binding.btnPlay.visibility = View.VISIBLE

        val sessionId = binding.edtRoomId.text.toString().toIntOrNull() ?: return

        startExchangeTimer(20000L) {
            val hand = viewModel.hand.value ?: emptyList()
            val forcedSelection = hand.take(2)
            if (forcedSelection.isNotEmpty()) {
                log("Timer expired: forcing exchange with ${forcedSelection.joinToString(",")}")
                viewModel.exchangeRequest(sessionId, forcedSelection)
                binding.btnPlay.visibility = View.INVISIBLE
            }
        }

        binding.btnPlay.setOnClickListener {
            val selected = binding.gameView.getSelectedCardCodes()
            if (selected.isEmpty()) {
                log("⚠️ No cards selected for exchange")
                return@setOnClickListener
            }
            exchangeTimerJob?.cancel()
            log("Exchange completed early by user!")
            viewModel.exchangeRequest(sessionId, selected)
            binding.btnPlay.visibility = View.INVISIBLE
        }
    }

    private fun endExchangePhase() {
        binding.btnPlay.text = "Play"
        binding.btnPlay.setOnClickListener { handlePlayButtonClick() }
        updateTurnUI()
    }

    /** -------------------------
     *  Turn / UI Helpers
     * ------------------------- */
    private fun updateTurnUI() {
        val myId = viewModel.playerId.value
        val current = viewModel.currentTurn.value
        val isMyTurn = (myId != null && myId == current)
        binding.btnPlay.visibility = if (isMyTurn) View.VISIBLE else View.INVISIBLE
        binding.btnPass.visibility = if (isMyTurn) View.VISIBLE else View.INVISIBLE
    }

    private fun log(msg: String) {
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault())
            .format(java.util.Date())
        val line = "[$timestamp] $msg"
        println(line)
        binding.txtLog.append(line + "\n")
    }

    /** -------------------------
     *  Exchange Timer
     * ------------------------- */
    private fun startExchangeTimer(durationMillis: Long = 20000L, onTimeout: () -> Unit) {
        exchangeTimerJob?.cancel()
        exchangeTimerJob = coroutineScope.launch {
            var elapsed = 0L
            val tickInterval = 1000L
            while (elapsed < durationMillis) {
                delay(tickInterval)
                elapsed += tickInterval
                val secondsLeft = (durationMillis - elapsed) / 1000
                binding.txtLog.append("Time left: $secondsLeft\n")
            }
            onTimeout()
        }
    }

    fun onResume() { binding.gameView.startGame() }
    fun onPause() { binding.gameView.stopGame() }
}
