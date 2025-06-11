package com.example.localvoicechat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.localvoicechat.databinding.ParticipantItemBinding

class ParticipantAdapter(
    private val participants: List<Participant>
) : RecyclerView.Adapter<ParticipantAdapter.ParticipantViewHolder>() {

    inner class ParticipantViewHolder(private val binding: ParticipantItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(participant: Participant) {
            binding.nameTextView.text = participant.name
            binding.hostIndicator.visibility =
                if (participant.isHost) View.VISIBLE else View.GONE
            binding.speakingIndicator.visibility =
                if (participant.isSpeaking && !participant.isMuted) View.VISIBLE else View.GONE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ParticipantViewHolder {
        val binding = ParticipantItemBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ParticipantViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ParticipantViewHolder, position: Int) {
        holder.bind(participants[position])
    }

    override fun getItemCount() = participants.size
}