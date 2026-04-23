package com.sas.lostandfound;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.github.chrisbanes.photoview.PhotoView;
import java.util.List;

public class ImageSliderAdapter extends RecyclerView.Adapter<ImageSliderAdapter.SliderViewHolder> {

    private List<String> imageUrls;
    private OnImageClickListener onImageClickListener;
    private boolean useFitCenter = false;
    private boolean isZoomable = false;

    public interface OnImageClickListener {
        void onImageClick(int position);
    }

    public ImageSliderAdapter(List<String> imageUrls) {
        this.imageUrls = imageUrls;
        this.isZoomable = false;
    }

    public ImageSliderAdapter(List<String> imageUrls, boolean useFitCenter) {
        this.imageUrls = imageUrls;
        this.useFitCenter = useFitCenter;
        this.isZoomable = useFitCenter; // Enable zoom when in full-screen mode (fit center)
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
        
        holder.photoView.setZoomable(isZoomable);

        if (useFitCenter) {
            holder.photoView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            GlideApp.with(holder.itemView.getContext())
                    .load(SupabaseStorageHelper.ensurePublicUrl(url))
                    .placeholder(R.drawable.ic_package)
                    .thumbnail(0.1f)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .fitCenter()
                    .into(holder.photoView);
        } else {
            holder.photoView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            GlideApp.with(holder.itemView.getContext())
                    .load(SupabaseStorageHelper.ensurePublicUrl(url))
                    .placeholder(R.drawable.ic_package)
                    .thumbnail(0.1f)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .centerCrop()
                    .into(holder.photoView);
        }

        // Set consolidated click listeners to avoid double-triggering FullScreenImageActivity.
        // PhotoView captures taps internally to handle zoom; we use its tap listeners for consistency.
        // We use only one tap listener to avoid duplicate calls.
        holder.photoView.setOnPhotoTapListener((view, x, y) -> {
            if (onImageClickListener != null) {
                onImageClickListener.onImageClick(position);
            }
        });

        // Ensure the root item click also works but doesn't double-trigger if PhotoView is tapped
        holder.itemView.setOnClickListener(v -> {
            // PhotoView tap listeners usually handle this, but if they don't, we fallback here.
            // However, to prevent double-starts, we should be careful.
            // Most of the time PhotoView is full-match-parent so this is rarely hit directly.
        });
    }

    @Override
    public int getItemCount() {
        return imageUrls != null ? imageUrls.size() : 0;
    }

    static class SliderViewHolder extends RecyclerView.ViewHolder {
        PhotoView photoView;

        public SliderViewHolder(@NonNull View itemView) {
            super(itemView);
            photoView = itemView.findViewById(R.id.imageView);
        }
    }
}
