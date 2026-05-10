package com.sas.lostandfound;

import android.content.Context;
import android.graphics.Color;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

/**
 * Utility class to improve error message visibility across the app.
 * Provides interactive ways to view full error details.
 */
public class ErrorHelper {

    /**
     * Shows a Snackbar with a "VIEW" action to see the full error message in a dialog.
     * Use this to replace Toast for error messages.
     */
    public static void showError(View view, String message) {
        if (message == null || message.isEmpty()) return;

        SnackbarManager.show(SnackbarManager.Type.ERROR, message, "VIEW", () -> {
            if (view != null) {
                showFullErrorDialog(view.getContext(), message);
            }
        });
    }

    /**
     * Shows a Snackbar with a "VIEW" action to see the full error message in a dialog.
     * Uses a string resource for the message.
     */
    public static void showError(View view, int messageResId) {
        if (view == null) return;
        showError(view, view.getContext().getString(messageResId));
    }

    /**
     * Sets an error on a TextInputLayout, changes its background to a light red tint,
     * and shows a top-sliding snackbar with the error message.
     */
    public static void setFieldError(TextInputLayout til, String message) {
        if (til == null) return;
        
        til.setError(message);
        til.setBoxBackgroundColor(ContextCompat.getColor(til.getContext(), R.color.error_light_bg));
        
        // Show top snackbar feedback
        SnackbarManager.show(SnackbarManager.Type.ERROR, message);
        
        // Focus the field for better UX
        if (til.getEditText() != null) {
            til.getEditText().requestFocus();
        }
    }

    /**
     * Clears the error from a TextInputLayout and resets its background.
     */
    public static void clearFieldError(TextInputLayout til) {
        if (til == null) return;
        til.setError(null);
        til.setBoxBackgroundColor(Color.TRANSPARENT);
    }

    /**
     * Attaches a click listener to a TextInputLayout so that tapping the error
     * shows the full error message in a dialog. Also adds a TextWatcher to
     * clear the error state when the user starts typing.
     */
    public static void attachToTextInputLayout(TextInputLayout til) {
        if (til == null) return;

        View.OnClickListener clickListener = v -> {
            CharSequence error = til.getError();
            if (error != null && !error.toString().isEmpty()) {
                showFullErrorDialog(til.getContext(), error.toString());
            }
        };

        if (til.getEditText() != null) {
            til.getEditText().setOnLongClickListener(v -> {
                CharSequence error = til.getError();
                if (error != null && !error.toString().isEmpty()) {
                    showFullErrorDialog(til.getContext(), error.toString());
                    return true;
                }
                return false;
            });

            // Real-time consistency: Clear error when user starts typing
            til.getEditText().addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (til.getError() != null) {
                        clearFieldError(til);
                    }
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }
        
        til.setOnClickListener(clickListener);
    }

    /**
     * Displays a stylish custom alert dialog with the full error message.
     */
    public static void showFullErrorDialog(Context context, String fullMessage) {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_error_details, null);
        
        TextView tvMessage = dialogView.findViewById(R.id.tvErrorMessage);
        MaterialButton btnClose = dialogView.findViewById(R.id.btnClose);
        
        tvMessage.setText(fullMessage);
        
        AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().getAttributes().windowAnimations = android.R.style.Animation_Dialog;
        }

        btnClose.setOnClickListener(v -> dialog.dismiss());
        
        dialog.show();
    }
}
