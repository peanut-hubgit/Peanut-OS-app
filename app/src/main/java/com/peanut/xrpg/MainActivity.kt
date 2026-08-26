package com.peanut.xrpg

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup

private data class Player(
    var name: String,
    var antiJinx: Boolean = false,
    var percentageMode: Boolean = false,
    var luck: Int = 0,
    var curse: Int = 0,
    var luckPercent: Int = 0,
    var cursePercent: Int = 0,
    val results: MutableList<Int> = mutableListOf()
)

private enum class VisualMode { TWO_D, THREE_D }

class MainActivity : AppCompatActivity() {
    private val bg = Color.rgb(10, 11, 15)
    private val panel = Color.rgb(22, 24, 31)
    private val panel2 = Color.rgb(30, 33, 42)
    private val text = Color.rgb(245, 247, 250)
    private val muted = Color.rgb(154, 162, 176)
    private val accent = Color.rgb(121, 231, 181)
    private val handler = Handler(Looper.getMainLooper())

    private val players = mutableListOf(Player("Player 1"), Player("Player 2"))
    private var activePlayer = 0
    private var sides = 20
    private var diceCount = 1
    private var mode = VisualMode.TWO_D

    private lateinit var root: FrameLayout
    private lateinit var content: LinearLayout
    private lateinit var playerStrip: LinearLayout
    private lateinit var diceGrid: GridLayout
    private lateinit var resultsContainer: LinearLayout
    private lateinit var modeGroup: MaterialButtonToggleGroup
    private lateinit var quantityText: TextView
    private lateinit var typeText: TextView
    private lateinit var visual3d: Dice3DView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        showHome()
    }

    private fun showHome() {
        root = FrameLayout(this).apply { setBackgroundColor(bg) }
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(34), dp(30), dp(34), dp(30))
        }
        val logo = ImageView(this).apply {
            setImageResource(R.drawable.xrpg_logo)
            contentDescription = "Logo XRpg"
            scaleType = ImageView.ScaleType.FIT_CENTER
            alpha = 0f
            scaleX = .82f; scaleY = .82f
        }
        box.addView(logo, LinearLayout.LayoutParams(-1, dp(245)))
        val title = label("XRpg", 42f, text, true).apply { gravity = Gravity.CENTER; alpha = 0f }
        box.addView(title, marginParams(0, 8, 0, 4))
        val subtitle = label("Dados rápidos. Regras suas.", 15f, muted, false).apply { gravity = Gravity.CENTER; alpha = 0f }
        box.addView(subtitle, marginParams(0, 0, 0, 30))
        val play = actionButton("Jogar", text, accent, R.drawable.ic_play, 22).apply {
            alpha = 0f; scaleX = .86f; scaleY = .86f
        }
        box.addView(play, LinearLayout.LayoutParams(-1, dp(58)))
        val donation = actionButton("Doação", text, panel2, R.drawable.ic_favorite, 22)
        box.addView(donation, marginParams(0, 12, 0, 0))
        box.addView(label("@Peanut & Cyberleek", 12f, muted, false).apply { gravity = Gravity.CENTER }, marginParams(0, 28, 0, 0))
        root.addView(box, FrameLayout.LayoutParams(-1, -1))
        setContentView(root)

        AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(logo, View.ALPHA, 0f, 1f),
                ObjectAnimator.ofFloat(title, View.ALPHA, 0f, 1f),
                ObjectAnimator.ofFloat(subtitle, View.ALPHA, 0f, 1f),
                ObjectAnimator.ofFloat(play, View.ALPHA, 0f, 1f),
                ObjectAnimator.ofFloat(play, View.SCALE_X, .86f, 1f),
                ObjectAnimator.ofFloat(play, View.SCALE_Y, .86f, 1f)
            )
            duration = 650
            interpolator = DecelerateInterpolator()
            start()
        }
        play.setOnClickListener {
            play.animate().scaleX(.92f).scaleY(.92f).setDuration(90).withEndAction {
                play.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
                showMain()
            }.start()
        }
        donation.setOnClickListener { showDonation() }
    }

    private fun showMain() {
        buildMain()
    }

    private fun buildMain() {
        root = FrameLayout(this).apply { setBackgroundColor(bg) }
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(28))
        }
        val scroll = ScrollView(this).apply { isFillViewport = true; overScrollMode = View.OVER_SCROLL_NEVER }
        scroll.addView(content)
        root.addView(scroll, FrameLayout.LayoutParams(-1, -1))

        val header = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        val titleBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        titleBox.addView(label("XRpg", 28f, text, true))
        titleBox.addView(label("MESA DE DADOS", 10f, accent, true), marginParams(0, 1, 0, 0))
        header.addView(titleBox, LinearLayout.LayoutParams(0, -2, 1f))
        val newRound = iconButton("Nova rodada", R.drawable.ic_refresh, panel2, text, 18)
        header.addView(newRound, LinearLayout.LayoutParams(-2, dp(44)))
        content.addView(header, marginParams(0, 0, 0, 14))
        newRound.setOnClickListener { newRound() }

        content.addView(label("JOGADORES", 11f, accent, true), marginParams(0, 2, 0, 8))
        playerStrip = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        content.addView(playerStrip, marginParams(0, 0, 0, 12))
        renderPlayers()

        val config = card()
        config.addView(label("DADO", 11f, accent, true))
        typeText = label("D$sides", 24f, text, true)
        config.addView(typeText, marginParams(0, 4, 0, 6))
        diceGrid = GridLayout(this).apply { columnCount = 4; useDefaultMargins = false }
        listOf(4, 6, 8, 10, 12, 20, 100).forEach { d ->
            val b = actionButton("D$d", if (d == sides) bg else text, if (d == sides) accent else panel2, null, 16)
            diceGrid.addView(b, GridLayout.LayoutParams().apply {
                width = 0; height = dp(48); columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                setMargins(dp(3), dp(3), dp(3), dp(3))
            })
            b.setOnClickListener { sides = d; renderDiceType() }
        }
        config.addView(diceGrid)
        content.addView(config, marginParams(0, 0, 0, 12))

        val quantity = card()
        quantity.addView(label("QUANTIDADE DE DADOS", 11f, accent, true))
        val quantityRow = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        val minus = iconButton("Diminuir", R.drawable.ic_remove, panel2, text, 22)
        quantityText = label("1", 28f, text, true).apply { gravity = Gravity.CENTER }
        val plus = iconButton("Aumentar", R.drawable.ic_add, panel2, text, 22)
        quantityRow.addView(minus, LinearLayout.LayoutParams(dp(52), dp(52)))
        quantityRow.addView(quantityText, LinearLayout.LayoutParams(0, dp(52), 1f))
        quantityRow.addView(plus, LinearLayout.LayoutParams(dp(52), dp(52)))
        quantity.addView(quantityRow, marginParams(0, 5, 0, 0))
        minus.setOnClickListener { diceCount = (diceCount - 1).coerceAtLeast(1); renderQuantity() }
        plus.setOnClickListener { diceCount = (diceCount + 1).coerceAtMost(8); renderQuantity() }
        content.addView(quantity, marginParams(0, 0, 0, 12))

        val visual = card()
        visual.addView(label("VISUALIZAÇÃO", 11f, accent, true))
        modeGroup = MaterialButtonToggleGroup(this).apply { isSingleSelection = true; isSelectionRequired = true }
        val two = actionButton("2D", text, panel2, null, 18)
        val three = actionButton("3D", text, panel2, null, 18)
        modeGroup.addView(two, LinearLayout.LayoutParams(0, dp(50), 1f))
        modeGroup.addView(three, LinearLayout.LayoutParams(0, dp(50), 1f))
        modeGroup.check(if (mode == VisualMode.TWO_D) two.id else three.id)
        visual.addView(modeGroup, marginParams(0, 6, 0, 0))
        modeGroup.addOnButtonCheckedListener { _, checkedId, checked ->
            if (checked) {
                mode = if (checkedId == two.id) VisualMode.TWO_D else VisualMode.THREE_D
                updateVisualMode()
            }
        }
        visual3d = Dice3DView(this)
        visual3d.setDice(sides, diceCount)
        visual.addView(visual3d, LinearLayout.LayoutParams(-1, dp(245)).apply { topMargin = dp(8) })
        content.addView(visual, marginParams(0, 0, 0, 12))

        val settings = card()
        settings.addView(label("DESTINO DO JOGADOR", 11f, accent, true))
        val antiButton = actionButton("Sorte, azar e anti-zikamento", text, panel2, R.drawable.ic_settings, 18)
        settings.addView(antiButton, marginParams(0, 6, 0, 0))
        antiButton.setOnClickListener { showPlayerSettings() }
        content.addView(settings, marginParams(0, 0, 0, 12))

        val roll = actionButton("ROLAR DADO", bg, accent, R.drawable.ic_casino, 22)
        content.addView(roll, LinearLayout.LayoutParams(-1, dp(60)).apply { bottomMargin = dp(12) })
        roll.setOnClickListener { rollAll() }

        val result = card()
        result.addView(label("RESULTADOS", 11f, accent, true))
        resultsContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        result.addView(resultsContainer, marginParams(0, 6, 0, 0))
        content.addView(result, marginParams(0, 0, 0, 12))

        val tools = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val help = iconButton("Ajuda", R.drawable.ic_info, panel2, text, 18)
        val credits = iconButton("Créditos", R.drawable.ic_person, panel2, text, 18)
        tools.addView(help, LinearLayout.LayoutParams(0, dp(48), 1f).apply { rightMargin = dp(5) })
        tools.addView(credits, LinearLayout.LayoutParams(0, dp(48), 1f).apply { leftMargin = dp(5) })
        content.addView(tools)
        help.setOnClickListener { showHelp() }
        credits.setOnClickListener { showCredits() }

        setContentView(root)
        updateVisualMode()
        renderResults()
    }

    private fun renderPlayers() {
        playerStrip.removeAllViews()
        players.forEachIndexed { i, p ->
            val b = actionButton(p.name, textFor(i == activePlayer), if (i == activePlayer) accent else panel2, null, 18)
            playerStrip.addView(b, LinearLayout.LayoutParams(0, dp(44), 1f).apply { setMargins(dp(3), 0, dp(3), 0) })
            b.setOnClickListener { activePlayer = i; renderPlayers(); renderResults() }
        }
        val add = iconButton("Adicionar jogador", R.drawable.ic_add, panel2, text, 22)
        playerStrip.addView(add, LinearLayout.LayoutParams(dp(48), dp(44)).apply { leftMargin = dp(4) })
        add.setOnClickListener {
            players.add(Player("Player ${players.size + 1}"))
            activePlayer = players.lastIndex
            renderPlayers(); renderResults()
        }
    }

    private fun textFor(active: Boolean) = if (active) bg else text

    private fun renderDiceType() {
        typeText.text = "D$sides"
        for (i in 0 until diceGrid.childCount) {
            val b = diceGrid.getChildAt(i) as MaterialButton
            val selected = b.text.toString() == "D$sides"
            b.setTextColor(if (selected) bg else text)
            b.backgroundTintList = ColorStateList.valueOf(if (selected) accent else panel2)
        }
        visual3d.setDice(sides, diceCount)
    }

    private fun renderQuantity() {
        quantityText.text = diceCount.toString()
        visual3d.setDice(sides, diceCount)
    }

    private fun updateVisualMode() {
        val is3d = mode == VisualMode.THREE_D
        visual3d.visibility = if (is3d) View.VISIBLE else View.GONE
        if (is3d) visual3d.setDice(sides, diceCount)
    }

    private fun rollAll() {
        val p = players[activePlayer]
        p.results.clear()
        repeat(diceCount) {
            p.results.add(DiceEngine.roll(sides, p.luck, p.curse, p.antiJinx, p.percentageMode, p.luckPercent, p.cursePercent))
        }
        if (mode == VisualMode.THREE_D) visual3d.roll()
        renderResults(animate = mode == VisualMode.TWO_D)
    }

    private fun rollSingle(index: Int) {
        val p = players[activePlayer]
        while (p.results.size < diceCount) p.results.add(DiceEngine.roll(sides, p.luck, p.curse, p.antiJinx, p.percentageMode, p.luckPercent, p.cursePercent))
        if (index in p.results.indices) {
            p.results[index] = DiceEngine.roll(sides, p.luck, p.curse, p.antiJinx, p.percentageMode, p.luckPercent, p.cursePercent)
        }
        if (mode == VisualMode.THREE_D) visual3d.roll()
        renderResults(animate = mode == VisualMode.TWO_D)
    }

    private fun renderResults(animate: Boolean = false) {
        if (!::resultsContainer.isInitialized) return
        resultsContainer.removeAllViews()
        val p = players[activePlayer]
        if (p.results.isEmpty()) {
            resultsContainer.addView(label("Role para começar", 16f, muted, false).apply { gravity = Gravity.CENTER; setPadding(0, dp(18), 0, dp(18)) })
            return
        }
        p.results.forEachIndexed { index, value ->
            val row = LinearLayout(this).apply {
                gravity = Gravity.CENTER_VERTICAL
                background = rounded(panel2, 18)
                setPadding(dp(14), dp(10), dp(8), dp(10))
            }
            row.addView(label("D$sides", 13f, muted, true), LinearLayout.LayoutParams(0, -2, 1f))
            val result = label(value.toString(), 28f, text, true).apply { gravity = Gravity.CENTER }
            row.addView(result, LinearLayout.LayoutParams(dp(80), dp(48)))
            val reroll = iconButton("Rolar este dado", R.drawable.ic_refresh, panel, text, 18)
            row.addView(reroll, LinearLayout.LayoutParams(dp(48), dp(48)).apply { leftMargin = dp(6) })
            reroll.setOnClickListener { rollSingle(index) }
            resultsContainer.addView(row, marginParams(0, 4, 0, 4))
            if (animate) animate2DResult(result, value, sides)
        }
        if (p.results.size > 1) {
            resultsContainer.addView(label("Total  ${p.results.sum()}", 18f, accent, true).apply { gravity = Gravity.END; setPadding(0, dp(10), dp(6), 0) })
        }
    }

    private fun animate2DResult(target: TextView, finalValue: Int, max: Int) {
        var ticks = 0
        val runnable = object : Runnable {
            override fun run() {
                ticks++
                if (ticks < 13) {
                    target.text = (1..max).random().toString()
                    target.animate().scaleX(1.08f).scaleY(1.08f).setDuration(35).withEndAction {
                        target.animate().scaleX(1f).scaleY(1f).setDuration(35).start()
                    }.start()
                    handler.postDelayed(this, 55)
                } else {
                    target.text = finalValue.toString()
                    target.animate().scaleX(1.18f).scaleY(1.18f).setDuration(80).withEndAction {
                        target.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
                    }.start()
                }
            }
        }
        handler.post(runnable)
    }

    private fun showPlayerSettings() {
        val p = players[activePlayer]
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), 0, dp(20), 0) }
        val name = EditText(this).apply {
            setText(p.name); setTextColor(text); setHintTextColor(muted); hint = "Nome do jogador"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
        }
        box.addView(name, marginParams(0, 4, 0, 8))
        val anti = Switch(this).apply { text = "Anti-zikamento nesta rodada"; isChecked = p.antiJinx; setTextColor(text) }
        box.addView(anti)
        val percent = Switch(this).apply { text = "Sorte e azar em porcentagem"; isChecked = p.percentageMode; setTextColor(text) }
        box.addView(percent)
        val luck = numberField(if (p.percentageMode) "Sorte (%)" else "Sorte (pontos)", if (p.percentageMode) p.luckPercent else p.luck)
        val curse = numberField(if (p.percentageMode) "Azar (%)" else "Azar (pontos)", if (p.percentageMode) p.cursePercent else p.curse)
        box.addView(luck); box.addView(curse)
        percent.setOnCheckedChangeListener { _, checked ->
            luck.hint = if (checked) "Sorte (%)" else "Sorte (pontos)"
            curse.hint = if (checked) "Azar (%)" else "Azar (pontos)"
        }
        val builder = AlertDialog.Builder(this).setTitle("${p.name} • destino").setView(box)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Salvar") { _, _ ->
                p.name = name.text.toString().trim().ifEmpty { "Player ${activePlayer + 1}" }
                p.antiJinx = anti.isChecked
                p.percentageMode = percent.isChecked
                val a = luck.text.toString().toIntOrNull() ?: 0
                val c = curse.text.toString().toIntOrNull() ?: 0
                if (p.percentageMode) { p.luckPercent = a.coerceIn(0, 100); p.cursePercent = c.coerceIn(0, 100) }
                else { p.luck = a.coerceIn(0, 100); p.curse = c.coerceIn(0, 100) }
                renderPlayers(); renderResults()
            }
        if (players.size > 1) builder.setNeutralButton("Excluir jogador") { _, _ ->
            players.removeAt(activePlayer)
            activePlayer = activePlayer.coerceAtMost(players.lastIndex)
            renderPlayers(); renderResults()
        }
        builder.show()
    }

    private fun numberField(h: String, value: Int) = EditText(this).apply {
        hint = h; setText(value.toString()); setTextColor(text); setHintTextColor(muted); inputType = InputType.TYPE_CLASS_NUMBER
    }

    private fun newRound() {
        players.forEach { it.antiJinx = false; it.results.clear() }
        renderResults()
        Toast.makeText(this, "Nova rodada — anti-zikamento desligado", Toast.LENGTH_SHORT).show()
    }

    private fun showDonation() {
        val copy = actionButton("Copiar mensagem", text, panel2, R.drawable.ic_favorite, 18)
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), 0, dp(20), 0) }
        box.addView(label("O XRpg não usa anúncios. ❤️", 18f, text, true))
        box.addView(label("Se quiser apoiar o projeto, você pode copiar uma mensagem para combinar uma doação com o desenvolvedor.", 14f, muted, false), marginParams(0, 8, 0, 12))
        box.addView(copy)
        AlertDialog.Builder(this).setTitle("Doação").setView(box).setPositiveButton("Fechar", null).show()
        copy.setOnClickListener {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("XRpg", "Quero apoiar o XRpg — @Peanut & Cyberleek"))
            Toast.makeText(this, "Mensagem copiada", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showHelp() = AlertDialog.Builder(this).setTitle("Como usar").setMessage(
        "Escolha o jogador, o dado e a quantidade. Em 2D os números passam rapidamente até parar. Em 3D os dados rodam em OpenGL ES.\n\nSorte e azar nunca ultrapassam o limite do próprio dado: um D20 continua entre 1 e 20. O anti-zikamento é por rodada e é desligado ao tocar em Nova rodada."
    ).setPositiveButton("Entendi", null).show()

    private fun showCredits() = AlertDialog.Builder(this).setTitle("XRpg").setMessage("Feito por @Peanut & Cyberleek\n\nSem anúncios. Leve. AOSP.\nKotlin + Java + OpenGL ES 2.0.").setPositiveButton("Fechar", null).show()

    private fun card() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(15), dp(14), dp(15), dp(14))
        background = rounded(panel, 24)
    }

    private fun label(value: String, size: Float, color: Int, bold: Boolean) = TextView(this).apply {
        text = value; textSize = size; setTextColor(color); if (bold) typeface = Typeface.DEFAULT_BOLD
    }

    private fun actionButton(value: String, fg: Int, bgColor: Int, icon: Int?, radius: Int): MaterialButton = MaterialButton(this).apply {
        id = View.generateViewId(); text = value; textSize = 13f; setTextColor(fg)
        backgroundTintList = ColorStateList.valueOf(bgColor)
        cornerRadius = dp(radius); strokeWidth = 0; insetTop = 0; insetBottom = 0
        if (icon != null) { setIconResource(icon); iconTint = ColorStateList.valueOf(fg); iconGravity = MaterialButton.ICON_GRAVITY_TEXT_START; iconPadding = dp(8) }
        rippleColor = ColorStateList.valueOf(Color.argb(35, 255, 255, 255))
    }

    private fun iconButton(desc: String, icon: Int, bgColor: Int, fg: Int, radius: Int) = actionButton(desc, fg, bgColor, icon, radius).apply {
        contentDescription = desc
        iconGravity = MaterialButton.ICON_GRAVITY_TEXT_START
        text = ""
        iconPadding = 0
    }

    private fun rounded(color: Int, radius: Int) = GradientDrawable().apply { setColor(color); cornerRadius = dp(radius).toFloat() }
    private fun marginParams(l: Int, t: Int, r: Int, b: Int) = LinearLayout.LayoutParams(-1, -2).apply { setMargins(dp(l), dp(t), dp(r), dp(b)) }
    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
