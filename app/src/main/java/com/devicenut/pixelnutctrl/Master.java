package com.devicenut.pixelnutctrl;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

public class Master extends AppCompatActivity implements FragFavs.OnFragmentInteractionListener, FragCtrls.OnFragmentInteractionListener
{
    private final String LOGNAME = "Master";

    FragmentPagerAdapter adapterViewPager;

    @Override protected void onCreate(Bundle savedInstanceState)
    {
        Log.d(LOGNAME, ">>onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_master);

        ViewPager vpPager = (ViewPager) findViewById(R.id.vpPager);
        adapterViewPager = new MasterAdapter(getSupportFragmentManager());
        vpPager.setAdapter(adapterViewPager);
    }

    public void onFragmentInteraction(String s)
    {
        Log.d(LOGNAME, "Fragment says: " + s);
    }

    public static class MasterAdapter extends FragmentPagerAdapter
    {
        private final String LOGNAME = "Master";

        public MasterAdapter(FragmentManager fragmentManager)
        {
            super(fragmentManager);
            Log.d(LOGNAME, ">>Adapter");
        }

        @Override public int getCount() { return 2; } // number of pages

        @Override public Fragment getItem(int position)
        {
            Log.d(LOGNAME, "Select fragment " + position);
            if (position == 0) return FragFavs.newInstance();
            if (position == 1) return FragCtrls.newInstance();
            return null;
        }

        @Override public CharSequence getPageTitle(int position)
        {
            Log.d(LOGNAME, "Fragment Page " + position);
            return "Page " + position;
        }
    }
}