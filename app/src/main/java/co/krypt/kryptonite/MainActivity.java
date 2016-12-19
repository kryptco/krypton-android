package co.krypt.kryptonite;

import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import co.krypt.kryptonite.devices.DevicesFragment;
import co.krypt.kryptonite.me.MeFragment;
import co.krypt.kryptonite.pairing.PairFragment;
import co.krypt.kryptonite.silo.Silo;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int PAIR_FRAGMENT_POSITION = 1;
    private static final int DEVICES_FRAGMENT_POSITION = 2;

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;

    private Silo silo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (ConnectionResult.SUCCESS != GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(getApplicationContext())) {
            //  TODO: warn about no push notifications, prompt to install google play services
        }

        silo = Silo.shared(getApplicationContext());

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            switch (position) {
                case PAIR_FRAGMENT_POSITION:
                    return PairFragment.newInstance();
                case DEVICES_FRAGMENT_POSITION:
                    return DevicesFragment.newInstance(1);
                default:
                    return new MeFragment();
            }
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "Me";
                case PAIR_FRAGMENT_POSITION:
                    return "Pair";
                case DEVICES_FRAGMENT_POSITION:
                    return "Devices";
            }
            return null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "resume");
        silo.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "pause");
        silo.stop();
    }


    @Override
    public void onBackPressed() {
        for (Fragment fragment: getSupportFragmentManager().getFragments()) {
            if (fragment.isVisible()) {
                if (fragment.getChildFragmentManager().getBackStackEntryCount() > 0) {
                    fragment.getChildFragmentManager().popBackStack();
                    return;
                }
            }
        }
        super.onBackPressed();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if ( v instanceof EditText) {
                Rect outRect = new Rect();
                v.getGlobalVisibleRect(outRect);
                if (v.hasFocus() && !outRect.contains((int)event.getRawX(), (int)event.getRawY())) {
                    v.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
            }
        }
        return super.dispatchTouchEvent(event);
    }
}
