package com.gullesnuffs.codenames

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.widget.*
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation


class Board(var layout: TableLayout,
            val remainingLayout: ViewGroup,
            val autoCompleteAdapter: ArrayAdapter<String>,
            val gameState: Observable<GameState>,
            val context: Context) {
    var words = Array(5, { r -> Array(5, { c -> Word(r, c) })})
    var paintType = WordType.Red
    var width: Int = words[0].size
    var height: Int = words.size
    val cards = ArrayList<Card>()
    val redSpiesRemainingView = remainingLayout.findViewById(R.id.red_spies_remaining) as TextView
    val blueSpiesRemainingView = remainingLayout.findViewById(R.id.blue_spies_remaining) as TextView
    val civiliansRemainingView = remainingLayout.findViewById(R.id.civilians_remaining) as TextView
    var currentAnimationSet : AnimatorSet
    var displayScores = false
    var onClickCard : ((Word) -> Unit)? = null

    init {
        val inflater = LayoutInflater.from(context)

        layout.removeAllViews()
        currentAnimationSet = AnimatorSet()

        for (i in 0 until height) {
            val row: TableRow = TableRow(context)
            layout.addView(row)
            for (j in 0 until width) {
                inflater.inflate(R.layout.editable_word, row)
                val card = (row.getChildAt(j) as LinearLayout).getChildAt(0) as Card
                cards.add(card)
            }
        }

        cards.zip(words.flatten()).forEach { (card, word) ->
            val parent = card.parent as ViewGroup
            react({ _, new -> (card as TextView).text = new }, word.word)
            // Note that setSilently must be used because otherwise setting the word will
            // in turn trigger an update of the text field, which will trigger the word to be set again etc.
            card.onChanged = { s -> word.word.setSilently(s) }
            react({ _, new -> card.editable = new == GameState.EnterWords }, gameState)

            card.setAdapter<ArrayAdapter<String>>(autoCompleteAdapter)
            parent.setOnClickListener { _ ->
                onClickCard?.invoke(word)
            }

            react({
                if (gameState.value == GameState.GetClues) {
                    card.state = if (word.contacted.value) word.type.value else null
                } else {
                    card.state = word.type.value
                }
            }, word.type, word.contacted, gameState)

            // Make sure the card is up to date
            word.word.init()
            word.type.init()
        }

        react({
            val remainingCount = intArrayOf(0, 0, 0, 0)
            cards.zip(words.flatten()).forEach { (card, word) ->
                if (!word.contacted.value) {
                    remainingCount[word.type.value.ordinal]++
                }
            }
            layout.invalidate()
            redSpiesRemainingView.text = remainingCount[WordType.Red.ordinal].toString()
            blueSpiesRemainingView.text = remainingCount[WordType.Blue.ordinal].toString()
            civiliansRemainingView.text = remainingCount[WordType.Civilian.ordinal].toString()
            remainingLayout.invalidate()
        }, gameState, *words.flatten().map { it.contacted }.toTypedArray(), *words.flatten().map { it.type }.toTypedArray())

        initializeFocus(cards)
    }

    fun initializeFocus(textViews: ArrayList<Card>) {
        for (textView in textViews) {
            textView.id = View.generateViewId()
        }

        textViews.forEachIndexed { index, textView ->
            if (index + 1 < textViews.size) {
                textView.nextFocusDownId = textViews[index + 1].id
            }
        }
    }

    fun flashCards(flash : ((Card,Word) -> Unit)?) {
        currentAnimationSet.cancel()
        currentAnimationSet = AnimatorSet()
        cards.zip(words.flatten()).forEach { (card, word) ->
            val anim = ObjectAnimator.ofArgb(card, "borderOverrideColor", Color.argb(180, 255, 255, 255), Color.argb(0, 255, 255, 255))
            anim.duration = 600
            //anim.startDelay = (Math.random() * 300).toLong()
            val dr = word.row - (height-1)/2.0
            val dc = word.column - (width-1)/2.0
            //anim.startDelay = ((dr*dr + dc*dc) * 30).toLong()
            anim.startDelay = (word.column * 50 + Math.random()*50).toLong()
            anim.addUpdateListener { card.invalidate() }
            anim.addListener(object: Animator.AnimatorListener {
                override fun onAnimationEnd(p0: Animator?) {
                }

                override fun onAnimationCancel(p0: Animator?) {
                }

                override fun onAnimationStart(p0: Animator?) {
                    flash?.invoke(card, word)
                }

                override fun onAnimationRepeat(p0: Animator?) {
                }

            })
            currentAnimationSet.play(anim)
        }
        currentAnimationSet.start()
    }

    fun resetCardOverrideColors() {
        currentAnimationSet.cancel()
        currentAnimationSet = AnimatorSet()
        cards.zip(words.flatten()).forEach { (card, word) ->
            var anim = ObjectAnimator.ofArgb(card, "borderOverrideColor", card.borderOverrideColor, Color.argb(0, 255, 255, 255))
            anim.duration = 400
            anim.startDelay = (Math.random() * 200).toLong()
            anim.addUpdateListener { card.invalidate() }
            currentAnimationSet.play(anim)

            anim = ObjectAnimator.ofArgb(card, "surfaceOverrideColor", card.surfaceOverrideColor, Color.argb(0, 255, 255, 255))
            anim.duration = anim.duration
            anim.startDelay = anim.startDelay
            currentAnimationSet.play(anim)

            anim = ObjectAnimator.ofArgb(card, "textOverrideColor", card.textOverrideColor, Color.argb(0, 255, 255, 255))
            anim.duration = anim.duration
            anim.startDelay = anim.startDelay
            currentAnimationSet.play(anim)
        }
        currentAnimationSet.start()
    }

    fun animateCardScores() {
        val maxScore = words.flatten().maxBy { it.score }!!.score
        currentAnimationSet.cancel()
        currentAnimationSet = AnimatorSet()
        cards.zip(words.flatten()).forEach { (card, word) ->
            var targetColor1 = if(word.isTarget) Color.WHITE else Color.argb(255, 0, 0, 0)
            var alpha = Math.max(0f, word.score / maxScore)
            // Make things more distinct in the UI
            if (alpha < 0.5) alpha = 0f
            alpha *= alpha
            alpha = Math.round(alpha * 3) / 3f
            var targetColor2 = Color.argb(255, (alpha * 255f).toInt(), (alpha * 255f).toInt(), (alpha * 255f).toInt())

            targetColor2 = card.colorMultiply(targetColor2, card.brighten(context.resources.getColor(word.getColor(GameState.EnterColors)), 0.1f))

            var textTargetColor = if (word.isVisible) Color.TRANSPARENT else Color.argb(255, 60, 60, 60)
            if (!displayScores) {
                targetColor1 = Color.TRANSPARENT
                targetColor2 = Color.TRANSPARENT
                textTargetColor = Color.TRANSPARENT
            }

            // Get a nicer transition if we start with the correct color values and just animate the alpha
            if (Color.alpha(card.borderOverrideColor) == 0) card.borderOverrideColor = Color.argb(0, Color.red(targetColor1), Color.green(targetColor1), Color.blue(targetColor1))
            val anim = ObjectAnimator.ofArgb(card, "borderOverrideColor", card.borderOverrideColor, targetColor1)
            anim.duration = 400
            anim.startDelay = ((1 - alpha) * 100).toLong()
            anim.addUpdateListener { card.invalidate() }
            currentAnimationSet.play(anim)

            if (Color.alpha(card.surfaceOverrideColor) == 0) card.surfaceOverrideColor = Color.argb(0, Color.red(targetColor2), Color.green(targetColor2), Color.blue(targetColor2))
            val anim2 = ObjectAnimator.ofArgb(card, "surfaceOverrideColor", card.surfaceOverrideColor, targetColor2)
            anim2.duration = anim.duration
            anim2.startDelay = anim.startDelay
            currentAnimationSet.play(anim2)


            if (Color.alpha(card.textOverrideColor) == 0) card.textOverrideColor = Color.argb(0, Color.red(textTargetColor), Color.green(textTargetColor), Color.blue(textTargetColor))
            val anim3 = ObjectAnimator.ofArgb(card, "textOverrideColor", card.textOverrideColor, textTargetColor)
            anim3.duration = anim.duration
            anim3.startDelay = anim.startDelay
            currentAnimationSet.play(anim3)
        }
        currentAnimationSet.start()
    }

    fun onSaveInstanceState(outState: Bundle, prefix: String) {
        outState.putInt(prefix + "_width", width)
        outState.putInt(prefix + "_height", height)
        outState.putString(prefix + "_paint_type", paintType.toString())
        words.flatten().forEachIndexed { index, word ->  word.onSaveInstanceState(outState, prefix + "_word_" + index) }
    }

    fun onRestoreInstanceState(inState: Bundle, prefix: String) {
        width = inState.getInt(prefix + "_width")
        height = inState.getInt(prefix + "_height")
        paintType = WordType.valueOf(inState.getString(prefix + "_paint_type"))
        words.flatten().forEachIndexed { index, word ->  word.onRestoreInstanceState(inState, prefix + "_word_" + index) }
    }
}