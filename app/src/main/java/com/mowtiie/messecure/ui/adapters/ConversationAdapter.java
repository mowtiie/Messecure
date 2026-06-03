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
import java.util.Map;

public class ConversationAdapter extends RecyclerView.Adapter<ConversationAdapter.ViewHolder> {

    public interface OnConversationClickListener {
        void onConversationClick(Conversation conversation);
    }

    private final List<Conversation> conversations;
    private final Map<String, Integer> unreadCounts;
    private final OnConversationClickListener listener;

    public ConversationAdapter(List<Conversation> conversations,
                               Map<String, Integer> unreadCounts,
                               OnConversationClickListener listener) {
        this.conversations = conversations;
        this.unreadCounts  = unreadCounts;
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
        int unread = unreadCounts != null && unreadCounts.containsKey(conv.getId())
                ? unreadCounts.get(conv.getId()) : 0;
        holder.bind(conv, unread, listener);
    }

    @Override
    public int getItemCount() { return conversations.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView avatarLabel, nameText, previewText, timeText, timerBadge, unreadBadge;

        ViewHolder(View itemView) {
            super(itemView);
            avatarLabel = itemView.findViewById(R.id.avatarLabel);
            nameText    = itemView.findViewById(R.id.nameText);
            previewText = itemView.findViewById(R.id.previewText);
            timeText    = itemView.findViewById(R.id.timeText);
            timerBadge  = itemView.findViewById(R.id.timerBadge);
            unreadBadge = itemView.findViewById(R.id.unreadBadge);
        }

        void bind(Conversation conv, int unread, OnConversationClickListener listener) {
            avatarLabel.setText(conv.getAvatarLabel());
            nameText.setText(conv.getOtherUserName() != null ? conv.getOtherUserName() : "...");
            previewText.setText(conv.getLastMessage() != null ? conv.getLastMessage() : "");

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

            if (unread > 0) {
                unreadBadge.setVisibility(View.VISIBLE);
                unreadBadge.setText(unread > 99 ? "99+" : String.valueOf(unread));
                nameText.setTypeface(null, android.graphics.Typeface.BOLD);
            } else {
                unreadBadge.setVisibility(View.GONE);
                nameText.setTypeface(null, android.graphics.Typeface.NORMAL);
            }

            itemView.setOnClickListener(v -> listener.onConversationClick(conv));
        }

        private String formatTime(Date date) {
            long diff = System.currentTimeMillis() - date.getTime();
            if (diff < 60_000)     return "Just now";
            if (diff < 3_600_000)  return (diff / 60_000) + "m";
            if (diff < 86_400_000) return (diff / 3_600_000) + "h";
            return new SimpleDateFormat("MMM d", Locale.getDefault()).format(date);
        }
    }
}