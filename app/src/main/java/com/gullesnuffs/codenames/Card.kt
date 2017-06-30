package com.gullesnuffs.codenames

import android.content.Context
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.AutoCompleteTextView
import android.widget.LinearLayout

class Card : AutoCompleteTextView {
    private var mState : WordType = WordType.Civilian

    var onChanged : ((String) -> Unit)? = null

    companion object {
        val stateMapping = mapOf(
                WordType.Civilian to R.attr.civilian,
                WordType.Blue to R.attr.blue,
                WordType.Red to R.attr.red,
                WordType.Assassin to R.attr.assassin
        )
    }

    var state : WordType
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
        mState = WordType.Civilian

        val editFilters = filters
        val newFilters = Array<InputFilter>(editFilters.size + 1) {
            i ->
            if(i < editFilters.size){
                editFilters[i]
            }
            else{
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

    constructor(ctx : Context?) : super(ctx) {}
    constructor(ctx : Context?, attrs: AttributeSet?) : super(ctx, attrs) {}
    constructor(ctx : Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(ctx, attrs, defStyleAttr) {}

    override fun onCreateDrawableState(extraSpace: Int): IntArray {
        val values = super.onCreateDrawableState(extraSpace + 1)

        // mState = if (Math.random() > 0.5) WordType.Civilian else WordType.Red

        // Note: This will be called before state has been initialized
        if (state != null) {
            View.mergeDrawableStates(values, intArrayOf(stateMapping[state]!!))
        }

        return values
    }
}