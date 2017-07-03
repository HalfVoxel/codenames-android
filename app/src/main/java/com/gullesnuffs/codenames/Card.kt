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
import android.widget.TextView


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

    var borderOverrideColor: Int = Color.argb(0, 255, 255, 255)
    var surfaceOverrideColor: Int = Color.argb(0, 255, 255, 255)
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
        /*val hsv1 = FloatArray(3)
        val hsv2 = FloatArray(3)
        Color.colorToHSV(a, hsv1)
        Color.colorToHSV(b, hsv2)
        for (i in 0..2) hsv1[i] = hsv1[i] * (1 - fraction) + hsv2[i] * fraction
        val alpha = Color.alpha(a) * (1 - fraction) + Color.alpha(b) * fraction
        return Color.HSVToColor(alpha.toInt(), hsv1)*/

        val ca = Color.alpha(a)/255f * (1f - Color.alpha(b)/255f*fraction)
        val cb = Color.alpha(b)/255f * fraction
        val alpha = Color.alpha(a) * ca + Color.alpha(b) * cb
        val red = Color.red(a) * ca + Color.red(b) * cb
        val green = Color.green(a) * ca + Color.green(b) * cb
        val blue = Color.blue(a) * ca + Color.blue(b) * cb
        return Color.argb(alpha.toInt(), red.toInt(), green.toInt(), blue.toInt())

        //val a2 = colorScale(a, )
        //val b2 = colorScale(b, )
        //return colorAdd(a2, b2)
        /*val denominator = Color.alpha(a)/255f + fraction*(Color.alpha(b)/255f)*(1f - Color.alpha(a)/255f)
        if (denominator == 0f) return 0
        val a2 = colorScale(a, Color.alpha(a)/255f / denominator)
        val b2 = colorScale(b, fraction*Color.alpha(b)/255f*(1 - Color.alpha(a)/255f) / denominator)
        val res = a2 + b2
        val alpha = Color.alpha(a)/255f + fraction*(Color.alpha(b)/255f) * (1f - Color.alpha(a)/255f)
        return Color.argb((alpha*255).toInt(), Color.red(res), Color.green(res), Color.blue(res))*/

        /*val alpha = Color.alpha(a) * (1 - fraction) + Color.alpha(b) * fraction
        val red = Color.red(a) * (1 - fraction) + Color.red(b) * fraction
        val green = Color.green(a) * (1 - fraction) + Color.green(b) * fraction
        val blue = Color.blue(a) * (1 - fraction) + Color.blue(b) * fraction
        return Color.argb(alpha.toInt(), red.toInt(), green.toInt(), blue.toInt())*/
    }

    fun colorScale(color: Int, multiplier: Float): Int {
        return Color.argb(Color.alpha(color), (Color.red(color) * multiplier).toInt(), (Color.green(color) * multiplier).toInt(), (Color.blue(color) * multiplier).toInt())
    }

    fun colorAdd(a: Int, b : Int): Int {
        return Color.argb(Color.alpha(a) + Color.alpha(b), Color.red(a) + Color.red(b), Color.green(a) + Color.green(b), Color.blue(a) + Color.blue(b))
    }

    fun colorMultiply(a: Int, b : Int): Int {
        return Color.argb(Color.alpha(a) * Color.alpha(b) / 255, Color.red(a) * Color.red(b) / 255, Color.green(a) * Color.green(b) / 255, Color.blue(a) * Color.blue(b) / 255)
    }

    fun brighten(color: Int, amount: Float): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        hsv[2] = hsv[2] + (1 - hsv[2]) * amount
        hsv[1] *= 1 - amount
        return Color.HSVToColor(Color.alpha(color), hsv)
    }

    fun brightness(color: Int): Float {
        return (Color.red(color) + Color.green(color) + Color.blue(color)) / (255f*3)
    }

    val paint = Paint()
    override fun onDraw(canvas: Canvas) {
        val r = canvas.clipBounds
        val rf = RectF(r.left.toFloat(), r.top.toFloat(), r.right.toFloat(), r.bottom.toFloat())

        val color = backgroundTintList.getColorForState(drawableState, android.R.color.transparent)
        paint.color = colorScale(color, 0.6f)
        paint.color = colorLerp(paint.color, borderOverrideColor, Color.alpha(borderOverrideColor)/255f)

        paint.style = Paint.Style.FILL
        var radius = 10f
        canvas.drawRoundRect(rf, radius, radius, paint)
        val inset = 5f
        rf.inset(inset, inset)
        radius -= inset * 0.5f
        paint.color = colorLerp(color, surfaceOverrideColor, Color.alpha(surfaceOverrideColor)/255f)

        // setTextColor(if (brightness(paint.color) > 0.8f) Color.BLACK else Color.WHITE)

        setShadowLayer(10f, 0f, 0f, Color.argb((200 * brightness(paint.color)).toInt(), 0, 0, 0))
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