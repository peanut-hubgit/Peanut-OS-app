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
import android.text.InputType
import android.view.Gravity
import android.view.View
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
    private val players = mutableListOf(Player("Player 1"), Player("Player 2"))
    private var activePlayer = 0
    private var sides = 20
    private var diceCount = 1
    private var mode = VisualMode.TWO_D

    private lateinit var root: FrameLayout
    private lateinit var playerStrip: LinearLayout
    private lateinit var diceGrid: GridLayout
    private lateinit var results: LinearLayout
    private lateinit var quantity: TextView
    private lateinit var type: TextView
    private lateinit var visual2d: Dice2DView
    private lateinit var visual3d: Dice3DView

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        showHome()
    }

    private fun showHome() {
        root = FrameLayout(this).apply { setBackgroundColor(bg) }
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(32), dp(26), dp(32), dp(28))
        }
        val logo = ImageView(this).apply {
            setImageResource(R.drawable.xrpg_logo)
            contentDescription = "Logo XRpg"
            scaleType = ImageView.ScaleType.FIT_CENTER
            alpha = 0f
            scaleX = .82f
            scaleY = .82f
        }
        box.addView(logo, LinearLayout.LayoutParams(-1, dp(240)))
        val title = label("XRpg", 42f, text, true).apply { gravity = Gravity.CENTER; alpha = 0f }
        box.addView(title, margin(0, 8, 0, 3))
        box.addView(label("Dados rápidos. Regras suas.", 15f, muted, false).apply { gravity = Gravity.CENTER }, margin(0, 0, 0, 28))
        val play = action("Jogar", text, accent, R.drawable.ic_play, 22).apply { alpha = 0f; scaleX = .86f; scaleY = .86f }
        box.addView(play, LinearLayout.LayoutParams(-1, dp(58)))
        val donate = action("Doação", text, panel2, R.drawable.ic_favorite, 20)
        box.addView(donate, margin(0, 12, 0, 0))
        box.addView(label("@Peanut & Cyberleek", 12f, muted, false).apply { gravity = Gravity.CENTER }, margin(0, 28, 0, 0))
        root.addView(box, FrameLayout.LayoutParams(-1, -1))
        setContentView(root)
        AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(logo, View.ALPHA, 0f, 1f),
                ObjectAnimator.ofFloat(logo, View.SCALE_X, .82f, 1f),
                ObjectAnimator.ofFloat(logo, View.SCALE_Y, .82f, 1f),
                ObjectAnimator.ofFloat(title, View.ALPHA, 0f, 1f),
                ObjectAnimator.ofFloat(play, View.ALPHA, 0f, 1f),
                ObjectAnimator.ofFloat(play, View.SCALE_X, .86f, 1f),
                ObjectAnimator.ofFloat(play, View.SCALE_Y, .86f, 1f)
            )
            duration = 650
            start()
        }
        play.setOnClickListener {
            play.animate().scaleX(.92f).scaleY(.92f).setDuration(90).withEndAction {
                play.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
                showMain()
            }.start()
        }
        donate.setOnClickListener { showDonation() }
    }

    private fun showMain() {
        root = FrameLayout(this).apply { setBackgroundColor(bg) }
        val scroll = ScrollView(this).apply { isFillViewport = true; overScrollMode = View.OVER_SCROLL_NEVER }
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(12), dp(16), dp(28)) }
        scroll.addView(content)
        root.addView(scroll, FrameLayout.LayoutParams(-1, -1))

        val header = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        val tb = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        tb.addView(label("XRpg", 28f, text, true))
        tb.addView(label("MESA DE DADOS", 10f, accent, true), margin(0, 1, 0, 0))
        header.addView(tb, LinearLayout.LayoutParams(0, -2, 1f))
        val back = icon("Voltar", R.drawable.ic_info, panel2)
        header.addView(back, LinearLayout.LayoutParams(dp(48), dp(44)))
        val round = icon("Nova rodada", R.drawable.ic_refresh, panel2)
        header.addView(round, LinearLayout.LayoutParams(dp(48), dp(44)).apply { leftMargin = dp(6) })
        content.addView(header, margin(0, 0, 0, 14))
        back.setOnClickListener { showHome() }
        round.setOnClickListener { newRound() }

        content.addView(label("JOGADORES", 11f, accent, true), margin(0, 2, 0, 8))
        playerStrip = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        content.addView(playerStrip, margin(0, 0, 0, 12))
        renderPlayers()

        val dice = card()
        dice.addView(label("DADO", 11f, accent, true))
        type = label("D$sides", 24f, text, true)
        dice.addView(type, margin(0, 4, 0, 6))
        diceGrid = GridLayout(this).apply { columnCount = 4; useDefaultMargins = false }
        listOf(4, 6, 8, 10, 12, 20, 100).forEach { d ->
            val b = action("D$d", if (d == sides) bg else text, if (d == sides) accent else panel2, null, 16)
            diceGrid.addView(b, GridLayout.LayoutParams().apply {
                width = 0; height = dp(48)
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                setMargins(dp(3), dp(3), dp(3), dp(3))
            })
            b.setOnClickListener { sides = d; renderDiceType() }
        }
        dice.addView(diceGrid)
        content.addView(dice, margin(0, 0, 0, 12))

        val q = card()
        q.addView(label("QUANTIDADE DE DADOS", 11f, accent, true))
        val qr = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        val minus = icon("Menos", R.drawable.ic_remove, panel2)
        quantity = label("1", 28f, text, true).apply { gravity = Gravity.CENTER }
        val plus = icon("Mais", R.drawable.ic_add, panel2)
        qr.addView(minus, LinearLayout.LayoutParams(dp(52), dp(52)))
        qr.addView(quantity, LinearLayout.LayoutParams(0, dp(52), 1f))
        qr.addView(plus, LinearLayout.LayoutParams(dp(52), dp(52)))
        q.addView(qr, margin(0, 5, 0, 0))
        minus.setOnClickListener { diceCount = (diceCount - 1).coerceAtLeast(1); renderQuantity() }
        plus.setOnClickListener { diceCount = (diceCount + 1).coerceAtMost(8); renderQuantity() }
        content.addView(q, margin(0, 0, 0, 12))

        val visual = card()
        visual.addView(label("VISUALIZAÇÃO", 11f, accent, true))
        val modes = MaterialButtonToggleGroup(this).apply { isSingleSelection = true; isSelectionRequired = true }
        val two = action("2D", text, panel2, null, 18)
        val three = action("3D", text, panel2, null, 18)
        modes.addView(two, LinearLayout.LayoutParams(0, dp(50), 1f))
        modes.addView(three, LinearLayout.LayoutParams(0, dp(50), 1f))
        modes.check(if (mode == VisualMode.TWO_D) two.id else three.id)
        visual.addView(modes, margin(0, 6, 0, 0))
        val frame = FrameLayout(this).apply { background = rounded(panel2, 22); clipToOutline = true }
        visual2d = Dice2DView(this)
        visual3d = Dice3DView(this)
        frame.addView(visual2d, FrameLayout.LayoutParams(-1, dp(245)))
        frame.addView(visual3d, FrameLayout.LayoutParams(-1, dp(245)))
        visual.addView(frame, margin(0, 8, 0, 0))
        content.addView(visual, margin(0, 0, 0, 12))
        modes.addOnButtonCheckedListener { _, id, checked ->
            if (checked) {
                mode = if (id == two.id) VisualMode.TWO_D else VisualMode.THREE_D
                updateVisual()
            }
        }

        val destiny = card()
        destiny.addView(label("DESTINO DO JOGADOR", 11f, accent, true))
        val settings = action("Sorte, azar e anti-zikamento", text, panel2, R.drawable.ic_settings, 18)
        destiny.addView(settings, margin(0, 6, 0, 0))
        settings.setOnClickListener { showSettings() }
        content.addView(destiny, margin(0, 0, 0, 12))

        val roll = action("ROLAR DADO", bg, accent, R.drawable.ic_casino, 22)
        content.addView(roll, LinearLayout.LayoutParams(-1, dp(60)).apply { bottomMargin = dp(12) })
        roll.setOnClickListener { rollAll() }

        val rc = card()
        rc.addView(label("RESULTADOS", 11f, accent, true))
        results = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        rc.addView(results, margin(0, 6, 0, 0))
        content.addView(rc, margin(0, 0, 0, 12))
        val help = action("Como usar", text, panel2, R.drawable.ic_info, 18)
        content.addView(help, LinearLayout.LayoutParams(-1, dp(48)))
        help.setOnClickListener { showHelp() }
        setContentView(root)
        updateVisual()
        renderResults()
    }

    private fun renderPlayers() {
        playerStrip.removeAllViews()
        players.forEachIndexed { i, p ->
            val b = action(p.name, if (i == activePlayer) bg else text, if (i == activePlayer) accent else panel2, null, 18)
            playerStrip.addView(b, LinearLayout.LayoutParams(0, dp(44), 1f).apply { setMargins(dp(3), 0, dp(3), 0) })
            b.setOnClickListener { activePlayer = i; renderPlayers(); renderResults() }
        }
        val add = icon("Adicionar", R.drawable.ic_add, panel2)
        playerStrip.addView(add, LinearLayout.LayoutParams(dp(48), dp(44)).apply { leftMargin = dp(4) })
        add.setOnClickListener { players.add(Player("Player ${players.size + 1}")); activePlayer = players.lastIndex; renderPlayers(); renderResults() }
    }

    private fun renderDiceType() {
        type.text = "D$sides"
        for (i in 0 until diceGrid.childCount) {
            val b = diceGrid.getChildAt(i) as MaterialButton
            val selected = b.text.toString() == "D$sides"
            b.setTextColor(if (selected) bg else text)
            b.backgroundTintList = ColorStateList.valueOf(if (selected) accent else panel2)
        }
        visual3d.setDice(sides, diceCount)
        visual2d.setDice(sides, diceCount)
    }

    private fun renderQuantity() {
        quantity.text = diceCount.toString()
        visual3d.setDice(sides, diceCount)
        visual2d.setDice(sides, diceCount)
    }

    private fun updateVisual() {
        visual2d.visibility = if (mode == VisualMode.TWO_D) View.VISIBLE else View.GONE
        visual3d.visibility = if (mode == VisualMode.THREE_D) View.VISIBLE else View.GONE
        if (mode == VisualMode.THREE_D) visual3d.setDice(sides, diceCount)
    }

    private fun rollAll() {
        val p = players[activePlayer]
        p.results.clear()
        repeat(diceCount) {
            p.results.add(DiceEngine.roll(sides, p.luck, p.curse, p.antiJinx, p.percentageMode, p.luckPercent, p.cursePercent))
        }
        if (mode == VisualMode.TWO_D) visual2d.roll(sides, p.results.toIntArray()) else { visual3d.setDice(sides, diceCount); visual3d.roll() }
        renderResults()
    }

    private fun rollSingle(i: Int) {
        val p = players[activePlayer]
        while (p.results.size < diceCount) p.results.add(DiceEngine.roll(sides, p.luck, p.curse, p.antiJinx, p.percentageMode, p.luckPercent, p.cursePercent))
        if (i >= p.results.size) return
        p.results[i] = DiceEngine.roll(sides, p.luck, p.curse, p.antiJinx, p.percentageMode, p.luckPercent, p.cursePercent)
        if (mode == VisualMode.TWO_D) visual2d.roll(sides, p.results.toIntArray()) else { visual3d.setDice(sides, diceCount); visual3d.roll() }
        renderResults()
    }

    private fun renderResults() {
        if (!::results.isInitialized) return
        results.removeAllViews()
        val p = players[activePlayer]
        if (p.results.isEmpty()) {
            results.addView(label("Role para começar", 16f, muted, false).apply { gravity = Gravity.CENTER; setPadding(0, dp(18), 0, dp(18)) })
            return
        }
        p.results.forEachIndexed { i, v ->
            val row = LinearLayout(this).apply {
                gravity = Gravity.CENTER_VERTICAL
                background = rounded(panel2, 18)
                setPadding(dp(14), dp(8), dp(8), dp(8))
            }
            row.addView(label("D$sides", 13f, muted, true), LinearLayout.LayoutParams(0, -2, 1f))
            row.addView(label(v.toString(), 28f, text, true).apply { gravity = Gravity.CENTER }, LinearLayout.LayoutParams(dp(80), dp(48)))
            val re = icon("Rolar este dado", R.drawable.ic_refresh, panel)
            row.addView(re, LinearLayout.LayoutParams(dp(48), dp(48)).apply { leftMargin = dp(6) })
            re.setOnClickListener { rollSingle(i) }
            results.addView(row, margin(0, 4, 0, 4))
        }
        if (p.results.size > 1) results.addView(label("Total  ${p.results.sum()}", 18f, accent, true).apply { gravity = Gravity.END; setPadding(0, dp(10), dp(6), 0) })
    }

    private fun showSettings() {
        val p = players[activePlayer]
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), 0, dp(20), 0) }
        val name = EditText(this).apply { setText(p.name); setTextColor(this@MainActivity.text); setHintTextColor(muted); hint = "Nome do jogador"; inputType = InputType.TYPE_CLASS_TEXT; isSingleLine = true }
        box.addView(name, margin(0, 4, 0, 8))
        val anti = Switch(this).apply { text = "Anti-zikamento nesta rodada"; isChecked = p.antiJinx; setTextColor(this@MainActivity.text) }
        box.addView(anti)
        val percent = Switch(this).apply { text = "Sorte e azar em porcentagem"; isChecked = p.percentageMode; setTextColor(this@MainActivity.text) }
        box.addView(percent)
        val luck = number(if (p.percentageMode) "Sorte (%)" else "Sorte (pontos)", if (p.percentageMode) p.luckPercent else p.luck)
        val curse = number(if (p.percentageMode) "Azar (%)" else "Azar (pontos)", if (p.percentageMode) p.cursePercent else p.curse)
        box.addView(luck); box.addView(curse)
        percent.setOnCheckedChangeListener { _, c -> luck.hint = if (c) "Sorte (%)" else "Sorte (pontos)"; curse.hint = if (c) "Azar (%)" else "Azar (pontos)" }
        val dialog = AlertDialog.Builder(this).setTitle("${p.name} • destino").setView(box).setNegativeButton("Cancelar", null).setPositiveButton("Salvar", null).create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                p.name = name.text.toString().trim().ifEmpty { "Player ${activePlayer + 1}" }
                p.antiJinx = anti.isChecked
                p.percentageMode = percent.isChecked
                val a = luck.text.toString().toIntOrNull() ?: 0
                val c = curse.text.toString().toIntOrNull() ?: 0
                if (p.percentageMode) { p.luckPercent = a.coerceIn(0, 100); p.cursePercent = c.coerceIn(0, 100) }
                else { p.luck = a.coerceIn(0, 100); p.curse = c.coerceIn(0, 100) }
                renderPlayers(); renderResults(); dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun newRound() {
        players.forEach { it.antiJinx = false; it.results.clear() }
        renderResults()
        Toast.makeText(this, "Nova rodada — anti-zikamento desligado", Toast.LENGTH_SHORT).show()
    }

    private fun showDonation() {
        val copy = action("Copiar chave PIX", text, panel2, R.drawable.ic_favorite, 18)
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), 0, dp(20), 0) }
        box.addView(label("O XRpg não usa anúncios. ❤️", 18f, text, true))
        box.addView(label("Cada real importa. ❤️", 16f, accent, true), margin(0, 8, 0, 4))
        box.addView(label("Se quiser apoiar o projeto, você pode enviar qualquer valor pela chave PIX abaixo.", 14f, muted, false), margin(0, 0, 0, 10))
        box.addView(label("CHAVE PIX", 11f, accent, true))
        box.addView(label("6292068@vakinha.com.br", 17f, text, true), margin(0, 4, 0, 12))
        box.addView(copy)
        AlertDialog.Builder(this).setTitle("Doação").setView(box).setPositiveButton("Fechar", null).show()
        copy.setOnClickListener {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("PIX XRpg", "6292068@vakinha.com.br"))
            Toast.makeText(this, "Chave PIX copiada", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showHelp() = AlertDialog.Builder(this).setTitle("Como usar").setMessage("Escolha o player, o dado e a quantidade. 2D mostra dados visuais e anima os números até o resultado. 3D usa OpenGL ES nativo, com polígonos reais e suporte a vários dados. Sorte/azar é aplicado dentro do limite do dado: D20 nunca passa de 20. Nova rodada desliga o anti-zikamento.").setPositiveButton("Entendi", null).show()

    private fun action(v: String, fg: Int, bgColor: Int, icon: Int?, radius: Int) = MaterialButton(this).apply {
        id = View.generateViewId(); text = v; textSize = 13f; setTextColor(fg); backgroundTintList = ColorStateList.valueOf(bgColor); cornerRadius = dp(radius); insetTop = 0; insetBottom = 0; strokeWidth = 0; stateListAnimator = null
        if (icon != null) { setIconResource(icon); iconTint = ColorStateList.valueOf(fg); iconGravity = MaterialButton.ICON_GRAVITY_TEXT_START; iconPadding = dp(8) }
    }

    private fun icon(desc: String, res: Int, bgColor: Int) = action("", text, bgColor, res, 16).apply { contentDescription = desc; iconGravity = MaterialButton.ICON_GRAVITY_TEXT_START; iconPadding = 0 }
    private fun card() = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(15), dp(14), dp(15), dp(14)); background = rounded(panel, 24) }
    private fun label(v: String, s: Float, c: Int, b: Boolean) = TextView(this).apply { text = v; textSize = s; setTextColor(c); if (b) typeface = Typeface.DEFAULT_BOLD }
    private fun number(h: String, v: Int) = EditText(this).apply { hint = h; setText(v.toString()); setTextColor(this@MainActivity.text); setHintTextColor(muted); inputType = InputType.TYPE_CLASS_NUMBER; isSingleLine = true }
    private fun rounded(c: Int, r: Int) = GradientDrawable().apply { setColor(c); cornerRadius = dp(r).toFloat() }
    private fun margin(l: Int, t: Int, r: Int, b: Int) = LinearLayout.LayoutParams(-1, -2).apply { setMargins(dp(l), dp(t), dp(r), dp(b)) }
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
