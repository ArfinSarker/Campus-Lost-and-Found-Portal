package com.sas.lostandfound;

import android.os.Handler;
import android.os.Looper;

import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SupabaseDatabaseHelper {

    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();

    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final Gson gson = new Gson();
    private static String authToken = null;

    public static void setAuthToken(String token) {
        authToken = token;
    }

    private static String getAuthHeader() {
        if (authToken != null && !authToken.isEmpty()) {
            return "Bearer " + authToken;
        }
        return "Bearer " + SupabaseConfig.SUPABASE_KEY;
    }

    public interface DatabaseCallback<T> {
        void onSuccess(T result);
        void onFailure(String errorMessage);
    }

    private static String getBaseUrl() {
        String url = SupabaseConfig.SUPABASE_URL;
        if (url != null && url.endsWith("/")) {
            return url.substring(0, url.length() - 1);
        }
        return url != null ? url : "";
    }

    /**
     * Generic Select (GET)
     */
    public static <T> void select(String table, String query, Type type, DatabaseCallback<T> callback) {
        String url = getBaseUrl() + "/rest/v1/" + table + (query != null ? "?" + query : "");

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.SUPABASE_KEY)
                .addHeader("Authorization", getAuthHeader())
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> callback.onFailure("Network error: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String body = response.body() != null ? response.body().string() : "";
                    if (response.isSuccessful()) {
                        T result = gson.fromJson(body, type);
                        mainHandler.post(() -> callback.onSuccess(result));
                    } else {
                        mainHandler.post(() -> callback.onFailure("Server error: " + response.code() + " " + body));
                    }
                } catch (Exception e) {
                    mainHandler.post(() -> callback.onFailure("Parsing error: " + e.getMessage()));
                } finally {
                    if (response.body() != null) response.close();
                }
            }
        });
    }

    /**
     * Generic Insert (POST)
     */
    public static void insert(String table, Object data, DatabaseCallback<String> callback) {
        String url = getBaseUrl() + "/rest/v1/" + table;
        String jsonStr = gson.toJson(data);

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.SUPABASE_KEY)
                .addHeader("Authorization", getAuthHeader())
                .addHeader("Prefer", "return=representation")
                .post(RequestBody.create(jsonStr, JSON))
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> callback.onFailure("Network error: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String body = response.body() != null ? response.body().string() : "";
                    if (response.isSuccessful()) {
                        mainHandler.post(() -> callback.onSuccess(body));
                    } else {
                        mainHandler.post(() -> callback.onFailure("Server error: " + response.code() + " " + body));
                    }
                } catch (Exception e) {
                    mainHandler.post(() -> callback.onFailure("Parsing error: " + e.getMessage()));
                } finally {
                    if (response.body() != null) response.close();
                }
            }
        });
    }

    /**
     * Generic Update (PATCH)
     */
    public static void update(String table, String query, Object data, DatabaseCallback<String> callback) {
        String url = getBaseUrl() + "/rest/v1/" + table + "?" + query;
        String jsonStr = gson.toJson(data);

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.SUPABASE_KEY)
                .addHeader("Authorization", getAuthHeader())
                .addHeader("Prefer", "return=representation")
                .patch(RequestBody.create(jsonStr, JSON))
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> callback.onFailure("Network error: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String body = response.body() != null ? response.body().string() : "";
                    if (response.isSuccessful()) {
                        mainHandler.post(() -> callback.onSuccess(body));
                    } else {
                        mainHandler.post(() -> callback.onFailure("Server error: " + response.code() + " " + body));
                    }
                } catch (Exception e) {
                    mainHandler.post(() -> callback.onFailure("Parsing error: " + e.getMessage()));
                } finally {
                    if (response.body() != null) response.close();
                }
            }
        });
    }

    /**
     * Generic Delete (DELETE)
     */
    public static void delete(String table, String query, DatabaseCallback<Void> callback) {
        String url = getBaseUrl() + "/rest/v1/" + table + "?" + query;

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.SUPABASE_KEY)
                .addHeader("Authorization", getAuthHeader())
                .delete()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> callback.onFailure("Network error: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    mainHandler.post(() -> callback.onSuccess(null));
                } else {
                    String body = response.body() != null ? response.body().string() : "";
                    mainHandler.post(() -> callback.onFailure("Server error: " + response.code() + " " + body));
                }
                if (response.body() != null) response.close();
            }
        });
    }

    /**
     * Call RPC Function (POST /rpc/func)
     */
    public static void rpc(String functionName, Map<String, Object> params, DatabaseCallback<String> callback) {
        String url = getBaseUrl() + "/rest/v1/rpc/" + functionName;
        String jsonStr = gson.toJson(params);

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.SUPABASE_KEY)
                .addHeader("Authorization", getAuthHeader())
                .post(RequestBody.create(jsonStr, JSON))
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> callback.onFailure("Network error: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String body = response.body() != null ? response.body().string() : "";
                    if (response.isSuccessful()) {
                        mainHandler.post(() -> callback.onSuccess(body));
                    } else {
                        mainHandler.post(() -> callback.onFailure("Server error: " + response.code() + " " + body));
                    }
                } catch (Exception e) {
                    mainHandler.post(() -> callback.onFailure("Parsing error: " + e.getMessage()));
                } finally {
                    if (response.body() != null) response.close();
                }
            }
        });
    }
}
