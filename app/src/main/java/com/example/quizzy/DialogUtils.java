package com.example.quizzy;

import android.content.Context;
import android.content.DialogInterface;

import androidx.appcompat.app.AlertDialog;

public class DialogUtils {

    public static void showConfirmationDialog(
            Context context,
            String title,
            String message,
            String positiveButtonText,
            String negativeButtonText,
            DialogInterface.OnClickListener positiveListener,
            DialogInterface.OnClickListener negativeListener) {
        new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(positiveButtonText, positiveListener)
                .setNegativeButton(negativeButtonText, negativeListener)
                .setCancelable(false)
                .show();
    }

    // Overloaded method with default button texts
    public static void showConfirmationDialog(
            Context context,
            String title,
            String message,
            DialogInterface.OnClickListener positiveListener,
            DialogInterface.OnClickListener negativeListener) {
        showConfirmationDialog(context, title, message, "OK", "Cancel", positiveListener, negativeListener);
    }
}