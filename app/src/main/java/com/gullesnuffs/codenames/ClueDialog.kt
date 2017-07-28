package com.gullesnuffs.codenames

import android.app.AlertDialog
import android.app.Dialog
import android.app.DialogFragment
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView


class ClueDialog : DialogFragment() {

    var clueWordView: TextView? = null
    var clueNumberView: TextView? = null
    var progressBar: ProgressBar? = null
    var team: Team? = null
    var clue: Clue? = null
        set(value)
        {
            field = value
            if(value != null) {
                clueWordView?.text = value.word
                clueWordView?.setTextColor(resources.getColor(value.getColor()))
                clueWordView?.visibility = View.VISIBLE
                clueNumberView?.text = value.number.toString()
                clueNumberView?.setTextColor(resources.getColor(value.getColor()))
                clueNumberView?.visibility = View.VISIBLE
                progressBar?.visibility = View.GONE
            }
        }


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        var restoredClue: Clue? = null
        if (savedInstanceState != null) {
            restoredClue = Clue("", 0, Team.Red)
            restoredClue.onRestoreInstanceState(savedInstanceState, "clue")
        }

        val builder = AlertDialog.Builder(activity)

        val inflater = activity.layoutInflater

        val dialogView = inflater.inflate(R.layout.clue_dialog, null)
        builder.setView(dialogView)
                .setPositiveButton("OK") { dialog, id -> }

        clueWordView = dialogView.findViewById(R.id.clue_word) as TextView
        clueNumberView = dialogView.findViewById(R.id.clue_number) as TextView
        progressBar = dialogView.findViewById(R.id.clue_progress_bar) as ProgressBar

        if(team != null) {
            val color =
                if(team == Team.Red) R.color.red_team_color
                else R.color.blue_team_color
            progressBar?.indeterminateDrawable?.setColorFilter(
                    resources.getColor(color),
                    android.graphics.PorterDuff.Mode.MULTIPLY)
        }

        clue = restoredClue

        return builder.create()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        clue?.onSaveInstanceState(outState, "clue")
        super.onSaveInstanceState(outState)
    }
}