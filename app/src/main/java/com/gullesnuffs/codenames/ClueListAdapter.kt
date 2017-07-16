package com.gullesnuffs.codenames

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.clue_list_entry.view.*

class ClueListAdapter(val clueList: ClueList, val itemClick: (Clue) -> Unit) :
        RecyclerView.Adapter<ClueListAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.clue_list_entry, parent, false)
        return ViewHolder(view, itemClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindClue(clueList.list[position])
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payload: List<Any>) {
        holder.bindClue(clueList.list[position])
        if(payload.size > 0) {
            if(payload[0] == Team.Red){
                holder.itemView.setBackgroundResource(R.drawable.selected_clue_red_outline)
            }
            else if(payload[0] == Team.Red){
                holder.itemView.setBackgroundResource(R.drawable.selected_clue_blue_outline)
            }
        }
    }

    override fun getItemCount() = clueList.list.size

    class ViewHolder(view: View, val itemClick: (Clue) -> Unit) : RecyclerView.ViewHolder(view) {

        fun bindClue(clue: Clue) {
            with(clue) {
                itemView.clue_word.text = clue.word
                itemView.clue_word.setTextColor(
                        itemView.resources.getColor(clue.getColor()))
                itemView.clue_number.text = clue.number.toString()
                itemView.clue_number.setTextColor(
                        itemView.resources.getColor(clue.getColor()))
                itemView.setOnClickListener { itemClick(this) }

            }
        }
    }
}