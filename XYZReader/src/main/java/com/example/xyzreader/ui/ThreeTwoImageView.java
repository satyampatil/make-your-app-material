package com.example.xyzreader.ui;

import android.content.Context;
import androidx.appcompat.widget.AppCompatImageView;
import android.util.AttributeSet;

public class ThreeTwoImageView extends AppCompatImageView {

    private static final int TWO = 2;
    private static final int THREE = 3;

    public ThreeTwoImageView(Context context) {
        super(context);
    }

    public ThreeTwoImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ThreeTwoImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int threeTwoHeight = MeasureSpec.getSize(widthMeasureSpec) * TWO / THREE;
        int threeTwoHeightSpec =
                MeasureSpec.makeMeasureSpec(threeTwoHeight, MeasureSpec.EXACTLY);
        super.onMeasure(widthMeasureSpec, threeTwoHeightSpec);
    }
}