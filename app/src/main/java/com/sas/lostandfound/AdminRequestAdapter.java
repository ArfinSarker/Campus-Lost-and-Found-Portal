package com.sas.lostandfound;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
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
        holder.tvEmail.setText(request.getEmail() != null && !request.getEmail().isEmpty() ? request.getEmail() : "No Email Provided");
        holder.tvPhone.setText(request.getPhoneNumber());
        holder.tvCode.setText("Code: " + request.getVerificationCode());

        if (request.getProfileImageUrl() != null && !request.getProfileImageUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(request.getProfileImageUrl())
                    .placeholder(R.drawable.ic_user)
                    .circleCrop()
                    .into(holder.ivProfile);
        } else {
            holder.ivProfile.setImageResource(R.drawable.ic_user);
        }

        holder.btnAccept.setOnClickListener(v -> listener.onAccept(request));
        holder.btnDeny.setOnClickListener(v -> listener.onDeny(request));
    }

    @Override
    public int getItemCount() {
        return requests.size();
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
