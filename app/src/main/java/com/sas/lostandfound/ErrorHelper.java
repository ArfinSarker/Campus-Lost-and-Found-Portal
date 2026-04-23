package com.sas.lostandfound;

import androidx.appcompat.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;

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
        if (view == null || message == null || message.isEmpty()) return;

        Snackbar snackbar = Snackbar.make(view, message, Snackbar.LENGTH_LONG);
        snackbar.setAction("VIEW", v -> showFullErrorDialog(view.getContext(), message));
        snackbar.show();
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
     * Attaches a click listener to a TextInputLayout so that tapping the error
     * shows the full error message in a dialog.
     */
    public static void attachToTextInputLayout(TextInputLayout til) {
        if (til == null) return;

        // When the error is shown, we want to make the error view clickable
        // Unfortunately, TextInputLayout doesn't expose the error view directly easily
        // But we can listen to error changes or just set a click listener on the TIL itself
        // or the EditText within it.
        
        View.OnClickListener clickListener = v -> {
            CharSequence error = til.getError();
            if (error != null && !error.toString().isEmpty()) {
                showFullErrorDialog(til.getContext(), error.toString());
            }
        };

        if (til.getEditText() != null) {
            // Long click on edit text to show error if it exists
            til.getEditText().setOnLongClickListener(v -> {
                CharSequence error = til.getError();
                if (error != null && !error.toString().isEmpty()) {
                    showFullErrorDialog(til.getContext(), error.toString());
                    return true;
                }
                return false;
            });
        }
        
        // Also try to catch clicks on the TIL itself
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

        // Ensure background is transparent to show the card's rounded corners
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            // Apply a smooth animation
            dialog.getWindow().getAttributes().windowAnimations = android.R.style.Animation_Dialog;
        }

        btnClose.setOnClickListener(v -> dialog.dismiss());
        
        dialog.show();
    }
}
