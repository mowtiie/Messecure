package com.mowtiie.messecure.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mowtiie.messecure.R;
import com.mowtiie.messecure.data.User;

import java.util.List;

public class ContactsAdapter extends RecyclerView.Adapter<ContactsAdapter.ViewHolder> {

    public interface OnContactClickListener {
        void onContactClick(User user);
    }

    private final List<User> users;
    private final OnContactClickListener listener;

    public ContactsAdapter(List<User> users, OnContactClickListener listener) {
        this.users    = users;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_contact, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(users.get(position), listener);
    }

    @Override
    public int getItemCount() { return users.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView avatarLabel, nameText, emailText;

        ViewHolder(View itemView) {
            super(itemView);
            avatarLabel = itemView.findViewById(R.id.avatarLabel);
            nameText    = itemView.findViewById(R.id.nameText);
            emailText   = itemView.findViewById(R.id.emailText);
        }

        void bind(User user, OnContactClickListener listener) {
            avatarLabel.setText(user.getAvatarLabel());
            nameText.setText(user.getDisplayName() != null ? user.getDisplayName() : "");
            emailText.setText(user.getEmail() != null ? user.getEmail() : "");
            itemView.setOnClickListener(v -> listener.onContactClick(user));
        }
    }
}
