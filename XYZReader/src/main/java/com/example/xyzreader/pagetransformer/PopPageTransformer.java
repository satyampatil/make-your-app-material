package com.example.xyzreader.pagetransformer;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.ViewPager;
import android.view.View;

public class PopPageTransformer implements ViewPager.PageTransformer {
    @Override
    public void transformPage(@NonNull View view, float position) {

        view.setTranslationX(-position * view.getWidth());

        if (Math.abs(position) < 0.5) {
            view.setVisibility(View.VISIBLE);
            view.setScaleX(1 - Math.abs(position));
            view.setScaleY(1 - Math.abs(position));
        } else if (Math.abs(position) > 0.5) {
            view.setVisibility(View.GONE);
        }

    }
}
