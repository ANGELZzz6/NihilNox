package com.example.colorblend.ui.gacha

enum class Suit { CORAZONES, DIAMANTES, TREBOLES, PICAS }
enum class Color { ROJO, NEGRO }

data class Card(
    val suit: Suit,
    val value: Int, // 1=As, 2-10, 11=J, 12=Q, 13=K
    var faceUp: Boolean = false
) {
    val color: Color get() = if (suit == Suit.CORAZONES || suit == Suit.DIAMANTES) Color.ROJO else Color.NEGRO

    val displayValue: String get() = when(value) {
        1 -> "A"; 11 -> "J"; 12 -> "Q"; 13 -> "K"; else -> value.toString()
    }

    val displaySuit: String get() = when(suit) {
        Suit.CORAZONES -> "♥"; Suit.DIAMANTES -> "♦"
        Suit.TREBOLES -> "♣"; Suit.PICAS -> "♠"
    }
}
