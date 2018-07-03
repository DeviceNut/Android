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
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.devicenut.pixelnutctrl.Main.CMD_BLUENAME;
import static com.devicenut.pixelnutctrl.Main.CMD_PAUSE;
import static com.devicenut.pixelnutctrl.Main.CMD_RESUME;
import static com.devicenut.pixelnutctrl.Main.CMD_SEQ_END;
import static com.devicenut.pixelnutctrl.Main.DEVSTAT_FAILED;
import static com.devicenut.pixelnutctrl.Main.DEVSTAT_SUCCESS;
import static com.devicenut.pixelnutctrl.Main.createViewCtrls;
import static com.devicenut.pixelnutctrl.Main.createViewFavs;
import static com.devicenut.pixelnutctrl.Main.doRefreshCache;
import static com.devicenut.pixelnutctrl.Main.helpActive;
import static com.devicenut.pixelnutctrl.Main.numFragments;
import static com.devicenut.pixelnutctrl.Main.pageControls;
import static com.devicenut.pixelnutctrl.Main.pageDetails;
import static com.devicenut.pixelnutctrl.Main.pageFavorites;
import static com.devicenut.pixelnutctrl.Main.pageCurrent;
import static com.devicenut.pixelnutctrl.Main.masterPager;
import static com.devicenut.pixelnutctrl.Main.pixelDensity;
import static com.devicenut.pixelnutctrl.Main.pixelHeight;

import static com.devicenut.pixelnutctrl.Main.ble;
import static com.devicenut.pixelnutctrl.Main.wifi;
import static com.devicenut.pixelnutctrl.Main.devName;
import static com.devicenut.pixelnutctrl.Main.devIsBLE;
import static com.devicenut.pixelnutctrl.Main.msgWriteEnable;
import static com.devicenut.pixelnutctrl.Main.msgWriteQueue;
import static com.devicenut.pixelnutctrl.Main.isConnected;

public class Master extends AppCompatActivity implements FragFavs.FavoriteSelectInterface,
                                                         FragCtrls.FavoriteDeselectInterface,
                                                         FragCtrls.FavoriteCreateInterface,
                                                         FragCtrls.PatternSelectInterface,
                                                         FragCtrls.DeviceCommandInterface,
                                                         Bluetooth.BleCallbacks,
                                                         Wifi.WifiCallbacks
{
    private final String LOGNAME = "Master";
    private final Activity context = this;

    private boolean doUpdate = true;
    private boolean isEditing = false;
    private String devNameSaved = "";

    private LinearLayout llFragPages;
    private RelativeLayout llGoToText;
    private Button pauseButton, helpButton;
    private TextView nameText;
    private TextView leftText, rightText;

    private final Fragment[] myFragments = new Fragment[numFragments];
    private boolean inLandscape;

    public void onFavoriteSelect(int seg, int pnum, String vals)
    {
        ((FragCtrls)myFragments[pageControls]).ChangePattern(seg, pnum, vals);
    }

    public void onFavoriteDeselect()
    {
        if (pageFavorites >= 0)
            ((FragFavs)myFragments[pageFavorites]).FavoriteDeselect();
    }

    public void onFavoriteCreate(String name, int seg, int pnum, String vals)
    {
        if (pageFavorites >= 0)
            ((FragFavs)myFragments[pageFavorites]).FavoriteCreate(name, seg, pnum, vals);
    }

    public boolean onPatternSelect(String name, int seg, int pnum, String vals)
    {
        return pageFavorites >= 0 && ((FragFavs) myFragments[pageFavorites]).IsFavoritePattern(name, seg, pnum, vals);
    }

    public void onDeviceCommand(String str)
    {
        if (str.equals(CMD_PAUSE))
        {
            if (doUpdate) // not paused
            {
                // don't change text
                doUpdate = false;
            }
            // else already paused
        }
        else if (str.equals(CMD_RESUME))
        {
            if (!doUpdate) // user had paused, so change the text
            {
                pauseButton.setText(getResources().getString(R.string.name_pause));
                doUpdate = true;
            }
            // else already resumed
        }
        SendString(str);
    }

    @Override protected void onCreate(Bundle savedInstanceState)
    {
        Log.d(LOGNAME, ">>onCreate: isConnected=" + isConnected);
        super.onCreate(savedInstanceState);

        if (!isConnected)
        {
            finish();
            return;
        }

        setContentView(R.layout.activity_master);

        if (pageFavorites >= 0) myFragments[pageFavorites] = FragFavs.newInstance();
        myFragments[pageControls] = FragCtrls.newInstance();
        //FIXME myFragments[pageDetails] = FragAdv.newInstance();

        inLandscape = (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE);
        if (inLandscape && getResources().getBoolean(R.bool.portrait_only))
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        masterPager = findViewById(R.id.myViewPager);
        FragmentPagerAdapter adapterViewPager = new MasterAdapter(getSupportFragmentManager());
        masterPager.setAdapter(adapterViewPager);
        //masterPager.setOffscreenPageLimit(3);

        llFragPages = findViewById(R.id.ll_FragPages);
        llGoToText  = findViewById(R.id.ll_GoToText);

        pauseButton = findViewById(R.id.button_Pause);
        helpButton  = findViewById(R.id.button_HelpPage);
        nameText    = findViewById(R.id.text_Devname);
        leftText    = findViewById(R.id.text_GoLeft);
        rightText   = findViewById(R.id.text_GoRight);

        SetFragViewPageHeight(false);
        SetupGoToText();
    }

    private void SetFragViewPageHeight(boolean inhelp)
    {
        int margin = (inhelp ? 85 : 120); // must be at least 85 to get above bottom part of screen
        int h = pixelHeight - (int)(margin * ((float)pixelDensity / DisplayMetrics.DENSITY_DEFAULT));

        ViewGroup.LayoutParams params = llFragPages.getLayoutParams();
        params.height = h;
        llFragPages.setLayoutParams(params);
    }

    private void SetupGoToText()
    {
        Log.d(LOGNAME, "SetupGoToText: curpage=" + pageCurrent);

        if (pageCurrent == pageDetails)
        {
            leftText.setVisibility(VISIBLE);
            leftText.setText(getResources().getString(R.string.action_ctrls_left));
        }
        else if ((pageCurrent == pageControls) && (pageFavorites >= 0))
        {
            leftText.setVisibility(VISIBLE);
            leftText.setText(getResources().getString(R.string.action_favs));
        }
        else leftText.setVisibility(GONE);

        if (inLandscape)
        {
            /* if (pageCurrent != pageControls)
            {
                rightText.setVisibility(VISIBLE);
                rightText.setText(getResources().getString(R.string.action_details));
            }
            else */
                rightText.setVisibility(GONE);
        }
        else if (pageCurrent == pageFavorites)
        {
            rightText.setVisibility(VISIBLE);
            rightText.setText(getResources().getString(R.string.action_ctrls_rite));
        }
        /* FIXME else if (pageCurrent == pageControls)
        {
            rightText.setVisibility(VISIBLE);
            rightText.setText(getResources().getString(R.string.action_details));
        }
        */
        else rightText.setVisibility(GONE);
    }

    @Override protected void onResume()
    {
        Log.d(LOGNAME, ">>onResume");
        super.onResume();

        if (!isConnected)
        {
            finish();
            return;
        }

        if (isEditing && (ble != null))
        {
            isEditing = false;
            if (!devNameSaved.equals(devName))
            {
                Log.d(LOGNAME, "Renaming device=" + devName);
                SendString(CMD_BLUENAME + devName);
                SendString(CMD_SEQ_END);

                if (Build.VERSION.SDK_INT < 23)
                {
                    doRefreshCache = true; // force refreshing cache before do next scan: doesn't work FIXME
                    Toast.makeText(context, "Rescan from Settings to see name change", Toast.LENGTH_SHORT).show();
                }
            }
        }
        else
        {
            if (devIsBLE)
                 ble.setCallbacks(this);
            else wifi.setCallbacks(this);

            devNameSaved = devName;

            // set pause button to correct state
            pauseButton.setText(getResources().getString(doUpdate ? R.string.name_pause : R.string.name_resume));
        }

        nameText.setText(devName);

        SetHelpMode(helpActive);
    }

    @Override public void onBackPressed()
    {
        if (helpActive) SetHelpMode(false);
        else
        {
            if (isConnected)
            {
                Log.d(LOGNAME, "Disconnecting...");
                if (devIsBLE)
                     ble.disconnect();
                else wifi.disconnect();
            }

            super.onBackPressed();
        }
    }

    private void SetHelpMode(boolean enable)
    {
        // FIXME: show help for Details page

        if (createViewFavs && createViewCtrls)
        {
            if (enable) // turn controls help on
            {
                SetFragViewPageHeight(true);
                llGoToText.setVisibility(GONE);

                ((FragFavs)myFragments[pageFavorites]).setHelpMode(true);
                ((FragCtrls)myFragments[pageControls]).setHelpMode(true);

                helpButton.setText(getResources().getString(R.string.name_action));
                helpActive = true;
            }
            else
            {
                ((FragFavs)myFragments[pageFavorites]).setHelpMode(false);
                ((FragCtrls)myFragments[pageControls]).setHelpMode(false);

                helpButton.setText(getResources().getString(R.string.name_help));
                helpActive = false;

                llGoToText.setVisibility(VISIBLE);
                SetFragViewPageHeight(false);
                SetupGoToText();
            }
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
                    SendString(CMD_SEQ_END);
                    pauseButton.setText(getResources().getString(R.string.name_resume));
                }
                else
                {
                    SendString(CMD_RESUME);
                    SendString(CMD_SEQ_END);
                    pauseButton.setText(getResources().getString(R.string.name_pause));
                }
                doUpdate = !doUpdate;
                break;
            }
            case R.id.button_HelpPage:
            {
                SetHelpMode(!helpActive);
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
        Log.v(LOGNAME, "Queue command: \"" + str + "\"");
        if (isConnected && !msgWriteQueue.put(str))
            Log.e(LOGNAME, "Msg queue full: str=\"" + str + "\"");
    }

    private void DeviceDisconnect(final String reason)
    {
        Log.v(LOGNAME, "Device disconnect: reason=" + reason + " connected=" + isConnected);
        if (isConnected)
        {
            context.runOnUiThread(new Runnable()
            {
                public void run()
                {
                    Toast.makeText(context, "Disconnect: " + reason, Toast.LENGTH_SHORT).show();
                }
            });

            isConnected = false;
            Log.d(LOGNAME, "Finishing controls activity...");
            finish();
        }
    }

    @Override public void onScan(String name, int id, boolean isble)
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
        if (status == DEVSTAT_SUCCESS)
            msgWriteEnable = true;

        else if (status == DEVSTAT_FAILED)
        {
            Log.e(LOGNAME, "OnWrite: failed");
            DeviceDisconnect("Write");
        }
        else Log.w(LOGNAME, "OnWrite: bad device state");
    }

    @Override public void onRead(String reply)
    {
        Log.e(LOGNAME, "Unexpected onRead");
    }

    private class MasterAdapter extends FragmentPagerAdapter
    {
        private final String LOGNAME = "Master";

        MasterAdapter(FragmentManager fragmentManager)
        {
            super(fragmentManager);
            Log.d(LOGNAME, ">>Adapter");
        }

        @Override public int getCount() { return numFragments; }

        @Override public Fragment getItem(int position)
        {
            Log.d(LOGNAME, "Select fragment " + position);
            return myFragments[position];
        }

        @Override public CharSequence getPageTitle(int position) // never see this called!
        {
            Log.d(LOGNAME, "Fragment Page " + position);
            return "Page " + position;
        }

        @Override public float getPageWidth(int position)
        {
            //Log.d(LOGNAME, "GetPageWidth: landscape=" + inLandscape);
            return( inLandscape ? 0.5f : 1.0f ); // allow for 2 pages side by side
        }
    }
}
