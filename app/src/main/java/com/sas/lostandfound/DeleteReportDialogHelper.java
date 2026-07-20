package com.sas.lostandfound;

import android.app.Activity;
import android.view.View;
import androidx.appcompat.app.AlertDialog;

public class DeleteReportDialogHelper {
    public interface Callback {
        void onDelete();
    }

    public static void show(Activity activity, Callback callback) {
        View dialogView = activity.getLayoutInflater().inflate(R.layout.dialog_delete_report_confirm, null);
        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        View btnCancel = dialogView.findViewById(R.id.btnCancel);
        View btnDelete = dialogView.findViewById(R.id.btnDelete);

        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }
        if (btnDelete != null) {
            btnDelete.setOnClickListener(v -> {
                dialog.dismiss();
                callback.onDelete();
            });
        }

        dialog.show();
    }
}
