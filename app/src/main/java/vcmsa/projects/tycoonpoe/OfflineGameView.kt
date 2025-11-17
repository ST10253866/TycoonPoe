package vcmsa.projects.tycoonpoe

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.SurfaceView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import vcmsa.projects.tycoonpoe.SettingsEntity
import vcmsa.projects.tycoonpoe.TycoonDatabase

class OfflineGameView(
    context: Context,
    attrs: AttributeSet? = null,
    val engine: OfflineGameEngine
) : SurfaceView(context, attrs), Runnable {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val cardWidth = 120
    private val cardHeight = 180

    private var cardBack: Bitmap? = null
    private val tableBitmap: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.table)
        .let { Bitmap.createScaledBitmap(it, resources.displayMetrics.widthPixels, resources.displayMetrics.heightPixels, true) }

    private val selectedIndices = mutableSetOf<Int>()
    private var thread: Thread? = null
    private var running = false
    private val surfaceHolder = holder

    init {
        loadCardBackFromSettings(context)
    }

    // --- Room: Load selected card back ---
    fun loadCardBackFromSettings(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = TycoonDatabase.getDatabase(context).settingsDao()
                val settings: SettingsEntity? = dao.getSettings()
                val cardBackName = settings?.selectedCardBack ?: "card_back1"

                val resId = context.resources.getIdentifier(cardBackName, "drawable", context.packageName)
                val bitmap = BitmapFactory.decodeResource(context.resources, resId)
                    .let { Bitmap.createScaledBitmap(it, cardWidth, cardHeight, true) }

                withContext(Dispatchers.Main) {
                    cardBack = bitmap
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    cardBack = BitmapFactory.decodeResource(resources, R.drawable.card_back1)
                        .let { Bitmap.createScaledBitmap(it, cardWidth, cardHeight, true) }
                }
            }
        }
    }

    override fun run() {
        Log.d("GameLoop", "Game loop started.")

        while (running) {
            if (!surfaceHolder.surface.isValid) continue

            val canvas = surfaceHolder.lockCanvas()
            drawGame(canvas)
            surfaceHolder.unlockCanvasAndPost(canvas)

            if (!engine.currentPlayerTurn && !engine.isGameOver) {
                Thread.sleep(1000)
                val botMove = engine.botPlay()
                if (botMove != null) postInvalidate()
            }

            Thread.sleep(16) // ~60fps
        }

        Log.d("GameLoop", "Game loop ended.")
    }

    private fun drawGame(canvas: Canvas) {
        canvas.drawBitmap(tableBitmap, 0f, 0f, paint)

        val playerCards = engine.getPlayerHand()
        val botCards = engine.getBotHand()
        val potCards = engine.getFullPot()

        // Draw player's hand
        for (i in playerCards.indices) {
            val cardBitmap = generateCardImage(context, playerCards[i], cardWidth, cardHeight)
            val x = 50 + i * 60
            val y = height - 220
            val drawY = if (selectedIndices.contains(i)) y - 30 else y
            canvas.drawBitmap(cardBitmap, x.toFloat(), drawY.toFloat(), paint)
        }

        // Draw pot cards
        for (i in potCards.indices) {
            val cardBitmap = generateCardImage(context, potCards[i], cardWidth, cardHeight)
            val x = width / 2 - (potCards.size * 30) + i * 60
            val y = height / 2 - 80
            canvas.drawBitmap(cardBitmap, x.toFloat(), y.toFloat(), paint)
        }

        // Draw bot's cards as backs
        for (i in botCards.indices) {
            val x = 50 + i * 60
            val y = 50
            cardBack?.let { canvas.drawBitmap(it, x.toFloat(), y.toFloat(), paint) }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN && engine.currentPlayerTurn && !engine.isGameOver) {
            val touchX = event.x.toInt()
            val touchY = event.y.toInt()
            val playerCards = engine.getPlayerHand()

            for (i in playerCards.indices.reversed()) {
                val x = 50 + i * 60
                val y = height - 220
                val topY = if (selectedIndices.contains(i)) y - 30 else y
                val rect = Rect(x, topY, x + cardWidth, topY + cardHeight)
                if (rect.contains(touchX, touchY)) {
                    toggleCardSelection(i)
                    break
                }
            }
        }
        return true
    }

    private fun toggleCardSelection(index: Int) {
        val playerCards = engine.getPlayerHand()
        val rank = playerCards[index].dropLast(1)
        val isJoker = playerCards[index].startsWith("JOKER") || playerCards[index] == "Joker"

        if (selectedIndices.contains(index)) {
            selectedIndices.remove(index)
        } else {
            val selectedRanks = selectedIndices.map { playerCards[it].dropLast(1) }.toSet()
            if (selectedIndices.size < 4 &&
                (selectedRanks.isEmpty() || selectedRanks.all { it == rank } || isJoker)
            ) {
                selectedIndices.add(index)
            }
        }
    }

    fun playSelectedCards(): Boolean {
        val playerCards = engine.getPlayerHand()
        val selectedCards = selectedIndices.map { playerCards[it] }
        val success = engine.playerPlay(selectedCards)
        if (success) selectedIndices.clear()
        return success
    }

    fun startGame() {
        running = true
        thread = Thread(this)
        thread?.start()
    }

    fun stopGame() {
        running = false
        thread?.join()
    }
}
private fun generateCardImage(context: Context, cardCode: String, width: Int, height: Int): Bitmap {
    val padding = (width * 0.05f) // 5% padding
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    // Card base
    paint.color = Color.WHITE
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
    paint.color = Color.BLACK
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 4f
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
    paint.style = Paint.Style.FILL

    // Declare somewhere accessible in your engine or view
    var isNextJokerRed = true  // first Joker drawn will be red

// Inside your card drawing logic
    if (cardCode.startsWith("Joker", ignoreCase = true)) {
        val isRed = isNextJokerRed
        isNextJokerRed = !isNextJokerRed  // toggle for the next Joker

        val jokerRes = if (isRed) R.drawable.face_joker_red else R.drawable.face_joker_black
        val jokerBitmap = BitmapFactory.decodeResource(context.resources, jokerRes)
        val scaled = Bitmap.createScaledBitmap(
            jokerBitmap,
            (width * 0.8).toInt(),
            (height * 0.8).toInt(),
            true
        )
        val left = (width - scaled.width) / 2f
        val top = (height - scaled.height) / 2f
        canvas.drawBitmap(scaled, left, top, paint)

        // Draw corner rank
        val colorName = if (isRed) "red" else "black"
        val cornerRankName = "rank_joker_$colorName"
        val cornerResId = context.resources.getIdentifier(cornerRankName, "drawable", context.packageName)
        if (cornerResId != 0) {
            val rankBitmap = BitmapFactory.decodeResource(context.resources, cornerResId)
            val scaledRank = Bitmap.createScaledBitmap(
                rankBitmap,
                (width * 0.18).toInt(),
                (height * 0.18).toInt(),
                true
            )

            // Top-left corner rank
            canvas.drawBitmap(scaledRank, padding, padding, paint)

            // Bottom-right corner rank (rotated 180°)
            canvas.save()
            canvas.rotate(180f, width - padding - scaledRank.width / 2, height - padding - scaledRank.height / 2)
            canvas.drawBitmap(scaledRank, width - scaledRank.width - padding, height - scaledRank.height - padding, paint)
            canvas.restore()
        }

        return bitmap
    }

    // Parse rank & suit
    val rank: String
    val suitChar: Char
    if (cardCode.length == 3) { // e.g., 10C
        rank = cardCode.substring(0, 2)
        suitChar = cardCode[2]
    } else {
        rank = cardCode.substring(0, 1)
        suitChar = cardCode[1]
    }

    val isRed = suitChar == 'H' || suitChar == 'D'
    val colorName = if (isRed) "red" else "black"

    // Load suit image
    val suitDrawableId = when (suitChar) {
        'S' -> R.drawable.suit_spades
        'C' -> R.drawable.suit_clubs
        'H' -> R.drawable.suit_hearts
        'D' -> R.drawable.suit_diamonds
        else -> 0
    }
    val suitBitmap = if (suitDrawableId != 0) {
        BitmapFactory.decodeResource(context.resources, suitDrawableId)
    } else null
    val scaledSuit = suitBitmap?.let {
        Bitmap.createScaledBitmap(it, (width * 0.12).toInt(), (height * 0.12).toInt(), true)
    }

    // ----- Center Content -----
    if (rank.toIntOrNull() != null) {
        val num = rank.toInt()
        val positions = getCardPipPositions(num, width, height, scaledSuit?.height ?: 0)
        positions.forEach { pos ->
            scaledSuit?.let {
                canvas.drawBitmap(it, pos.first - it.width / 2, pos.second - it.height / 2, paint)
            }
        }
    } else {
        if (rank == "A") {
            // Ace: center the suit symbol bigger than normal
            suitBitmap?.let { originalSuit ->
                val aceSuitSize = (width * 0.35).toInt()  // bigger size for Ace center pip
                val scaledAceSuit = Bitmap.createScaledBitmap(
                    originalSuit,
                    aceSuitSize,
                    aceSuitSize,
                    true
                )
                val left = (width - scaledAceSuit.width) / 2f
                val top = (height - scaledAceSuit.height) / 2f
                canvas.drawBitmap(scaledAceSuit, left, top, paint)
            }
        } else {
            val drawableName = when (rank) {
                "J" -> "jack_$colorName"
                "Q" -> "queen_$colorName"
                "K" -> "king_$colorName"
                else -> null
            }
            drawableName?.let {
                val resId = context.resources.getIdentifier(it, "drawable", context.packageName)
                if (resId != 0) {
                    val artBitmap = BitmapFactory.decodeResource(context.resources, resId)
                    val scaled = Bitmap.createScaledBitmap(
                        artBitmap,
                        (width * 0.5).toInt(),
                        (height * 0.5).toInt(),
                        true
                    )
                    val left = (width - scaled.width) / 2f
                    val top = (height - scaled.height) / 2f
                    canvas.drawBitmap(scaled, left, top, paint)
                }
            }
        }
    }

    // ----- Corner Rank Images -----
    val cornerRankName = "rank_${rank.lowercase()}_$colorName"
    val cornerResId = context.resources.getIdentifier(cornerRankName, "drawable", context.packageName)
    if (cornerResId != 0) {
        val rankBitmap = BitmapFactory.decodeResource(context.resources, cornerResId)
        val scaledRank = Bitmap.createScaledBitmap(
            rankBitmap,
            (width * 0.18).toInt(),
            (height * 0.18).toInt(),
            true
        )

        // Top-left (rank above suit)
        canvas.drawBitmap(scaledRank, padding, padding, paint)

        // Bottom-right (rank below suit, rotated 180°)
        canvas.save()
        canvas.rotate(180f, width - padding - scaledRank.width / 2, height - padding - scaledRank.height / 2)
        canvas.drawBitmap(scaledRank, width - scaledRank.width - padding, height - scaledRank.height - padding, paint)
        canvas.restore()
    }

    // ----- Corner Suit Images -----
    scaledSuit?.let { suitBitmap ->
        val rankUpper = rank.uppercase() // normalize case

        if (rankUpper in listOf("J", "Q", "K", "A")) {
            // Top-left below rank
            canvas.drawBitmap(suitBitmap, padding, suitBitmap.height + padding + 10, paint)

            // Bottom-right above rank, rotated 180°
            canvas.save()
            val pivotX = width - padding - suitBitmap.width / 2f
            val pivotY = height - padding - suitBitmap.height / 2f

            canvas.rotate(180f, pivotX, pivotY)

            val rankHeight = (width * 0.18).toInt()
            val yOffset = rankHeight + 10 // space between suit and rank

            // Draw the suit shifted **down** by yOffset BEFORE rotation
            canvas.drawBitmap(
                suitBitmap,
                width - suitBitmap.width - padding,
                height - suitBitmap.height - padding + yOffset,
                paint
            )

            canvas.restore()
        }
    }

    return bitmap
}

private fun getCardPipPositions(num: Int, cardW: Int, cardH: Int, pipSize: Int): List<Pair<Float, Float>> {
    val positions = mutableListOf<Pair<Float, Float>>()
    val cx = cardW / 2f

    // Smaller horizontal spread for a tighter look
    val xOffset = pipSize * 1.2f
    val xLeft = cx - xOffset
    val xRight = cx + xOffset

    // Smaller vertical spacing
    val yUnit = cardH / 16f
    val top = 2.5f * yUnit
    val upper = 5f * yUnit
    val upperMid = 6.5f * yUnit
    val mid = cardH / 2f
    val lowerMid = cardH - upperMid
    val lower = cardH - upper
    val bottom = cardH - top

    when (num) {
        1 -> positions.add(cx to mid)

        2 -> positions.addAll(listOf(cx to top, cx to bottom))

        3 -> positions.addAll(listOf(cx to top, cx to mid, cx to bottom))

        4 -> positions.addAll(listOf(
            xLeft to top, xRight to top,
            xLeft to bottom, xRight to bottom
        ))

        5 -> positions.addAll(getCardPipPositions(4, cardW, cardH, pipSize) + listOf(cx to mid))

        6 -> positions.addAll(listOf(
            xLeft to top, xRight to top,
            xLeft to mid, xRight to mid,
            xLeft to bottom, xRight to bottom
        ))

        7 -> positions.addAll(getCardPipPositions(6, cardW, cardH, pipSize) + listOf(cx to upperMid))

        8 -> positions.addAll(listOf(
            xLeft to top, xRight to top,
            xLeft to upperMid, xRight to upperMid,
            xLeft to lowerMid, xRight to lowerMid,
            xLeft to bottom, xRight to bottom
        ))

        9 -> positions.addAll(getCardPipPositions(8, cardW, cardH, pipSize) + listOf(cx to mid))

        10 -> positions.addAll(listOf(
            // Left and right columns, 4 pips each vertically spaced (top, upperMid, lowerMid, bottom)
            xLeft to top, xRight to top,
            xLeft to upperMid, xRight to upperMid,
            xLeft to lowerMid, xRight to lowerMid,
            xLeft to bottom, xRight to bottom,
            cx to upper,
            cx to lower
        ))
    }
    return positions
}


