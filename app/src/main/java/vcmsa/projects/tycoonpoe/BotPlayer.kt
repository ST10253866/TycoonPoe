package vcmsa.projects.tycoonpoe

class BotPlayer(private val gameRules: GameRules) {
    var hand: MutableList<String> = mutableListOf()

    fun makeMove(pot: List<String>): List<String>? {
        println("Bot making move. Current pot: $pot")

        val possibleHands = if (pot.isEmpty())
            generateAllValidHands(hand)
        else
            generatePossibleHands(hand, pot.size)

        // Evaluate all possible hands
        val scoredHands = possibleHands
            .filter { handAttempt ->
                val isValid = gameRules.isValidHand(handAttempt)
                val beatsPot = gameRules.beatsCurrentPot(handAttempt, pot)
                println("Checking hand $handAttempt: valid=$isValid, beatsPot=$beatsPot")
                isValid && beatsPot
            }
            .map { handAttempt ->
                handAttempt to scoreHand(handAttempt)
            }
            .sortedWith(compareBy({ if (gameRules.isRevolution) -it.second else it.second }))

        // If there are valid options
        if (scoredHands.isNotEmpty()) {
            val (chosenHand, score) = scoredHands.first()
            hand.removeAll(chosenHand)
            println("Bot plays: $chosenHand with score $score")
            return chosenHand
        }

        println("Bot passes.")
        return null
    }

    private fun scoreHand(cards: List<String>): Int {
        var score = 0
        for (card in cards) {
            val value = gameRules.getCardValue(card)
            val cardScore = when {
                value == "Joker" -> 100
                value == "2" -> 50 + gameRules.getCardIndex(value)
                else -> gameRules.getCardIndex(value)
            }
            score += cardScore
        }

        if (cards.count { gameRules.getCardValue(it) == "Joker" } > 1) score += 100

        return if (gameRules.isRevolution) -score else score
    }

    private fun generatePossibleHands(cards: List<String>, handSize: Int): List<List<String>> {
        if (handSize == 0) return listOf(emptyList())
        val possibleHands = mutableListOf<List<String>>()

        fun combinations(current: List<String>, start: Int) {
            if (current.size == handSize) {
                possibleHands.add(current)
                return
            }
            for (i in start until cards.size) {
                combinations(current + cards[i], i + 1)
            }
        }

        combinations(emptyList(), 0)
        return possibleHands
    }
    private fun generateAllValidHands(cards: List<String>): List<List<String>> {
        val grouped = cards.groupBy { cardValueWithoutSuit(it) }
        val allHands = mutableListOf<List<String>>()

        // Singles
        allHands += cards.map { listOf(it) }

        // Pairs, Triples, Quads
        for ((_, sameValueCards) in grouped) {
            val count = sameValueCards.size
            if (count >= 2) allHands += listOf(sameValueCards.take(2))
            if (count >= 3) allHands += listOf(sameValueCards.take(3))
            if (count >= 4) allHands += listOf(sameValueCards.take(4))
        }

        // Optional: Revolution set (e.g., quad + joker)
        val jokers = cards.filter { isJoker(it) }
        for ((_, sameValueCards) in grouped) {
            if (sameValueCards.size == 3 && jokers.isNotEmpty()) {
                allHands += listOf(sameValueCards + jokers.first()) // 3 + Joker
            }
        }

        return allHands.filter { gameRules.isValidHand(it) }
    }
    fun cardValueWithoutSuit(card: String): String {
        return if (card.length == 3) card.substring(0, 2) else card[0].toString()
    }

    fun isJoker(card: String): Boolean {
        return card.equals("JOKER", ignoreCase = true)
    }
  }

