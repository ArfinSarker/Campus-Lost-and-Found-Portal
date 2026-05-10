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
    private final Queue<SnackbarRequest> snackbarQueue = new LinkedList<>();
    private boolean isShowing = false;
    private WeakReference<Activity> currentActivityRef;
    private View currentSnackbarView;
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

        isShowing = true;
        SnackbarRequest request = snackbarQueue.poll();
        if (request != null) {
            displaySnackbar(request);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void displaySnackbar(SnackbarRequest request) {
        Activity activity = currentActivityRef.get();
        if (activity == null) {
            isShowing = false;
            return;
        }

        ViewGroup rootView = activity.findViewById(android.R.id.content);
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
                backgroundColor = R.color.success;
                iconRes = R.drawable.ic_check_circle;
                break;
            case ERROR:
                backgroundColor = R.color.error;
                iconRes = R.drawable.ic_error_outline;
                break;
            case WARNING:
                backgroundColor = R.color.warningColor;
                iconRes = R.drawable.ic_warning;
                break;
            case PRIMARY:
                backgroundColor = R.color.primaryColor;
                iconRes = R.drawable.ic_info;
                break;
            case GENERAL:
            default:
                backgroundColor = R.color.generalColor;
                iconRes = R.drawable.ic_info;
                break;
        }

        cardView.setCardBackgroundColor(ContextCompat.getColor(activity, backgroundColor));
        ivIcon.setImageResource(iconRes);

        if (request.actionLabel != null && request.actionCallback != null) {
            tvAction.setVisibility(View.VISIBLE);
            tvAction.setText(request.actionLabel);
            tvAction.setOnClickListener(v -> {
                request.actionCallback.run();
                dismiss();
            });
        }

        btnClose.setOnClickListener(v -> dismiss());

        rootView.addView(currentSnackbarView);

        // Slide in animation from top
        currentSnackbarView.setTranslationY(-300);
        currentSnackbarView.animate()
                .translationY(0)
                .setDuration(300)
                .setInterpolator(new DecelerateInterpolator())
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

        handler.removeCallbacks(dismissRunnable);

        currentSnackbarView.animate()
                .translationY(-300)
                .setDuration(300)
                .setInterpolator(new AccelerateInterpolator())
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
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {
        if (this.currentActivityRef != null && this.currentActivityRef.get() == activity) {
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
    }
}
