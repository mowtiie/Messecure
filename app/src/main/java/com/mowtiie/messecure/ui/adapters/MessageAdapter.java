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
import com.mowtiie.messecure.util.TimestampFormatter;

import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface OnMessageLongClickListener {
        void onLongClick(Message message);
    }

    private static final int VIEW_TYPE_SENT     = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;

    private final List<Message> messages;
    private final String currentUid;
    private final OnMessageLongClickListener longClickListener;

    public MessageAdapter(List<Message> messages, String currentUid,
                          OnMessageLongClickListener longClickListener) {
        this.messages          = messages;
        this.currentUid        = currentUid;
        this.longClickListener = longClickListener;
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
            View v = inflater.inflate(R.layout.item_message_sent, parent, false);
            return new SentViewHolder(v);
        } else {
            View v = inflater.inflate(R.layout.item_message_received, parent, false);
            return new ReceivedViewHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message msg  = messages.get(position);
        Message prev = position > 0 ? messages.get(position - 1) : null;

        String text = msg.getDecryptedText() != null ? msg.getDecryptedText() : msg.getText();
        String time = TimestampFormatter.formatTime(msg.getSentAt());

        boolean showTime = TimestampFormatter.shouldShowTime(
                prev != null ? prev.getSentAt()  : null,
                prev != null ? prev.getSenderId() : null,
                msg.getSentAt(), msg.getSenderId());

        boolean showDateChip = TimestampFormatter.shouldShowDateChip(
                prev != null ? prev.getSentAt() : null, msg.getSentAt());
        String dateChip = showDateChip
                ? TimestampFormatter.formatDateChip(msg.getSentAt()) : null;

        if (holder instanceof SentViewHolder) {
            ((SentViewHolder) holder).bind(msg, text, time, showTime, dateChip,
                    longClickListener);
        } else {
            ((ReceivedViewHolder) holder).bind(msg, text, time, showTime, dateChip,
                    longClickListener);
        }
    }

    @Override
    public int getItemCount() { return messages.size(); }

    static class SentViewHolder extends RecyclerView.ViewHolder {
        TextView bubbleText, timeText, dateChip, replyPreview, selfDestructLabel;
        View replyContainer;
        ImageView readIcon, selfDestructIcon;

        SentViewHolder(View v) {
            super(v);
            bubbleText        = v.findViewById(R.id.bubbleText);
            timeText          = v.findViewById(R.id.timeText);
            readIcon          = v.findViewById(R.id.readIcon);
            dateChip          = v.findViewById(R.id.dateChip);
            replyContainer    = v.findViewById(R.id.replyContainer);
            replyPreview      = v.findViewById(R.id.replyPreview);
            selfDestructIcon  = v.findViewById(R.id.selfDestructIcon);
            selfDestructLabel = v.findViewById(R.id.selfDestructLabel);
        }

        void bind(Message msg, String text, String time, boolean showTime,
                  String dateChipText, OnMessageLongClickListener listener) {
            bubbleText.setText(text);
            timeText.setText(time);
            timeText.setVisibility(showTime ? View.VISIBLE : View.GONE);
            if (showTime) {
                readIcon.setVisibility(View.VISIBLE);
                readIcon.setImageResource(msg.isRead() ? R.drawable.ic_done_all : R.drawable.ic_done);
            } else {
                readIcon.setVisibility(View.GONE);
            }

            if (dateChipText != null) {
                dateChip.setVisibility(View.VISIBLE);
                dateChip.setText(dateChipText);
            } else {
                dateChip.setVisibility(View.GONE);
            }

            if (msg.hasReply()) {
                replyContainer.setVisibility(View.VISIBLE);
                replyPreview.setText(msg.getDecryptedReplyPreview() != null
                        ? msg.getDecryptedReplyPreview() : "(replied message)");
            } else {
                replyContainer.setVisibility(View.GONE);
            }

            if (msg.isSelfDestruct() && msg.getDestructAfterMinutes() > 0) {
                selfDestructIcon.setVisibility(View.VISIBLE);
                selfDestructLabel.setVisibility(View.VISIBLE);
                selfDestructLabel.setText(formatDestructLabel(msg.getDestructAfterMinutes()));
            } else {
                selfDestructIcon.setVisibility(View.GONE);
                selfDestructLabel.setVisibility(View.GONE);
            }

            itemView.setOnLongClickListener(v -> {
                if (listener != null) listener.onLongClick(msg);
                return true;
            });
        }
    }

    static class ReceivedViewHolder extends RecyclerView.ViewHolder {
        TextView bubbleText, timeText, dateChip, replyPreview, selfDestructLabel;
        View replyContainer;
        ImageView selfDestructIcon;

        ReceivedViewHolder(View v) {
            super(v);
            bubbleText        = v.findViewById(R.id.bubbleText);
            timeText          = v.findViewById(R.id.timeText);
            dateChip          = v.findViewById(R.id.dateChip);
            replyContainer    = v.findViewById(R.id.replyContainer);
            replyPreview      = v.findViewById(R.id.replyPreview);
            selfDestructIcon  = v.findViewById(R.id.selfDestructIcon);
            selfDestructLabel = v.findViewById(R.id.selfDestructLabel);
        }

        void bind(Message msg, String text, String time, boolean showTime,
                  String dateChipText, OnMessageLongClickListener listener) {
            bubbleText.setText(text);
            timeText.setText(time);
            timeText.setVisibility(showTime ? View.VISIBLE : View.GONE);

            if (dateChipText != null) {
                dateChip.setVisibility(View.VISIBLE);
                dateChip.setText(dateChipText);
            } else {
                dateChip.setVisibility(View.GONE);
            }

            if (msg.hasReply()) {
                replyContainer.setVisibility(View.VISIBLE);
                replyPreview.setText(msg.getDecryptedReplyPreview() != null
                        ? msg.getDecryptedReplyPreview() : "(replied message)");
            } else {
                replyContainer.setVisibility(View.GONE);
            }

            if (msg.isSelfDestruct() && msg.getDestructAfterMinutes() > 0) {
                selfDestructIcon.setVisibility(View.VISIBLE);
                selfDestructLabel.setVisibility(View.VISIBLE);
                selfDestructLabel.setText(formatDestructLabel(msg.getDestructAfterMinutes()));
            } else {
                selfDestructIcon.setVisibility(View.GONE);
                selfDestructLabel.setVisibility(View.GONE);
            }

            itemView.setOnLongClickListener(v -> {
                if (listener != null) listener.onLongClick(msg);
                return true;
            });
        }
    }

    private static String formatDestructLabel(int minutes) {
        if (minutes >= 60 && minutes % 60 == 0) {
            int hrs = minutes / 60;
            return hrs + (hrs == 1 ? " hour" : " hours");
        }
        return minutes + (minutes == 1 ? " minute" : " minutes");
    }
}