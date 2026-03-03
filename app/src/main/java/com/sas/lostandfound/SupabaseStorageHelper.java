package com.sas.lostandfound;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SupabaseStorageHelper {
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build();

    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface UploadCallback {
        void onSuccess(String publicUrl);
        void onFailure(Exception e);
    }

    public static void uploadImage(Context context, Uri fileUri, String folder, String fileName, UploadCallback callback) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(fileUri);
            if (inputStream == null) {
                callback.onFailure(new IOException("Could not open input stream"));
                return;
            }

            // Using a simple way to read bytes for small images. 
            // For very large images, a more memory-efficient buffer approach is better.
            byte[] data = readAllBytes(inputStream);
            inputStream.close();

            String url = SupabaseConfig.SUPABASE_URL + "/storage/v1/object/" + SupabaseConfig.BUCKET_NAME + "/" + folder + "/" + fileName;

            RequestBody requestBody = RequestBody.create(data, MediaType.parse("image/jpeg"));

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("apikey", SupabaseConfig.SUPABASE_KEY)
                    .addHeader("Authorization", "Bearer " + SupabaseConfig.SUPABASE_KEY)
                    .addHeader("Content-Type", "image/jpeg")
                    .post(requestBody)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    mainHandler.post(() -> callback.onFailure(e));
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        String publicUrl = SupabaseConfig.SUPABASE_URL + "/storage/v1/object/public/" +
                                SupabaseConfig.BUCKET_NAME + "/" + folder + "/" + fileName;
                        mainHandler.post(() -> callback.onSuccess(publicUrl));
                    } else {
                        String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                        mainHandler.post(() -> callback.onFailure(new IOException("Upload failed: " + response.code() + " " + errorBody)));
                    }
                    response.close();
                }
            });
        } catch (Exception e) {
            callback.onFailure(e);
        }
    }

    private static byte[] readAllBytes(InputStream inputStream) throws IOException {
        java.io.ByteArrayOutputStream byteBuffer = new java.io.ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];
        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }
        return byteBuffer.toByteArray();
    }
}
