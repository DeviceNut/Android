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
import static com.devicenut.pixelnutctrl.Bluetooth.BLESTAT_DISCONNECTED;
import static com.devicenut.pixelnutctrl.Main.CMD_BLUENAME;
import static com.devicenut.pixelnutctrl.Main.CMD_PAUSE;
import static com.devicenut.pixelnutctrl.Main.CMD_RESUME;
import static com.devicenut.pixelnutctrl.Main.ble;
import static com.devicenut.pixelnutctrl.Main.devName;
import static com.devicenut.pixelnutctrl.Main.doUpdate;
import static com.devicenut.pixelnutctrl.Main.isConnected;
import static com.devicenut.pixelnutctrl.Main.numFragments;
import static com.devicenut.pixelnutctrl.Main.pageControls;
import static com.devicenut.pixelnutctrl.Main.pageDetails;
import static com.devicenut.pixelnutctrl.Main.pageFavorites;
import static com.devicenut.pixelnutctrl.Main.pageCurrent;
import static com.devicenut.pixelnutctrl.Main.masterPager;
import static com.devicenut.pixelnutctrl.Main.pixelDensity;
import static com.devicenut.pixelnutctrl.Main.pixelHeight;

public class Master extends AppCompatActivity implements FragFavs.FavoriteSelectInterface,
                                                         FragCtrls.FavoriteDeselectInterface,
                                                         FragCtrls.FavoriteCreateInterface,
                                                         FragCtrls.PatternSelectInterface,
                                                         FragCtrls.DeviceCommandInterface,
                                                         Bluetooth.BleCallbacks
{
    private final String LOGNAME = "Master";
    private final Activity context = this;

    private boolean isEditing = false;
    private boolean helpActive = false;
    private String devNameSaved = "";

    private LinearLayout llFragPages;
    private RelativeLayout llGoToText;
    private Button pauseButton, helpButton;
    private TextView nameText;
    private TextView leftText, rightText;

    private Fragment[] myFragments = new Fragment[numFragments];
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
        if (pageFavorites < 0) return false;
        return ((FragFavs)myFragments[pageFavorites]).IsFavoritePattern(name, seg, pnum, vals);
    }

    public void onDeviceCommand(String str)
    {
        if (str.equals(CMD_PAUSE))
        {
            if (doUpdate)
            {
                // don't change text
                SendString(CMD_PAUSE);
                doUpdate = false;
            }
            // else already paused
        }
        else if (str.equals(CMD_RESUME))
        {
            if (!doUpdate)
            {
                // resuming, but user had paused, so change the text
                pauseButton.setText(getResources().getString(R.string.name_pause));
                SendString(CMD_RESUME);
                doUpdate = true;
            }
            // else already resumed
        }
        else SendString(str);
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

        masterPager = (MyPager)findViewById(R.id.myViewPager);
        FragmentPagerAdapter adapterViewPager = new MasterAdapter(getSupportFragmentManager());
        masterPager.setAdapter(adapterViewPager);
        //masterPager.setOffscreenPageLimit(3);

        llFragPages = (LinearLayout)findViewById(R.id.ll_FragPages);
        llGoToText  = (RelativeLayout)findViewById(R.id.ll_GoToText);

        pauseButton = (Button)  findViewById(R.id.button_Pause);
        helpButton  = (Button)  findViewById(R.id.button_HelpPage);
        nameText    = (TextView)findViewById(R.id.text_Devname);
        leftText    = (TextView)findViewById(R.id.text_GoLeft);
        rightText   = (TextView)findViewById(R.id.text_GoRight);

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
        // TODO: show help for Details page

        if (helpActive) // turn controls help off
        {
            ((FragFavs)myFragments[pageFavorites]).setHelpMode(false);
            ((FragCtrls)myFragments[pageControls]).setHelpMode(false);

            helpButton.setText(getResources().getString(R.string.name_help));
            helpActive = false;

            llGoToText.setVisibility(VISIBLE);
            SetFragViewPageHeight(false);
            SetupGoToText();
        }
        else
        {
            SetFragViewPageHeight(true);
            llGoToText.setVisibility(GONE);

            ((FragFavs)myFragments[pageFavorites]).setHelpMode(true);
            ((FragCtrls)myFragments[pageControls]).setHelpMode(true);

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
