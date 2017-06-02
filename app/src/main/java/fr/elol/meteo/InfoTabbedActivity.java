package fr.elol.meteo;

import java.util.Locale;

import android.graphics.Typeface;
import android.support.design.widget.TabLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.FragmentPagerAdapter;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import fr.elol.meteo.helpers.WeatherIcon;


public class InfoTabbedActivity extends AppCompatActivity {

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    ViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info_tabbed);

        // Toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);
//        tabLayout.setTabMode(TabLayout.MODE_SCROLLABLE);
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
            return PlaceholderFragment.newInstance(position + 1);
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            Locale l = Locale.getDefault();
            switch (position) {
                case 0:
                    return getString(R.string.title_section1).toUpperCase(l);
                case 1:
                    return getString(R.string.title_section2).toUpperCase(l);
            }
            return null;
        }
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {

            Bundle args = getArguments();
            int position = args.getInt(ARG_SECTION_NUMBER);
            View rootView = null;
            switch (position) {
                case 1:
                    rootView = inflater.inflate(R.layout.activity_info, container, false);
                    Typeface tf = Typeface.createFromAsset(getActivity().getApplicationContext().getAssets(), "fonts/weathericons.ttf");
                    setBeaufort (rootView, tf, R.id.beaufort0, WeatherIcon.WI_BEAFORT_0);
                    setBeaufort (rootView, tf, R.id.beaufort1, WeatherIcon.WI_BEAFORT_1);
                    setBeaufort (rootView, tf, R.id.beaufort2, WeatherIcon.WI_BEAFORT_2);
                    setBeaufort (rootView, tf, R.id.beaufort3, WeatherIcon.WI_BEAFORT_3);
                    setBeaufort (rootView, tf, R.id.beaufort4, WeatherIcon.WI_BEAFORT_4);
                    setBeaufort (rootView, tf, R.id.beaufort5, WeatherIcon.WI_BEAFORT_5);
                    setBeaufort (rootView, tf, R.id.beaufort6, WeatherIcon.WI_BEAFORT_6);
                    setBeaufort (rootView, tf, R.id.beaufort7, WeatherIcon.WI_BEAFORT_7);
                    setBeaufort (rootView, tf, R.id.beaufort8, WeatherIcon.WI_BEAFORT_8);
                    setBeaufort (rootView, tf, R.id.beaufort9, WeatherIcon.WI_BEAFORT_9);
                    setBeaufort (rootView, tf, R.id.beaufort10, WeatherIcon.WI_BEAFORT_10);
                    setBeaufort (rootView, tf, R.id.beaufort11, WeatherIcon.WI_BEAFORT_11);
                    setBeaufort (rootView, tf, R.id.beaufort12, WeatherIcon.WI_BEAFORT_12);
                    ((TextView)rootView.findViewById(R.id.infos_version)).setText(BuildConfig.VERSION_NAME);
                    break;
                case 2:
                    rootView = inflater.inflate(R.layout.activity_info_wear, container, false);
                    break;
            }
            return rootView;
        }

        private void setBeaufort (View rootView, Typeface tf, int id, String s) {
            TextView tv = ((TextView)rootView.findViewById(id));
            tv.setTypeface(tf);
            tv.setText(s);
        }


    }

}
