package com.peanut.xrpg

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

private data class Player(
    var name: String,
    var antiJinx: Boolean = false,
    var percentageMode: Boolean = false,
    var luck: Int = 0,
    var curse: Int = 0,
    var luckPercent: Int = 0,
    var cursePercent: Int = 0,
    var lastResult: String = "—"
)

class MainActivity : AppCompatActivity() {
    private val bg = Color.rgb(15, 17, 23)
    private val panel = Color.rgb(25, 28, 36)
    private val panel2 = Color.rgb(32, 36, 46)
    private val text = Color.rgb(245, 247, 250)
    private val muted = Color.rgb(155, 163, 178)
    private val accent = Color.rgb(105, 226, 174)

    private val players = mutableListOf(Player("Player 1"), Player("Player 2"))
    private var activePlayer = 0
    private var dice2d = false
    private var dice3d = false
    private lateinit var playerStrip: LinearLayout
    private lateinit var resultText: TextView
    private lateinit var modeText: TextView
    private lateinit var dice3dView: Dice3DView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        buildUi()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(20), dp(16), dp(20), dp(10))
        }
        header.addView(label("XRpg", 28f, text, true), LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(button("NOVA RODADA", accent, bg) { newRound() }, LinearLayout.LayoutParams(-2, dp(42)))
        root.addView(header)

        val scroll = ScrollView(this).apply { isFillViewport = true }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), 0, dp(16), dp(24))
        }
        scroll.addView(content)

        content.addView(label("Escolha o jogador", 13f, muted, true), marginParams(0, 8, 0, 8))
        playerStrip = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        content.addView(playerStrip, marginParams(0, 0, 0, 12))
        renderPlayers()

        val status = card()
        modeText = label("Dados clássicos • 1 dado por toque", 13f, muted, false)
        status.addView(label("MESA", 11f, accent, true))
        status.addView(modeText, marginParams(0, 4, 0, 0))
        content.addView(status, marginParams(0, 0, 0, 12))

        val diceCard = card()
        diceCard.addView(label("DADOS", 11f, accent, true))
        val diceGrid = GridLayout(this).apply {
            columnCount = 4
            rowCount = 2
            useDefaultMargins = false
        }
        listOf(4, 6, 8, 10, 12, 20, 100).forEach { sides ->
            val b = button("D$sides", text, panel2) { rollDie(sides, it) }
            diceGrid.addView(b, GridLayout.LayoutParams().apply {
                width = 0; height = dp(52)
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                setMargins(dp(4), dp(4), dp(4), dp(4))
            })
        }
        diceCard.addView(diceGrid, marginParams(0, 6, 0, 0))
        content.addView(diceCard, marginParams(0, 0, 0, 12))

        val modes = card()
        val twoD = Switch(this).apply {
            text = "Modo Dados 2D"
            textSize = 15f
            setTextColor(this@MainActivity.text)
            isChecked = false
            setOnCheckedChangeListener { _, checked ->
                dice2d = checked
                if (checked) dice3d = false
                modeText.text = when {
                    dice3d -> "Dados 3D • renderização nativa • toque em qualquer dado"
                    checked -> "Dados 2D • dois dados por toque • soma automática"
                    else -> "Dados clássicos • 1 dado por toque"
                }
                dice3dView.visibility = if (dice3d) View.VISIBLE else View.GONE
            }
        }
        modes.addView(twoD)

        val threeD = Switch(this).apply {
            text = "Modo Dados 3D"
            textSize = 15f
            setTextColor(this@MainActivity.text)
            isChecked = false
            setOnCheckedChangeListener { _, checked ->
                dice3d = checked
                if (checked) twoD.isChecked = false
                modeText.text = when {
                    checked -> "Dados 3D • renderização nativa • toque em qualquer dado"
                    dice2d -> "Dados 2D • dois dados por toque • soma automática"
                    else -> "Dados clássicos • 1 dado por toque"
                }
                dice3dView.visibility = if (checked) View.VISIBLE else View.GONE
            }
        }
        modes.addView(threeD, marginParams(0, 2, 0, 0))

        val threeDCard = card()
        threeDCard.addView(label("VISUAL 3D", 11f, accent, true))
        dice3dView = Dice3DView(this).apply {
            visibility = View.GONE
            setBackgroundColor(panel)
        }
        threeDCard.addView(dice3dView, LinearLayout.LayoutParams(-1, dp(230)).apply {
            topMargin = dp(6)
        })
        modes.addView(threeDCard, marginParams(0, 8, 0, 0))

        val anti = button("CONFIGURAR ANTI-ZIKAMENTO", text, panel2) { showPlayerSettings() }
        modes.addView(anti, marginParams(0, 8, 0, 0))
        content.addView(modes, marginParams(0, 0, 0, 12))

        val resultCard = card()
        resultCard.addView(label("ÚLTIMO RESULTADO", 11f, accent, true))
        resultText = label("Role um dado para começar", 24f, text, true).apply { gravity = Gravity.CENTER }
        resultCard.addView(resultText, marginParams(0, 14, 0, 4))
        content.addView(resultCard, marginParams(0, 0, 0, 12))

        content.addView(label("JOGADORES", 11f, accent, true), marginParams(0, 4, 0, 8))
        val playersList = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            tag = "playersList"
        }
        content.addView(playersList)
        renderPlayerCards(playersList)

        setContentView(root.apply { addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f)) })
    }

    private fun renderPlayers() {
        playerStrip.removeAllViews()
        players.forEachIndexed { index, player ->
            val b = button(player.name, if (index == activePlayer) bg else text, if (index == activePlayer) accent else panel2) {
                activePlayer = index
                renderPlayers()
                refreshCards()
            }
            playerStrip.addView(b, LinearLayout.LayoutParams(0, dp(44), 1f).apply { setMargins(dp(3), 0, dp(3), 0) })
        }
        playerStrip.addView(button("+", text, panel2) { addPlayer() }, LinearLayout.LayoutParams(dp(48), dp(44)).apply { setMargins(dp(3), 0, 0, 0) })
    }

    private fun addPlayer() {
        players += Player("Player ${players.size + 1}")
        activePlayer = players.lastIndex
        rebuildPlayerSection()
    }

    private fun rebuildPlayerSection() = buildUi()

    private fun rollDie(sides: Int, view: View) {
        val p = players[activePlayer]
        val first = DiceEngine.roll(sides, p.luck, p.curse, p.antiJinx, p.percentageMode, p.luckPercent, p.cursePercent)
        val second = if (dice2d && !dice3d) DiceEngine.roll(sides, p.luck, p.curse, p.antiJinx, p.percentageMode, p.luckPercent, p.cursePercent) else 0
        val output = when {
            dice3d -> "D$sides  •  $first  •  visual 3D"
            dice2d -> "D$sides  •  $first + $second = ${first + second}"
            else -> "D$sides  •  $first"
        }
        p.lastResult = output
        resultText.text = "${p.name}\n$output"
        animateRoll(view)
        if (dice3d) dice3dView.setResult("D$sides", first)
        refreshCards()
    }

    private fun animateRoll(view: View) {
        val scaleX = ObjectAnimator.ofFloat(view, View.SCALE_X, 1f, 0.88f, 1.08f, 1f)
        val scaleY = ObjectAnimator.ofFloat(view, View.SCALE_Y, 1f, 0.88f, 1.08f, 1f)
        val rotation = ObjectAnimator.ofFloat(view, View.ROTATION, 0f, -5f, 5f, 0f)
        AnimatorSet().apply {
            playTogether(scaleX, scaleY, rotation)
            duration = 320
            interpolator = DecelerateInterpolator()
            start()
        }
    }

    private fun showPlayerSettings() {
        val p = players[activePlayer]
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), 0, dp(20), 0) }
        val name = EditText(this).apply { setText(p.name); setTextColor(this@MainActivity.text); hint = "Nome do jogador"; setHintTextColor(muted) }
        box.addView(name)
        val anti = Switch(this).apply { text = "Anti-zikamento nesta rodada"; isChecked = p.antiJinx; setTextColor(this@MainActivity.text) }
        box.addView(anti)
        val percent = Switch(this).apply { text = "Modo de porcentagem"; isChecked = p.percentageMode; setTextColor(this@MainActivity.text) }
        box.addView(percent)
        val luck = numberField(if (p.percentageMode) "Sorte (%)" else "Sorte em pontos", if (p.percentageMode) p.luckPercent else p.luck)
        val curse = numberField(if (p.percentageMode) "Azar (%)" else "Azar em pontos", if (p.percentageMode) p.cursePercent else p.curse)
        box.addView(luck); box.addView(curse)
        percent.setOnCheckedChangeListener { _, checked ->
            luck.hint = if (checked) "Sorte (%)" else "Sorte (pontos)"
            curse.hint = if (checked) "Azar (%)" else "Azar (pontos)"
        }
        AlertDialog.Builder(this)
            .setTitle("${p.name} • destino")
            .setView(box)
            .setNegativeButton("CANCELAR", null)
            .setPositiveButton("SALVAR") { _, _ ->
                p.name = name.text.toString().trim().ifEmpty { "Player ${activePlayer + 1}" }
                p.antiJinx = anti.isChecked
                p.percentageMode = percent.isChecked
                val a = luck.text.toString().toIntOrNull() ?: 0
                val c = curse.text.toString().toIntOrNull() ?: 0
                if (p.percentageMode) { p.luckPercent = a.coerceIn(0, 100); p.cursePercent = c.coerceIn(0, 100) }
                else { p.luck = a.coerceIn(0, 100); p.curse = c.coerceIn(0, 100) }
                rebuildPlayerSection()
            }.show()
    }

    private fun numberField(hintText: String, value: Int) = EditText(this).apply {
        hint = hintText; setHintTextColor(muted); setTextColor(this@MainActivity.text); setText(value.toString()); inputType = 2
    }

    private fun refreshCards() {
        val root = findViewById<ViewGroup>(android.R.id.content) ?: return
        val list = findTagged(root, "playersList") ?: return
        renderPlayerCards(list as LinearLayout)
    }

    private fun renderPlayerCards(list: LinearLayout) {
        list.removeAllViews()
        players.forEachIndexed { index, p ->
            val c = card()
            c.addView(label("${p.name}${if (index == activePlayer) "  •  ATIVO" else ""}", 16f, text, true))
            c.addView(label(p.lastResult, 13f, muted, false), marginParams(0, 5, 0, 0))
            c.addView(label("${if (p.antiJinx) "Anti-zikamento ON" else "Anti-zikamento OFF"}  •  ${if (p.percentageMode) "${p.luckPercent}% sorte / ${p.cursePercent}% azar" else "+${p.luck} sorte / -${p.curse} azar"}", 11f, muted, false), marginParams(0, 4, 0, 0))
            list.addView(c, marginParams(0, 0, 0, 8))
        }
    }

    private fun newRound() {
        players.forEach { it.antiJinx = false; it.lastResult = "—" }
        resultText.text = "Nova rodada"
        refreshCards()
        Toast.makeText(this, "Rodada reiniciada — anti-zikamento desligado", Toast.LENGTH_SHORT).show()
    }

    private fun findTagged(parent: ViewGroup, wanted: String): View? {
        if (parent.tag == wanted) return parent
        for (i in 0 until parent.childCount) {
            val child = parent.getChildAt(i)
            if (child is ViewGroup) findTagged(child, wanted)?.let { return it }
        }
        return null
    }

    private fun card() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(16), dp(14), dp(16), dp(14))
        setBackgroundColor(panel)
    }

    private fun label(value: String, size: Float, color: Int, bold: Boolean) = TextView(this).apply {
        text = value; textSize = size; setTextColor(color)
        if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
    }

    private fun button(value: String, fg: Int, bgColor: Int, action: (View) -> Unit) = Button(this).apply {
        text = value; textSize = 12f; setTextColor(fg); setBackgroundColor(bgColor); isAllCaps = false
        setOnClickListener(action)
    }

    private fun marginParams(l: Int, t: Int, r: Int, b: Int) = LinearLayout.LayoutParams(-1, -2).apply {
        setMargins(dp(l), dp(t), dp(r), dp(b))
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
