package com.example.xyzreader.ui;

import android.annotation.TargetApi;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.loader.app.LoaderManager;
import androidx.core.app.SharedElementCallback;
import androidx.loader.content.Loader;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.ImageView;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;
import com.example.xyzreader.pagetransformer.CubeOutPageTransformer;
import com.example.xyzreader.pagetransformer.DepthPageTransformer;
import com.example.xyzreader.pagetransformer.PopPageTransformer;
import com.example.xyzreader.pagetransformer.ZoomOutPageTransformer;

import java.util.List;
import java.util.Map;

import static com.example.xyzreader.ui.ArticleListActivity.EXTRA_CURRENT_POSITION;
import static com.example.xyzreader.ui.ArticleListActivity.EXTRA_PAGE_TRANSFORMATION;
import static com.example.xyzreader.ui.ArticleListActivity.EXTRA_STARTING_POSITION;
import static com.example.xyzreader.ui.ArticleListActivity.XYZ_LOADER_ID;

public class ArticleDetailActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String STATE_CURRENT_PAGE_POSITION = "state_current_page_position";
    public static final String POP = "pop";
    public static final String ZOOM = "zoom";
    public static final String DEPTH = "depth";
    public static final String CUBE = "cube";
    public static final String SMALL = "small";
    public static final String MEDIUM = "medium";
    public static final String LARGE = "large";
    public static final String EXTRA_LARGE = "extra_large";

    public static final int ANIM_DURATION = 300;

    private Cursor mCursor;
    private long mStartId;

    private long mSelectedItemId;
    private int mSelectedItemUpButtonFloor = Integer.MAX_VALUE;
    private int mTopInset;

    private ViewPager mPager;
    private MyPagerAdapter mPagerAdapter;
    private View mUpButtonContainer;
    private View mUpButton;

    private int mCurrentPosition;
    private int mStartingPosition;
    private boolean mIsReturning;
    private ArticleDetailFragment mCurrentDetailFragment;
    private ViewPager.PageTransformer mPageTransformer;
    private String mPageTransformerStr;

    private final SharedElementCallback mCallback = new SharedElementCallback() {
        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
            if (mIsReturning) {
                ImageView sharedElement = mCurrentDetailFragment.getPhotoView();
                if (sharedElement == null) {
                    names.clear();
                    sharedElements.clear();
                } else if (mStartingPosition != mCurrentPosition) {
                    names.clear();
                    names.add(sharedElement.getTransitionName());
                    sharedElements.clear();
                    sharedElements.put(sharedElement.getTransitionName(), sharedElement);
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            ActivityCompat.postponeEnterTransition(this);
            setEnterSharedElementCallback(mCallback);
        }
        setContentView(R.layout.activity_article_detail);
        mStartingPosition = getIntent().getIntExtra(EXTRA_STARTING_POSITION, 0);
        if (savedInstanceState == null) {
            mCurrentPosition = mStartingPosition;
        } else {
            mCurrentPosition = savedInstanceState.getInt(STATE_CURRENT_PAGE_POSITION);
        }
        mPageTransformerStr = getIntent().getStringExtra(EXTRA_PAGE_TRANSFORMATION);

        getSupportLoaderManager().initLoader(XYZ_LOADER_ID, null, this);

        mPagerAdapter = new MyPagerAdapter(getSupportFragmentManager());
        mPager = findViewById(R.id.pager);
        mPager.setAdapter(mPagerAdapter);
        mPager.setPageMargin((int) TypedValue
                .applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, getResources().getDisplayMetrics()));
        mPager.setPageMarginDrawable(new ColorDrawable(0x22000000));
        mPager.setCurrentItem(mCurrentPosition);
        mPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageScrollStateChanged(int state) {
                super.onPageScrollStateChanged(state);
                mUpButton.animate()
                        .alpha((state == ViewPager.SCROLL_STATE_IDLE) ? 1f : 0f)
                        .setDuration(ANIM_DURATION);
            }

            @Override
            public void onPageSelected(int position) {
                if (mCursor != null) {
                    mCursor.moveToPosition(position);
                }
                mSelectedItemId = mCursor.getLong(ArticleLoader.Query._ID);
                updateUpButtonPosition();
                mCurrentPosition = position;
            }
        });

        mUpButtonContainer = findViewById(R.id.up_container);

        mUpButton = findViewById(R.id.action_up);
        mUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                supportFinishAfterTransition();
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mUpButtonContainer.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
                @Override
                public WindowInsets onApplyWindowInsets(View view, WindowInsets windowInsets) {
                    view.onApplyWindowInsets(windowInsets);
                    mTopInset = windowInsets.getSystemWindowInsetTop();
                    mUpButtonContainer.setTranslationY(mTopInset);
                    updateUpButtonPosition();
                    return windowInsets;
                }
            });
        }

        if (savedInstanceState == null) {
            if (getIntent() != null && getIntent().getData() != null) {
                mStartId = ItemsContract.Items.getItemId(getIntent().getData());
                mSelectedItemId = mStartId;
            }
        }
    }

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newAllArticlesInstance(this);
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> cursorLoader, Cursor cursor) {
        mCursor = cursor;
        mPagerAdapter.notifyDataSetChanged();
        mPager.setCurrentItem(mCurrentPosition, false);
        mCursor.moveToPosition(mCurrentPosition);
        mPager.setPageTransformer(true, getPageTransformer());
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> cursorLoader) {
        mCursor = null;
        mPagerAdapter.notifyDataSetChanged();
    }

    public void onUpButtonFloorChanged(long itemId, ArticleDetailFragment fragment) {
        if (itemId == mSelectedItemId) {
            mSelectedItemUpButtonFloor = fragment.getUpButtonFloor();
            updateUpButtonPosition();
        }
    }

    private void updateUpButtonPosition() {
        int upButtonNormalBottom = mTopInset + mUpButton.getHeight();
        mUpButton.setTranslationY(Math.min(mSelectedItemUpButtonFloor - upButtonNormalBottom, 0));
    }
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_CURRENT_PAGE_POSITION, mCurrentPosition);
    }
    @Override
    public void finishAfterTransition() {
        mIsReturning = true;
        Intent data = new Intent();
        data.putExtra(EXTRA_STARTING_POSITION, mStartingPosition);
        data.putExtra(EXTRA_CURRENT_POSITION, mCurrentPosition);
        setResult(RESULT_OK, data);
        super.finishAfterTransition();
    }
    public ViewPager.PageTransformer getPageTransformer() {
        if (!TextUtils.isEmpty(mPageTransformerStr)) {
            switch (mPageTransformerStr) {
                case POP:
                    mPageTransformer = new PopPageTransformer();
                    break;
                case ZOOM:
                    mPageTransformer = new ZoomOutPageTransformer();
                    break;
                case DEPTH:
                    mPageTransformer = new DepthPageTransformer();
                    break;
                case CUBE:
                    mPageTransformer = new CubeOutPageTransformer();
                    break;
                default:
                    mPageTransformer = new PopPageTransformer();
            }
        }
        return mPageTransformer;
    }

    private class MyPagerAdapter extends FragmentStatePagerAdapter {
        public MyPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public void setPrimaryItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
            super.setPrimaryItem(container, position, object);
            mCurrentDetailFragment = (ArticleDetailFragment) object;
            if (mCurrentDetailFragment != null) {
                mSelectedItemUpButtonFloor = mCurrentDetailFragment.getUpButtonFloor();
                updateUpButtonPosition();
            }
        }

        @Override
        public Fragment getItem(int position) {
            mCursor.moveToPosition(position);
            return ArticleDetailFragment.newInstance(mCursor.getLong(ArticleLoader.Query._ID), position, mStartingPosition);
        }

        @Override
        public int getCount() {
            return (mCursor != null) ? mCursor.getCount() : 0;
        }
    }
}
