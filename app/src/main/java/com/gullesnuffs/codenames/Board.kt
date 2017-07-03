package com.gullesnuffs.codenames

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.widget.*
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup


class Board(var words: Array<Array<Word>>,
            var paintType: WordType,
            var layout: TableLayout,
            val remainingLayout: ViewGroup,
            val autoCompleteAdapter: ArrayAdapter<String>,
            val gameState: GameState,
            val context: Context) {
    var width: Int = words[0].size
    var height: Int = words.size
    val cards = ArrayList<Card>()
    val redSpiesRemainingView = remainingLayout.findViewById(R.id.red_spies_remaining) as TextView
    val blueSpiesRemainingView = remainingLayout.findViewById(R.id.blue_spies_remaining) as TextView
    val civiliansRemainingView = remainingLayout.findViewById(R.id.civilians_remaining) as TextView
    var currentAnimationSet : AnimatorSet
    var displayScores = false

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
            when (gameState) {
                GameState.EnterWords -> {
                    card.setAdapter<ArrayAdapter<String>>(autoCompleteAdapter)
                    card.onChanged = { s -> word.word = s }
                    card.editable = true
                }
                GameState.EnterColors -> {
                    parent.setOnClickListener { _ ->
                        word.type = paintType
                        updateLayout()
                    }
                    card.editable = false
                }
                GameState.GetClues -> {
                    parent.setOnClickListener { _ ->
                        word.contacted = !word.contacted
                        updateLayout()
                    }
                    card.editable = false
                }
            }
        }

        initializeFocus(cards)
        updateLayout()
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

    fun flashCards() {
        currentAnimationSet.cancel()
        currentAnimationSet = AnimatorSet()
        cards.zip(words.flatten()).forEach { (card, word) ->
            val anim = ObjectAnimator.ofArgb(card, "borderOverrideColor", Color.argb(180, 255, 255, 255), Color.argb(0, 255, 255, 255))
            anim.duration = 400
            anim.startDelay = (Math.random() * 200).toLong()
            anim.addUpdateListener { card.invalidate() }
            currentAnimationSet.play(anim)
        }
        currentAnimationSet.start()
    }

    fun resetCardOverrideColors() {
        currentAnimationSet.cancel()
        currentAnimationSet = AnimatorSet()
        cards.zip(words.flatten()).forEach { (card, word) ->
            val anim = ObjectAnimator.ofArgb(card, "borderOverrideColor", card.borderOverrideColor, Color.argb(0, 255, 255, 255))
            anim.duration = 400
            anim.startDelay = (Math.random() * 200).toLong()
            anim.addUpdateListener { card.invalidate() }
            currentAnimationSet.play(anim)
        }
        currentAnimationSet.start()
    }

    fun updateLayout() {
        val maxScore = words.flatten().maxBy { it.score }!!.score
        currentAnimationSet.cancel()
        currentAnimationSet = AnimatorSet()
        val remainingCount = intArrayOf(0, 0, 0, 0)
        cards.zip(words.flatten()).forEach { (card, word) ->
            if (!word.contacted) {
                remainingCount[word.type.ordinal]++
            }

            (card as TextView).text = word.word
            if (gameState == GameState.GetClues) {
                card.state = if (word.contacted) word.type else null
            } else {
                card.state = word.type
            }

            var targetColor1 = if(word.isTarget) Color.WHITE else Color.argb(255, 0, 0, 0)
            var alpha = Math.max(0f, word.score / maxScore)
            // Make things more distinct in the UI
            if (alpha < 0.5) alpha = 0f
            alpha *= alpha
            alpha = Math.round(alpha * 3) / 3f
            var targetColor2 = Color.argb(255, (alpha * 255f).toInt(), (alpha * 255f).toInt(), (alpha * 255f).toInt())

            targetColor2 = card.colorMultiply(targetColor2, card.brighten(context.resources.getColor(word.getColor(GameState.EnterColors)), 0.1f))
            if (!displayScores) {
                targetColor1 = Color.argb(0, 0, 0, 0)
                targetColor2 = Color.argb(0, 0, 0, 0)
            }

            val anim = ObjectAnimator.ofArgb(card, "borderOverrideColor", card.borderOverrideColor, targetColor1)
            anim.duration = 400
            anim.startDelay = ((1 - alpha) * 100).toLong()
            anim.addUpdateListener { card.invalidate() }
            currentAnimationSet.play(anim)

            val anim2 = ObjectAnimator.ofArgb(card, "surfaceOverrideColor", card.surfaceOverrideColor, targetColor2)
            anim2.duration = anim.duration
            anim2.startDelay = anim.startDelay
            currentAnimationSet.play(anim2)
        }
        currentAnimationSet.start()

        layout.invalidate()

        redSpiesRemainingView.text = remainingCount[WordType.Red.ordinal].toString()
        blueSpiesRemainingView.text = remainingCount[WordType.Blue.ordinal].toString()
        civiliansRemainingView.text = remainingCount[WordType.Civilian.ordinal].toString()
        remainingLayout.invalidate()
    }

    fun onSaveInstanceState(outState: Bundle, prefix: String) {
        outState.putInt(prefix + "_width", width)
        outState.putInt(prefix + "_height", height)
        outState.putString(prefix + "_paint_type", paintType.toString())
        for (i in 0 until width) {
            for (j in 0 until height) {
                words[i][j].onSaveInstanceState(outState, prefix + "_word_" + i + "_" + j);
            }
        }
    }

    fun onRestoreInstanceState(inState: Bundle, prefix: String) {
        width = inState.getInt(prefix + "_width")
        height = inState.getInt(prefix + "_height")
        paintType = WordType.valueOf(inState.getString(prefix + "_paint_type"))
        words = Array<Array<Word>>(height) {
            i ->
            Array<Word>(width) {
                j ->
                Word(inState, prefix + "_word_" + i + "_" + j)
            }
        }
    }
}