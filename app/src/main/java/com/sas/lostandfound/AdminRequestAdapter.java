package com.sas.lostandfound;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class AdminRequestAdapter extends RecyclerView.Adapter<AdminRequestAdapter.ViewHolder> {

    private List<AdminRequest> requests;
    private OnRequestListener listener;

    public interface OnRequestListener {
        void onAccept(AdminRequest request);

        void onDeny(AdminRequest request);
    }

    public AdminRequestAdapter(List<AdminRequest> requests, OnRequestListener listener) {
        this.requests = requests;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_request, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AdminRequest request = requests.get(position);

        holder.tvName.setText(request.getFullName());
        holder.tvDesignation.setText(request.getDesignation());
        holder.tvId.setText("ID: " + request.getUniversityId());
        
        final String email = request.getEmail();
        final String phone = request.getPhoneNumber();
        final Context context = holder.itemView.getContext();

        holder.tvEmail.setText(
                email != null && !email.isEmpty() ? email : "No Email Provided");
        holder.tvPhone.setText(phone);
        holder.tvCode.setText("Code: " + request.getVerificationCode());

        if (email != null && !email.isEmpty()) {
            holder.tvEmail.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_SENDTO);
                intent.setData(Uri.parse("mailto:" + email));
                try {
                    context.startActivity(Intent.createChooser(intent, "Send Email"));
                } catch (android.content.ActivityNotFoundException e) {
                    SnackbarManager.show(SnackbarManager.Type.ERROR, "No email client found on this device");
                }
            });

            holder.tvEmail.setOnLongClickListener(v -> {
                android.content.ClipboardManager clipboard = (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                android.content.ClipData clip = android.content.ClipData.newPlainText("Email Address", email);
                if (clipboard != null) {
                    clipboard.setPrimaryClip(clip);
                    SnackbarManager.show(SnackbarManager.Type.SUCCESS, "Email copied to clipboard");
                }
                return true;
            });
        } else {
            holder.tvEmail.setOnClickListener(null);
            holder.tvEmail.setOnLongClickListener(null);
        }

        if (phone != null && !phone.isEmpty()) {
            holder.tvPhone.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:" + phone));
                try {
                    context.startActivity(intent);
                } catch (android.content.ActivityNotFoundException e) {
                    SnackbarManager.show(SnackbarManager.Type.ERROR, "No phone dialer found on this device");
                }
            });

            holder.tvPhone.setOnLongClickListener(v -> {
                android.content.ClipboardManager clipboard = (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                android.content.ClipData clip = android.content.ClipData.newPlainText("Phone Number", phone);
                if (clipboard != null) {
                    clipboard.setPrimaryClip(clip);
                    SnackbarManager.show(SnackbarManager.Type.SUCCESS, "Phone number copied to clipboard");
                }
                return true;
            });
        } else {
            holder.tvPhone.setOnClickListener(null);
            holder.tvPhone.setOnLongClickListener(null);
        }

        if (request.getProfileImageUrl() != null && !request.getProfileImageUrl().isEmpty()) {
            GlideApp.with(holder.itemView.getContext())
                    .load(SupabaseStorageHelper.ensurePublicUrl(request.getProfileImageUrl()))
                    .placeholder(R.drawable.ic_default_avatar)
                    .thumbnail(0.1f)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .circleCrop()
                    .into(holder.ivProfile);
        } else {
            holder.ivProfile.setImageResource(R.drawable.ic_default_avatar);
        }

        holder.btnAccept.setOnClickListener(v -> listener.onAccept(request));
        holder.btnDeny.setOnClickListener(v -> listener.onDeny(request));
    }

    @Override
    public int getItemCount() {
        return requests.size();
    }

    public void updateRequests(List<AdminRequest> newRequests) {
        androidx.recyclerview.widget.DiffUtil.DiffResult diffResult = androidx.recyclerview.widget.DiffUtil
                .calculateDiff(new androidx.recyclerview.widget.DiffUtil.Callback() {
                    @Override
                    public int getOldListSize() {
                        return requests.size();
                    }

                    @Override
                    public int getNewListSize() {
                        return newRequests.size();
                    }

                    @Override
                    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                        AdminRequest oldItem = requests.get(oldItemPosition);
                        AdminRequest newItem = newRequests.get(newItemPosition);
                        return oldItem.getUniversityId() != null && newItem.getUniversityId() != null
                                && oldItem.getUniversityId().equals(newItem.getUniversityId());
                    }

                    @Override
                    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                        AdminRequest oldItem = requests.get(oldItemPosition);
                        AdminRequest newItem = newRequests.get(newItemPosition);
                        return java.util.Objects.equals(oldItem.getFullName(), newItem.getFullName()) &&
                                java.util.Objects.equals(oldItem.getDesignation(), newItem.getDesignation()) &&
                                java.util.Objects.equals(oldItem.getEmail(), newItem.getEmail()) &&
                                java.util.Objects.equals(oldItem.getPhoneNumber(), newItem.getPhoneNumber()) &&
                                java.util.Objects.equals(oldItem.getVerificationCode(), newItem.getVerificationCode())
                                &&
                                java.util.Objects.equals(oldItem.getProfileImageUrl(), newItem.getProfileImageUrl());
                    }
                });

        this.requests.clear();
        this.requests.addAll(newRequests);
        diffResult.dispatchUpdatesTo(this);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvId, tvEmail, tvPhone, tvCode, tvDesignation;
        ImageView ivProfile;
        MaterialButton btnAccept, btnDeny;

        ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvRequestName);
            tvDesignation = itemView.findViewById(R.id.tvRequestDesignation);
            tvId = itemView.findViewById(R.id.tvRequestId);
            tvEmail = itemView.findViewById(R.id.tvRequestEmail);
            tvPhone = itemView.findViewById(R.id.tvRequestPhone);
            tvCode = itemView.findViewById(R.id.tvVerificationCode);
            ivProfile = itemView.findViewById(R.id.ivRequestProfile);
            btnAccept = itemView.findViewById(R.id.btnAccept);
            btnDeny = itemView.findViewById(R.id.btnDeny);
        }
    }

}
