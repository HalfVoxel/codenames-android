package com.gullesnuffs.codenames

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.widget.TextView

class ClueList(var listView: RecyclerView,
               val context: Context) {
    val list = mutableListOf<Clue>()

    fun addClue(clue: Clue){
        val newInd = list.size
        list.add(clue)

        val inflater = LayoutInflater.from(context)

        val listEntry = inflater.inflate(R.layout.clue_list_entry, null)
        val clueWordView = listEntry.findViewById(R.id.clue_word) as TextView
        clueWordView.setText(clue.word)
        clueWordView.setTextColor(context.getResources().getColor(clue.getColor()))

        val clueNumberView = listEntry.findViewById(R.id.clue_number) as TextView
        clueNumberView.setText(clue.number.toString())
        clueNumberView.setTextColor(context.getResources().getColor(clue.getColor()))

        listView.adapter.notifyItemInserted(newInd)
    }

}