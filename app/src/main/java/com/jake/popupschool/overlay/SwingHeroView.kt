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
import kotlin.math.hypot

/**
 * A small original illustration (not Marvel artwork) of a masked hero that hangs under
 * gravity and, on tap, shoots a web toward the tapped point and launches that way.
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
    private var velocityX = 0f
    private var velocityY = 0f

    private var swinging = false
    private var swingAnchorX = 0f
    private var swingAnchorY = 0f
    private var pointerDown = false

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
            if (!pointerDown) swinging = false
        } else if (posX + radius > w) {
            posX = w - radius
            velocityX = -abs(velocityX)
            if (!pointerDown) swinging = false
        }

        if (posY - radius < 0f) {
            posY = radius
            velocityY = abs(velocityY)
            if (!pointerDown) swinging = false
        } else if (posY + radius > h) {
            posY = h - radius
            velocityY = -abs(velocityY) * FLOOR_DAMPING
            if (!pointerDown) swinging = false
        }

        if (!pointerDown && swinging && hypot(posX - swingAnchorX, posY - swingAnchorY) < radius * 1.5f) {
            swinging = false
        }

        invalidate()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                pointerDown = true
                shootWebTo(event.x, event.y)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (pointerDown) {
                    // Web stays attached to the finger while held down; the hero keeps
                    // its existing momentum, only the drawn anchor point follows the touch.
                    swingAnchorX = event.x
                    swingAnchorY = event.y
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                pointerDown = false
                swinging = false
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    /** Launches the hero toward [targetX], [targetY], drawing a web line to that point until it arrives or bounces. */
    private fun shootWebTo(targetX: Float, targetY: Float) {
        val dx = targetX - posX
        val dy = targetY - posY
        val distance = hypot(dx, dy).coerceAtLeast(1f)
        velocityX = dx / distance * WEB_LAUNCH_SPEED
        velocityY = dy / distance * WEB_LAUNCH_SPEED
        swinging = true
        swingAnchorX = targetX
        swingAnchorY = targetY
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (swinging) {
            canvas.drawLine(swingAnchorX, swingAnchorY, posX, posY, webPaint)
        }
        canvas.drawCircle(posX, posY, radius, bodyPaint)
        canvas.drawRect(posX - radius, posY - radius * 0.35f, posX + radius, posY, maskPaint)
        canvas.drawLine(posX - radius, posY - radius * 0.35f, posX + radius, posY - radius * 0.35f, webPaint)
    }

    private companion object {
        const val GRAVITY = 0.6f
        const val MAX_FALL_SPEED = 18f
        const val FLOOR_DAMPING = 0.6f
        const val WEB_LAUNCH_SPEED = 15f
    }
}
