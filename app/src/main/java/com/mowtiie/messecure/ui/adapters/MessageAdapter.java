package com.mowtiie.messecure.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mowtiie.messecure.R;
import com.mowtiie.messecure.data.Message;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_SENT     = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;

    private final List<Message> messages;
    private final String currentUid;

    public MessageAdapter(List<Message> messages, String currentUid) {
        this.messages   = messages;
        this.currentUid = currentUid;
    }

    @Override
    public int getItemViewType(int position) {
        return messages.get(position).isSentByCurrentUser(currentUid)
                ? VIEW_TYPE_SENT : VIEW_TYPE_RECEIVED;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_SENT) {
            View view = inflater.inflate(R.layout.item_message_sent, parent, false);
            return new SentViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_message_received, parent, false);
            return new ReceivedViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message msg = messages.get(position);
        String displayText = msg.getDecryptedText() != null
                ? msg.getDecryptedText() : msg.getText();
        String time = msg.getSentAt() != null
                ? new SimpleDateFormat("h:mm a", Locale.getDefault()).format(msg.getSentAt())
                : "";

        if (holder instanceof SentViewHolder) {
            ((SentViewHolder) holder).bind(displayText, time, msg.isRead());
        } else {
            ((ReceivedViewHolder) holder).bind(displayText, time);
        }
    }

    @Override
    public int getItemCount() { return messages.size(); }

    static class SentViewHolder extends RecyclerView.ViewHolder {
        private final TextView bubbleText, timeText;
        private final ImageView readIcon;

        SentViewHolder(View itemView) {
            super(itemView);
            bubbleText = itemView.findViewById(R.id.bubbleText);
            timeText   = itemView.findViewById(R.id.timeText);
            readIcon   = itemView.findViewById(R.id.readIcon);
        }

        void bind(String text, String time, boolean isRead) {
            bubbleText.setText(text);
            timeText.setText(time);
            readIcon.setImageResource(isRead
                    ? R.drawable.ic_done_all
                    : R.drawable.ic_done);
        }
    }

    static class ReceivedViewHolder extends RecyclerView.ViewHolder {
        private final TextView bubbleText, timeText;

        ReceivedViewHolder(View itemView) {
            super(itemView);
            bubbleText = itemView.findViewById(R.id.bubbleText);
            timeText   = itemView.findViewById(R.id.timeText);
        }

        void bind(String text, String time) {
            bubbleText.setText(text);
            timeText.setText(time);
        }
    }
}
