package com.gullesnuffs.codenames

import android.app.AlertDialog
import android.app.Dialog
import android.app.DialogFragment
import android.content.DialogInterface
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.widget.TextView
import com.gullesnuffs.codenames.R.id.clue_word


class ClueDialog : DialogFragment() {

    var word = "CLUE"
    var number = 0
    var team = Team.Red

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(activity)

        val inflater = activity.layoutInflater

        val dialogView = inflater.inflate(R.layout.clue_dialog, null)
        builder.setView(dialogView)
                .setPositiveButton("OK") { dialog, id -> }

        val color = if(team == Team.Red) R.color.red_team_color else R.color.blue_team_color

        val clueWordView = dialogView.findViewById(R.id.clue_word) as TextView
        clueWordView.setText(word)
        clueWordView.setTextColor(getResources().getColor(color))

        val clueNumberView = dialogView.findViewById(R.id.clue_number) as TextView
        clueNumberView.setText(number.toString())
        clueNumberView.setTextColor(getResources().getColor(color))

        return builder.create()
    }

    fun setClue(word: String, number: Int, team: Team){
        this.word = word
        this.number = number
        this.team = team
    }
}