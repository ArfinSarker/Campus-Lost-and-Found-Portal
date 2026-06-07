package com.sas.lostandfound;

import android.content.Context;
import android.content.SharedPreferences;
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
import okhttp3.Interceptor;
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
            .addInterceptor(new Interceptor() {
                @Override
                public Response intercept(Chain chain) throws IOException {
                    Request request = chain.request();
                    Response response = chain.proceed(request);

                    // If unauthorized, try to refresh token
                    if (response.code() == 401) {
                        String body = response.peekBody(2048).string();
                        if (body.contains("JWT expired") || body.contains("PGRST301") || body.contains("JWT invalid")) {
                            synchronized (SupabaseDatabaseHelper.class) {
                                // Check if token was already updated by another thread
                                SharedPreferences prefs = LostAndFoundApplication.getContext().getSharedPreferences("MyApp", Context.MODE_PRIVATE);
                                String currentToken = prefs.getString("accessToken", "");
                                String refreshToken = prefs.getString("refreshToken", "");

                                String requestToken = request.header("Authorization");
                                if (requestToken != null && requestToken.equals("Bearer " + currentToken)) {
                                    // Token in request is the same as current, so we really need a refresh
                                    if (!refreshToken.isEmpty()) {
                                        String refreshResponse = SupabaseAuthHelper.refreshSessionSync(refreshToken);
                                        if (refreshResponse != null) {
                                            try {
                                                JSONObject json = new JSONObject(refreshResponse);
                                                String newAccessToken = json.getString("access_token");
                                                String newRefreshToken = json.optString("refresh_token", refreshToken);

                                                // Save new tokens
                                                prefs.edit()
                                                        .putString("accessToken", newAccessToken)
                                                        .putString("refreshToken", newRefreshToken)
                                                        .apply();

                                                setAuthToken(newAccessToken);

                                                // Retry request with new token
                                                response.close(); // Close the 401 response
                                                Request newRequest = request.newBuilder()
                                                        .header("Authorization", "Bearer " + newAccessToken)
                                                        .build();
                                                return chain.proceed(newRequest);
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    }
                                } else if (!currentToken.isEmpty()) {
                                    // Token was already refreshed by another thread, just retry
                                    response.close();
                                    Request newRequest = request.newBuilder()
                                            .header("Authorization", "Bearer " + currentToken)
                                            .build();
                                    return chain.proceed(newRequest);
                                }
                            }
                        }
                    }
                    return response;
                }
            })
            .build();

    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final Gson gson = new Gson();
    private static String authToken = null;

    public static void init(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("MyApp", Context.MODE_PRIVATE);
        authToken = prefs.getString("accessToken", null);
    }

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
        String baseUrl = getBaseUrl();
        if (baseUrl == null || baseUrl.isEmpty()) {
            mainHandler.post(() -> callback.onFailure("Supabase URL is not configured. buildConfig: " + SupabaseConfig.getDebugStatus()));
            return;
        }
        
        String url = baseUrl + "/rest/v1/" + table + (query != null ? "?" + query : "");
        android.util.Log.d("SupabaseDB", "SELECT URL: " + url);

        try {
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("apikey", SupabaseConfig.SUPABASE_KEY != null ? SupabaseConfig.SUPABASE_KEY : "")
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
                        android.util.Log.d("SupabaseDB", "SELECT RESPONSE [" + response.code() + "]: " + body);
                        
                        if (response.isSuccessful()) {
                            T result = gson.fromJson(body, type);
                            mainHandler.post(() -> callback.onSuccess(result));
                        } else {
                            String errorMessage = parseDatabaseError(response.code(), body);
                            mainHandler.post(() -> callback.onFailure(errorMessage));
                        }
                    } catch (Exception e) {
                        android.util.Log.e("SupabaseDB", "Parsing error: " + e.getMessage());
                        mainHandler.post(() -> callback.onFailure("Parsing error: " + e.getMessage()));
                    } finally {
                        if (response.body() != null) response.close();
                    }
                }
            });
        } catch (IllegalArgumentException e) {
            mainHandler.post(() -> callback.onFailure("Invalid Supabase URL. Please check your configuration."));
        }
    }

    private static String parseDatabaseError(int code, String body) {
        try {
            if (body != null && !body.isEmpty()) {
                JSONObject json = new JSONObject(body);
                if (json.has("message")) {
                    String msg = json.getString("message");
                    if (json.has("details") && !json.isNull("details") && !json.getString("details").isEmpty()) {
                        return msg + " (" + json.getString("details") + ")";
                    }
                    return msg;
                }
            }
        } catch (Exception ignored) {}
        return "Server error: " + code + (body != null && !body.isEmpty() ? " " + body : "");
    }

    /**
     * Generic Insert (POST)
     */
    public static void insert(String table, Object data, DatabaseCallback<String> callback) {
        String baseUrl = getBaseUrl();
        if (baseUrl == null || baseUrl.isEmpty()) {
            mainHandler.post(() -> callback.onFailure("Supabase URL is not configured."));
            return;
        }
        
        String url = baseUrl + "/rest/v1/" + table;
        String jsonStr = gson.toJson(data);

        try {
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("apikey", SupabaseConfig.SUPABASE_KEY != null ? SupabaseConfig.SUPABASE_KEY : "")
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
                            String errorMessage = parseDatabaseError(response.code(), body);
                            mainHandler.post(() -> callback.onFailure(errorMessage));
                        }
                    } catch (Exception e) {
                        mainHandler.post(() -> callback.onFailure("Parsing error: " + e.getMessage()));
                    } finally {
                        if (response.body() != null) response.close();
                    }
                }
            });
        } catch (IllegalArgumentException e) {
            mainHandler.post(() -> callback.onFailure("Invalid Supabase URL."));
        }
    }

    /**
     * Generic Update (PATCH)
     */
    public static void update(String table, String query, Object data, DatabaseCallback<String> callback) {
        String baseUrl = getBaseUrl();
        if (baseUrl == null || baseUrl.isEmpty()) {
            mainHandler.post(() -> callback.onFailure("Supabase URL is not configured."));
            return;
        }
        
        String url = baseUrl + "/rest/v1/" + table + "?" + query;
        String jsonStr = gson.toJson(data);
        android.util.Log.d("SupabaseDB", "UPDATE URL: " + url);
        android.util.Log.d("SupabaseDB", "UPDATE BODY: " + jsonStr);

        try {
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("apikey", SupabaseConfig.SUPABASE_KEY != null ? SupabaseConfig.SUPABASE_KEY : "")
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
                        android.util.Log.d("SupabaseDB", "UPDATE RESPONSE [" + response.code() + "]: " + body);
                        
                        if (response.isSuccessful()) {
                            if (body.equals("[]")) {
                                mainHandler.post(() -> callback.onFailure("No records were updated. Check your permissions."));
                            } else {
                                mainHandler.post(() -> callback.onSuccess(body));
                            }
                        } else {
                            String errorMessage = parseDatabaseError(response.code(), body);
                            mainHandler.post(() -> callback.onFailure(errorMessage));
                        }
                    } catch (Exception e) {
                        android.util.Log.e("SupabaseDB", "Parsing error: " + e.getMessage());
                        mainHandler.post(() -> callback.onFailure("Parsing error: " + e.getMessage()));
                    } finally {
                        if (response.body() != null) response.close();
                    }
                }
            });
        } catch (IllegalArgumentException e) {
            mainHandler.post(() -> callback.onFailure("Invalid Supabase URL."));
        }
    }

    /**
     * Generic Delete (DELETE)
     */
    public static void delete(String table, String query, DatabaseCallback<Void> callback) {
        String baseUrl = getBaseUrl();
        if (baseUrl == null || baseUrl.isEmpty()) {
            mainHandler.post(() -> callback.onFailure("Supabase URL is not configured."));
            return;
        }
        
        String url = baseUrl + "/rest/v1/" + table + "?" + query;

        try {
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("apikey", SupabaseConfig.SUPABASE_KEY != null ? SupabaseConfig.SUPABASE_KEY : "")
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
                        String errorMessage = parseDatabaseError(response.code(), body);
                        mainHandler.post(() -> callback.onFailure(errorMessage));
                    }
                    if (response.body() != null) response.close();
                }
            });
        } catch (IllegalArgumentException e) {
            mainHandler.post(() -> callback.onFailure("Invalid Supabase URL."));
        }
    }

    /**
     * Call RPC Function (POST /rpc/func)
     */
    public static void rpc(String functionName, Map<String, Object> params, DatabaseCallback<String> callback) {
        String baseUrl = getBaseUrl();
        if (baseUrl == null || baseUrl.isEmpty()) {
            mainHandler.post(() -> callback.onFailure("Supabase URL is not configured."));
            return;
        }
        
        String url = baseUrl + "/rest/v1/rpc/" + functionName;
        String jsonStr = gson.toJson(params);

        try {
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("apikey", SupabaseConfig.SUPABASE_KEY != null ? SupabaseConfig.SUPABASE_KEY : "")
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
                            String errorMessage = parseDatabaseError(response.code(), body);
                            mainHandler.post(() -> callback.onFailure(errorMessage));
                        }
                    } catch (Exception e) {
                        mainHandler.post(() -> callback.onFailure("Parsing error: " + e.getMessage()));
                    } finally {
                        if (response.body() != null) response.close();
                    }
                }
            });
        } catch (IllegalArgumentException e) {
            mainHandler.post(() -> callback.onFailure("Invalid Supabase URL."));
        }
    }
    /**
     * Call Supabase Edge Function (POST /functions/v1/name)
     */
    public static void callEdgeFunction(String functionName, Map<String, Object> params, DatabaseCallback<String> callback) {
        String baseUrl = getBaseUrl();
        if (baseUrl == null || baseUrl.isEmpty()) {
            mainHandler.post(() -> callback.onFailure("Supabase URL is not configured."));
            return;
        }
        
        String url = baseUrl + "/functions/v1/" + functionName;
        String jsonStr = gson.toJson(params);

        try {
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("apikey", SupabaseConfig.SUPABASE_KEY != null ? SupabaseConfig.SUPABASE_KEY : "")
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
                            String errorMessage = parseDatabaseError(response.code(), body);
                            mainHandler.post(() -> callback.onFailure(errorMessage));
                        }
                    } catch (Exception e) {
                        mainHandler.post(() -> callback.onFailure("Parsing error: " + e.getMessage()));
                    } finally {
                        if (response.body() != null) response.close();
                    }
                }
            });
        } catch (IllegalArgumentException e) {
            mainHandler.post(() -> callback.onFailure("Invalid Supabase URL."));
        }
    }
}
