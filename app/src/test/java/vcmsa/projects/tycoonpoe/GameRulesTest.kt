package vcmsa.projects.tycoonpoe

import org.junit.Assert.*
import org.junit.Test

class GameRulesTest {

    @Test
    fun `getCardValue returns correct value`() {
        val rules = GameRules()
        assertEquals("3", rules.getCardValue("3S"))
        assertEquals("10", rules.getCardValue("10H"))
        assertEquals("Joker", rules.getCardValue("Joker"))
    }

    @Test
    fun `isValidHand returns true for matching cards`() {
        val rules = GameRules()
        val hand = listOf("5H", "5D", "5C")
        assertTrue(rules.isValidHand(hand))
    }

    @Test
    fun `isValidHand returns false for mismatched cards`() {
        val rules = GameRules()
        val hand = listOf("4H", "5D")
        assertFalse(rules.isValidHand(hand))
    }

    @Test
    fun `beatsCurrentPot returns true when hand beats pot normally`() {
        val rules = GameRules()
        val pot = listOf("7H")
        val hand = listOf("8H")
        assertTrue(rules.beatsCurrentPot(hand, pot))
    }

    @Test
    fun `beatsCurrentPot returns false when hand weaker`() {
        val rules = GameRules()
        val pot = listOf("9H")
        val hand = listOf("8H")
        assertFalse(rules.beatsCurrentPot(hand, pot))
    }

    @Test
    fun `isFourOfAKind detects four of same value`() {
        val rules = GameRules()
        val hand = listOf("5H", "5D", "5S", "5C")
        assertTrue(rules.isFourOfAKind(hand))
    }

    @Test
    fun `isFourOfAKind with Joker still valid`() {
        val rules = GameRules()
        val hand = listOf("6H", "6S", "6C", "Joker")
        assertTrue(rules.isFourOfAKind(hand))
    }

    @Test
    fun `playHand accepts valid higher hand`() {
        val rules = GameRules()
        rules.currentPot = listOf("7H")
        val result = rules.playHand(listOf("10H"))
        assertTrue(result)
        assertEquals(listOf("10H"), rules.currentPot)
    }

    @Test
    fun `playHand rejects weaker hand`() {
        val rules = GameRules()
        rules.currentPot = listOf("10H")
        val result = rules.playHand(listOf("9H"))
        assertFalse(result)
    }

    @Test
    fun `playing an 8 clears pot`() {
        val rules = GameRules()
        rules.currentPot = listOf("7H")
        val result = rules.playHand(listOf("8D"))
        assertTrue(result)
        assertTrue(rules.currentPot.isEmpty())
        assertTrue(rules.fullPot.isEmpty())
    }

    @Test
    fun `four of a kind toggles revolution`() {
        val rules = GameRules()
        val hand = listOf("5H", "5D", "5S", "5C")
        rules.playHand(hand)
        assertTrue(rules.isRevolution)
    }

    @Test
    fun `clearPotOnly clears both pots`() {
        val rules = GameRules()
        rules.currentPot = listOf("7H", "7D")
        rules.fullPot.addAll(rules.currentPot)
        rules.clearPotOnly()
        assertTrue(rules.currentPot.isEmpty())
        assertTrue(rules.fullPot.isEmpty())
    }

    @Test
    fun `resetRound resets revolution and pot`() {
        val rules = GameRules()
        rules.isRevolution = true
        rules.currentPot = listOf("9H")
        rules.resetRound()
        assertFalse(rules.isRevolution)
        assertTrue(rules.currentPot.isEmpty())
    }
}
