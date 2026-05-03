package com.invoke.android.overlay

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.content.ContextCompat
import com.invoke.android.R

/**
 * Floating bubble overlay for INVOKE.
 * States: IDLE (purple) → RECORDING (red pulse) → PROCESSING (gray spinner) → DONE (green flash)
 * Draggable with edge-snap. Shows feedback pill below.
 */
class InvokeBubble(private val context: Context) {

    companion object {
        private const val TAG = "INVOKE"
        private const val BUBBLE_DP = 56
        private const val PULSE_EXTRA = 0.4f
        private const val TAP_THRESHOLD_DP = 10
        private const val FEEDBACK_OFFSET_DP = 68

        // Colors
        private const val COLOR_IDLE = 0xFF8B5CF6.toInt()      // Purple
        private const val COLOR_RECORDING = 0xFFEF4444.toInt()  // Red
        private const val COLOR_PROCESSING = 0xFF6B7280.toInt() // Gray
        private const val COLOR_DONE = 0xFF22C55E.toInt()       // Green
        private const val COLOR_FEEDBACK_BG = 0xEE1F2937.toInt()// Dark pill
    }

    enum class State { IDLE, RECORDING, PROCESSING, DONE }

    private val dp get() = context.resources.displayMetrics.density
    private val screenW get() = context.resources.displayMetrics.widthPixels
    private val screenH get() = context.resources.displayMetrics.heightPixels

    private var bubbleView: View? = null
    private var feedbackView: android.widget.TextView? = null
    private var windowManager: WindowManager? = null
    private var bubbleParams: WindowManager.LayoutParams? = null
    private var feedbackParams: WindowManager.LayoutParams? = null

    private var state = State.IDLE
    private var onTapListener: (() -> Unit)? = null
    private var pulseAnimator: ValueAnimator? = null

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private var currentColor = COLOR_IDLE
    private var pulseRadius = 0f

    fun attach(wm: WindowManager) {
        windowManager = wm
        val bubbleSize = (BUBBLE_DP * dp).toInt()

        // Bubble view with custom draw
        bubbleView = object : View(context) {
            override fun onDraw(canvas: Canvas) {
                super.onDraw(canvas)
                val cx = width / 2f
                val cy = height / 2f
                val radius = (BUBBLE_DP * dp) / 2f

                // Pulse ring
                if (state == State.RECORDING && pulseRadius > 0) {
                    val pulsePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = 0x40EF4444.toInt()
                        style = Paint.Style.FILL
                    }
                    canvas.drawCircle(cx, cy, pulseRadius, pulsePaint)
                }

                // Main circle
                paint.color = currentColor
                canvas.drawCircle(cx, cy, radius, paint)

                // Mic icon
                val mic = ContextCompat.getDrawable(context, R.drawable.ic_mic)
                mic?.setTint(0xFFFFFFFF.toInt())
                val iconSize = (24 * dp).toInt()
                val left = (cx - iconSize / 2).toInt()
                val top = (cy - iconSize / 2).toInt()
                mic?.setBounds(left, top, left + iconSize, top + iconSize)
                mic?.draw(canvas)
            }

            override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
                val size = (BUBBLE_DP * dp * (1 + PULSE_EXTRA)).toInt()
                setMeasuredDimension(size, size)
            }
        }

        val extraSpace = (BUBBLE_DP * dp * PULSE_EXTRA / 2).toInt()
        bubbleParams = WindowManager.LayoutParams(
            (BUBBLE_DP * dp * (1 + PULSE_EXTRA)).toInt(),
            (BUBBLE_DP * dp * (1 + PULSE_EXTRA)).toInt(),
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = screenW - (BUBBLE_DP * dp).toInt() - (8 * dp).toInt() - extraSpace
            y = screenH / 2 - bubbleSize / 2
        }

        setupTouch()

        // Feedback pill
        feedbackView = android.widget.TextView(context).apply {
            textSize = 13f
            setTextColor(0xFFFFFFFF.toInt())
            setPadding(
                (12 * dp).toInt(), (8 * dp).toInt(),
                (12 * dp).toInt(), (8 * dp).toInt()
            )
            val bg = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadius = 16 * dp
                setColor(COLOR_FEEDBACK_BG)
            }
            background = bg
            alpha = 0f
            visibility = View.GONE
        }

        feedbackParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }

        positionFeedback()

        wm.addView(bubbleView, bubbleParams)
        wm.addView(feedbackView, feedbackParams)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupTouch() {
        var startX = 0; var startY = 0
        var touchX = 0f; var touchY = 0f

        bubbleView?.setOnTouchListener { v, ev ->
            val params = bubbleParams ?: return@setOnTouchListener false
            val wm = windowManager ?: return@setOnTouchListener false

            when (ev.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = params.x; startY = params.y
                    touchX = ev.rawX; touchY = ev.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = startX + (ev.rawX - touchX).toInt()
                    params.y = startY + (ev.rawY - touchY).toInt()
                    wm.updateViewLayout(v, params)
                    positionFeedback()
                    feedbackParams?.let { wm.updateViewLayout(feedbackView, it) }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val moved = Math.abs(ev.rawX - touchX) + Math.abs(ev.rawY - touchY)
                    if (moved < TAP_THRESHOLD_DP * dp) {
                        onTapListener?.invoke()
                    } else {
                        // Edge snap
                        val bubbleWidth = (BUBBLE_DP * dp * (1 + PULSE_EXTRA)).toInt()
                        val margin = (8 * dp).toInt()
                        params.x = if (params.x + bubbleWidth / 2 > screenW / 2)
                            screenW - bubbleWidth - margin else margin
                        wm.updateViewLayout(v, params)
                        positionFeedback()
                        feedbackParams?.let { wm.updateViewLayout(feedbackView, it) }
                    }
                    true
                }
                else -> false
            }
        }
    }

    fun setOnTapListener(listener: () -> Unit) { onTapListener = listener }

    fun setState(newState: State) {
        state = newState
        currentColor = when (newState) {
            State.IDLE -> COLOR_IDLE
            State.RECORDING -> COLOR_RECORDING
            State.PROCESSING -> COLOR_PROCESSING
            State.DONE -> COLOR_DONE
        }
        bubbleView?.invalidate()

        if (newState == State.RECORDING) startPulse() else stopPulse()

        // Green done flash → back to idle after 1s
        if (newState == State.DONE) {
            bubbleView?.postDelayed({
                state = State.IDLE
                currentColor = COLOR_IDLE
                bubbleView?.invalidate()
            }, 1000)
        }
    }

    fun showFeedback(text: String, durationMs: Long = 2500) {
        val view = feedbackView ?: return
        view.post {
            view.text = text
            view.visibility = View.VISIBLE
            view.alpha = 0f
            view.animate().alpha(1f).setDuration(150).start()
            view.removeCallbacks(hideFeedback)
            view.postDelayed(hideFeedback, durationMs)
        }
    }

    private val hideFeedback = Runnable {
        feedbackView?.animate()?.alpha(0f)?.setDuration(200)?.withEndAction {
            feedbackView?.visibility = View.GONE
        }?.start()
    }

    private fun positionFeedback() {
        val bp = bubbleParams ?: return
        val fp = feedbackParams ?: return
        val margin = (8 * dp).toInt()
        fp.x = maxOf(margin, bp.x - (FEEDBACK_OFFSET_DP * dp).toInt())
        fp.y = maxOf(margin, bp.y - margin)
    }

    private fun startPulse() {
        pulseAnimator?.cancel()
        val baseRadius = (BUBBLE_DP * dp) / 2f
        val maxRadius = baseRadius * (1 + PULSE_EXTRA)
        pulseAnimator = ValueAnimator.ofFloat(baseRadius, maxRadius).apply {
            duration = 700
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener {
                pulseRadius = it.animatedValue as Float
                bubbleView?.invalidate()
            }
            start()
        }
    }

    private fun stopPulse() {
        pulseAnimator?.cancel()
        pulseAnimator = null
        pulseRadius = 0f
        bubbleView?.invalidate()
    }

    fun detach() {
        stopPulse()
        val wm = windowManager ?: return
        bubbleView?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        feedbackView?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        bubbleView = null
        feedbackView = null
    }
}
