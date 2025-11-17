package vcmsa.projects.tycoonpoe

import android.util.Log

class OfflineGameEngine(
    internal val gameRules: GameRules,
    internal val bot: BotPlayer
) {
    private val fullDeck = listOf(
        "3C", "3D", "3H", "3S", "4C", "4D", "4H", "4S", "5C", "5D", "5H", "5S",
        "6C", "6D", "6H", "6S", "7C", "7D", "7H", "7S", "8C", "8D", "8H", "8S",
        "9C", "9D", "9H", "9S", "10C", "10D", "10H", "10S", "JC", "JD", "JH", "JS",
        "QC", "QD", "QH", "QS", "KC", "KD", "KH", "KS", "AC", "AD", "AH", "AS",
        "2C", "2D", "2H", "2S", "Joker", "Joker"
    )

    internal var playerHand: MutableList<String> = mutableListOf()
    var currentPlayerTurn = true
    var isRoundActive = true
    var isGameOver = false

    private var playerWins = 0
    private var botWins = 0
    private var totalRounds = 0
    private var lastPlayerToPlay: String? = null
    private var roundNumber = 1
    private var lastRoundLoser: String? = null

    init {
        Log.d("OfflineGameEngine", "OfflineGameEngine created")
        startNewRound()
    }

    private fun dealCards() {
        val shuffledDeck = fullDeck.shuffled()
        val half = shuffledDeck.size / 2
        playerHand = shuffledDeck.take(half).toMutableList()
        bot.hand = shuffledDeck.drop(half).toMutableList()
    }

    fun getPlayerHand(): List<String> = playerHand.toList()
    fun getBotHand(): List<String> = bot.hand.toList()
    fun getFullPot(): List<String> = gameRules.fullPot


    fun playerPlay(hand: List<String>): Boolean {
        if (!currentPlayerTurn || !playerHand.containsAll(hand)) return false

        val previousPot = gameRules.currentPot.toList()

        val success = gameRules.playHand(hand)
        if (!success) return false

        gameRules.currentPot = hand.toMutableList()

        // Remove only one instance per played card
        hand.forEach { card -> playerHand.remove(card) }

        lastPlayerToPlay = "player"

        if (playerHand.isEmpty()) {
            playerWins++
            println("Player wins round!")
            checkGameOver()
            return true
        }

        val clearsPot = hand.any { gameRules.getCardValue(it) == "8" } ||
                (previousPot == listOf("Joker") && hand == listOf("3S"))

        if (clearsPot) {
            println("Player clears pot (8, four-of-a-kind, or 3♠ beats Joker).")
            endPot("player")
            return true
        }

        currentPlayerTurn = false
        return true
    }

    fun botPlay(): List<String>? {
        if (currentPlayerTurn || isGameOver) return null

        val previousPot = gameRules.currentPot.toList()

        val botMove = bot.makeMove(previousPot)
        if (botMove != null) {
            val success = gameRules.playHand(botMove)
            if (!success) {
                println("Bot tried invalid move.")
                currentPlayerTurn = true
                return null
            }

            gameRules.currentPot = botMove.toMutableList()

            // Remove only one instance per played card
            botMove.forEach { card -> bot.hand.remove(card) }

            lastPlayerToPlay = "bot"

            if (bot.hand.isEmpty()) {
                botWins++
                println("Bot wins round!")
                checkGameOver()
                return botMove
            }

            val clearsPot = botMove.any { gameRules.getCardValue(it) == "8" } ||
                    (previousPot == listOf("Joker") && botMove == listOf("3S"))

            if (clearsPot) {
                println("Bot clears pot (8, four-of-a-kind, or 3♠ beats Joker).")
                endPot("bot")
                return botMove
            }
        } else {
            botPass()
            return null
        }

        currentPlayerTurn = true
        return botMove
    }
    fun playerPass(): Boolean {
        if (!currentPlayerTurn || isGameOver) return false
        println("Player passes.")
        if (lastPlayerToPlay == "bot") {
            endPot("bot")
        }
        currentPlayerTurn = false

        return true
    }

    fun botPass() {
        if (currentPlayerTurn || isGameOver) return
        println("Bot passes.")
        if (lastPlayerToPlay == "player") {
            endPot("player")
        }
        currentPlayerTurn = true
    }

    private fun endPot(lastToPlay: String) {
        println("Pot ends. Last winning move was by: $lastToPlay")
        gameRules.clearPotOnly()
        lastPlayerToPlay = lastToPlay
        currentPlayerTurn = (lastToPlay == "player")
        println("Next turn: ${if (currentPlayerTurn) "player" else "bot"}")
    }

    private fun checkGameOver() {
        totalRounds++

        if (playerHand.isEmpty() || bot.hand.isEmpty()) {
            if (playerHand.isEmpty()) {
                playerWins++
                lastRoundLoser = "bot"  // Bot lost because player finished cards
                println("Player wins round!")
            }
            if (bot.hand.isEmpty()) {
                botWins++
                lastRoundLoser = "player" // Player lost because bot finished cards
                println("Bot wins round!")
            }

            println("=== Round Over ===")
            println("Player Wins: $playerWins | Bot Wins: $botWins")

            if (totalRounds >= 3) {
                println("=== Game Over ===")
                if (playerWins > botWins) {
                    println(" Player is the Tycoon!")
                } else if (botWins > playerWins) {
                    println(" Bot is the Tycoon!")
                } else {
                    println("It's a draw. Both remain commoners.")
                }
                isGameOver = true
            } else {
                startNewRound()
            }
        }
    }
    private fun doTradingPhase() {
        println("Trading phase begins...")

        if (lastRoundLoser == "player") {
            val playerToGive = playerHand.sortedByDescending { gameRules.getCardStrength(it) }.take(2)
            val botToGive = bot.hand.sortedBy { gameRules.getCardStrength(it) }.take(2)

            playerHand.removeAll(playerToGive)
            bot.hand.addAll(playerToGive)

            bot.hand.removeAll(botToGive)
            playerHand.addAll(botToGive)

            println("Player gave highest cards: $playerToGive")
            println("Bot gave lowest cards: $botToGive")
        } else if (lastRoundLoser == "bot") {
            val botToGive = bot.hand.sortedByDescending { gameRules.getCardStrength(it) }.take(2)

            // For now, just take first 2 from player as any 2
            val playerToGive = playerHand.take(2)

            bot.hand.removeAll(botToGive)
            playerHand.addAll(botToGive)

            playerHand.removeAll(playerToGive)
            bot.hand.addAll(playerToGive)

            println("Bot gave highest cards: $botToGive")
            println("Player gave 2 cards: $playerToGive")
        }
    }

    private fun startNewRound() {
        println("Starting round $roundNumber...")
        isGameOver = false
        isRoundActive = true
        lastPlayerToPlay = null
        gameRules.resetRound()
        dealCards()


        if (roundNumber == 1) {
            currentPlayerTurn = playerHand.contains("3D")
            println("Starting player: ${if (currentPlayerTurn) "Player (has 3♦)" else "Bot (has 3♦)"}")
        } else {
            currentPlayerTurn = lastRoundLoser != "player"
            println("Starting player: ${if (currentPlayerTurn) "Player (lost last round)" else "Bot (lost last round)"}")
        }
        if (!currentPlayerTurn && !isGameOver) {
            botPlay()
        }

        if (roundNumber in listOf(2, 3)) {
            doTradingPhase()
        }

        roundNumber++
    }
}
