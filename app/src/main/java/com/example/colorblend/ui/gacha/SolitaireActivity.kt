package com.example.colorblend.ui.gacha

import android.app.AlertDialog
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.example.colorblend.R
import kotlinx.coroutines.*
import androidx.core.view.children
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.AccelerateDecelerateInterpolator

class SolitaireActivity : AppCompatActivity() {

    private lateinit var game: SolitaireGame
    private var difficulty = Difficulty.FACIL
    private var score = 0
    private var timerJob: Job? = null
    private var secondsElapsed = 0
    private var selectedCards = mutableListOf<Card>()
    private var selectedSource: Any? = null // columna o waste origen
    private var selectedColIndex: Int = -1
    private var selectedDepth: Int = 1 // cuántas cartas boca arriba están seleccionadas
    private var isAnimating = false
    private lateinit var btnCompletar: Button
    private var isAutoCompleting = false
    private var autoCompleteStep = 0

    private val moveHistory = ArrayDeque<GameSnapshot>()

    data class GameSnapshot(
        val stock: List<Card>,
        val waste: List<Card>,
        val foundations: List<List<Card>>,
        val tableau: List<List<Card>>,
        val score: Int
    )

    // Views del tablero
    private lateinit var stockPile: FrameLayout
    private lateinit var wastePile: FrameLayout
    private val foundations = arrayOfNulls<FrameLayout>(4)
    private val columns = arrayOfNulls<FrameLayout>(7)
    private lateinit var tvScore: TextView
    private lateinit var tvTimer: TextView
    private lateinit var tvDifficulty: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_solitaire)

        game = SolitaireGame()
        bindViews()
        setupClickListeners()

        val resume = intent.getBooleanExtra("RESUME", false)
        if (resume && cargarPartida()) {
            renderBoard()
            startTimer()
        } else {
            difficulty = intent.getSerializableExtra("DIFFICULTY") as? Difficulty ?: Difficulty.FACIL
            tvDifficulty.text = difficulty.label
            startNewGame()
        }
    }

    private fun bindViews() {
        stockPile = findViewById(R.id.stockPile)
        wastePile = findViewById(R.id.wastePile)
        tvScore = findViewById(R.id.tvScore)
        tvTimer = findViewById(R.id.tvTimer)
        tvDifficulty = findViewById(R.id.tvDifficulty)
        tvDifficulty.text = difficulty.label

        foundations[0] = findViewById(R.id.foundation0)
        foundations[1] = findViewById(R.id.foundation1)
        foundations[2] = findViewById(R.id.foundation2)
        foundations[3] = findViewById(R.id.foundation3)

        columns[0] = findViewById(R.id.col0)
        columns[1] = findViewById(R.id.col1)
        columns[2] = findViewById(R.id.col2)
        columns[3] = findViewById(R.id.col3)
        columns[4] = findViewById(R.id.col4)
        columns[5] = findViewById(R.id.col5)
        columns[6] = findViewById(R.id.col6)

        btnCompletar = findViewById(R.id.btnCompletar)
    }

    // ── Nueva partida ──────────────────────────────────────────────────
    private fun startNewGame() {
        timerJob?.cancel()
        secondsElapsed = 0
        score = 0
        selectedCards.clear()
        selectedSource = null
        isAutoCompleting = false
        isAnimating = false
        btnCompletar.visibility = View.GONE
        game.deal(difficulty)
        renderBoard()
        animarReparto {}
        startTimer()
    }

    // ── Timer ──────────────────────────────────────────────────────────
    private fun startTimer() {
        if (difficulty.timeLimit == 0) { tvTimer.text = ""; return }
        timerJob = CoroutineScope(Dispatchers.Main).launch {
            while (true) {
                delay(1000)
                secondsElapsed++
                val remaining = difficulty.timeLimit - secondsElapsed
                if (remaining <= 0) { onTimeUp(); break }
                val m = remaining / 60; val s = remaining % 60
                tvTimer.text = "%d:%02d".format(m, s)
            }
        }
    }

    private fun onTimeUp() {
        AlertDialog.Builder(this)
            .setTitle("⏰ Tiempo agotado")
            .setMessage("Puntuación final: $score")
            .setPositiveButton("Nueva partida") { _, _ -> startNewGame() }
            .setNegativeButton("Salir") { _, _ -> finish() }
            .show()
    }

    // ── Render completo del tablero ────────────────────────────────────
    private fun renderBoard() {
        renderStock()
        renderWaste()
        foundations.forEachIndexed { i, f -> renderFoundation(i, f!!) }
        columns.forEachIndexed { i, c -> renderColumn(i, c!!) }
        tvScore.text = "Puntos: $score"

        // Mostrar botón completar si el tablero está desbloqueado
        if (game.isBoardUnlocked() && !isAutoCompleting) {
            if (btnCompletar.visibility != View.VISIBLE) {
                btnCompletar.visibility = View.VISIBLE
                btnCompletar.alpha = 0f
                btnCompletar.animate().alpha(1f).setDuration(500).start()
            }
        } else if (!game.isBoardUnlocked()) {
            btnCompletar.visibility = View.GONE
        }
    }

    private fun renderStock() {
        stockPile.removeAllViews()
        if (game.stock.isNotEmpty()) {
            val back = View(this).apply {
                setBackgroundResource(R.drawable.card_back_bg)
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT)
            }
            stockPile.addView(back)
            val count = TextView(this).apply {
                text = "${game.stock.size}"
                setTextColor(0xFFFFFFFF.toInt())
                textSize = 10f
                gravity = android.view.Gravity.CENTER
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT)
            }
            stockPile.addView(count)
        }
    }

    private fun renderWaste() {
        wastePile.removeAllViews()
        if (game.waste.isNotEmpty()) {
            val card = game.waste.last()
            wastePile.addView(buildCardView(card, isSelected = false))
        }
    }

    private fun renderFoundation(index: Int, container: FrameLayout) {
        container.removeAllViews()
        val suits = listOf("♥", "♦", "♣", "♠")
        if (game.foundations[index].isEmpty()) {
            val hint = TextView(this).apply {
                text = suits[index]
                textSize = 24f
                setTextColor(0x55FFFFFF.toInt())
                gravity = android.view.Gravity.CENTER
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT)
            }
            container.addView(hint)
        } else {
            val card = game.foundations[index].last()
            container.addView(buildCardView(card, isSelected = false))
        }
    }

    private fun renderColumn(colIndex: Int, container: FrameLayout) {
        container.removeAllViews()
        val col = game.tableau[colIndex]
        if (col.isEmpty()) return

        val density = resources.displayMetrics.density
        val offsetBocaAbajo = (20 * density).toInt()
        val offsetBocaArriba = (32 * density).toInt()
        val cardHeight = (95 * density).toInt()

        var topAccum = 0

        col.forEachIndexed { cardIndex, card ->
            val isSelected = selectedCards.contains(card)
            val cardView = buildCardView(card, isSelected, cardHeight)
            val lp = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, cardHeight)
            lp.topMargin = topAccum
            cardView.layoutParams = lp

            // Oscurecer cartas seleccionadas
            if (isSelected) {
                cardView.alpha = 0.65f
            }

            // Listener individual por carta
            if (card.faceUp) {
                cardView.setOnClickListener {
                    if (isAnimating) return@setOnClickListener
                    onCardClick(card, cardIndex, colIndex, col)
                }
            } else {
                cardView.setOnClickListener {
                    if (isAnimating) return@setOnClickListener
                    // Carta boca abajo: si hay selección activa intentar mover a esta columna
                    if (selectedCards.isNotEmpty()) {
                        onColumnDestinationClick(colIndex)
                    }
                }
            }

            container.addView(cardView)
            topAccum += if (card.faceUp) offsetBocaArriba else offsetBocaAbajo
        }

        // Listener en el contenedor vacío para recibir movimientos
        container.setOnClickListener {
            if (isAnimating) return@setOnClickListener
            if (selectedCards.isNotEmpty()) onColumnDestinationClick(colIndex)
        }
    }

    private fun buildCardView(card: Card, isSelected: Boolean, cardHeight: Int = 110): View {
        val density = resources.displayMetrics.density
        return if (!card.faceUp) {
            View(this).apply {
                setBackgroundResource(R.drawable.card_back_bg)
                if (isSelected) alpha = 0.6f
            }
        } else {
            FrameLayout(this).apply {
                setBackgroundResource(R.drawable.card_front_bg)
                if (isSelected) { alpha = 0.75f; scaleX = 0.96f; scaleY = 0.96f }

                val redColor = 0xFFCC0000.toInt()
                val blackColor = 0xFF111111.toInt()
                val tColor = if (card.color == Color.ROJO) redColor else blackColor

                val tvTopLeft = TextView(context).apply {
                    text = "${card.displayValue}${card.displaySuit}"
                    textSize = 11f
                    setTextColor(tColor)
                    setPadding((3 * density).toInt(), (2 * density).toInt(), 0, 0)
                }
                val tvCenter = TextView(context).apply {
                    text = card.displaySuit
                    textSize = 22f
                    setTextColor(tColor)
                    gravity = android.view.Gravity.CENTER
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT).also {
                        it.gravity = android.view.Gravity.CENTER
                    }
                }
                val tvBottomRight = TextView(context).apply {
                    text = "${card.displayValue}${card.displaySuit}"
                    textSize = 11f
                    setTextColor(tColor)
                    setPadding(0, 0, (3 * density).toInt(), (2 * density).toInt())
                    gravity = android.view.Gravity.END
                    rotation = 180f
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT).also {
                        it.gravity = android.view.Gravity.BOTTOM or android.view.Gravity.END
                    }
                }
                addView(tvCenter); addView(tvTopLeft); addView(tvBottomRight)
            }
        }
    }

    // ── Click listeners ────────────────────────────────────────────────
    private fun setupClickListeners() {
        // Mazo: robar carta(s)
        stockPile.setOnClickListener {
            if (isAnimating) return@setOnClickListener
            saveSnapshot()
            if (game.stock.isEmpty()) {
                // Reciclar descarte
                game.waste.reversed().forEach { it.faceUp = false; game.stock.add(it) }
                game.waste.clear()
                addScore(-20)
            } else {
                repeat(difficulty.drawCount) {
                    if (game.stock.isNotEmpty()) {
                        val c = game.stock.removeLast()
                        c.faceUp = true
                        game.waste.add(c)
                    }
                }
            }
            clearSelection()
            renderBoard()
        }

        // Descarte: seleccionar carta del waste
        wastePile.setOnClickListener {
            if (isAnimating) return@setOnClickListener
            if (game.waste.isEmpty()) {
                animarSacudida(wastePile)
                return@setOnClickListener
            }
            val card = game.waste.last()
            if (selectedCards.isEmpty()) {
                selectedCards = mutableListOf(card)
                selectedSource = "WASTE"
                renderBoard()
                // Auto-mover con animación
                autoMoverCartaDesdeWaste(card)
            } else {
                clearSelection()
                renderBoard()
            }
        }

        // Fundaciones: intentar mover selección
        foundations.forEachIndexed { fi, f ->
            f?.setOnClickListener {
                if (isAnimating) return@setOnClickListener
                if (selectedCards.size == 1) {
                    if (game.canMoveToFoundation(selectedCards[0], fi)) {
                        val origenColIndex = selectedSource?.toString()?.split("_")?.getOrNull(1)?.toIntOrNull()
                        animarCartaHaciaColumna(origenColIndex, fi) {
                            moveToFoundation(selectedCards[0], fi)
                        }
                    } else {
                        animarSacudida(f)
                        clearSelection()
                        renderBoard()
                    }
                } else if (selectedCards.isEmpty() && game.foundations[fi].isNotEmpty()) {
                    selectedCards = mutableListOf(game.foundations[fi].last())
                    selectedSource = "FOUNDATION_$fi"
                    renderBoard()
                    autoMoverCartaDesdeFoundation(selectedCards[0], fi)
                } else {
                    animarSacudida(f)
                    clearSelection()
                    renderBoard()
                }
            }
        }

        // Columnas: recibir movimientos si hay selección
        columns.forEachIndexed { ci, c ->
            c?.setOnClickListener {
                if (isAnimating) return@setOnClickListener
                if (selectedCards.isNotEmpty()) onColumnDestinationClick(ci)
            }
        }

        findViewById<Button>(R.id.btnUndo).setOnClickListener {
            if (isAnimating) return@setOnClickListener
            undoLastMove()
        }
        findViewById<Button>(R.id.btnNewGame).setOnClickListener {
            if (isAnimating) return@setOnClickListener
            startNewGame()
        }
        btnCompletar.setOnClickListener {
            if (!isAutoCompleting) iniciarAutoCompletar()
        }
    }

    // Toque en carta específica dentro de una columna
    private fun onCardClick(card: Card, cardIndex: Int, colIndex: Int, col: List<Card>) {
        if (selectedCards.isEmpty()) {
            // Seleccionar desde esta carta hasta el final de la columna
            val cardsToSelect = col.subList(cardIndex, col.size).toMutableList()
            selectedCards = cardsToSelect
            selectedSource = "COL_$colIndex"
            selectedColIndex = colIndex
            selectedDepth = cardsToSelect.size
            renderBoard()

            // Si es una sola carta, auto-mover
            if (cardsToSelect.size == 1) {
                autoMoverCarta(card, "COL_$colIndex", colIndex)
            }
            return
        }

        // Hay selección: si es la misma columna, cambiar punto de selección
        if (selectedSource == "COL_$colIndex") {
            val faceUpStart = col.indexOfFirst { it.faceUp }
            if (faceUpStart == -1) { clearSelection(); renderBoard(); return }
            val cardsToSelect = col.subList(cardIndex, col.size).toMutableList()
            selectedCards = cardsToSelect
            selectedDepth = cardsToSelect.size
            renderBoard()

            // Si quedó una sola carta seleccionada, auto-mover
            if (cardsToSelect.size == 1) {
                autoMoverCarta(card, "COL_$colIndex", colIndex)
            }
            return
        }

        // Columna diferente: intentar mover selección aquí
        onColumnDestinationClick(colIndex)
    }

    // Intentar mover selección activa a una columna destino
    private fun onColumnDestinationClick(colIndex: Int) {
        val colView = columns[colIndex] ?: return
        val cardToMove = selectedCards.firstOrNull() ?: return

        if (game.canMoveToTableau(cardToMove, colIndex)) {
            isAnimating = true
            val origenColIndex = selectedSource?.toString()?.split("_")?.getOrNull(1)?.toIntOrNull()
            animarCartaHaciaColumna(origenColIndex, colIndex) {
                moveToColumn(colIndex)
                isAnimating = false
            }
        } else {
            animarSacudida(colView)
            clearSelection()
            renderBoard()
        }
    }

    // ── Movimientos ────────────────────────────────────────────────────
    private fun moveToFoundation(card: Card, foundationIndex: Int) {
        saveSnapshot()
        when {
            selectedSource == "WASTE" -> game.waste.removeLast()
            selectedSource?.toString()?.startsWith("COL") == true -> {
                val ci = selectedSource.toString().split("_")[1].toInt()
                game.tableau[ci].removeLast()
                flipTopCard(ci)
            }
            selectedSource?.toString()?.startsWith("FOUNDATION") == true -> {
                val fi = selectedSource.toString().split("_")[1].toInt()
                game.foundations[fi].removeLast()
            }
        }
        game.foundations[foundationIndex].add(card)
        addScore(10)
        clearSelection()
        renderBoard()
        if (game.isWon()) onWin()
    }

    private fun moveToColumn(targetCol: Int) {
        saveSnapshot()
        when {
            selectedSource == "WASTE" -> {
                game.waste.removeLast()
                game.tableau[targetCol].addAll(selectedCards)
                addScore(5)
            }
            selectedSource?.toString()?.startsWith("COL") == true -> {
                val ci = selectedSource.toString().split("_")[1].toInt()
                repeat(selectedCards.size) { game.tableau[ci].removeLast() }
                flipTopCard(ci)
                game.tableau[targetCol].addAll(selectedCards)
                addScore(3)
            }
            selectedSource?.toString()?.startsWith("FOUNDATION") == true -> {
                val fi = selectedSource.toString().split("_")[1].toInt()
                game.foundations[fi].removeLast()
                game.tableau[targetCol].addAll(selectedCards)
                addScore(-15)
            }
        }
        clearSelection()
        renderBoard()
    }

    private fun flipTopCard(colIndex: Int) {
        val col = game.tableau[colIndex]
        if (col.isNotEmpty() && !col.last().faceUp) {
            col.last().faceUp = true
            addScore(5)
        }
    }

    // ── Sacudida cuando no hay movimiento posible ─────────────────────────
    private fun animarSacudida(v: View) {
        v.animate().translationX(10f).setDuration(60).withEndAction {
            v.animate().translationX(-10f).setDuration(60).withEndAction {
                v.animate().translationX(6f).setDuration(50).withEndAction {
                    v.animate().translationX(-6f).setDuration(50).withEndAction {
                        v.animate().translationX(0f).setDuration(40).start()
                    }.start()
                }.start()
            }.start()
        }.start()
    }

    // Anima solo la vista de la carta superior, no el contenedor entero
    private fun animarCartaHaciaColumna(origenCol: Int?, destinoIndex: Int, onDone: () -> Unit) {
        val destinoView = if (destinoIndex in 0..6 && selectedCards.firstOrNull()?.value != 1) {
             columns[destinoIndex] // Destino es columna
        } else {
             if (selectedCards.firstOrNull()?.value == 1) {
                 foundations.getOrNull(destinoIndex) ?: columns.getOrNull(destinoIndex)
             } else {
                 columns.getOrNull(destinoIndex) ?: foundations.getOrNull(destinoIndex)
             }
        } ?: run { onDone(); return }

        val origenContainer: FrameLayout? = when {
            selectedSource == "WASTE" -> wastePile
            selectedSource?.toString()?.startsWith("FOUNDATION") == true -> {
                val fi = selectedSource.toString().split("_")[1].toInt()
                foundations[fi]
            }
            origenCol != null -> columns[origenCol]
            else -> null
        }

        val cartaView = origenContainer?.children?.lastOrNull()
        if (cartaView == null) { onDone(); return }

        val origenLoc = IntArray(2); cartaView.getLocationOnScreen(origenLoc)
        val destinoLoc = IntArray(2); destinoView.getLocationOnScreen(destinoLoc)

        val dx = (destinoLoc[0] - origenLoc[0]).toFloat()
        val dy = (destinoLoc[1] - origenLoc[1]).toFloat()

        cartaView.animate()
            .translationX(dx).translationY(dy)
            .setDuration(220)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction {
                cartaView.translationX = 0f
                cartaView.translationY = 0f
                onDone()
            }.start()
    }

    // ── Buscar mejor destino para una carta ──────────────────────────────
    private fun findBestDestination(card: Card, isStack: Boolean): Pair<String, Int>? {
        if (!isStack) {
            for (fi in 0..3) {
                if (game.canMoveToFoundation(card, fi)) return Pair("FOUNDATION", fi)
            }
        }
        for (ci in 0..6) {
            if (game.canMoveToTableau(card, ci)) return Pair("COL", ci)
        }
        return null
    }

    // ── Auto-mover: click inteligente en carta o waste ────────────────────
    private fun autoMoverCarta(card: Card, source: String, origenColIndex: Int) {
        val destino = findBestDestination(card, isStack = false)
        val origenContainer = columns[origenColIndex] ?: wastePile
        val cartaView = origenContainer.children.lastOrNull()

        if (destino == null) {
            animarSacudida(cartaView ?: origenContainer)
            clearSelection()
            renderBoard()
            return
        }

        val destinoView: View? = when (destino.first) {
            "FOUNDATION" -> foundations[destino.second]
            "COL" -> columns[destino.second]
            else -> null
        }

        if (destinoView == null || cartaView == null) {
            clearSelection(); renderBoard(); return
        }

        val origenLoc = IntArray(2); cartaView.getLocationOnScreen(origenLoc)
        val destinoLoc = IntArray(2); destinoView.getLocationOnScreen(destinoLoc)
        val dx = (destinoLoc[0] - origenLoc[0]).toFloat()
        val dy = (destinoLoc[1] - origenLoc[1]).toFloat()

        isAnimating = true
        cartaView.animate()
            .translationX(dx).translationY(dy)
            .setDuration(220)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction {
                cartaView.translationX = 0f
                cartaView.translationY = 0f
                selectedSource = source
                when (destino.first) {
                    "FOUNDATION" -> moveToFoundation(card, destino.second)
                    "COL" -> moveToColumn(destino.second)
                }
                isAnimating = false
            }.start()
    }

    private fun autoMoverCartaDesdeWaste(card: Card) {
        val destino = findBestDestination(card, isStack = false)
        val cartaView = wastePile.children.lastOrNull()

        if (destino == null) {
            animarSacudida(cartaView ?: wastePile)
            clearSelection(); renderBoard(); return
        }

        val destinoView: View? = when (destino.first) {
            "FOUNDATION" -> foundations[destino.second]
            "COL" -> columns[destino.second]
            else -> null
        }

        if (destinoView == null || cartaView == null) {
            clearSelection(); renderBoard(); return
        }

        val origenLoc = IntArray(2); cartaView.getLocationOnScreen(origenLoc)
        val destinoLoc = IntArray(2); destinoView.getLocationOnScreen(destinoLoc)
        val dx = (destinoLoc[0] - origenLoc[0]).toFloat()
        val dy = (destinoLoc[1] - origenLoc[1]).toFloat()

        isAnimating = true
        cartaView.animate()
            .translationX(dx).translationY(dy)
            .setDuration(220)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction {
                cartaView.translationX = 0f
                cartaView.translationY = 0f
                selectedSource = "WASTE"
                when (destino.first) {
                    "FOUNDATION" -> moveToFoundation(card, destino.second)
                    "COL" -> moveToColumn(destino.second)
                }
                isAnimating = false
            }.start()
    }

    private fun autoMoverCartaDesdeFoundation(card: Card, foundationIndex: Int) {
        val destino = findBestDestination(card, isStack = false)
        val origenContainer = foundations[foundationIndex]
        val cartaView = origenContainer?.children?.lastOrNull()

        if (destino == null) {
            animarSacudida(cartaView ?: origenContainer!!)
            clearSelection(); renderBoard(); return
        }

        val destinoView: View? = when (destino.first) {
            "FOUNDATION" -> foundations[destino.second]
            "COL" -> columns[destino.second]
            else -> null
        }

        if (destinoView == null || cartaView == null) {
            clearSelection(); renderBoard(); return
        }

        val origenLoc = IntArray(2); cartaView.getLocationOnScreen(origenLoc)
        val destinoLoc = IntArray(2); destinoView.getLocationOnScreen(destinoLoc)
        val dx = (destinoLoc[0] - origenLoc[0]).toFloat()
        val dy = (destinoLoc[1] - origenLoc[1]).toFloat()

        isAnimating = true
        cartaView.animate()
            .translationX(dx).translationY(dy)
            .setDuration(220)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction {
                cartaView.translationX = 0f
                cartaView.translationY = 0f
                selectedSource = "FOUNDATION_$foundationIndex"
                when (destino.first) {
                    "FOUNDATION" -> moveToFoundation(card, destino.second)
                    "COL" -> moveToColumn(destino.second)
                }
                isAnimating = false
            }.start()
    }

    // ── Auto-completar: envía cartas una a una a su fundación ─────────────
    private fun iniciarAutoCompletar() {
        isAutoCompleting = true
        isAnimating = true
        autoCompleteStep = 0
        btnCompletar.visibility = View.GONE
        timerJob?.cancel()
        enviarSiguienteCarta()
    }

    private fun enviarSiguienteCarta() {
        val movimiento = encontrarCartaParaFundacion()
        if (movimiento == null) {
            isAnimating = false
            isAutoCompleting = false
            if (game.isWon()) onWin()
            return
        }

        val (card, colIndex, foundationIndex) = movimiento
        val origenContainer = columns[colIndex] ?: run { enviarSiguienteCarta(); return }
        val cartaView = origenContainer.children.lastOrNull()
        val destinoView = foundations[foundationIndex] ?: run { enviarSiguienteCarta(); return }
        if (cartaView == null) { enviarSiguienteCarta(); return }

        // Arranca rápido desde carta 1, llega al mínimo en carta 10
        val duracionAnim = maxOf(60L, 200L - (autoCompleteStep * 18L))
        val delayEntreCartas = maxOf(20L, 100L - (autoCompleteStep * 10L))
        autoCompleteStep++

        val origenLoc = IntArray(2); cartaView.getLocationOnScreen(origenLoc)
        val destinoLoc = IntArray(2); destinoView.getLocationOnScreen(destinoLoc)
        val dx = (destinoLoc[0] - origenLoc[0]).toFloat()
        val dy = (destinoLoc[1] - origenLoc[1]).toFloat()

        cartaView.animate()
            .translationX(dx).translationY(dy)
            .scaleX(0.85f).scaleY(0.85f)
            .setDuration(duracionAnim)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction {
                cartaView.translationX = 0f; cartaView.translationY = 0f
                cartaView.scaleX = 1f; cartaView.scaleY = 1f
                game.tableau[colIndex].removeLast()
                game.foundations[foundationIndex].add(card)
                addScore(10)
                renderBoard()
                android.os.Handler(mainLooper).postDelayed({
                    enviarSiguienteCarta()
                }, delayEntreCartas)
            }.start()
    }

    private data class CartaMovimiento(val card: Card, val colIndex: Int, val foundationIndex: Int)

    private fun encontrarCartaParaFundacion(): CartaMovimiento? {
        // Orden de prioridad: primero los valores más bajos para construir fundaciones equilibradas
        for (ci in 0..6) {
            val col = game.tableau[ci]
            if (col.isEmpty()) continue
            val card = col.last()
            if (!card.faceUp) continue
            for (fi in 0..3) {
                if (game.canMoveToFoundation(card, fi)) {
                    return CartaMovimiento(card, ci, fi)
                }
            }
        }
        return null
    }

    // ── Utilidades ─────────────────────────────────────────────────────
    private fun clearSelection() {
        selectedCards.clear()
        selectedSource = null
        selectedColIndex = -1
        selectedDepth = 1
    }

    private fun addScore(points: Int) {
        score = maxOf(0, score + (points * difficulty.scoreMultiplier).toInt())
    }

    private fun onWin() {
        borrarPartidaGuardada()
        timerJob?.cancel()
        val bonus = if (difficulty.timeLimit > 0)
            ((difficulty.timeLimit - secondsElapsed) * 2 * difficulty.scoreMultiplier).toInt() else 0
        score += bonus
        animarVictoria()
        android.os.Handler(mainLooper).postDelayed({
            AlertDialog.Builder(this)
                .setTitle("🎉 ¡Ganaste!")
                .setMessage("Dificultad: ${difficulty.label}\nPuntuación: $score${if (bonus > 0) "\nBonus tiempo: +$bonus" else ""}")
                .setPositiveButton("Nueva partida") { _, _ -> startNewGame() }
                .setNegativeButton("Salir") { _, _ -> finish() }
                .show()
        }, 1500)
    }

    private fun saveSnapshot() {
        val snapshot = GameSnapshot(
            stock = game.stock.map { it.copy() },
            waste = game.waste.map { it.copy() },
            foundations = game.foundations.map { col -> col.map { it.copy() } },
            tableau = game.tableau.map { col -> col.map { it.copy() } },
            score = score
        )
        moveHistory.addLast(snapshot)
        // Máximo 30 movimientos hacia atrás
        if (moveHistory.size > 30) moveHistory.removeFirst()
    }

    private fun undoLastMove() {
        if (moveHistory.isEmpty()) {
            Toast.makeText(this, "No hay movimientos para deshacer", Toast.LENGTH_SHORT).show()
            return
        }
        val snapshot = moveHistory.removeLast()
        game.stock.clear(); snapshot.stock.forEach { game.stock.add(it) }
        game.waste.clear(); snapshot.waste.forEach { game.waste.add(it) }
        game.foundations.forEachIndexed { i, f ->
            f.clear(); snapshot.foundations[i].forEach { f.add(it) }
        }
        game.tableau.forEachIndexed { i, col ->
            col.clear(); snapshot.tableau[i].forEach { col.add(it) }
        }
        score = snapshot.score
        clearSelection()
        renderBoard()
        animarUndo()
    }

    private fun animarUndo() {
        columns.forEach { col ->
            col?.animate()?.translationX(-12f)?.setDuration(80)?.withEndAction {
                col.animate().translationX(0f).setDuration(80)
                    .setInterpolator(DecelerateInterpolator()).start()
            }?.start()
        }
    }

    // ── Animación: repartir cartas al inicio ──────────────────────────────
    private fun animarReparto(onDone: () -> Unit) {
        val allCards = columns.flatMapIndexed { _, col ->
            col?.children?.toList() ?: emptyList()
        }
        allCards.forEachIndexed { i, v ->
            v.alpha = 0f
            v.translationY = -40f
            v.animate()
                .alpha(1f).translationY(0f)
                .setDuration(200)
                .setStartDelay(i * 30L)
                .setInterpolator(DecelerateInterpolator())
                .withEndAction { if (i == allCards.size - 1) onDone() }
                .start()
        }
        if (allCards.isEmpty()) onDone()
    }

    // ── Animación: flip de carta al voltear ───────────────────────────────
    private fun animarFlip(container: FrameLayout, onMidPoint: () -> Unit) {
        container.animate()
            .scaleX(0f).setDuration(120)
            .withEndAction {
                onMidPoint()
                container.animate()
                    .scaleX(1f).setDuration(120)
                    .setInterpolator(DecelerateInterpolator())
                    .start()
            }.start()
    }

    // ── Animación: victoria con lluvia de cartas ──────────────────────────
    private fun animarVictoria() {
        val suits = listOf("♥", "♦", "♣", "♠")
        val colors = listOf(0xFFCC0000.toInt(), 0xFFCC0000.toInt(),
            0xFF111111.toInt(), 0xFF111111.toInt())
        val root = findViewById<View>(android.R.id.content)
        val overlay = FrameLayout(this)
        overlay.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT)
        (root as? FrameLayout)?.addView(overlay)

        repeat(30) { i ->
            val tv = TextView(this).apply {
                text = suits.random()
                textSize = (20..36).random().toFloat()
                setTextColor(colors[suits.indexOf(text)])
                alpha = 0f
            }
            val startX = (0..root.width).random().toFloat()
            tv.x = startX; tv.y = -80f
            overlay.addView(tv)

            tv.postDelayed({
                tv.animate().alpha(1f).translationY(root.height.toFloat() + 100)
                    .setDuration((1500..2500).random().toLong())
                    .setInterpolator(AccelerateInterpolator())
                    .withEndAction { overlay.removeView(tv) }
                    .start()
            }, i * 80L)
        }
        // Quitar overlay después de la animación
        overlay.postDelayed({ (root as? FrameLayout)?.removeView(overlay) }, 4000)
    }

    override fun onDestroy() { super.onDestroy(); timerJob?.cancel() }

    override fun onPause() {
        super.onPause()
        guardarPartida()
    }

    private fun guardarPartida() {
        if (game.isWon() || isAutoCompleting) return
        val prefs = getSharedPreferences("solitaire_save", MODE_PRIVATE)
        val editor = prefs.edit()

        // Serializar estado
        editor.putString("difficulty", difficulty.name)
        editor.putInt("score", score)
        editor.putInt("seconds", secondsElapsed)

        fun serializarLista(lista: List<Card>) =
            lista.joinToString(",") { "${it.suit.name}:${it.value}:${it.faceUp}" }

        editor.putString("stock", serializarLista(game.stock.toList()))
        editor.putString("waste", serializarLista(game.waste.toList()))

        game.foundations.forEachIndexed { i, f ->
            editor.putString("foundation_$i", serializarLista(f.toList()))
        }
        game.tableau.forEachIndexed { i, col ->
            editor.putString("tableau_$i", serializarLista(col.toList()))
        }
        editor.putBoolean("has_save", true)
        editor.apply()
    }

    private fun cargarPartida(): Boolean {
        val prefs = getSharedPreferences("solitaire_save", MODE_PRIVATE)
        if (!prefs.getBoolean("has_save", false)) return false

        fun deserializarLista(raw: String?): List<Card> {
            if (raw.isNullOrEmpty()) return emptyList()
            return raw.split(",").mapNotNull {
                val parts = it.split(":")
                if (parts.size != 3) return@mapNotNull null
                Card(
                    suit = Suit.valueOf(parts[0]),
                    value = parts[1].toInt(),
                    faceUp = parts[2].toBoolean()
                )
            }
        }

        try {
            difficulty = Difficulty.valueOf(prefs.getString("difficulty", "FACIL") ?: "FACIL")
            score = prefs.getInt("score", 0)
            secondsElapsed = prefs.getInt("seconds", 0)

            game.stock.clear(); deserializarLista(prefs.getString("stock", "")).forEach { game.stock.add(it) }
            game.waste.clear(); deserializarLista(prefs.getString("waste", "")).forEach { game.waste.add(it) }

            game.foundations.forEachIndexed { i, f ->
                f.clear()
                deserializarLista(prefs.getString("foundation_$i", "")).forEach { f.add(it) }
            }
            game.tableau.forEachIndexed { i, col ->
                col.clear()
                deserializarLista(prefs.getString("tableau_$i", "")).forEach { col.add(it) }
            }

            tvDifficulty.text = difficulty.label
            return true
        } catch (e: Exception) {
            return false
        }
    }

    fun borrarPartidaGuardada() {
        getSharedPreferences("solitaire_save", MODE_PRIVATE).edit()
            .putBoolean("has_save", false).apply()
    }
}

// Extensión para acceder a drawCount desde el juego
val SolitaireGame.difficulty: Difficulty get() = Difficulty.FACIL // se sobreescribe al iniciar
