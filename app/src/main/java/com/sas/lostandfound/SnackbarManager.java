package com.sas.lostandfound;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.material.card.MaterialCardView;

import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.Queue;

public class SnackbarManager implements Application.ActivityLifecycleCallbacks {

    private static final int COLOR_SUCCESS = android.graphics.Color.parseColor("#10B981");
    private static final int COLOR_ERROR = android.graphics.Color.parseColor("#EF4444");
    private static final int COLOR_WARNING = android.graphics.Color.parseColor("#F59E0B");
    private static final int COLOR_INFO = android.graphics.Color.parseColor("#3B82F6");
    private static final int COLOR_GENERAL = android.graphics.Color.parseColor("#1F2937");

    public enum Type {
        PRIMARY, SUCCESS, ERROR, WARNING, GENERAL
    }

    private static class SnackbarRequest {
        Type type;
        String message;
        String actionLabel;
        Runnable actionCallback;

        SnackbarRequest(Type type, String message, String actionLabel, Runnable actionCallback) {
            this.type = type;
            this.message = message;
            this.actionLabel = actionLabel;
            this.actionCallback = actionCallback;
        }
    }

    private static SnackbarManager instance;
    private final LinkedList<SnackbarRequest> snackbarQueue = new LinkedList<>();
    private boolean isShowing = false;
    private WeakReference<Activity> currentActivityRef;
    private View currentSnackbarView;
    private SnackbarRequest currentRequest;
    private long showStartTime;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable dismissRunnable;
    private long remainingTime = 4000;
    private long startTime;

    private SnackbarManager() {}

    public static synchronized SnackbarManager getInstance() {
        if (instance == null) {
            instance = new SnackbarManager();
        }
        return instance;
    }

    public static void show(Type type, String message) {
        show(type, message, null, null);
    }

    public static void show(Type type, String message, String actionLabel, Runnable actionCallback) {
        getInstance().enqueue(new SnackbarRequest(type, message, actionLabel, actionCallback));
    }

    private void enqueue(SnackbarRequest request) {
        handler.post(() -> {
            snackbarQueue.add(request);
            processQueue();
        });
    }

    private void processQueue() {
        if (isShowing || snackbarQueue.isEmpty() || currentActivityRef == null || currentActivityRef.get() == null) return;

        Activity activity = currentActivityRef.get();
        if (activity.isFinishing() || activity.isDestroyed()) {
            return;
        }

        isShowing = true;
        SnackbarRequest request = snackbarQueue.poll();
        if (request != null) {
            displaySnackbar(request);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void displaySnackbar(SnackbarRequest request) {
        Activity activity = currentActivityRef.get();
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            isShowing = false;
            snackbarQueue.addFirst(request);
            return;
        }

        ViewGroup rootView = activity.findViewById(android.R.id.content);
        if (rootView == null) {
            isShowing = false;
            snackbarQueue.addFirst(request);
            return;
        }
        LayoutInflater inflater = LayoutInflater.from(activity);
        currentSnackbarView = inflater.inflate(R.layout.layout_custom_snackbar, rootView, false);

        MaterialCardView cardView = currentSnackbarView.findViewById(R.id.snackbarCard);
        ImageView ivIcon = currentSnackbarView.findViewById(R.id.ivIcon);
        TextView tvMessage = currentSnackbarView.findViewById(R.id.tvMessage);
        TextView tvAction = currentSnackbarView.findViewById(R.id.tvAction);
        ImageView btnClose = currentSnackbarView.findViewById(R.id.btnClose);

        tvMessage.setText(request.message);

        int backgroundColor;
        int iconRes;

        switch (request.type) {
            case SUCCESS:
                backgroundColor = COLOR_SUCCESS;
                iconRes = R.drawable.ic_check_circle;
                break;
            case ERROR:
                backgroundColor = COLOR_ERROR;
                iconRes = R.drawable.ic_alert_circle;
                break;
            case WARNING:
                backgroundColor = COLOR_WARNING;
                iconRes = R.drawable.ic_warning;
                break;
            case PRIMARY:
                backgroundColor = COLOR_INFO;
                iconRes = R.drawable.ic_info;
                break;
            case GENERAL:
            default:
                backgroundColor = COLOR_GENERAL;
                iconRes = R.drawable.ic_info;
                break;
        }

        cardView.setCardBackgroundColor(android.content.res.ColorStateList.valueOf(backgroundColor));
        ivIcon.setImageResource(iconRes);

        if (request.actionLabel != null && request.actionCallback != null) {
            tvAction.setVisibility(View.VISIBLE);
            tvAction.setText(request.actionLabel);
            
            // Programmatically apply a rounded capsule background for a modern chip look
            android.graphics.drawable.GradientDrawable actionBg = new android.graphics.drawable.GradientDrawable();
            actionBg.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
            actionBg.setCornerRadius(100);
            actionBg.setColor(android.graphics.Color.parseColor("#33FFFFFF")); // 20% transparent white
            tvAction.setBackground(actionBg);

            tvAction.setOnClickListener(v -> {
                request.actionCallback.run();
                dismiss();
            });
        }

        btnClose.setOnClickListener(v -> dismiss());

        // Calculate status bar height dynamically to avoid phone status bar / notches
        int statusBarHeight = 0;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            android.view.WindowInsets insets = activity.getWindow().getDecorView().getRootWindowInsets();
            if (insets != null) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    statusBarHeight = insets.getInsets(android.view.WindowInsets.Type.statusBars()).top;
                } else {
                    statusBarHeight = insets.getSystemWindowInsetTop();
                }
            }
        }
        if (statusBarHeight == 0) {
            int resourceId = activity.getResources().getIdentifier("status_bar_height", "dimen", "android");
            if (resourceId > 0) {
                statusBarHeight = activity.getResources().getDimensionPixelSize(resourceId);
            }
        }
        if (statusBarHeight == 0) {
            statusBarHeight = (int) (24 * activity.getResources().getDisplayMetrics().density);
        }

        // Apply margins programmatically
        int margin16 = (int) (16 * activity.getResources().getDisplayMetrics().density);
        android.widget.FrameLayout.LayoutParams lp = new android.widget.FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        lp.leftMargin = margin16;
        lp.rightMargin = margin16;
        lp.topMargin = statusBarHeight + margin16;
        lp.bottomMargin = margin16;
        currentSnackbarView.setLayoutParams(lp);

        currentRequest = request;
        showStartTime = System.currentTimeMillis();

        rootView.addView(currentSnackbarView);

        // Slide in animation from top with a premium Overshoot spring effect
        currentSnackbarView.setTranslationY(-500);
        currentSnackbarView.animate()
                .translationY(0)
                .setDuration(450)
                .setInterpolator(new android.view.animation.OvershootInterpolator(1.2f))
                .start();

        startDismissTimer(4000);

        // Pause on touch
        currentSnackbarView.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    pauseDismissTimer();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.performClick();
                    resumeDismissTimer();
                    break;
            }
            return true;
        });
    }

    private void startDismissTimer(long duration) {
        remainingTime = duration;
        startTime = System.currentTimeMillis();
        dismissRunnable = this::dismiss;
        handler.postDelayed(dismissRunnable, remainingTime);
    }

    private void pauseDismissTimer() {
        handler.removeCallbacks(dismissRunnable);
        long elapsed = System.currentTimeMillis() - startTime;
        remainingTime -= elapsed;
        if (remainingTime < 0) remainingTime = 0;
    }

    private void resumeDismissTimer() {
        startTime = System.currentTimeMillis();
        handler.postDelayed(dismissRunnable, remainingTime);
    }

    private void dismiss() {
        if (currentSnackbarView == null || !isShowing) return;

        currentRequest = null;
        handler.removeCallbacks(dismissRunnable);

        currentSnackbarView.animate()
                .translationY(-500)
                .setDuration(350)
                .setInterpolator(new AccelerateInterpolator(1.5f))
                .withEndAction(() -> {
                    ViewGroup rootView = (ViewGroup) currentSnackbarView.getParent();
                    if (rootView != null) {
                        rootView.removeView(currentSnackbarView);
                    }
                    currentSnackbarView = null;
                    isShowing = false;
                    processQueue();
                })
                .start();
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {}

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        this.currentActivityRef = new WeakReference<>(activity);
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        this.currentActivityRef = new WeakReference<>(activity);
        handler.post(this::processQueue);
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {
        if (this.currentActivityRef != null && this.currentActivityRef.get() == activity) {
            if (isShowing && currentRequest != null) {
                long elapsed = System.currentTimeMillis() - showStartTime;
                if (elapsed < 2000) {
                    snackbarQueue.addFirst(currentRequest);
                }
            }
            if (currentSnackbarView != null) {
                try {
                    android.view.ViewParent parent = currentSnackbarView.getParent();
                    if (parent instanceof ViewGroup) {
                        ((ViewGroup) parent).removeView(currentSnackbarView);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                currentSnackbarView = null;
            }
            isShowing = false;
            currentRequest = null;
            handler.removeCallbacks(dismissRunnable);
            this.currentActivityRef = null;
        }
    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {}

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {}

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
        if (this.currentActivityRef != null && this.currentActivityRef.get() == activity) {
            this.currentActivityRef = null;
        }
        if (currentSnackbarView != null) {
            try {
                android.content.Context context = currentSnackbarView.getContext();
                boolean isLinked = false;
                android.content.Context current = context;
                while (current instanceof android.content.ContextWrapper) {
                    if (current == activity) {
                        isLinked = true;
                        break;
                    }
                    current = ((android.content.ContextWrapper) current).getBaseContext();
                }
                if (current == activity) {
                    isLinked = true;
                }
                if (isLinked) {
                    currentSnackbarView = null;
                    isShowing = false;
                    currentRequest = null;
                }
            } catch (Exception e) {
                e.printStackTrace();
                currentSnackbarView = null;
                isShowing = false;
                currentRequest = null;
            }
        }
    }
}
