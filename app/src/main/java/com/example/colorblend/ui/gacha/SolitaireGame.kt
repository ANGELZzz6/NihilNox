package com.example.colorblend.ui.gacha

class SolitaireGame {

    val stock = ArrayDeque<Card>()       // mazo
    val waste = ArrayDeque<Card>()       // descarte
    val foundations = Array(4) { ArrayDeque<Card>() }  // fundaciones (As→K)
    val tableau = Array(7) { ArrayDeque<Card>() }      // columnas del tablero

    fun initDeck(): List<Card> {
        val deck = mutableListOf<Card>()
        Suit.values().forEach { suit ->
            (1..13).forEach { value -> deck.add(Card(suit, value)) }
        }
        return deck.shuffled()
    }

    fun deal(difficulty: Difficulty) {
        stock.clear(); waste.clear()
        foundations.forEach { it.clear() }
        tableau.forEach { it.clear() }

        val deck = initDeck().toMutableList()

        // Repartir columnas: columna i tiene i+1 cartas
        for (i in 0..6) {
            for (j in 0..i) {
                val card = deck.removeFirst()
                card.faceUp = (j == i) // solo la última boca arriba
                tableau[i].add(card)
            }
        }
        // El resto va al mazo
        deck.forEach { it.faceUp = false; stock.add(it) }
    }

    fun canMoveToFoundation(card: Card, foundationIndex: Int): Boolean {
        val foundation = foundations[foundationIndex]
        if (foundation.isEmpty()) return card.value == 1
        val top = foundation.last()
        return top.suit == card.suit && card.value == top.value + 1
    }

    fun canMoveToTableau(card: Card, targetCol: Int): Boolean {
        val col = tableau[targetCol]
        if (col.isEmpty()) return card.value == 13 // solo K en columna vacía
        val top = col.last()
        return top.faceUp && top.color != card.color && card.value == top.value - 1
    }

    fun isWon(): Boolean = foundations.all { it.size == 13 }

    fun isBoardUnlocked(): Boolean {
        // Mazo y descarte vacíos
        if (stock.isNotEmpty() || waste.isNotEmpty()) return false
        // Todas las cartas del tablero boca arriba
        return tableau.all { col -> col.all { it.faceUp } }
    }
}

enum class Difficulty(val label: String, val drawCount: Int, val timeLimit: Int, val scoreMultiplier: Float) {
    FACIL("Fácil", 1, 0, 1.0f),
    NORMAL("Normal", 1, 0, 1.5f),
    DIFICIL("Difícil", 3, 0, 2.0f),
    EXPERTO("Experto", 3, 600, 3.0f),
    GRAN_MAESTRO("Gran Maestro", 3, 300, 5.0f)
}
