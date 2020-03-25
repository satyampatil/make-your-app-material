package com.example.xyzreader.pagetransformer;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.ViewPager;
import android.view.View;

import static com.example.xyzreader.pagetransformer.DepthPageTransformer.ALPHA_ONE;
import static com.example.xyzreader.pagetransformer.DepthPageTransformer.ALPHA_ZERO;
import static com.example.xyzreader.pagetransformer.ZoomOutPageTransformer.PIVOT_X;
import static com.example.xyzreader.pagetransformer.ZoomOutPageTransformer.ROTATION_NINETY;
import static com.example.xyzreader.pagetransformer.ZoomOutPageTransformer.ROTATION_NINETY_N;

public class CubeOutPageTransformer implements ViewPager.PageTransformer {

    @Override
    public void transformPage(@NonNull View view, float position) {
        if (position < -1){
            view.setAlpha(ALPHA_ZERO);

        }
        else if (position <= 0) {
            view.setAlpha(ALPHA_ONE);
            view.setPivotX(view.getWidth());
            view.setRotationY(ROTATION_NINETY_N * Math.abs(position));

        }
        else if (position <= 1){
            view.setAlpha(ALPHA_ONE);
            view.setPivotX(PIVOT_X);
            view.setRotationY(ROTATION_NINETY * Math.abs(position));

        }
        else {
            view.setAlpha(ALPHA_ZERO);

        }
    }
}
