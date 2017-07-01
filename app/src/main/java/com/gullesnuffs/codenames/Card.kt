package com.gullesnuffs.codenames

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.AutoCompleteTextView
import android.widget.LinearLayout
import android.graphics.Color.parseColor


class Card : AutoCompleteTextView {
    private var mState: WordType? = null

    var onChanged: ((String) -> Unit)? = null

    companion object {
        val stateMapping = mapOf(
                WordType.Civilian to R.attr.civilian,
                WordType.Blue to R.attr.blue,
                WordType.Red to R.attr.red,
                WordType.Assassin to R.attr.assassin
        )
    }

    var state: WordType?
        get() = mState
        set(value) {
            mState = value
            refreshDrawableState()
        }

    var editable = true

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (editable) return super.onTouchEvent(event)
        else return false
    }

    init {
        val editFilters = filters
        val newFilters = Array<InputFilter>(editFilters.size + 1) {
            i ->
            if (i < editFilters.size) {
                editFilters[i]
            } else {
                InputFilter.AllCaps()
            }
        }
        filters = newFilters

        addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?,
                                           start: Int, count: Int, after: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
            }

            override fun onTextChanged(s: CharSequence?,
                                       start: Int, before: Int, count: Int) {
                onChanged?.invoke(s.toString())
            }
        })
    }

    fun colorLerp(a: Int, b: Int, fraction: Float): Int {
        val hsv1 = FloatArray(3)
        val hsv2 = FloatArray(3)
        Color.colorToHSV(a, hsv1)
        Color.colorToHSV(b, hsv2)
        for (i in 0..2) hsv1[i] = hsv1[i] * (1 - fraction) + hsv2[i] * fraction
        val alpha = Color.alpha(a) * (1 - fraction) + Color.alpha(b) * fraction
        return Color.HSVToColor(alpha.toInt(), hsv1)
    }

    fun colorMultiply(color: Int, multiplier: Float): Int {
        return Color.argb(Color.alpha(color), (Color.red(color) * multiplier).toInt(), (Color.green(color) * multiplier).toInt(), (Color.blue(color) * multiplier).toInt())
    }

    val paint = Paint()
    override fun onDraw(canvas: Canvas) {
        val r = canvas.clipBounds
        val rf = RectF(r.left.toFloat(), r.top.toFloat(), r.right.toFloat(), r.bottom.toFloat())

        val color = backgroundTintList.getColorForState(drawableState, android.R.color.transparent)
        paint.color = colorMultiply(color, 0.6f)
        paint.style = Paint.Style.FILL
        var radius = 10f
        canvas.drawRoundRect(rf, radius, radius, paint)
        val inset = 5f
        rf.inset(inset, inset)
        radius -= inset * 0.5f
        paint.color = color
        canvas.drawRoundRect(rf, radius, radius, paint)
        super.onDraw(canvas)
    }

    constructor(ctx: Context?) : super(ctx) {}
    constructor(ctx: Context?, attrs: AttributeSet?) : super(ctx, attrs) {}
    constructor(ctx: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(ctx, attrs, defStyleAttr) {}

    override fun onCreateDrawableState(extraSpace: Int): IntArray {
        // Note: This will be called before state has been initialized
        val state = state
        if (state != null) {
            val values = super.onCreateDrawableState(extraSpace + 1)
            View.mergeDrawableStates(values, intArrayOf(stateMapping[state]!!))
            return values
        } else {
            return super.onCreateDrawableState(extraSpace)
        }
    }
}