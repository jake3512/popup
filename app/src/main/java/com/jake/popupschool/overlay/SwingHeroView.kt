package com.jake.popupschool.overlay

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.MotionEvent
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

/**
 * A small original illustration (not Marvel artwork) of a masked hero that bounces
 * back and forth inside its container and, on tap, swings across on a web line.
 * Labeled "스파이더맨" in the UI per the user's explicit request.
 */
class SwingHeroView(context: Context) : View(context) {

    private val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#D32F2F") }
    private val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#1A237E") }
    private val webPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#CCCCCC")
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    private val radius = 22f

    private var posX = 60f
    private var posY = 60f
    private var velocityX = 5f
    private var velocityY = 3f

    private var swinging = false
    private var swingProgress = 0f
    private var swingStartX = 0f
    private var swingStartY = 0f
    private var swingAnchorX = 0f

    private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 16
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener { step() }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        animator.start()
    }

    override fun onDetachedFromWindow() {
        animator.cancel()
        super.onDetachedFromWindow()
    }

    private fun step() {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        if (swinging) {
            swingProgress += 0.05f
            if (swingProgress >= 1f) {
                swinging = false
            } else {
                val angle = Math.PI * swingProgress
                val arcWidth = (swingAnchorX - swingStartX) * 2f
                posX = swingStartX + arcWidth * swingProgress
                posY = (swingStartY - sin(angle).toFloat() * 60f).coerceAtLeast(radius)
            }
        } else {
            posX += velocityX
            posY += velocityY
            if (posX - radius < 0f) {
                posX = radius
                velocityX = abs(velocityX)
            } else if (posX + radius > w) {
                posX = w - radius
                velocityX = -abs(velocityX)
            }
            if (posY - radius < 0f) {
                posY = radius
                velocityY = abs(velocityY)
            } else if (posY + radius > h) {
                posY = h - radius
                velocityY = -abs(velocityY)
            }
        }
        invalidate()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN && !swinging) {
            val dx = event.x - posX
            val dy = event.y - posY
            val hitRadius = radius * 2.5f
            if (dx * dx + dy * dy <= hitRadius * hitRadius) {
                startSwing()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun startSwing() {
        swinging = true
        swingProgress = 0f
        swingStartX = posX
        swingStartY = posY
        val towardRight = if (width > 0) posX < width / 2f else Random.nextBoolean()
        swingAnchorX = if (towardRight) posX + 90f else posX - 90f
        velocityX = if (towardRight) abs(velocityX) else -abs(velocityX)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (swinging) {
            canvas.drawLine(swingStartX, 0f, posX, posY, webPaint)
        }
        canvas.drawCircle(posX, posY, radius, bodyPaint)
        canvas.drawRect(posX - radius, posY - radius * 0.35f, posX + radius, posY, maskPaint)
        canvas.drawLine(posX - radius, posY - radius * 0.35f, posX + radius, posY - radius * 0.35f, webPaint)
    }
}
