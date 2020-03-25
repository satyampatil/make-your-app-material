
package com.example.xyzreader.ui;

import android.annotation.TargetApi;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.core.app.ShareCompat;
import androidx.loader.content.Loader;
import androidx.palette.graphics.Palette;
import android.text.Html;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

import static com.example.xyzreader.ui.ArticleListActivity.EXTRA_TEXT_SIZE;

public class ArticleDetailFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "ArticleDetailFragment";
    private static final String ARG_IMAGE_POSITION = "arg_image_position";
    private static final String ARG_STARTING_IMAGE_POSITION = "arg_starting_image_position";

    private int mPosition;
    private int mStartingPosition;
    private String mTextSizeStr;

    private static final int START_INDEX = 0;
    private static final int END_INDEX = 1000;

    public static final String ARG_ITEM_ID = "item_id";
    private static final float PARALLAX_FACTOR = 1.25f;

    private Cursor mCursor;
    private long mItemId;
    private View mRootView;
    private int mVibrantColor = 0xFF006F7A;
    private ObservableScrollView mScrollView;
    private DrawInsetsFrameLayout mDrawInsetsFrameLayout;
    private ColorDrawable mStatusBarColorDrawable;

    private int mTopInset;
    private View mPhotoContainerView;
    private ImageView mPhotoView;
    private int mScrollY;
    private boolean mIsCard = false;
    private int mStatusBarFullOpacityBottom;

    private static final String BY_FONT_COLOR = " by <font color='#ffffff'>";
    private static final String FONT = "</font>";
    private static final String REPLACEMENT_BR = "<br />";

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss");
    private SimpleDateFormat outputFormat = new SimpleDateFormat();
    private GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2,1,1);
    private Target mTarget = new Target() {
        @Override
        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
            if (bitmap != null) {
                Palette.from(bitmap).maximumColorCount(12).generate(new Palette.PaletteAsyncListener() {
                    @Override
                    public void onGenerated(Palette palette) {
                        Palette.Swatch vibrant = palette.getVibrantSwatch();
                        if (vibrant != null) {
                            mVibrantColor = vibrant.getRgb();
                            mRootView.findViewById(R.id.meta_bar)
                                    .setBackgroundColor(mVibrantColor);
                        }
                    }
                });
                mPhotoView.setImageBitmap(bitmap);
                updateStatusBar();
            }
        }

        @Override
        public void onBitmapFailed(Drawable errorDrawable) {
            Log.e(TAG, "Failed to load images");
        }

        @Override
        public void onPrepareLoad(Drawable placeHolderDrawable) {
        }
    };
    public ArticleDetailFragment() {
    }

    public static ArticleDetailFragment newInstance(long itemId, int position, int startingPosition) {
        Bundle arguments = new Bundle();
        arguments.putLong(ARG_ITEM_ID, itemId);
        arguments.putInt(ARG_IMAGE_POSITION, position);
        arguments.putInt(ARG_STARTING_IMAGE_POSITION, startingPosition);

        ArticleDetailFragment fragment = new ArticleDetailFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(ARG_ITEM_ID)) {
            mItemId = getArguments().getLong(ARG_ITEM_ID);
        }
        if (getArguments().containsKey(ARG_IMAGE_POSITION)) {
            mPosition = getArguments().getInt(ARG_IMAGE_POSITION);
        }
        if (getArguments().containsKey(ARG_STARTING_IMAGE_POSITION)) {
            mStartingPosition = getArguments().getInt(ARG_STARTING_IMAGE_POSITION);
        }

        mIsCard = getResources().getBoolean(R.bool.detail_is_card);
        mStatusBarFullOpacityBottom = getResources().getDimensionPixelSize(
                R.dimen.detail_card_top_margin);
        setHasOptionsMenu(true);
    }

    public ArticleDetailActivity getActivityCast() {
        return (ArticleDetailActivity) getActivity();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_article_detail, container, false);
        mDrawInsetsFrameLayout = mRootView.findViewById(R.id.draw_insets_frame_layout);
        mDrawInsetsFrameLayout.setOnInsetsCallback(new DrawInsetsFrameLayout.OnInsetsCallback() {
            @Override
            public void onInsetsChanged(Rect insets) {
                mTopInset = insets.top;
            }
        });

        mScrollView = mRootView.findViewById(R.id.scrollview);
        mScrollView.setCallbacks(new ObservableScrollView.Callbacks() {
            @Override
            public void onScrollChanged() {
                mScrollY = mScrollView.getScrollY();
                getActivityCast().onUpButtonFloorChanged(mItemId, ArticleDetailFragment.this);
                mPhotoContainerView.setTranslationY((int) (mScrollY - mScrollY / PARALLAX_FACTOR));
                updateStatusBar();
            }
        });
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            hideFab();
        }

        mTextSizeStr = getActivityCast().getIntent().getStringExtra(EXTRA_TEXT_SIZE);

        mPhotoView = mRootView.findViewById(R.id.photo);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            String transitionName = getString(R.string.transition_photo) + mPosition;
            mPhotoView.setTransitionName(transitionName);
        }

        mPhotoContainerView = mRootView.findViewById(R.id.photo_container);

        mStatusBarColorDrawable = new ColorDrawable(0);

        mRootView.findViewById(R.id.share_fab).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String title = mCursor.getString(ArticleLoader.Query.TITLE);
                String author = mCursor.getString(ArticleLoader.Query.AUTHOR);
                String text = title + getString(R.string.share_text_by) + author;
                startActivity(Intent.createChooser(ShareCompat.IntentBuilder.from(getActivity())
                        .setType("text/plain")
                        .setText(text)
                        .getIntent(), getString(R.string.action_share)));
            }
        });
        bindViews();
        updateStatusBar();
        return mRootView;
    }
    @TargetApi(Build.VERSION_CODES.M)
    private void hideFab() {
        mScrollView.setOnScrollChangeListener(new View.OnScrollChangeListener() {
            @Override
            public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                if (scrollY > oldScrollY) {
                    // Hide FAB
                    mRootView.findViewById(R.id.share_fab).setVisibility(View.INVISIBLE);
                } else {
                    // Show FAB
                    mRootView.findViewById(R.id.share_fab).setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void updateStatusBar() {
        int color = 0;
        if (mPhotoView != null && mTopInset != 0 && mScrollY > 0) {
            float f = progress(mScrollY,
                    mStatusBarFullOpacityBottom - mTopInset * 3,
                    mStatusBarFullOpacityBottom - mTopInset);
            color = Color.argb((int) (255 * f),
                    (int) (Color.red(mVibrantColor) * 0.9),
                    (int) (Color.green(mVibrantColor) * 0.9),
                    (int) (Color.blue(mVibrantColor) * 0.9));
        }
        mStatusBarColorDrawable.setColor(color);
        mDrawInsetsFrameLayout.setInsetBackground(mStatusBarColorDrawable);
    }

    static float progress(float v, float min, float max) {
        return constrain((v - min) / (max - min), 0, 1);
    }

    static float constrain(float val, float min, float max) {
        if (val < min) {
            return min;
        } else if (val > max) {
            return max;
        } else {
            return val;
        }
    }

    private Date parsePublishedDate() {
        try {
            String date = mCursor.getString(ArticleLoader.Query.PUBLISHED_DATE);
            return dateFormat.parse(date);
        } catch (ParseException ex) {
            Log.e(TAG, ex.getMessage());
            Log.i(TAG, "passing today's date");
            return new Date();
        }
    }

    private void bindViews() {
        if (mRootView == null) {
            return;
        }

        TextView titleView = mRootView.findViewById(R.id.article_title);
        TextView bylineView = mRootView.findViewById(R.id.article_byline);
        bylineView.setMovementMethod(new LinkMovementMethod());
        TextView bodyView = mRootView.findViewById(R.id.article_body);

        setBodyTextSize(bodyView);

        setBodyTextSelectable(bodyView);

        if (mCursor != null) {
            mRootView.setAlpha(0);
            mRootView.setVisibility(View.VISIBLE);
            mRootView.animate().alpha(1);
            titleView.setText(mCursor.getString(ArticleLoader.Query.TITLE));
            Date publishedDate = parsePublishedDate();
            if (!publishedDate.before(START_OF_EPOCH.getTime())) {
                bylineView.setText(Html.fromHtml(
                        DateUtils.getRelativeTimeSpanString(
                                publishedDate.getTime(),
                                System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                                DateUtils.FORMAT_ABBREV_ALL).toString()
                                + BY_FONT_COLOR
                                + mCursor.getString(ArticleLoader.Query.AUTHOR)
                                + FONT));

            } else {
                bylineView.setText(Html.fromHtml(
                        outputFormat.format(publishedDate) + BY_FONT_COLOR
                        + mCursor.getString(ArticleLoader.Query.AUTHOR)
                                + FONT));

            }
            bodyView.setText(Html.fromHtml(mCursor.getString(ArticleLoader.Query.BODY)
                    .substring(START_INDEX, END_INDEX).replaceAll("(\r\n|\n)", REPLACEMENT_BR)));
            Picasso.with(getActivity())
                    .load(mCursor.getString(ArticleLoader.Query.PHOTO_URL))
                    .error(R.drawable.photo_error)
                    .into(mTarget);
            scheduleStartPostponedTransition(mPhotoView);
        } else {
            mRootView.setVisibility(View.GONE);
            titleView.setText("N/A");
            bylineView.setText("N/A" );
            bodyView.setText("N/A");
        }
    }

    private void setBodyTextSize(TextView textView) {
        if (mTextSizeStr.equals(getString(R.string.pref_text_size_small))) {
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, getActivityCast()
                    .getResources().getDimension(R.dimen.sp18));
        } else if (mTextSizeStr.equals(getString(R.string.pref_text_size_medium))) {
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, getActivityCast()
                    .getResources().getDimension(R.dimen.sp20));
        } else if (mTextSizeStr.equals(getString(R.string.pref_text_size_large))) {
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, getActivityCast()
                    .getResources().getDimension(R.dimen.sp22));
        } else if (mTextSizeStr.equals(getString(R.string.pref_text_size_extra_large))) {
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, getActivityCast()
                    .getResources().getDimension(R.dimen.sp24));
        }
    }

    private void setBodyTextSelectable(final TextView textView) {
        textView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                textView.setTextIsSelectable(true);
                return false;
            }
        });
    }

    private void scheduleStartPostponedTransition(final View sharedElement) {
        if (mPosition == mStartingPosition) {
            sharedElement.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    sharedElement.getViewTreeObserver().removeOnPreDrawListener(this);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        ActivityCompat.startPostponedEnterTransition(getActivityCast());
                    }
                    return true;
                }
            });
        }
    }

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newInstanceForItemId(getActivity(), mItemId);
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> cursorLoader, Cursor cursor) {
        if (!isAdded()) {
            if (cursor != null) {
                cursor.close();
            }
            return;
        }

        mCursor = cursor;
        if (mCursor != null && !mCursor.moveToFirst()) {
            Log.e(TAG, "Error reading item detail cursor");
            mCursor.close();
            mCursor = null;
        }

        bindViews();
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> cursorLoader) {
        mCursor = null;
        bindViews();
    }

    public int getUpButtonFloor() {
        if (mPhotoContainerView == null || mPhotoView.getHeight() == 0) {
            return Integer.MAX_VALUE;
        }

        // account for parallax
        return mIsCard
                ? (int) mPhotoContainerView.getTranslationY() + mPhotoView.getHeight() - mScrollY
                : mPhotoView.getHeight() - mScrollY;
    }

    @Nullable
    ImageView getPhotoView() {
        if (isViewInBounds(getActivityCast().getWindow().getDecorView(), mPhotoView)) {
            return mPhotoView;
        }
        return null;
    }

    private static boolean isViewInBounds(@NonNull View container, @NonNull View view) {
        Rect containerBounds = new Rect();
        container.getHitRect(containerBounds);
        return view.getLocalVisibleRect(containerBounds);
    }
}
