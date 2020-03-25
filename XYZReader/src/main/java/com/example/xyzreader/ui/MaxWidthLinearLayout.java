

package com.example.xyzreader.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.LinearLayout;

public class MaxWidthLinearLayout extends LinearLayout {
    private static final int[] ATTRS = {
            android.R.attr.maxWidth
    };

    private int mMaxWidth = Integer.MAX_VALUE;

    public MaxWidthLinearLayout(Context context) {
        super(context);
        init(context, null, 0, 0);
    }

    public MaxWidthLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0, 0);
    }

    public MaxWidthLinearLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs, defStyle, 0);
    }

    public MaxWidthLinearLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs, defStyleAttr, defStyleRes);
    }

    public void init(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        TypedArray a = context.obtainStyledAttributes(attrs, ATTRS);
        mMaxWidth = a.getLayoutDimension(0, Integer.MAX_VALUE);
        a.recycle();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int newSpecWidth = Math.min(MeasureSpec.getSize(widthMeasureSpec), mMaxWidth);
        widthMeasureSpec = MeasureSpec.makeMeasureSpec(newSpecWidth, MeasureSpec.getMode(widthMeasureSpec));
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }
}