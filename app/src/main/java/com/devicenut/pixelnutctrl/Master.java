package com.devicenut.pixelnutctrl;

import android.app.Activity;
import android.content.Intent;
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
import static com.devicenut.pixelnutctrl.Main.TITLE_NONAME;
import static com.devicenut.pixelnutctrl.Main.TITLE_PIXELNUT;
import static com.devicenut.pixelnutctrl.Main.ble;
import static com.devicenut.pixelnutctrl.Main.devName;
import static com.devicenut.pixelnutctrl.Main.doUpdate;
import static com.devicenut.pixelnutctrl.Main.haveFavorites;
import static com.devicenut.pixelnutctrl.Main.masterPager;
import static com.devicenut.pixelnutctrl.Main.multiStrands;
import static com.devicenut.pixelnutctrl.Main.numSegments;

public class Master extends AppCompatActivity implements FragFavs.OnFragmentInteractionListener, FragCtrls.OnFragmentInteractionListener, Bluetooth.BleCallbacks
{
    private final String LOGNAME = "Master";
    private final Activity context = this;

    private boolean isConnected = false;
    private boolean isEditing = false;
    private boolean helpActive = false;
    private String devNameSaved = "";

    private TextView nameText;
    private Button pauseButton, helpButton;
    private TextView helpText, helpTitle;
    private ScrollView helpPage;
    private LinearLayout viewPager;

    FragmentPagerAdapter adapterViewPager;

    @Override protected void onCreate(Bundle savedInstanceState)
    {
        Log.d(LOGNAME, ">>onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_master);

        masterPager = (MyPager)findViewById(R.id.myViewPager);
        adapterViewPager = new MasterAdapter(getSupportFragmentManager());
        masterPager.setAdapter(adapterViewPager);
        //masterPager.setPagingEnabled(false);

        viewPager       = (LinearLayout) findViewById(R.id.ll_ViewPager);
        helpPage        = (ScrollView)   findViewById(R.id.ll_HelpPage);
        pauseButton     = (Button)       findViewById(R.id.button_Pause);
        helpButton      = (Button)       findViewById(R.id.button_ControlsHelp);
        helpTitle       = (TextView)     findViewById(R.id.view_HelpTitle);
        helpText        = (TextView)     findViewById(R.id.view_HelpText);
        nameText        = (TextView)     findViewById(R.id.text_Devname);
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
            ble = new Bluetooth(this);
            ble.setCallbacks(this);

            devName = ble.getCurDevName();
            if ((devName == null) || (devName.length() < 3)) // have disconnected
            {
                Log.w(LOGNAME, "Lost connection (no device name)");
                Toast.makeText(context, "Lost connection", Toast.LENGTH_SHORT).show();
                onBackPressed();
                return;
            }

            isConnected = true;

            if (devName.startsWith(TITLE_PIXELNUT))
                 devName = devName.substring(2);
            else devName = TITLE_NONAME;
            Log.d(LOGNAME, "Device name=" + devName);
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
            viewPager.setVisibility(VISIBLE);

            helpButton.setText(getResources().getString(R.string.name_help));
            helpActive = false;
        }
        else
        {
            viewPager.setVisibility(GONE);
            helpPage.setVisibility(VISIBLE);

            String str = getResources().getString(R.string.text_help_head);
            if (numSegments > 1)
            {
                if (multiStrands)
                    str += getResources().getString(R.string.text_help_segs_physical);
                else str += getResources().getString(R.string.text_help_segs_logical);
            }
            helpText.setText(str + getResources().getString(R.string.text_help_tail));

            helpButton.setText(getResources().getString(R.string.name_controls));
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

    public void onFragmentInteraction(String str)
    {
        Log.d(LOGNAME, "Fragment says: " + str);
        SendString(str);
    }

    public static class MasterAdapter extends FragmentPagerAdapter
    {
        private final String LOGNAME = "Master";

        public MasterAdapter(FragmentManager fragmentManager)
        {
            super(fragmentManager);
            Log.d(LOGNAME, ">>Adapter");
        }

        @Override public int getCount() { return (haveFavorites  ? 2 : 1); } // number of pages

        @Override public Fragment getItem(int position)
        {
            Log.d(LOGNAME, "Select fragment " + position);
            if (position == 0) return (haveFavorites ? FragFavs.newInstance() : FragCtrls.newInstance());
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
