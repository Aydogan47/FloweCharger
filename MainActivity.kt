package com.example.flowercharger

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.os.Bundle
import android.view.View
import android.animation.ValueAnimator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var flowerView: FlowerView
    private lateinit var statusText: TextView
    private var isCharging = false

    private val chargingReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_POWER_CONNECTED -> {
                    isCharging = true
                    flowerView.bloom()
                    statusText.text = "🌱 Şarj oluyor... Çiçek açıyor!"
                    animateStatusText()
                }
                Intent.ACTION_POWER_DISCONNECTED -> {
                    isCharging = false
                    flowerView.wither()
                    statusText.text = "🍂 Şarj bitti... Çiçek soluyor."
                    animateStatusText()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ana layout
        val rootLayout = FrameLayout(this)
        rootLayout.setBackgroundColor(Color.parseColor("#0D1B0E"))

        // Çiçek view
        flowerView = FlowerView(this)
        val flowerParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
        rootLayout.addView(flowerView, flowerParams)

        // Durum yazısı
        statusText = TextView(this)
        statusText.text = "Telefonu şarja takın..."
        statusText.textSize = 18f
        statusText.setTextColor(Color.parseColor("#A8D5A2"))
        statusText.textAlignment = View.TEXT_ALIGNMENT_CENTER
        statusText.setPadding(40, 0, 40, 100)
        val textParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = android.view.Gravity.BOTTOM or android.view.Gravity.CENTER_HORIZONTAL
        }
        rootLayout.addView(statusText, textParams)

        setContentView(rootLayout)

        // Şarj alıcısını kaydet
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }
        registerReceiver(chargingReceiver, filter)

        // Başlangıç şarj durumunu kontrol et
        checkInitialChargingState()
    }

    private fun checkInitialChargingState() {
        val batteryIntent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val plugged = batteryIntent?.getIntExtra(android.os.BatteryManager.EXTRA_PLUGGED, -1) ?: -1
        isCharging = plugged == android.os.BatteryManager.BATTERY_PLUGGED_AC ||
                plugged == android.os.BatteryManager.BATTERY_PLUGGED_USB ||
                plugged == android.os.BatteryManager.BATTERY_PLUGGED_WIRELESS

        if (isCharging) {
            flowerView.post {
                flowerView.bloom()
                statusText.text = "🌱 Şarj oluyor... Çiçek açıyor!"
            }
        } else {
            flowerView.post {
                flowerView.setWithered()
                statusText.text = "Telefonu şarja takın..."
            }
        }
    }

    private fun animateStatusText() {
        val scaleX = ObjectAnimator.ofFloat(statusText, "scaleX", 0.8f, 1.1f, 1.0f)
        val scaleY = ObjectAnimator.ofFloat(statusText, "scaleY", 0.8f, 1.1f, 1.0f)
        val animSet = AnimatorSet()
        animSet.playTogether(scaleX, scaleY)
        animSet.duration = 500
        animSet.interpolator = OvershootInterpolator()
        animSet.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(chargingReceiver)
    }
}

class FlowerView(context: Context) : View(context) {

    // 0.0 = tamamen solmuş, 1.0 = tamamen açmış
    private var bloomProgress: Float = 0f

    private val stemPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#2D5A1B")
        strokeWidth = 18f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val leafPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#3A7D2C")
        style = Paint.Style.FILL
    }

    private val petalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E84393")
        style = Paint.Style.FILL
    }

    private val centerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#F5C518")
        style = Paint.Style.FILL
    }

    private val potPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#C1440E")
        style = Paint.Style.FILL
    }

    private val potRimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E05A20")
        style = Paint.Style.FILL
    }

    private val soilPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#5C3A1E")
        style = Paint.Style.FILL
    }

    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#60D4A017")
        style = Paint.Style.FILL
    }

    private var bloomAnimator: ValueAnimator? = null
    private var swayAnimator: ValueAnimator? = null
    private var swayAngle: Float = 0f

    init {
        startSwayAnimation()
    }

    fun bloom() {
        bloomAnimator?.cancel()
        bloomAnimator = ValueAnimator.ofFloat(bloomProgress, 1f).apply {
            duration = 2500
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener {
                bloomProgress = it.animatedValue as Float
                // Renk geçişi: soluk sarı -> canlı pembe
                val r = lerp(80f, 232f, bloomProgress).toInt()
                val g = lerp(80f, 67f, bloomProgress).toInt()
                val b = lerp(80f, 147f, bloomProgress).toInt()
                petalPaint.color = Color.rgb(r, g, b)

                val sg = lerp(40f, 125f, bloomProgress).toInt()
                stemPaint.color = Color.rgb(30, sg, 27)
                leafPaint.color = Color.rgb(30, lerp(50f, 125f, bloomProgress).toInt(), 44)

                invalidate()
            }
            start()
        }
    }

    fun wither() {
        bloomAnimator?.cancel()
        bloomAnimator = ValueAnimator.ofFloat(bloomProgress, 0f).apply {
            duration = 2000
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener {
                bloomProgress = it.animatedValue as Float
                val r = lerp(80f, 232f, bloomProgress).toInt()
                val g = lerp(80f, 67f, bloomProgress).toInt()
                val b = lerp(80f, 147f, bloomProgress).toInt()
                petalPaint.color = Color.rgb(r, g, b)

                val sg = lerp(40f, 125f, bloomProgress).toInt()
                stemPaint.color = Color.rgb(30, sg, 27)
                leafPaint.color = Color.rgb(30, lerp(50f, 125f, bloomProgress).toInt(), 44)

                invalidate()
            }
            start()
        }
    }

    fun setWithered() {
        bloomProgress = 0f
        petalPaint.color = Color.parseColor("#504343")
        stemPaint.color = Color.parseColor("#1E281B")
        leafPaint.color = Color.parseColor("#1E322C")
        invalidate()
    }

    private fun startSwayAnimation() {
        swayAnimator = ValueAnimator.ofFloat(-8f, 8f).apply {
            duration = 3000
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener {
                swayAngle = (it.animatedValue as Float) * bloomProgress
                invalidate()
            }
            start()
        }
    }

    private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t.coerceIn(0f, 1f)

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val cx = width / 2f
        val cy = height / 2f
        val scale = minOf(width, height) / 600f

        // Saksı
        drawPot(canvas, cx, cy + 180 * scale, scale)

        // Glow efekti (şarj olunca)
        if (bloomProgress > 0.3f) {
            glowPaint.alpha = (120 * bloomProgress).toInt()
            canvas.drawCircle(cx, cy - 60 * scale, 110 * scale * bloomProgress, glowPaint)
        }

        // Çiçeği sway ile çiz
        canvas.save()
        canvas.rotate(swayAngle, cx, cy + 150 * scale)
        drawStem(canvas, cx, cy, scale)
        drawLeaves(canvas, cx, cy, scale)
        if (bloomProgress > 0.05f) {
            drawFlower(canvas, cx, cy - 80 * scale, scale)
        }
        canvas.restore()
    }

    private fun drawPot(canvas: Canvas, cx: Float, cy: Float, scale: Float) {
        // Toprak
        val soilRect = RectF(cx - 75 * scale, cy - 25 * scale, cx + 75 * scale, cy + 5 * scale)
        canvas.drawRoundRect(soilRect, 8f * scale, 8f * scale, soilPaint)

        // Saksı gövdesi (trapez şekli)
        val potPath = Path().apply {
            moveTo(cx - 60 * scale, cy - 20 * scale)
            lineTo(cx - 80 * scale, cy + 120 * scale)
            lineTo(cx + 80 * scale, cy + 120 * scale)
            lineTo(cx + 60 * scale, cy - 20 * scale)
            close()
        }
        canvas.drawPath(potPath, potPaint)

        // Saksı kenarı (üst rim)
        val rimRect = RectF(cx - 78 * scale, cy - 35 * scale, cx + 78 * scale, cy - 10 * scale)
        canvas.drawRoundRect(rimRect, 10f * scale, 10f * scale, potRimPaint)

        // Saksı alt yuvarlak
        val bottomRect = RectF(cx - 78 * scale, cy + 110 * scale, cx + 78 * scale, cy + 130 * scale)
        canvas.drawRoundRect(bottomRect, 12f * scale, 12f * scale, potPaint)
    }

    private fun drawStem(canvas: Canvas, cx: Float, cy: Float, scale: Float) {
        val stemHeight = lerp(60f, 220f, bloomProgress)
        stemPaint.strokeWidth = lerp(8f, 18f, bloomProgress) * scale
        canvas.drawLine(cx, cy + 150 * scale, cx, cy + (150 - stemHeight) * scale, stemPaint)
    }

    private fun drawLeaves(canvas: Canvas, cx: Float, cy: Float, scale: Float) {
        if (bloomProgress < 0.2f) return
        val leafAlpha = ((bloomProgress - 0.2f) / 0.8f).coerceIn(0f, 1f)
        leafPaint.alpha = (255 * leafAlpha).toInt()

        val leafSize = lerp(0f, 55f, bloomProgress) * scale

        // Sol yaprak
        val leftLeaf = Path().apply {
            moveTo(cx, cy + 80 * scale)
            quadTo(cx - leafSize * 1.8f, cy + 50 * scale, cx - leafSize * 1.2f, cy + 30 * scale)
            quadTo(cx - leafSize * 0.5f, cy + 60 * scale, cx, cy + 80 * scale)
        }
        canvas.save()
        canvas.rotate(-20f * bloomProgress, cx, cy + 80 * scale)
        canvas.drawPath(leftLeaf, leafPaint)
        canvas.restore()

        // Sağ yaprak
        val rightLeaf = Path().apply {
            moveTo(cx, cy + 50 * scale)
            quadTo(cx + leafSize * 1.8f, cy + 20 * scale, cx + leafSize * 1.2f, cy + 0 * scale)
            quadTo(cx + leafSize * 0.5f, cy + 30 * scale, cx, cy + 50 * scale)
        }
        canvas.save()
        canvas.rotate(20f * bloomProgress, cx, cy + 50 * scale)
        canvas.drawPath(rightLeaf, leafPaint)
        canvas.restore()
    }

    private fun drawFlower(canvas: Canvas, cx: Float, flowerY: Float, scale: Float) {
        val petalCount = 8
        val petalLength = lerp(10f, 70f, bloomProgress) * scale
        val petalWidth = lerp(5f, 32f, bloomProgress) * scale

        petalPaint.alpha = (255 * bloomProgress).toInt()

        for (i in 0 until petalCount) {
            val angle = (i * 360f / petalCount)
            canvas.save()
            canvas.rotate(angle, cx, flowerY)

            val petalPath = Path().apply {
                moveTo(cx, flowerY)
                cubicTo(
                    cx - petalWidth, flowerY - petalLength * 0.3f,
                    cx - petalWidth * 0.5f, flowerY - petalLength,
                    cx, flowerY - petalLength
                )
                cubicTo(
                    cx + petalWidth * 0.5f, flowerY - petalLength,
                    cx + petalWidth, flowerY - petalLength * 0.3f,
                    cx, flowerY
                )
            }
            canvas.drawPath(petalPath, petalPaint)
            canvas.restore()
        }

        // Merkez daire
        val centerRadius = lerp(5f, 28f, bloomProgress) * scale
        centerPaint.alpha = (255 * bloomProgress).toInt()
        canvas.drawCircle(cx, flowerY, centerRadius, centerPaint)

        // Merkez nokta detayları
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#C8860A")
            alpha = (200 * bloomProgress).toInt()
        }
        canvas.drawCircle(cx, flowerY, centerRadius * 0.5f, dotPaint)
    }
}
