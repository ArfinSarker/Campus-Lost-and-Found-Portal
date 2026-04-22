package com.sas.lostandfound;

import android.content.Context;
import androidx.annotation.NonNull;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.engine.cache.InternalCacheDiskCacheFactory;
import com.bumptech.glide.load.engine.cache.LruResourceCache;
import com.bumptech.glide.module.AppGlideModule;
import com.bumptech.glide.request.RequestOptions;

@GlideModule
public final class MyAppGlideModule extends AppGlideModule {
    @Override
    public void applyOptions(@NonNull Context context, @NonNull GlideBuilder builder) {
        // Increase memory cache size
        int memoryCacheSizeBytes = 1024 * 1024 * 20; // 20mb
        builder.setMemoryCache(new LruResourceCache(memoryCacheSizeBytes));

        // Increase disk cache size
        int diskCacheSizeBytes = 1024 * 1024 * 100; // 100mb
        builder.setDiskCache(new InternalCacheDiskCacheFactory(context, diskCacheSizeBytes));

        // Prefer RGB_565 for better performance and lower memory usage (at slight cost of quality)
        builder.setDefaultRequestOptions(
                new RequestOptions()
                        .format(DecodeFormat.PREFER_RGB_565)
                        .disallowHardwareConfig() // Sometimes helps with stability on older devices
        );
    }
}
