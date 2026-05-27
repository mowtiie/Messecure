package com.mowtiie.messecure.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mowtiie.messecure.R;
import com.mowtiie.messecure.data.Conversation;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ConversationAdapter extends RecyclerView.Adapter<ConversationAdapter.ViewHolder> {

    public interface OnConversationClickListener {
        void onConversationClick(Conversation conversation);
    }

    private final List<Conversation> conversations;
    private final OnConversationClickListener listener;

    public ConversationAdapter(List<Conversation> conversations, OnConversationClickListener listener) {
        this.conversations = conversations;
        this.listener      = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_conversation, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Conversation conv = conversations.get(position);
        holder.bind(conv, listener);
    }

    @Override
    public int getItemCount() { return conversations.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView avatarLabel, nameText, previewText, timeText, timerBadge;

        ViewHolder(View itemView) {
            super(itemView);
            avatarLabel = itemView.findViewById(R.id.avatarLabel);
            nameText    = itemView.findViewById(R.id.nameText);
            previewText = itemView.findViewById(R.id.previewText);
            timeText    = itemView.findViewById(R.id.timeText);
            timerBadge  = itemView.findViewById(R.id.timerBadge);
        }

        void bind(Conversation conv, OnConversationClickListener listener) {
            avatarLabel.setText(conv.getAvatarLabel());
            nameText.setText(conv.getOtherUserName() != null ? conv.getOtherUserName() : "...");
            previewText.setText(conv.getLastMessage() != null ? conv.getLastMessage() : "");

            // Format the timestamp
            if (conv.getLastMessageTime() != null) {
                timeText.setText(formatTime(conv.getLastMessageTime()));
            } else {
                timeText.setText("");
            }

            if (conv.getDestructTimer() > 0) {
                timerBadge.setVisibility(View.VISIBLE);
                timerBadge.setText(conv.getDestructTimer() + "m");
            } else {
                timerBadge.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(v -> listener.onConversationClick(conv));
        }

        private String formatTime(Date date) {
            long now    = System.currentTimeMillis();
            long millis = date.getTime();
            long diff   = now - millis;

            if (diff < 60_000)                  return "Just now";
            if (diff < 3_600_000)               return (diff / 60_000) + "m";
            if (diff < 86_400_000)              return (diff / 3_600_000) + "h";
            return new SimpleDateFormat("MMM d", Locale.getDefault()).format(date);
        }
    }
}
