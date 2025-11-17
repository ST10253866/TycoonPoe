package vcmsa.projects.tycoonpoe

import android.content.Context
import android.graphics.*
import android.graphics.BitmapFactory
import android.os.Process
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import vcmsa.projects.tycoonpoe.SettingsEntity
import vcmsa.projects.tycoonpoe.TycoonDatabase
import kotlin.math.min

class GameView(context: Context, attrs: AttributeSet? = null) : SurfaceView(context, attrs), Runnable {

    // ---------- Threading ----------
    @Volatile private var running = false
    @Volatile private var needsRedraw = false
    private var thread: Thread? = null

    // Round message
    @Volatile private var roundMessage: String? = null
    @Volatile private var roundMessageShownAt: Long = 0

    private val surfaceHolder: SurfaceHolder = holder
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    // Layout
    private val cardWidth = 140
    private val cardHeight = 210
    private val cardSpacing = 70
    private val cardBaseY = 240

    // Snapshots
    @Volatile private var playerCardCodes: List<String> = emptyList()
    @Volatile private var playerHand: List<Bitmap> = emptyList()
    @Volatile private var selectedOrder: List<Int> = emptyList()
    @Volatile private var otherHands: Map<String, List<Bitmap>> = emptyMap()
    @Volatile private var potGroups: List<List<String>> = emptyList()
    @Volatile private var potCardCodes: List<String> = emptyList()
    @Volatile private var potCards: List<Bitmap> = emptyList()
    @Volatile private var orderedPlayers: List<String> = emptyList()
    @Volatile private var playerCounts: Map<String, Int> = emptyMap()
    @Volatile private var localPlayerId: String? = null

    // Background / table
    @Volatile private var tableBitmap: Bitmap? = null

    // Card cache
    private val cardCache = mutableMapOf<String, Bitmap>()
    @Volatile private var cardBackCache: Bitmap = loadCardBack() // dynamic card back

    interface SelectionListener { fun onSelectionChanged(selectedCodes: List<String>) }
    var selectionListener: SelectionListener? = null

    init {
        surfaceHolder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                if (running) startRenderThread()
                needsRedraw = true
            }
            override fun surfaceChanged(holder: SurfaceHolder, f: Int, w: Int, h: Int) {
                tableBitmap = null
                needsRedraw = true
            }
            override fun surfaceDestroyed(holder: SurfaceHolder) {
                stopRenderThread()
            }
        })

        // Load card back dynamically from Room
        loadCardBackFromSettings()
    }

    // ------------------ ROOM: dynamic card back ------------------
    private fun loadCardBackFromSettings() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = TycoonDatabase.getDatabase(context).settingsDao()
                val settings: SettingsEntity? = dao.getSettings()
                val cardBackName = settings?.selectedCardBack ?: "card_back" // fallback

                val resId = resources.getIdentifier(cardBackName, "drawable", context.packageName)
                val bmp = if (resId != 0) {
                    BitmapFactory.decodeResource(resources, resId)
                        .let { Bitmap.createScaledBitmap(it, cardWidth, cardHeight, true) }
                } else {
                    loadDrawableBitmap("card_back", cardWidth, cardHeight, true)
                }

                withContext(Dispatchers.Main) {
                    cardBackCache = bmp
                    needsRedraw = true
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // ---------- Public ----------
    fun startGame() {
        if (running) return
        running = true
        needsRedraw = true
        if (surfaceHolder.surface.isValid) startRenderThread()
    }

    fun stopGame() {
        running = false
        stopRenderThread()
        synchronized(cardCache) { cardCache.clear() }
        playerHand = emptyList()
        potCards = emptyList()
        needsRedraw = false
    }

    fun setPlayersFromTurnOrder(turnOrder: List<String>, localId: String?, counts: Map<String, Int>) {
        val ordered = if (localId == null) {
            turnOrder
        } else {
            val idx = turnOrder.indexOf(localId)
            if (idx >= 0) turnOrder.drop(idx) + turnOrder.take(idx)
            else listOf(localId) + turnOrder.filter { it != localId }
        }
        localPlayerId = localId
        orderedPlayers = ordered
        playerCounts = counts.toMap()
        needsRedraw = true
    }

    fun setPlayerHandFromCodes(codes: List<String>) {
        val bitmaps = codes.map { getOrCreateCard(it) }
        playerCardCodes = codes.toList()
        playerHand = bitmaps
        selectedOrder = emptyList()
        selectionListener?.onSelectionChanged(getSelectedCardCodes())
        needsRedraw = true
    }

    fun setPotFromCodes(groups: List<List<String>>) {
        val flat = groups.flatten()
        val bitmaps = flat.map { getOrCreateCard(it) }
        potGroups = groups.map { it.toList() }
        potCardCodes = flat
        potCards = bitmaps
        needsRedraw = true
    }

    fun setOtherPlayerCount(playerId: String, count: Int) {
        val newMap = HashMap(otherHands)
        newMap[playerId] = List(count) { cardBackCache }
        otherHands = newMap
        needsRedraw = true
    }

    fun clearPot() {
        potGroups = emptyList()
        potCardCodes = emptyList()
        potCards = emptyList()
        needsRedraw = true
    }

    fun getSelectedCardCodes(): List<String> =
        selectedOrder.mapNotNull { playerCardCodes.getOrNull(it) }

    fun clearSelection() {
        selectedOrder = emptyList()
        selectionListener?.onSelectionChanged(getSelectedCardCodes())
        needsRedraw = true
    }

    fun showRoundMessage(msg: String) {
        roundMessage = msg
        roundMessageShownAt = System.currentTimeMillis()
        needsRedraw = true
    }

    // ---------- Input ----------
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val touchX = event.x.toInt()
            val touchY = event.y.toInt()
            val hand = playerHand
            val handY = height - cardBaseY
            val startX = computeHandStartX(hand.size)

            for (i in hand.indices.reversed()) {
                val x = startX + i * cardSpacing
                val raised = selectedOrder.contains(i)
                val y = if (raised) handY - 30 else handY
                if (Rect(x, y, x + cardWidth, y + cardHeight).contains(touchX, touchY)) {
                    toggleSelection(i)
                    break
                }
            }
        }
        return true
    }

    private fun toggleSelection(index: Int) {
        selectedOrder = if (selectedOrder.contains(index))
            selectedOrder.filterNot { it == index } else selectedOrder + index
        selectionListener?.onSelectionChanged(getSelectedCardCodes())
        needsRedraw = true
    }

    // ---------- Render ----------
    override fun run() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_DISPLAY)
        while (running) {
            if (!surfaceHolder.surface.isValid) {
                try { Thread.sleep(16) } catch (_: InterruptedException) { break }
                continue
            }
            if (!needsRedraw) {
                try { Thread.sleep(16) } catch (_: InterruptedException) { break }
                continue
            }
            if (width < 2 || height < 2) {
                try { Thread.sleep(16) } catch (_: InterruptedException) { break }
                continue
            }

            var canvas: Canvas? = null
            try {
                canvas = surfaceHolder.lockCanvas()
                if (canvas != null) {
                    needsRedraw = false
                    drawGame(canvas)
                }
            } catch (t: Throwable) {
                t.printStackTrace()
            } finally {
                if (canvas != null) {
                    try { surfaceHolder.unlockCanvasAndPost(canvas) } catch (_: Throwable) {}
                }
            }

            try { Thread.sleep(16) } catch (_: InterruptedException) { break }
        }
    }

    private fun drawGame(canvas: Canvas) {
        if (tableBitmap == null && width > 0 && height > 0) {
            tableBitmap = loadDrawableBitmap("table", width, height, true)
        }
        tableBitmap?.let {
            canvas.drawBitmap(it, null, Rect(0, 0, width, height), paint)
        } ?: canvas.drawColor(Color.parseColor("#006400"))

        val handSnap = playerHand
        val selectedSnap = selectedOrder.toSet()
        val potGroupsSnap = potGroups
        val potCodesSnap = potCardCodes
        val potCardsSnap = potCards
        val otherHandsSnap = otherHands
        val countsSnap = playerCounts

        drawPot(canvas, potGroupsSnap, potCodesSnap, potCardsSnap)
        drawRoundMessage(canvas)
        drawOtherPlayersHands(canvas, otherHandsSnap, countsSnap)

        val startX = computeHandStartX(handSnap.size)
        val handY = height - cardBaseY
        handSnap.forEachIndexed { i, bmp ->
            val y = if (selectedSnap.contains(i)) handY - 30 else handY
            canvas.drawBitmap(bmp, startX + i * cardSpacing.toFloat(), y.toFloat(), paint)
        }
    }

    private fun computeHandStartX(num: Int) = ((width - ((num - 1) * cardSpacing + cardWidth)) / 2f).toInt()

    private fun drawRoundMessage(canvas: Canvas) {
        val msg = roundMessage ?: return
        if (System.currentTimeMillis() - roundMessageShownAt > 3000) {
            roundMessage = null
            return
        }
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 64f
        paint.color = Color.WHITE
        paint.setShadowLayer(6f, 2f, 2f, Color.BLACK)
        canvas.drawText(msg, width / 2f, height / 2f, paint)
        paint.clearShadowLayer()
    }

    private fun drawOtherPlayersHands(
        canvas: Canvas,
        others: Map<String, List<Bitmap>>,
        counts: Map<String, Int>
    ) {
        val spacing = 40f
        val cardW = cardBackCache.width.toFloat()
        val cardH = cardBackCache.height.toFloat()
        val playerIds = others.keys.toList()

        playerIds.forEachIndexed { idx, playerId ->
            val hand = List(others[playerId]?.size ?: 0) { cardBackCache }

            when (idx) {
                0 -> { // Top center
                    val y = 50f
                    val totalW = (hand.size - 1) * spacing + cardW
                    val startX = (width - totalW) / 2f
                    hand.forEachIndexed { i, bmp -> canvas.drawBitmap(bmp, startX + i * spacing, y, paint) }
                    drawPlayerLabel(canvas, playerId, counts, width / 2f, y - 10f)
                }
                1 -> { // Left middle
                    val x = 50f
                    val totalH = (hand.size - 1) * spacing + cardH
                    val startY = (height - totalH) / 2f
                    hand.forEachIndexed { i, bmp -> canvas.drawBitmap(bmp, x, startY + i * spacing, paint) }
                    drawPlayerLabel(canvas, playerId, counts, x + cardW / 2f, startY - 20f)
                }
                2 -> { // Right middle
                    val x = width - cardW - 50f
                    val totalH = (hand.size - 1) * spacing + cardH
                    val startY = (height - totalH) / 2f
                    hand.forEachIndexed { i, bmp -> canvas.drawBitmap(bmp, x, startY + i * spacing, paint) }
                    drawPlayerLabel(canvas, playerId, counts, x + cardW / 2f, startY - 20f)
                }
                else -> { // fallback
                    val y = 50f + idx * (cardH + 20f)
                    hand.forEachIndexed { i, bmp -> canvas.drawBitmap(bmp, 100f + i * spacing, y, paint) }
                    drawPlayerLabel(canvas, playerId, counts, 100f, y - 10f)
                }
            }
        }
    }

    private fun drawPlayerLabel(canvas: Canvas, playerId: String, counts: Map<String, Int>, cx: Float, cy: Float) {
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 32f
        paint.color = Color.WHITE
        paint.setShadowLayer(4f, 2f, 2f, Color.BLACK)
        val count = counts[playerId] ?: 0
        canvas.drawText("$playerId ($count)", cx, cy, paint)
        paint.clearShadowLayer()
    }

    private fun drawPot(canvas: Canvas, groups: List<List<String>>, codes: List<String>, cards: List<Bitmap>) {
        if (groups.isEmpty()) return
        val cx = width / 2f; val cy = height / 2f
        val rimW = min(width, height) * 0.52f; val rimH = rimW * 0.58f
        val rim = RectF(cx - rimW / 2f, cy - rimH / 2f, cx + rimW / 2f, cy + rimH / 2f)
        paint.color = Color.argb(230, 25, 25, 25); paint.style = Paint.Style.FILL
        canvas.drawOval(rim, paint)
        val inner = RectF(rim.left + 8f, rim.top + 8f, rim.right - 8f, rim.bottom - 8f)
        paint.color = Color.argb(170, 60, 60, 60)
        canvas.drawOval(inner, paint)

        val scale = 0.9f; val drawW = cardWidth * scale; val drawH = cardHeight * scale
        groups.forEachIndexed { groupIndex, group ->
            val (angle, offset) = stableJitter(group.joinToString(""), groupIndex)
            val (dx, dy) = offset
            val centerX = cx + dx; val centerY = cy + dy
            val spacing = drawW * 0.4f
            val groupWidth = (group.size - 1) * spacing
            val startX = centerX - groupWidth / 2f
            group.forEachIndexed { i, code ->
                val globalIdx = codes.indexOf(code)
                val bmp = cards.getOrNull(globalIdx) ?: return@forEachIndexed
                val gx = startX + i * spacing; val gy = centerY
                canvas.save(); canvas.translate(gx, gy); canvas.rotate(angle)
                canvas.drawBitmap(bmp, null, RectF(-drawW / 2, -drawH / 2, drawW / 2, drawH / 2), paint)
                canvas.restore()
            }
        }
    }

    // ---------- Bitmaps ----------
    private fun getOrCreateCard(code: String): Bitmap =
        synchronized(cardCache) { cardCache.getOrPut(code) { composeCardFromAssets(code) } }

    private fun loadCardBack(): Bitmap = loadDrawableBitmap("card_back", cardWidth, cardHeight, true)

    private fun loadDrawableBitmap(name: String, w: Int, h: Int, scale: Boolean = false): Bitmap {
        val resId = resources.getIdentifier(name, "drawable", context.packageName)
        if (resId == 0) return Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val b = BitmapFactory.decodeResource(resources, resId)
        return if (scale) Bitmap.createScaledBitmap(b, w, h, true) else b
    }

    private fun composeCardFromAssets(code: String): Bitmap {
        val bmp = Bitmap.createBitmap(cardWidth, cardHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.WHITE; paint.style = Paint.Style.FILL
        canvas.drawRoundRect(0f, 0f, cardWidth.toFloat(), cardHeight.toFloat(), 12f, 12f, paint)
        paint.color = Color.BLACK; paint.style = Paint.Style.STROKE; paint.strokeWidth = 3f
        canvas.drawRoundRect(0f, 0f, cardWidth.toFloat(), cardHeight.toFloat(), 12f, 12f, paint)

        val (rankFile, suitFile) = when (code.uppercase()) {
            "RJ" -> "rank_joker_red" to null
            "BJ" -> "rank_joker_black" to null
            else -> {
                val rank = code.dropLast(1).lowercase()
                val suitChar = code.last().uppercaseChar()
                val rankName = when (rank) {
                    "j" -> "rank_j"; "q" -> "rank_q"; "k" -> "rank_k"; "a" -> "rank_a"
                    else -> "rank_$rank"
                }
                val suitName = when (suitChar) {
                    'H' -> "suit_hearts"; 'D' -> "suit_diamonds"; 'S' -> "suit_spades"; 'C' -> "suit_clubs"
                    else -> null
                }
                val color = if (suitChar == 'H' || suitChar == 'D') "red" else "black"
                "${rankName}_$color" to suitName
            }
        }

        try { loadDrawableBitmap(rankFile, 60, 60, true).let { canvas.drawBitmap(it, 12f, 12f, paint) } } catch (_: Throwable) {}
        suitFile?.let { loadDrawableBitmap(it, 40, 40, true).let { bmp2 -> canvas.drawBitmap(bmp2, 60f, 60f, paint) } }
        return bmp
    }

    private fun stableJitter(str: String, seed: Int): Pair<Float, Pair<Float, Float>> {
        val s = (str.hashCode() + seed).toFloat()
        val angle = ((s % 360f) - 180f) * 0.07f
        val dx = ((s * 1.5f) % 30f) - 15f
        val dy = ((s * 2.5f) % 20f) - 10f
        return angle to (dx to dy)
    }

    // ---------- Thread ----------
    private fun startRenderThread() {
        if (thread?.isAlive == true) return
        thread = Thread(this, "GameViewThread").also { it.start() }
    }

    private fun stopRenderThread() {
        try { thread?.join(50) } catch (_: InterruptedException) {}
        thread = null
    }
}
