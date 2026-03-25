package com.sas.lostandfound;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;

public class ImageSliderAdapter extends RecyclerView.Adapter<ImageSliderAdapter.SliderViewHolder> {

    private List<String> imageUrls;
    private OnImageClickListener onImageClickListener;
    private boolean useFitCenter = false;

    public interface OnImageClickListener {
        void onImageClick(int position);
    }

    public ImageSliderAdapter(List<String> imageUrls) {
        this.imageUrls = imageUrls;
    }

    public ImageSliderAdapter(List<String> imageUrls, boolean useFitCenter) {
        this.imageUrls = imageUrls;
        this.useFitCenter = useFitCenter;
    }

    public void setOnImageClickListener(OnImageClickListener listener) {
        this.onImageClickListener = listener;
    }

    @NonNull
    @Override
    public SliderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_image_slider, parent, false);
        return new SliderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SliderViewHolder holder, int position) {
        String url = imageUrls.get(position);
        
        if (useFitCenter) {
            holder.imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            Glide.with(holder.itemView.getContext())
                    .load(url)
                    .placeholder(R.drawable.ic_package)
                    .into(holder.imageView);
        } else {
            holder.imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            Glide.with(holder.itemView.getContext())
                    .load(url)
                    .placeholder(R.drawable.ic_package)
                    .centerCrop()
                    .into(holder.imageView);
        }

        if (onImageClickListener != null) {
            holder.itemView.setOnClickListener(v -> onImageClickListener.onImageClick(position));
            holder.itemView.setClickable(true);
        } else {
            holder.itemView.setOnClickListener(null);
            holder.itemView.setClickable(false);
        }
    }

    @Override
    public int getItemCount() {
        return imageUrls != null ? imageUrls.size() : 0;
    }

    static class SliderViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        public SliderViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageView);
        }
    }
}
