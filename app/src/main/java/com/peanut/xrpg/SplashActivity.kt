package com.peanut.xrpg

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {
    private val bg = Color.rgb(10, 11, 15)
    private val text = Color.rgb(245, 247, 250)
    private val muted = Color.rgb(154, 162, 176)
    private val accent = Color.rgb(121, 231, 181)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(28), dp(24), dp(28), dp(28))
            setBackgroundColor(bg)
        }

        val logo = ImageView(this).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            contentDescription = "Logo XRpg"
        }
        try {
            logo.setImageResource(R.drawable.xrpg_logo)
        } catch (_: Throwable) {
            logo.visibility = View.GONE
        }
        root.addView(logo, LinearLayout.LayoutParams(-1, dp(230)))

        root.addView(TextView(this).apply {
            text = "XRpg"
            textSize = 42f
            setTextColor(text)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(-1, dp(58)))

        root.addView(TextView(this).apply {
            text = "Dados rápidos. Regras suas."
            textSize = 15f
            setTextColor(muted)
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(-1, dp(42)))

        val play = Button(this).apply {
            text = "Jogar"
            textSize = 17f
            setTextColor(bg)
            background = rounded(accent, 22)
            isAllCaps = false
            setOnClickListener {
                isEnabled = false
                startActivity(Intent(this@SplashActivity, MainActivity::class.java))
            }
        }
        root.addView(play, LinearLayout.LayoutParams(-1, dp(58)).apply {
            topMargin = dp(22)
        })

        root.addView(TextView(this).apply {
            text = "@Peanut & Cyberleek"
            textSize = 12f
            setTextColor(muted)
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(-1, dp(44)).apply {
            topMargin = dp(24)
        })

        setContentView(root)
    }

    private fun rounded(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
