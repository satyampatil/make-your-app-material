package com.example.xyzreader.ui;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import com.google.android.material.snackbar.Snackbar;
import androidx.core.app.ActivityCompat;
import androidx.core.app.ActivityOptionsCompat;
import androidx.loader.app.LoaderManager;
import androidx.core.app.SharedElementCallback;
import androidx.loader.content.Loader;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import androidx.appcompat.widget.Toolbar;
import android.text.Html;
import android.text.format.DateUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.TextView;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;
import com.example.xyzreader.data.UpdaterService;
import com.squareup.picasso.Picasso;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;

import static com.example.xyzreader.ui.ArticleDetailActivity.CUBE;
import static com.example.xyzreader.ui.ArticleDetailActivity.DEPTH;
import static com.example.xyzreader.ui.ArticleDetailActivity.EXTRA_LARGE;
import static com.example.xyzreader.ui.ArticleDetailActivity.LARGE;
import static com.example.xyzreader.ui.ArticleDetailActivity.MEDIUM;
import static com.example.xyzreader.ui.ArticleDetailActivity.POP;
import static com.example.xyzreader.ui.ArticleDetailActivity.SMALL;
import static com.example.xyzreader.ui.ArticleDetailActivity.ZOOM;

public class ArticleListActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor>,
        SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = ArticleListActivity.class.toString();
    public static final String EXTRA_STARTING_POSITION = "extra_starting_position";
    public static final String EXTRA_CURRENT_POSITION = "extra_current_position";
    public static final String EXTRA_PAGE_TRANSFORMATION = "extra_page_transformation";
    public static final String EXTRA_TEXT_SIZE = "extra_text_size";
    private String mPageTransformerStr;
    private String mTextSizeStr;
    private Bundle mReenterState;

    private Toolbar mToolbar;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private RecyclerView mRecyclerView;
    private CoordinatorLayout mCoordinatorLayout;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss");
    private SimpleDateFormat outputFormat = new SimpleDateFormat();
    private GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2,1,1);

    public static final int XYZ_LOADER_ID = 0;
    private final SharedElementCallback mCallback = new SharedElementCallback() {
        // Lets the SharedElementCallback adjust the mapping of shared element names to Views.
        // Check if the position has changed. If so, remove the references to the old shared element
        // and add the new one.
        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
            if (mReenterState != null) {
                int startingPosition = mReenterState.getInt(EXTRA_STARTING_POSITION);
                int currentPosition = mReenterState.getInt(EXTRA_CURRENT_POSITION);
                if (startingPosition != currentPosition) {
                    String newTransitionName = getString(R.string.transition_photo) + currentPosition;
                    View newSharedElement = mRecyclerView.findViewWithTag(newTransitionName);
                    if (newSharedElement != null) {
                        names.clear();
                        names.add(newTransitionName);
                        sharedElements.clear();
                        sharedElements.put(newTransitionName, newSharedElement);
                    }
                }

                mReenterState = null;
            } else {
                View navigationBar = findViewById(android.R.id.navigationBarBackground);
                View statusBar = findViewById(android.R.id.statusBarBackground);
                if (navigationBar != null) {
                    names.add(navigationBar.getTransitionName());
                    sharedElements.put(navigationBar.getTransitionName(), navigationBar);
                }
                if (statusBar != null) {
                    names.add(statusBar.getTransitionName());
                    sharedElements.put(statusBar.getTransitionName(), statusBar);
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_list);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setExitSharedElementCallback(mCallback);
        }

        mCoordinatorLayout = findViewById(R.id.coordinator);
        showSnackbar(isConnected());

        mToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        setSwipeRefreshLayout();

        mRecyclerView = findViewById(R.id.recycler_view);
        getSupportLoaderManager().initLoader(XYZ_LOADER_ID, null, this);

        if (savedInstanceState == null) {
            refresh();
        }

        mPageTransformerStr = getPreferredPageTransformationStr();
        mTextSizeStr = getPreferredTextSizeStr();
        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(this);
    }
    private String getPreferredPageTransformationStr() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String keyForPageAnimation = getString(R.string.pref_page_animation_key);
        String defaultPageAnimation = getString(R.string.pref_page_animation_default);
        return prefs.getString(keyForPageAnimation, defaultPageAnimation);
    }
    private String getPreferredTextSizeStr() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String keyForTextSize = getString(R.string.pref_text_size_key);
        String defaultTextSize = getString(R.string.pref_text_size_default);
        return prefs.getString(keyForTextSize, defaultTextSize);
    }

    private void setSwipeRefreshLayout() {
        mSwipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        mSwipeRefreshLayout.setColorSchemeColors(getResources().getColor(R.color.colorAccent),
                getResources().getColor(R.color.color_swipe_deep_purple),
                getResources().getColor(R.color.color_swipe_red));
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refresh();
                runLayoutAnimation(mRecyclerView);
                mSwipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    private void refresh() {
        startService(new Intent(this, UpdaterService.class));
    }

    private void runLayoutAnimation(RecyclerView recyclerView) {
        Context context = recyclerView.getContext();
        LayoutAnimationController controller =
                AnimationUtils.loadLayoutAnimation(context, R.anim.layout_animation_from_bottom);
        recyclerView.setLayoutAnimation(controller);
        recyclerView.getAdapter().notifyDataSetChanged();
        recyclerView.scheduleLayoutAnimation();
    }

    @Override
    public void onActivityReenter(int resultCode, Intent data) {
        super.onActivityReenter(resultCode, data);
        mReenterState = new Bundle(data.getExtras());
        int startingPosition = mReenterState.getInt(EXTRA_STARTING_POSITION);
        int currentPosition = mReenterState.getInt(EXTRA_CURRENT_POSITION);
        if (startingPosition != currentPosition) {
            mRecyclerView.scrollToPosition(currentPosition);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ActivityCompat.postponeEnterTransition(this);
        }
        scheduleStartPostponedTransition(mRecyclerView);
    }

    private void scheduleStartPostponedTransition(final View sharedElement) {
        sharedElement.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                sharedElement.getViewTreeObserver().removeOnPreDrawListener(this);
                mRecyclerView.requestLayout();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    ActivityCompat.startPostponedEnterTransition(ArticleListActivity.this);
                }
                return true;
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(mRefreshingReceiver,
                new IntentFilter(UpdaterService.BROADCAST_ACTION_STATE_CHANGE));
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(mRefreshingReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        PreferenceManager.getDefaultSharedPreferences(this)
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    private boolean mIsRefreshing = false;

    private BroadcastReceiver mRefreshingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (UpdaterService.BROADCAST_ACTION_STATE_CHANGE.equals(intent.getAction())) {
                mIsRefreshing = intent.getBooleanExtra(UpdaterService.EXTRA_REFRESHING, false);
                updateRefreshingUI();
            }
        }
    };

    private void updateRefreshingUI() {
        mSwipeRefreshLayout.setRefreshing(mIsRefreshing);
    }

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newAllArticlesInstance(this);
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> cursorLoader, Cursor cursor) {
        Adapter adapter = new Adapter(cursor);
        adapter.setHasStableIds(true);
        mRecyclerView.setAdapter(adapter);
        int columnCount = getResources().getInteger(R.integer.list_column_count);
        StaggeredGridLayoutManager sglm =
                new StaggeredGridLayoutManager(columnCount, StaggeredGridLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(sglm);
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
        mRecyclerView.setAdapter(null);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.refresh:
                refresh();
                runLayoutAnimation(mRecyclerView);
                return true;
            case R.id.action_settings:
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(getString(R.string.pref_page_animation_key))) {
            String pageAnimation = sharedPreferences
                    .getString(key, getString(R.string.pref_page_animation_default));
            switch (pageAnimation) {
                case POP:
                    mPageTransformerStr = getString(R.string.pref_page_animation_pop);
                    break;
                case ZOOM:
                    mPageTransformerStr = getString(R.string.pref_page_animation_zoom);
                    break;
                case DEPTH:
                    mPageTransformerStr = getString(R.string.pref_page_animation_depth);
                    break;
                case CUBE:
                    mPageTransformerStr = getString(R.string.pref_page_animation_cube);
                    break;
                default:
                    mPageTransformerStr = getString(R.string.pref_page_animation_pop);
            }

        } else if (key.equals(getString(R.string.pref_text_size_key))) {
            String textSize = sharedPreferences
                    .getString(key, getString(R.string.pref_text_size_default));
            switch (textSize) {
                case SMALL:
                    mTextSizeStr = getString(R.string.pref_text_size_small);
                    break;
                case MEDIUM:
                    mTextSizeStr = getString(R.string.pref_text_size_medium);
                    break;
                case LARGE:
                    mTextSizeStr = getString(R.string.pref_text_size_large);
                    break;
                case EXTRA_LARGE:
                    mTextSizeStr = getString(R.string.pref_text_size_extra_large);
                    break;
                default:
                    mTextSizeStr = getString(R.string.pref_text_size_medium);
            }
        }
    }

    private boolean isConnected() {
        ConnectivityManager connectivityManager = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    private void showSnackbar(boolean isConnected) {
        String snackMessage;
        Snackbar snackbar;
        if (isConnected) {
            snackMessage = getString(R.string.snackbar_online);
            snackbar = Snackbar.make(mCoordinatorLayout, snackMessage, Snackbar.LENGTH_LONG);
        } else {
            snackMessage = getString(R.string.snackbar_offline);
            snackbar = Snackbar.make(mCoordinatorLayout, snackMessage, Snackbar.LENGTH_LONG);
            snackbar.setAction(getString(R.string.snackbar_action_retry), new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    refresh();
                    runLayoutAnimation(mRecyclerView);
                }
            });
        }
        snackbar.show();
    }

    private class Adapter extends RecyclerView.Adapter<ViewHolder> {
        private Cursor mCursor;

        public Adapter(Cursor cursor) {
            mCursor = cursor;
        }

        @Override
        public long getItemId(int position) {
            mCursor.moveToPosition(position);
            return mCursor.getLong(ArticleLoader.Query._ID);
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.list_item_article, parent, false);
            final ViewHolder vh = new ViewHolder(view);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(Intent.ACTION_VIEW,
                            ItemsContract.Items.buildItemUri(getItemId(vh.getAdapterPosition())));
                    intent.putExtra(EXTRA_STARTING_POSITION, vh.getAdapterPosition());
                    intent.putExtra(EXTRA_PAGE_TRANSFORMATION, mPageTransformerStr);
                    intent.putExtra(EXTRA_TEXT_SIZE, mTextSizeStr);

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        String transitionName = vh.thumbnailView.getTransitionName();
                        Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(
                                ArticleListActivity.this,
                                vh.thumbnailView,
                                transitionName
                        ).toBundle();
                        startActivity(intent, bundle);
                    } else {
                        startActivity(intent);
                    }
                }
            });
            return vh;
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

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            mCursor.moveToPosition(position);
            holder.titleView.setText(mCursor.getString(ArticleLoader.Query.TITLE));
            Date publishedDate = parsePublishedDate();
            if (!publishedDate.before(START_OF_EPOCH.getTime())) {

                holder.subtitleView.setText(Html.fromHtml(
                        DateUtils.getRelativeTimeSpanString(
                                publishedDate.getTime(),
                                System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                                DateUtils.FORMAT_ABBREV_ALL).toString()
                                + "<br/>" + " by "
                                + mCursor.getString(ArticleLoader.Query.AUTHOR)));
            } else {
                holder.subtitleView.setText(Html.fromHtml(
                        outputFormat.format(publishedDate)
                        + "<br/>" + " by "
                        + mCursor.getString(ArticleLoader.Query.AUTHOR)));
            }

            setTextSize(holder);
            Picasso.with(ArticleListActivity.this)
                    .load(mCursor.getString(ArticleLoader.Query.THUMB_URL))
                    .error(R.drawable.photo_error)
                    .into(holder.thumbnailView);
            holder.thumbnailView.setAspectRatio(mCursor.getFloat(ArticleLoader.Query.ASPECT_RATIO));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                holder.thumbnailView.setTransitionName(getString(R.string.transition_photo) + position);
            }
            holder.thumbnailView.setTag(getString(R.string.transition_photo) + position);
        }

        @Override
        public int getItemCount() {
            return mCursor.getCount();
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public DynamicHeightNetworkImageView thumbnailView;
        public TextView titleView;
        public TextView subtitleView;

        public ViewHolder(View view) {
            super(view);
            thumbnailView = view.findViewById(R.id.thumbnail);
            titleView = view.findViewById(R.id.article_title);
            subtitleView = view.findViewById(R.id.article_subtitle);
        }
    }

    private void setTextSize(ViewHolder holder) {
        if (mTextSizeStr.equals(getString(R.string.pref_text_size_small))) {
            holder.titleView.setTextSize(TypedValue.COMPLEX_UNIT_PX, holder.itemView.getContext()
                    .getResources().getDimension(R.dimen.sp14));
            holder.subtitleView.setTextSize(TypedValue.COMPLEX_UNIT_PX, holder.itemView.getContext()
                    .getResources().getDimension(R.dimen.sp12));
        } else if (mTextSizeStr.equals(getString(R.string.pref_text_size_medium))) {
            holder.titleView.setTextSize(TypedValue.COMPLEX_UNIT_PX, holder.itemView.getContext()
                    .getResources().getDimension(R.dimen.sp16));
            holder.subtitleView.setTextSize(TypedValue.COMPLEX_UNIT_PX, holder.itemView.getContext()
                    .getResources().getDimension(R.dimen.sp14));
        } else if (mTextSizeStr.equals(getString(R.string.pref_text_size_large))) {
            holder.titleView.setTextSize(TypedValue.COMPLEX_UNIT_PX, holder.itemView.getContext()
                    .getResources().getDimension(R.dimen.sp18));
            holder.subtitleView.setTextSize(TypedValue.COMPLEX_UNIT_PX, holder.itemView.getContext()
                    .getResources().getDimension(R.dimen.sp16));
        } else if (mTextSizeStr.equals(getString(R.string.pref_text_size_extra_large))) {
            holder.titleView.setTextSize(TypedValue.COMPLEX_UNIT_PX, holder.itemView.getContext()
                    .getResources().getDimension(R.dimen.sp20));
            holder.subtitleView.setTextSize(TypedValue.COMPLEX_UNIT_PX, holder.itemView.getContext()
                    .getResources().getDimension(R.dimen.sp18));
        }
    }
}
