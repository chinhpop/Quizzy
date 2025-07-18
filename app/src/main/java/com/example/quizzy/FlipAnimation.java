package com.example.quizzy;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

public class FlipAnimation {
    public static void flipCard(View cardView, View frontView, View backView, boolean showBack) {
        AccelerateDecelerateInterpolator interpolator = new AccelerateDecelerateInterpolator();
        float currentAngle = cardView.getRotationY();
        float endAngle = showBack ? 180f : 0f;

        ObjectAnimator animator = ObjectAnimator.ofFloat(cardView, "rotationY", currentAngle, endAngle);
        animator.setDuration(400);
        animator.setInterpolator(interpolator);

        animator.addUpdateListener(animation -> {
            float fraction = animation.getAnimatedFraction();
            if (fraction >= 0.5f) {
                frontView.setVisibility(showBack ? View.GONE : View.VISIBLE);
                backView.setVisibility(showBack ? View.VISIBLE : View.GONE);
            }
        });

        animator.start();
    }
}