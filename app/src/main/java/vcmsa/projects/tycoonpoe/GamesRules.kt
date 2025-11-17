package vcmsa.projects.tycoonpoe

class GameRules {
    var isRevolution: Boolean = false
    var currentPot: List<String> = emptyList()
    var fullPot: MutableList<String> = mutableListOf()

    private val cardStrengths: List<String>
        get() = if (isRevolution)
            listOf("Joker" ,"3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K", "A", "2") //This for for revolution
        else
            listOf("3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K", "A", "2" ,"Joker" ) // This for Normal mode

    fun getCardValue(card: String): String {
        return if (card == "Joker") "Joker"
        else card.substring(0, card.length - 1)
    }

    fun getCardIndex(cardValue: String): Int {
        return cardStrengths.indexOf(cardValue)
    }

    fun isValidHand(hand: List<String>): Boolean {
        if (hand.isEmpty()) {
            println("Invalid hand: empty")
            return false
        }
        val baseValue = hand.firstOrNull { getCardValue(it) != "Joker" }?.let { getCardValue(it) } ?: "Joker"
        val valid = hand.all {
            val value = getCardValue(it)
            value == "Joker" || value == baseValue
        }
        if (!valid) {
            println("Invalid hand: cards do not match base value $baseValue. Hand: $hand")
        }
        return valid
    }

    fun beatsCurrentPot(hand: List<String>, currentPot: List<String>): Boolean {
        if (currentPot.isEmpty()) return true
        if (hand.size == currentPot.size && hand.sorted() == currentPot.sorted()) return false
        if (hand.size != currentPot.size) return false

        val baseHand = hand.firstOrNull { getCardValue(it) != "Joker" }?.let { getCardValue(it) } ?: "Joker"
        val basePot = currentPot.firstOrNull { getCardValue(it) != "Joker" }?.let { getCardValue(it) } ?: "Joker"

        if (currentPot.size == 1 && currentPot[0] == "Joker" && hand[0] == "3S") {
            println("Special rule: 3♠ beats single Joker")
            return true
        }

        return if (isRevolution)
            getCardIndex(baseHand) < getCardIndex(basePot)
        else
            getCardIndex(baseHand) > getCardIndex(basePot)
    }

    fun isFourOfAKind(hand: List<String>): Boolean {
        if (hand.size != 4) return false

        val baseCards = hand.filter { getCardValue(it) != "Joker" }
        if (baseCards.isEmpty()) return false

        val baseValue = getCardValue(baseCards.first())
        val matchCount = hand.count {
            val v = getCardValue(it)
            v == baseValue || v == "Joker"
        }

        return matchCount == 4
    }

    fun playHand(hand: List<String>): Boolean {
        println("Attempting to play hand: $hand")
        if (!isValidHand(hand)) {
            println("Rejected: Invalid hand")
            return false
        }
        if (!beatsCurrentPot(hand, currentPot)) {
            println("Rejected: Does not beat current pot")
            return false
        }

        currentPot = hand
        fullPot.addAll(hand) // ✅ Keep a record of all cards in the pot

        if (hand.any { getCardValue(it) == "8" }) {
            println("Played an 8: round ends, pot cleared")
            currentPot = emptyList()
            fullPot.clear() // ✅ Clear full pot as round ends
        }

        if (isFourOfAKind(hand)) {
            isRevolution = !isRevolution
            println("Revolution triggered! Mode is now: $isRevolution")
        }

        println("Play accepted: current pot is now $currentPot")
        return true
    }

    fun clearPotOnly() {
        currentPot = emptyList()
        fullPot.clear() // ✅ Also clear full pot display
    }
    fun getCardStrength(card: String): Int {
        val order = listOf("3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K", "A", "2", "Joker")
        val value = getCardValue(card)
        return order.indexOf(value)
    }

    fun resetRound() {
        currentPot = emptyList()
        isRevolution = false
        println("Round reset")
    }
}