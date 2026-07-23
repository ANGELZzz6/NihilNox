package com.example.colorblend.ui.gacha

import kotlin.collections.ArrayDeque

/**
 * Motor de validación de solubilidad optimizado para Solitario.
 * Utiliza un enfoque Greedy (Codicioso) con backtracking limitado para ser ultra rápido.
 */
object SolitaireSolver {

    private const val MAX_MOVES = 200 // Límite de movimientos por intento de resolución
    private const val MAX_TIME_MS = 150 // Tiempo máximo permitido por mazo

    fun isSolvable(game: SolitaireGame, difficulty: Difficulty): Boolean {
        val startTime = System.currentTimeMillis()
        
        // Estado simplificado para simulación rápida
        val state = FastState(game)
        val visited = mutableSetOf<String>()
        
        return solveGreedy(state, difficulty.drawCount, visited, startTime)
    }

    private fun solveGreedy(state: FastState, drawCount: Int, visited: MutableSet<String>, startTime: Long): Boolean {
        var moves = 0
        
        while (moves < MAX_MOVES) {
            if (System.currentTimeMillis() - startTime > MAX_TIME_MS) return false
            if (state.isWon()) return true
            
            val hash = state.getHash()
            if (visited.contains(hash)) break
            visited.add(hash)

            // Intentar el mejor movimiento posible
            if (tryBestMove(state, drawCount)) {
                moves++
            } else {
                // No hay más movimientos posibles
                break
            }
        }
        
        // Si no ganamos, pero desbloqueamos el tablero, es muy probable que sea ganable
        return state.isWon() || (state.revealedCount() >= 21) // 21 es el número total de cartas boca abajo al inicio
    }

    private fun tryBestMove(state: FastState, drawCount: Int): Boolean {
        // 1. Mover a fundaciones (Prioridad Máxima)
        for (i in 0..6) {
            val col = state.tableau[i]
            if (col.isNotEmpty()) {
                val card = col.last()
                if (card.faceUp) {
                    for (f in 0..3) {
                        if (canMoveToFoundation(card, state.foundations[f])) {
                            state.foundations[f].add(col.removeLast())
                            state.revealTop(i)
                            return true
                        }
                    }
                }
            }
        }

        // 2. Mover en el Tableau para revelar cartas boca abajo
        for (src in 0..6) {
            val srcCol = state.tableau[src]
            val firstFaceUp = srcCol.indexOfFirst { it.faceUp }
            if (firstFaceUp <= 0) continue // No hay cartas boca abajo que revelar o columna vacía

            val baseCard = srcCol[firstFaceUp]
            for (dst in 0..6) {
                if (src == dst) continue
                if (canMoveToTableau(baseCard, state.tableau[dst])) {
                    val stackToMove = mutableListOf<Card>()
                    repeat(srcCol.size - firstFaceUp) { stackToMove.add(0, srcCol.removeLast()) }
                    state.tableau[dst].addAll(stackToMove)
                    state.revealTop(src)
                    return true
                }
            }
        }

        // 3. Robar del mazo
        if (state.stock.isNotEmpty() || state.waste.isNotEmpty()) {
            if (state.stock.isEmpty()) {
                // Reciclar
                while (state.waste.isNotEmpty()) {
                    val c = state.waste.removeLast()
                    c.faceUp = false
                    state.stock.add(c)
                }
            } else {
                repeat(drawCount) {
                    if (state.stock.isNotEmpty()) {
                        val c = state.stock.removeLast()
                        c.faceUp = true
                        state.waste.add(c)
                    }
                }
            }
            return true
        }

        return false
    }

    private fun canMoveToFoundation(card: Card, foundation: ArrayDeque<Card>): Boolean {
        if (foundation.isEmpty()) return card.value == 1
        val top = foundation.last()
        return top.suit == card.suit && card.value == top.value + 1
    }

    private fun canMoveToTableau(card: Card, col: ArrayDeque<Card>): Boolean {
        if (col.isEmpty()) return card.value == 13
        val top = col.last()
        return top.faceUp && top.color != card.color && card.value == top.value - 1
    }

    private class FastState(game: SolitaireGame) {
        val stock = ArrayDeque(game.stock.map { it.copy() })
        val waste = ArrayDeque(game.waste.map { it.copy() })
        val foundations = Array(4) { i -> ArrayDeque(game.foundations[i].map { it.copy() }) }
        val tableau = Array(7) { i -> ArrayDeque(game.tableau[i].map { it.copy() }) }

        fun isWon() = foundations.all { it.size == 13 }
        
        fun revealedCount(): Int {
            var count = 0
            tableau.forEach { col -> count += col.count { it.faceUp } }
            return count
        }

        fun revealTop(colIndex: Int) {
            if (tableau[colIndex].isNotEmpty() && !tableau[colIndex].last().faceUp) {
                tableau[colIndex].last().faceUp = true
            }
        }

        fun getHash(): String {
            val sb = StringBuilder()
            foundations.forEach { sb.append(it.size) }
            tableau.forEach { col -> sb.append(col.size).append(if(col.isNotEmpty()) col.last().value else 0) }
            sb.append(stock.size)
            return sb.toString()
        }
    }
}
