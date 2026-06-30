package com.sas.lostandfound;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SupabaseAuthHelper {

    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build();

    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    public interface AuthCallback {
        void onSuccess(String userId, String accessToken, String refreshToken);
        void onFailure(String errorMessage);
    }

    public static void login(String email, String password, AuthCallback callback) {
        String baseUrl = sanitizeUrl(SupabaseConfig.SUPABASE_URL);
        String url = baseUrl + "/auth/v1/token?grant_type=password";

        try {
            JSONObject json = new JSONObject();
            json.put("email", email);
            json.put("password", password);

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("apikey", SupabaseConfig.SUPABASE_KEY)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(json.toString(), JSON))
                    .build();

            enqueueRequest(request, callback);

        } catch (Exception e) {
            callback.onFailure("Something went wrong. Please try again.");
        }
    }

    public static void refreshSession(String refreshToken, AuthCallback callback) {
        String baseUrl = sanitizeUrl(SupabaseConfig.SUPABASE_URL);
        String url = baseUrl + "/auth/v1/token?grant_type=refresh_token";

        try {
            JSONObject json = new JSONObject();
            json.put("refresh_token", refreshToken);

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("apikey", SupabaseConfig.SUPABASE_KEY)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(json.toString(), JSON))
                    .build();

            enqueueRequest(request, callback);

        } catch (Exception e) {
            callback.onFailure("Session renewal failed. Please log in again.");
        }
    }

    /**
     * Synchronous version of refreshSession for use in OkHttp Interceptors.
     * Returns a JSON string containing the new tokens or null if failed.
     */
    public static String refreshSessionSync(String refreshToken) {
        String baseUrl = sanitizeUrl(SupabaseConfig.SUPABASE_URL);
        String url = baseUrl + "/auth/v1/token?grant_type=refresh_token";

        try {
            JSONObject json = new JSONObject();
            json.put("refresh_token", refreshToken);

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("apikey", SupabaseConfig.SUPABASE_KEY)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(json.toString(), JSON))
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    return response.body().string();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void signUp(String email, String password, AuthCallback callback) {
        String baseUrl = sanitizeUrl(SupabaseConfig.SUPABASE_URL);
        String url = baseUrl + "/auth/v1/signup";

        try {
            JSONObject json = new JSONObject();
            json.put("email", email);
            json.put("password", password);

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("apikey", SupabaseConfig.SUPABASE_KEY)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(json.toString(), JSON))
                    .build();

            enqueueRequest(request, callback);

        } catch (Exception e) {
            callback.onFailure("Something went wrong. Please try again.");
        }
    }

    public static void updateUserPassword(String currentPassword, String newPassword, AuthCallback callback) {
        String baseUrl = sanitizeUrl(SupabaseConfig.SUPABASE_URL);
        String url = baseUrl + "/auth/v1/user";

        try {
            JSONObject json = new JSONObject();
            json.put("password", newPassword);
            if (currentPassword != null && !currentPassword.isEmpty()) {
                json.put("current_password", currentPassword);
            }

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("apikey", SupabaseConfig.SUPABASE_KEY)
                    .addHeader("Authorization", "Bearer " + SupabaseDatabaseHelper.getAuthToken())
                    .addHeader("Content-Type", "application/json")
                    .put(RequestBody.create(json.toString(), JSON))
                    .build();

            enqueueRequest(request, callback);

        } catch (Exception e) {
            callback.onFailure("Password update failed. Please try again.");
        }
    }

    public static void signOut() {
        // Clear the static auth token to ensure security after logout
        SupabaseDatabaseHelper.setAuthToken(null);
    }

    private static String sanitizeUrl(String url) {
        if (url == null) return "";
        if (url.endsWith("/")) {
            return url.substring(0, url.length() - 1);
        }
        return url;
    }

    private static void enqueueRequest(Request request, AuthCallback callback) {
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                String message;
                if (e instanceof SocketTimeoutException) {
                    message = "Connection timeout. Please try again.";
                } else if (e instanceof UnknownHostException) {
                    message = "No internet connection.";
                } else if (e instanceof ConnectException) {
                    message = "Unable to connect to server.";
                } else if (e instanceof SSLException) {
                    message = "Secure connection failed.";
                } else if (e instanceof IOException) {
                    message = "Network error. Please check your connection.";
                } else {
                    message = "Unexpected error occurred.";
                }

                String finalMessage = message;
                mainHandler.post(() -> callback.onFailure(finalMessage));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = "";
                try {
                    if (response.body() != null) {
                        responseBody = response.body().string();
                    }

                    if (response.isSuccessful()) {
                        try {
                            JSONObject json = new JSONObject(responseBody);
                            
                            // SignUp might return different structure than Login
                            String userId = "";
                            String accessToken = "";
                            String refreshToken = "";

                            if (json.has("user")) {
                                userId = json.getJSONObject("user").optString("id", "");
                            } else if (json.has("id")) {
                                userId = json.optString("id", "");
                            }
                            
                            if (json.has("access_token")) {
                                accessToken = json.getString("access_token");
                            }

                            if (json.has("refresh_token")) {
                                refreshToken = json.getString("refresh_token");
                            }

                            if (userId.isEmpty()) {
                                String finalResponseBody = responseBody;
                                mainHandler.post(() ->
                                        callback.onFailure("Success but no ID found. Body: " + finalResponseBody)
                                );
                            } else {
                                String finalUserId = userId;
                                String finalToken = accessToken;
                                String finalRefreshToken = refreshToken;
                                mainHandler.post(() -> callback.onSuccess(finalUserId, finalToken, finalRefreshToken));
                            }
                        } catch (Exception e) {
                            String finalResponseBody1 = responseBody;
                            mainHandler.post(() ->
                                    callback.onFailure("JSON Parsing error: " + e.getMessage() + ". Body: " + finalResponseBody1)
                            );
                        }
                    } else {
                        String errorMessage = parseError(response.code(), responseBody);
                        mainHandler.post(() -> callback.onFailure(errorMessage));
                    }
                } catch (Exception e) {
                    mainHandler.post(() ->
                            callback.onFailure("Unexpected error: " + e.getMessage())
                    );
                } finally {
                    if (response.body() != null) {
                        response.close();
                    }
                }
            }
        });
    }

    private static String parseError(int statusCode, String responseBody) {
        try {
            if (!responseBody.isEmpty()) {
                JSONObject json = new JSONObject(responseBody);
                String message = json.optString("error_description",
                        json.optString("error",
                                json.optString("msg",
                                        json.optString("message", ""))));

                if (!message.isEmpty()) {
                    String lower = message.toLowerCase();
                    if (lower.contains("invalid login credentials") ||
                            lower.contains("invalid email or password")) {
                        return "Invalid email or password.";
                    }
                    if (lower.contains("email not confirmed")) {
                        return "Please verify your email before logging in.";
                    }
                    if (lower.contains("user not found")) {
                        return "Account does not exist.";
                    }
                    if (lower.contains("password should be")) {
                        return "Password does not meet requirements.";
                    }
                    if (lower.contains("already registered") ||
                            lower.contains("user already exists")) {
                        return "This email is already registered.";
                    }
                    if (lower.contains("weak password")) {
                        return "Password is too weak.";
                    }
                    return message;
                }
            }
        } catch (Exception ignored) {}

        if (statusCode >= 500) {
            return "Server error. Please try again later.";
        }
        if (statusCode == 401 || statusCode == 400) {
            return "Invalid credentials. Please try again.";
        }
        if (statusCode == 403) {
            return "Access denied.";
        }
        if (statusCode == 404) {
            return "Service not found.";
        }
        return "Authentication error. Please try again.";
    }

    public static void cancelAllCalls() {
        try {
            if (client != null) {
                client.dispatcher().cancelAll();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
