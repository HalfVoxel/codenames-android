package com.gullesnuffs.codenames

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.widget.TextView

class ClueList(var listView: RecyclerView,
               val context: Context) {
    val list = mutableListOf<Clue>()
    var selectedClue: Clue? = null

    fun addClue(clue: Clue) {
        list.add(0, clue)

        val inflater = LayoutInflater.from(context)

        val listEntry = inflater.inflate(R.layout.clue_list_entry, null)
        val clueWordView = listEntry.findViewById(R.id.clue_word) as TextView
        clueWordView.text = clue.word
        clueWordView.setTextColor(context.resources.getColor(clue.getColor()))

        val clueNumberView = listEntry.findViewById(R.id.clue_number) as TextView
        clueNumberView.text = clue.number.toString()
        clueNumberView.setTextColor(context.resources.getColor(clue.getColor()))

        listView.adapter.notifyItemInserted(0)
        listView.scrollToPosition(0)
    }

    fun clear() {
        val oldSize = list.size
        list.clear()
        listView.adapter.notifyItemRangeRemoved(0, oldSize)
    }

    fun unselect() {
        val index = list.indexOf(selectedClue)
        if (index != -1) listView.adapter.notifyItemChanged(index)
        selectedClue = null
    }

    fun setSelected(clue: Clue) {
        unselect()
        val index = list.indexOf(clue)
        if (index != -1) listView.adapter.notifyItemChanged(index)
        selectedClue = clue
    }
}