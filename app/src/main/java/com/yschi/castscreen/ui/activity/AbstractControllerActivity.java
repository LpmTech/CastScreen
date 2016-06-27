package com.yschi.castscreen.ui.activity;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import org.androidannotations.annotations.EActivity;

import butterknife.Bind;
import butterknife.ButterKnife;
import de.greenrobot.event.EventBus;
import de.greenrobot.event.Subscribe;
import de.greenrobot.event.ThreadMode;
import eu.codlab.cypherx.R;
import eu.codlab.cypherx.ui._abstract.AbstractBackstackController;
import eu.codlab.cypherx.ui._abstract.AbstractBackstackProvider;
import eu.codlab.cypherx.ui._abstract.FragmentDescriptor;
import eu.codlab.cypherx.ui._abstract.IToolbarManipulation;
import eu.codlab.cypherx.ui.events.OnEventAwaitingPhotos;
import eu.codlab.cypherx.ui.events.OnEventPopFragment;
import eu.codlab.cypherx.ui.events.OnEventPushFragment;

@EActivity
public abstract class AbstractControllerActivity extends AppCompatActivity
        implements IToolbarManipulation {

    private ActionBarDrawerToggle _actionbar_toggle;

    @Bind(R.id.container)
    ViewGroup mContainer;

    @Bind(R.id.toolbar)
    Toolbar mToolbar;

    @Nullable
    @Bind(R.id.drawerLayout)
    protected DrawerLayout mDrawerLayout;

    private Handler mHandler;
    private AbstractBackstackController sBackstackController;
    private AbstractBackstackProvider mBackstackProvider;

    protected abstract AbstractBackstackProvider createAbstractBackstackProvider();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);


        if (sBackstackController == null) {
            sBackstackController = new AbstractBackstackController() {
                @Override
                protected boolean isRepresentingValueLeftMenu(int value) {
                    return FragmentDescriptor.from(value).isLeftMenu();
                }

                @Override
                protected boolean isRepresentingValueBackable(int value) {
                    return FragmentDescriptor.from(value).isBackable();
                }
            };
        }

        mBackstackProvider = createAbstractBackstackProvider();
        sBackstackController.onCreate(this,
                mBackstackProvider,
                getSupportFragmentManager(),
                mContainer);


        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        if (mDrawerLayout != null) {
            _actionbar_toggle = new ActionBarDrawerToggle(
                    this,
                    mDrawerLayout,         /* DrawerLayout object */
                    getToolbar(),
                    0,
                    0) {

                /**
                 * Called when a drawer has settled in a completely closed state.
                 */
                public void onDrawerClosed(View view) {
                    super.onDrawerClosed(view);
                }

                /**
                 * Called when a drawer has settled in a completely open state.
                 */
                public void onDrawerOpened(View drawerView) {
                    super.onDrawerOpened(drawerView);
                    hideKeyboard(mDrawerLayout);
                }
            };
            mDrawerLayout.setDrawerListener(_actionbar_toggle);
        }

        setupToolbar();
        setupDrawer();
    }

    @Override
    public void setContentView(@LayoutRes int layoutResID) {
        super.setContentView(layoutResID);
        ButterKnife.bind(this);

        setSupportActionBar(mToolbar);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item != null) {
            switch (item.getItemId()) {
                case android.R.id.home:
                    manageToolbarButton();
                    return true;
            }
        }
        return false;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        _actionbar_toggle.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().register(this);

        sBackstackController.onResume();
    }

    @Override
    protected void onPause() {
        if (EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().unregister(this);

        sBackstackController.onPause();

        super.onPause();
    }

    @Override
    public void onBackPressed() {

        if (!manageToolbarButton()) {
            super.onBackPressed();
        }
        /*if (isDrawerOpen()) {
            closeDrawer();
        } else {
            if (back()) {
                //BACK WAS MANAGED
                return;
            }
            super.onBackPressed();
        }*/
    }

    @Subscribe(threadMode = ThreadMode.MainThread)
    public void onEvent(OnEventPushFragment event) {
        sBackstackController.push(event.getType().ordinal(),
                event.getData());
        Toast.makeText(this, "ee", Toast.LENGTH_SHORT).show();
    }

    @Subscribe(threadMode = ThreadMode.MainThread)
    public void onEvent(OnEventPopFragment event) {
        sBackstackController.pop();
    }

    public void setLeftMenu(boolean activated) {
        setDrawerState(activated);
    }

    public void setBackable(boolean backable) {
        getSupportActionBar().setDisplayHomeAsUpEnabled(backable);
        getSupportActionBar().setDefaultDisplayHomeAsUpEnabled(backable);
    }

    private int getDpToPixels(int dp) {
        Resources r = getResources();
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics());
    }

    public int getActionBarHeight() {
        final TypedArray ta = getTheme().obtainStyledAttributes(
                new int[]{android.R.attr.actionBarSize});
        int actionBarHeight = (int) ta.getDimension(0, 0);
        return actionBarHeight;
    }

    private boolean back() {
        if (sBackstackController.canPop()) {
            EventBus.getDefault().removeStickyEvent(OnEventAwaitingPhotos.class);

            sBackstackController.pop();
            return true;
        }
        return false;
    }

    private boolean isDrawerOpen() {
        return mDrawerLayout != null && mDrawerLayout.isDrawerOpen(GravityCompat.START);
    }

    protected void setupDrawer() {
        if (mDrawerLayout != null) {
            getToolbar().setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    manageToolbarButton();
                }
            });
        }
    }

    public void closeDrawer() {
        if (mDrawerLayout != null)
            mDrawerLayout.closeDrawer(GravityCompat.START);
    }

    public void setDrawerState(boolean activated) {
        if (mDrawerLayout != null) {
            mDrawerLayout.setActivated(activated);
            if (activated) {
                mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
            } else {
                mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
            }
            if (!activated)
                mDrawerLayout.closeDrawer(GravityCompat.START);
        }
    }

    protected void setupToolbar() {
        Toolbar toolbar = getToolbar();
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            updateToolbarButton();
        }
    }

    public void hideKeyboard(View view) {
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.RESULT_UNCHANGED_SHOWN);
        }
    }

    protected Toolbar getToolbar() {
        return mToolbar;
    }

    private boolean manageToolbarButton() {
        boolean managed = false;

        FragmentDescriptor fragments = FragmentDescriptor.from(sBackstackController.getHead());
        if (mDrawerLayout != null && fragments.isLeftMenu()) {
            if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
                mDrawerLayout.closeDrawer(GravityCompat.START);
                managed = true;
            } else {
                mDrawerLayout.openDrawer(GravityCompat.START);
            }
            //mDrawerLayout.openDrawer(GravityCompat.START);
        } else if (fragments.isBackable()) {
            managed = back();
        }

        updateToolbarButton();

        return managed;
    }

    private void updateToolbarButton() {
        FragmentDescriptor fragments = FragmentDescriptor.from(sBackstackController.getHead());

        if (mDrawerLayout != null) {
            if (fragments == null || fragments.isLeftMenu())
                mToolbar.setNavigationIcon(R.drawable.ic_menu_white);
        }
    }

    private void updateWindow() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Window w = getWindow();
            w.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        }
    }
}
