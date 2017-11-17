package com.devicenut.pixelnutctrl;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Build;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.devicenut.pixelnutctrl.Bluetooth.BLESTAT_DISCONNECTED;
import static com.devicenut.pixelnutctrl.Main.CMD_BLUENAME;
import static com.devicenut.pixelnutctrl.Main.CMD_PAUSE;
import static com.devicenut.pixelnutctrl.Main.CMD_RESUME;
import static com.devicenut.pixelnutctrl.Main.ble;
import static com.devicenut.pixelnutctrl.Main.devName;
import static com.devicenut.pixelnutctrl.Main.doUpdate;
import static com.devicenut.pixelnutctrl.Main.haveFavorites;
import static com.devicenut.pixelnutctrl.Main.multiStrands;
import static com.devicenut.pixelnutctrl.Main.numSegments;
import static com.devicenut.pixelnutctrl.Main.pageControls;
import static com.devicenut.pixelnutctrl.Main.pageDetails;
import static com.devicenut.pixelnutctrl.Main.pageFavorites;
import static com.devicenut.pixelnutctrl.Main.pageCurrent;
import static com.devicenut.pixelnutctrl.Main.masterPager;

public class Master extends AppCompatActivity implements FragListen, Bluetooth.BleCallbacks
{
    private final String LOGNAME = "Master";
    private final Activity context = this;

    private boolean isConnected = false;
    private boolean isEditing = false;
    private boolean helpActive = false;
    private String devNameSaved = "";

    private LinearLayout llFragPages;
    private ScrollView helpPage;
    private Button pauseButton, helpButton;
    private TextView nameText, helpText;
    private TextView leftText, rightText;

    private FragmentPagerAdapter adapterViewPager;
    private boolean inLandscape;

    @Override protected void onCreate(Bundle savedInstanceState)
    {
        Log.d(LOGNAME, ">>onCreate: SavedInstance=" + ((savedInstanceState == null) ? "0" : "1"));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_master);

        inLandscape = (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE);
        if (inLandscape && getResources().getBoolean(R.bool.portrait_only))
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        /*
        <LinearLayout
            android:layout_width="wrap_content"
            android:layout_height="match_parent"
            android:visibility="visible"
            android:orientation="horizontal"
            android:id="@+id/ll_FragPage1">
            <TextView
                android:layout_width="match_parent"
                android:layout_height="match_parent"
                android:textAppearance="@style/TextAppearance.AppCompat.Medium"
                android:text="Page1"/>
        </LinearLayout>

        <LinearLayout
            android:layout_width="wrap_content"
            android:layout_height="match_parent"
            android:visibility="visible"
            android:orientation="horizontal"
            android:id="@+id/ll_FragPage2">
            <TextView
                android:layout_width="match_parent"
                android:layout_height="match_parent"
                android:textAppearance="@style/TextAppearance.AppCompat.Medium"
                android:text="Page2"/>
        </LinearLayout>

            // display is large enough for 2 fragments at once
            // and currently in landscape mode, so display 2 fragments
            Log.w(LOGNAME, "Adding both fragments here...");
            //getSupportFragmentManager().beginTransaction().add(R.id.ll_FragPage1, new FragFavs(),  "Favorites Fragment").commit();
            //getSupportFragmentManager().beginTransaction().add(R.id.ll_FragPage2, new FragCtrls(), "Controls Fragment").commit();
            else // if (getResources().getBoolean(R.bool.portrait_only))
            {
                Log.w(LOGNAME, "Adding single fragment here...");
                getSupportFragmentManager().beginTransaction().add(R.id.ll_FragPage1, new FragFavs(),  "Favorites Fragment").commit();
            }
        */

        masterPager = (MyPager)findViewById(R.id.myViewPager);
        adapterViewPager = new MasterAdapter(getSupportFragmentManager());
        masterPager.setAdapter(adapterViewPager);
        //masterPager.setOffscreenPageLimit(3);

        llFragPages     = (LinearLayout) findViewById(R.id.ll_ViewPages);
        helpPage        = (ScrollView)   findViewById(R.id.ll_HelpPage);
        pauseButton     = (Button)       findViewById(R.id.button_Pause);
        helpButton      = (Button)       findViewById(R.id.button_HelpPage);
        helpText        = (TextView)     findViewById(R.id.view_HelpText);
        nameText        = (TextView)     findViewById(R.id.text_Devname);
        leftText        = (TextView)     findViewById(R.id.text_GoLeft);
        rightText       = (TextView)     findViewById(R.id.text_GoRight);

        SetupGoToText();
    }

    private void SetupGoToText()
    {
        if (pageCurrent == pageDetails)
        {
            leftText.setVisibility(VISIBLE);
            leftText.setText(getResources().getString(R.string.title_ctrls));
        }
        else if ((pageCurrent == pageControls) && (pageFavorites >= 0))
        {
            leftText.setVisibility(VISIBLE);
            leftText.setText(getResources().getString(R.string.title_favs));
        }
        else leftText.setVisibility(GONE);

        if (inLandscape)
        {
            if (pageCurrent != pageControls)
            {
                rightText.setVisibility(VISIBLE);
                rightText.setText(getResources().getString(R.string.title_adv));
            }
            else rightText.setVisibility(GONE);
        }
        else if (pageCurrent == pageFavorites)
        {
            rightText.setVisibility(VISIBLE);
            rightText.setText(getResources().getString(R.string.title_ctrls));
        }
        else if (pageCurrent == pageControls)
        {
            rightText.setVisibility(VISIBLE);
            rightText.setText(getResources().getString(R.string.title_adv));
        }
        else rightText.setVisibility(GONE);
    }

    @Override protected void onResume()
    {
        Log.d(LOGNAME, ">>onResume");
        super.onResume();

        if (isEditing && (ble != null))
        {
            isEditing = false;
            if (!devNameSaved.equals(devName))
            {
                Log.d(LOGNAME, "Renaming device=" + devName);
                SendString(CMD_BLUENAME + devName);

                if (Build.VERSION.SDK_INT < 23)
                {
                    //ble.refreshDeviceCache(); // doesn't work FIXME

                    Toast.makeText(context, "Rescan from Settings to see name change", Toast.LENGTH_SHORT).show();
                }
            }
        }
        else
        {
            assert ble != null;
            ble.setCallbacks(this);
            isConnected = true;
            devNameSaved = devName;

            // set pause button to correct state
            pauseButton.setText(getResources().getString(doUpdate ? R.string.name_pause : R.string.name_resume));
        }
        nameText.setText(devName);
    }

    @Override public void onBackPressed()
    {
        if (helpActive) ToggleHelp();
        else
        {
            if (isConnected) ble.disconnect();

            super.onBackPressed();
        }
    }

    private void ToggleHelp()
    {
        if (helpActive) // turn controls help off
        {
            helpPage.setVisibility(GONE);
            llFragPages.setVisibility(VISIBLE);

            helpButton.setText(getResources().getString(R.string.name_help));
            helpActive = false;
        }
        else
        {
            llFragPages.setVisibility(GONE);
            helpPage.setVisibility(VISIBLE);

            String str = getResources().getString(R.string.text_help_head);
            if (numSegments > 1)
            {
                if (multiStrands)
                     str += getResources().getString(R.string.text_help_segs_physical);
                else str += getResources().getString(R.string.text_help_segs_logical);
            }
            helpText.setText(str + getResources().getString(R.string.text_help_tail));

            helpButton.setText(getResources().getString(R.string.name_action));
            helpActive = true;
        }
    }

    public void onClick(View v)
    {
        switch (v.getId())
        {
            case R.id.text_Devname:
            {
                isEditing = true;
                startActivity( new Intent(context, EditName.class) );
                break;
            }
            case R.id.button_Pause:
            {
                if (doUpdate)
                {
                    SendString(CMD_PAUSE);
                    pauseButton.setText(getResources().getString(R.string.name_resume));
                }
                else
                {
                    SendString(CMD_RESUME);
                    pauseButton.setText(getResources().getString(R.string.name_pause));
                }
                doUpdate = !doUpdate;
                break;
            }
            case R.id.button_HelpPage:
            {
                ToggleHelp();
                break;
            }
            case R.id.text_GoLeft:
            {
                masterPager.setCurrentItem(--pageCurrent);
                SetupGoToText();
                break;
            }
            case R.id.text_GoRight:
            {
                masterPager.setCurrentItem(++pageCurrent);
                SetupGoToText();
                break;
            }
        }
    }

    private void SendString(String str)
    {
        ble.WriteString(str);
    }

    private void DeviceDisconnect(final String reason)
    {
        Log.v(LOGNAME, "Device disconnect: reason=" + reason + " connected=" + isConnected);
        if (isConnected)
        {
            isConnected = false;
            context.runOnUiThread(new Runnable()
            {
                public void run()
                {
                    Toast.makeText(context, "Disconnect: " + reason, Toast.LENGTH_SHORT).show();

                    Log.v(LOGNAME, "Finishing controls activity...");
                    finish();
                }
            });
        }
    }

    @Override public void onScan(String name, int id)
    {
        Log.e(LOGNAME, "Unexpected callback: onScan");
    }

    @Override public void onConnect(final int status)
    {
        Log.e(LOGNAME, "Unexpected callback: onConnect");
    }

    @Override public void onDisconnect()
    {
        Log.i(LOGNAME, "Received disconnect");
        DeviceDisconnect("Request");
    }

    @Override public void onWrite(final int status)
    {
        if ((status != 0) && (status != BLESTAT_DISCONNECTED))
        {
            Log.e(LOGNAME, "Write status: " + status); //Integer.toHexString(status));
            DeviceDisconnect("Write");
        }
    }

    @Override public void onRead(String reply)
    {
        Log.e(LOGNAME, "Unexpected onRead");
    }

    public void onDeviceCmdSend(String str)
    {
        Log.d(LOGNAME, "Fragment says: " + str);
        SendString(str);
    }

    private class MasterAdapter extends FragmentPagerAdapter
    {
        private final String LOGNAME = "Master";

        MasterAdapter(FragmentManager fragmentManager)
        {
            super(fragmentManager);
            Log.d(LOGNAME, ">>Adapter");
        }

        @Override public int getCount() { return (haveFavorites  ? 3 : 2); } // number of pages

        @Override public Fragment getItem(int position)
        {
            Log.d(LOGNAME, "Select fragment " + position);
            if (position == pageFavorites) return FragFavs.newInstance();
            if (position == pageControls)  return FragCtrls.newInstance();
            if (position == pageDetails)   return FragAdv.newInstance();
            return null;
        }

        @Override public CharSequence getPageTitle(int position) // never see this called!
        {
            Log.d(LOGNAME, "Fragment Page " + position);
            return "Page " + position;
        }

        @Override public float getPageWidth(int position)
        {
            //Log.d(LOGNAME, "GetPageWidth: landscape=" + inLandscape);
            return( inLandscape ? 0.5f : 1.0f );
        }
    }
}
