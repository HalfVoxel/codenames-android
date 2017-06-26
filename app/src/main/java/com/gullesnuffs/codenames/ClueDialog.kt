package com.gullesnuffs.codenames

import android.app.AlertDialog
import android.app.Dialog
import android.app.DialogFragment
import android.os.Bundle
import android.widget.TextView


class ClueDialog : DialogFragment() {

    var clue : Clue = Clue("CLUE", 0, Team.Red)

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        if(savedInstanceState != null)
            clue.onRestoreInstanceState(savedInstanceState, "clue")

        val builder = AlertDialog.Builder(activity)

        val inflater = activity.layoutInflater

        val dialogView = inflater.inflate(R.layout.clue_dialog, null)
        builder.setView(dialogView)
                .setPositiveButton("OK") { dialog, id -> }

        val color = clue.getColor()

        val clueWordView = dialogView.findViewById(R.id.clue_word) as TextView
        clueWordView.setText(clue.word)
        clueWordView.setTextColor(getResources().getColor(color))

        val clueNumberView = dialogView.findViewById(R.id.clue_number) as TextView
        clueNumberView.setText(clue.number.toString())
        clueNumberView.setTextColor(getResources().getColor(color))

        return builder.create()
    }

    override fun onSaveInstanceState(outState: Bundle)
    {
        clue.onSaveInstanceState(outState, "clue")
        super.onSaveInstanceState(outState);
    }
}