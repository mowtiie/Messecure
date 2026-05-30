package com.mowtiie.messecure.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mowtiie.messecure.R;
import com.mowtiie.messecure.data.User;

import java.util.List;

public class AdminUserAdapter extends RecyclerView.Adapter<AdminUserAdapter.ViewHolder> {

    public interface AdminActionListener {
        void onApprove(User user);

        void onRevoke(User user);

        void onDelete(User user);
    }

    private final List<User> users;
    private final String myUid;
    private final AdminActionListener listener;

    public AdminUserAdapter(List<User> users, String myUid, AdminActionListener listener) {
        this.users = users;
        this.myUid = myUid;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_user, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        h.bind(users.get(position), myUid, listener);
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView nameText, emailText, statusText;
        Button approveButton, revokeButton, deleteButton;

        ViewHolder(View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.nameText);
            emailText = itemView.findViewById(R.id.emailText);
            statusText = itemView.findViewById(R.id.statusText);
            approveButton = itemView.findViewById(R.id.approveButton);
            revokeButton = itemView.findViewById(R.id.revokeButton);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }

        void bind(User user, String myUid, AdminActionListener listener) {
            nameText.setText(user.getDisplayName() != null ? user.getDisplayName() : "(no name)");
            emailText.setText(user.getEmail() != null ? user.getEmail() : "");
            statusText.setText(user.getStatusLabel());

            boolean isSelf = user.getUid() != null && user.getUid().equals(myUid);

            approveButton.setVisibility(user.isVerified() ? View.GONE : View.VISIBLE);
            revokeButton.setVisibility(user.isVerified() && !isSelf ? View.VISIBLE : View.GONE);
            deleteButton.setVisibility(isSelf ? View.GONE : View.VISIBLE);

            approveButton.setOnClickListener(v -> listener.onApprove(user));
            revokeButton.setOnClickListener(v -> listener.onRevoke(user));
            deleteButton.setOnClickListener(v -> listener.onDelete(user));
        }
    }
}