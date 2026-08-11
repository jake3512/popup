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

/**
 * A small original illustration (not Marvel artwork) of a masked hero that bounces
 * around under gravity and, on tap, launches into a web-swing arc. Labeled
 * "스파이더맨" in the UI per the user's explicit request.
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
    private var velocityX = 4f
    private var velocityY = 0f

    private var swinging = false
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

        velocityY = (velocityY + GRAVITY).coerceAtMost(MAX_FALL_SPEED)
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
            if (swinging) {
                swinging = false
            }
            velocityY = -abs(velocityY) * FLOOR_DAMPING
            if (abs(velocityY) < 3f) {
                // keep a small hop alive so it doesn't come to a dead stop
                velocityY = -6f
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
        swingAnchorX = posX
        val towardRight = width <= 0 || posX < width / 2f
        velocityX = if (towardRight) SWING_SPEED else -SWING_SPEED
        velocityY = -SWING_LAUNCH_SPEED
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (swinging) {
            canvas.drawLine(swingAnchorX, 0f, posX, posY, webPaint)
        }
        canvas.drawCircle(posX, posY, radius, bodyPaint)
        canvas.drawRect(posX - radius, posY - radius * 0.35f, posX + radius, posY, maskPaint)
        canvas.drawLine(posX - radius, posY - radius * 0.35f, posX + radius, posY - radius * 0.35f, webPaint)
    }

    private companion object {
        const val GRAVITY = 0.6f
        const val MAX_FALL_SPEED = 18f
        const val FLOOR_DAMPING = 0.6f
        const val SWING_SPEED = 9f
        const val SWING_LAUNCH_SPEED = 15f
    }
}
