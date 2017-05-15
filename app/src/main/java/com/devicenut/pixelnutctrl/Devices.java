package com.devicenut.pixelnutctrl;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import static com.devicenut.pixelnutctrl.Bluetooth.BLESTAT_SUCCESS;
import static com.devicenut.pixelnutctrl.Main.CMD_GETINFO;
import static com.devicenut.pixelnutctrl.Main.MAXVAL_HUE;
import static com.devicenut.pixelnutctrl.Main.MAXVAL_PATTERN;
import static com.devicenut.pixelnutctrl.Main.MAXVAL_PERCENT;
import static com.devicenut.pixelnutctrl.Main.URL_PIXELNUT;
import static com.devicenut.pixelnutctrl.Main.countLayers;
import static com.devicenut.pixelnutctrl.Main.countPixels;
import static com.devicenut.pixelnutctrl.Main.countTracks;
import static com.devicenut.pixelnutctrl.Main.curBright;
import static com.devicenut.pixelnutctrl.Main.curDelay;
import static com.devicenut.pixelnutctrl.Main.curPattern;
import static com.devicenut.pixelnutctrl.Main.rangeDelay;
import static com.devicenut.pixelnutctrl.Main.xmodeEnabled;
import static com.devicenut.pixelnutctrl.Main.xmodeHue;
import static com.devicenut.pixelnutctrl.Main.xmodePixCnt;
import static com.devicenut.pixelnutctrl.Main.xmodeWhite;

public class Devices extends AppCompatActivity implements Bluetooth.BleCallbacks
{
    private final static String LOGNAME = "Devices";

    private final static String TITLE_PIXELNUT = "PixelNut!";

    private final Activity context = this;
    private TextView textConnecting;
    private TextView textSelectDevice;
    private Button buttonScan;
    private ProgressBar progressBar;
    private ScrollView scrollDevices;
    private final ArrayList<Integer> bleDevIDs = new ArrayList<>();
    private Bluetooth ble;

    //private Toast myToast; // for debugging

    private final int[] listButtons =
    {
        R.id.button_Device1,
        R.id.button_Device2,
        R.id.button_Device3,
        R.id.button_Device4,
        R.id.button_Device5,
        R.id.button_Device6,
        R.id.button_Device7,
        R.id.button_Device8,
    };

    private int buttonCount = 0;
    private boolean bleEnabled = false;
    private boolean isScanning = false;
    private boolean isConnecting = false;
    private boolean isConnected = false;
    private boolean didFail = false;

    private int replyState = 0;
    private boolean replyFail = false;

    @Override protected void onCreate(Bundle savedInstanceState)
    {
        Log.i(LOGNAME, ">>onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_devices);

        textSelectDevice = (TextView) findViewById(R.id.text_SelectDevice);
        textConnecting = (TextView) findViewById(R.id.text_Connecting);
        buttonScan = (Button) findViewById(R.id.button_ScanStop);
        scrollDevices = (ScrollView) findViewById(R.id.scroll_Devices);
        progressBar = (ProgressBar) findViewById(R.id.progress_Scanner);

        ble = new Bluetooth(this);
        if (!ble.checkForBlePresent())
        {
            Toast.makeText(this, "Cannot find Bluetooth LE", Toast.LENGTH_LONG).show();
            threadFinish.start();
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)   != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            Log.w(LOGNAME, "Asking for location permission...");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 0);
        }
    }

    @Override protected void onResume()
    {
        Log.i(LOGNAME, ">>onResume");
        super.onResume();

        bleEnabled = ble.checkForBleEnabled();
        if (!bleEnabled)
        {
            Toast.makeText(this, "Bluetooth LE not enabled", Toast.LENGTH_LONG).show();
            threadFinish.start();
        }
        else
        {
            isConnecting = false;
            isConnected = false;
            ble.setCallbacks(this);
            StartScanning();
        }
    }

    @Override protected void onPause()
    {
        Log.i(LOGNAME, ">>onPause");
        super.onPause();

        if (bleEnabled) StopScanning();
        //if (myToast != null) myToast.cancel();
    }

    @Override public void onBackPressed()
    {
        if (isConnected) DoDisconnect();

        super.onBackPressed();
    }

    private void SleepMsecs(int msecs)
    {
        //noinspection EmptyCatchBlock
        try { Thread.sleep(msecs); }
        catch (Exception ignored) {}
    }

    private final Thread threadFinish = new Thread()
    {
        @Override public void run()
        {
            SleepMsecs(2000);
            Devices.this.finish();
        }
    };

    public void onClick(View v)
    {
        switch (v.getId())
        {
            case R.id.text_Header:
            {
                StopScanning();
                startActivity(new Intent(this, About.class));
                break;
            }
            case R.id.button_ScanStop:
            {
                if (!isConnected)
                {
                    if (isConnecting)
                    {
                        Log.d(LOGNAME, "Canceling connection...");
                        DoDisconnect();
                        StartScanning();
                    }
                    else if (isScanning) StopScanning();
                    else StartScanning();
                }
                break;
            }
            case R.id.button_Website:
            {
                StopScanning();

                Uri uri = Uri.parse(URL_PIXELNUT);
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
                break;
            }
            default:
            {
                StopScanning();

                for (int i = 0; i < listButtons.length; ++i)
                {
                    if (listButtons[i] == v.getId())
                    {
                        int devid = bleDevIDs.get(i);
                        Log.v(LOGNAME, "ButtonID=" + i + " DeviceID=" + devid + " DevName=" + ble.getDevNameFromID(devid));

                        replyState = 0;
                        replyFail = false;

                        if (ble.connect(devid))
                        {
                            isConnecting = true;
                            SetupUserDisplay();
                        }
                        else Toast.makeText(context, "Cannot connect: retry", Toast.LENGTH_SHORT).show();
                        break;
                    }
                }
                break;
            }
        }
    }

    private void HideButtons()
    {
        for (int listButton : listButtons)
        {
            Button b = (Button) findViewById(listButton);
            b.setVisibility(View.GONE);
        }
        bleDevIDs.clear();
        buttonCount = 0;
    }

    private void SetupUserDisplay()
    {
        if (isConnecting)
        {
            HideButtons();
            textSelectDevice.setVisibility(View.GONE);
            textConnecting.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.VISIBLE);
            buttonScan.setText(getResources().getString(R.string.name_cancel));
        }
        else if (isScanning)
        {
            HideButtons();
            textConnecting.setVisibility(View.INVISIBLE);
            textSelectDevice.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.VISIBLE);
            buttonScan.setText(getResources().getString(R.string.name_stop));
        }
        else // scan stopped, waiting for user
        {
            progressBar.setVisibility(View.INVISIBLE);
            textConnecting.setVisibility(View.INVISIBLE);
            textSelectDevice.setVisibility(View.VISIBLE);
            buttonScan.setText(getResources().getString(R.string.name_scan));
        }
        buttonScan.setEnabled(true);

        didFail = false;
    }

    private void StartScanning()
    {
        if (!isScanning)
        {
            Log.d(LOGNAME, "Start scanning...");
            isScanning = true;
            SetupUserDisplay();
            ble.startScanning();
        }
    }

    private void StopScanning()
    {
        if (isScanning)
        {
            Log.d(LOGNAME, "Stop scanning...");
            isScanning = false;
            SetupUserDisplay();
            ble.stopScanning();
        }
    }

    private void DoDisconnect()
    {
        Log.d(LOGNAME, "Disconnecting...");
        isConnecting = false;
        isConnected = false;
        ble.disconnect();
    }

    private synchronized void DeviceFailed(final String errstr)
    {
        if (didFail) return;
        didFail = true;

        Log.w(LOGNAME, errstr);

        DoDisconnect();
        isScanning = false;

        context.runOnUiThread(new Runnable()
        {
            public void run()
            {
                Toast.makeText(context, errstr, Toast.LENGTH_SHORT).show();
                SetupUserDisplay();
            }
        });
    }

    @Override public void onScan(final String name, int id)
    {
        if (isScanning && !isConnecting && !isConnected && (name != null))
        {
            if (name.startsWith("P!"))
            {
                if (buttonCount < listButtons.length)
                {
                    Button b = (Button) findViewById(listButtons[buttonCount]);
                    b.setText(name.substring(2));
                    b.setVisibility(View.VISIBLE);

                    ++buttonCount;
                    bleDevIDs.add(id);

                    scrollDevices.post(new Runnable() {
                        @Override public void run() {
                            scrollDevices.scrollTo(0, scrollDevices.getBottom());
                        }
                    });
                }
                else StopScanning();
            }
            /*
            else context.runOnUiThread(new Runnable()
            {
                public void run()
                {
                    myToast = Toast.makeText(context, name, Toast.LENGTH_SHORT);
                    myToast.show();
                }
            });
            */
        }
    }

    @Override public void onConnect(final int status)
    {
        if (status == BLESTAT_SUCCESS)
        {
            Log.i(LOGNAME, "Connected to our device <<<<<<<<<<");
            isConnecting = false;
            isConnected = true;

            // no longer able to Cancel now
            buttonScan.post(new Runnable()
            {
                @Override public void run()
                {
                    buttonScan.setText(getResources().getString(R.string.name_wait));
                    buttonScan.setEnabled(false);
                }
            });

            new Thread()
            {
                @Override public void run()
                {
                    SleepMsecs(500); // don't send too soon...hack!

                    Log.d(LOGNAME, "Sending GetInfo command...");
                    ble.WriteString(CMD_GETINFO);
                }

            }.start();
        }
        else DeviceFailed("Connection Failed: Try Again");
    }

    @Override public void onDisconnect()
    {
        DeviceFailed("Device Disconnected: Try Again");
    }

    @Override public void onWrite(final int status)
    {
        if (status != 0)
        {
            Log.e(LOGNAME, "Write status: " + Integer.toHexString(status));
            DeviceFailed("Write Failed: Try Again");
        }
    }

    @Override public void onRead(String reply)
    {
        if (!isConnected || replyFail) return;

        reply = reply.trim();
        Log.v(LOGNAME, "Reply=" + reply);

        switch (replyState)
        {
            case 0:
            {
                if (reply.contains(TITLE_PIXELNUT)) ++replyState;
                else Log.w(LOGNAME, "Expected title: " + reply);
                break;
            }
            case 1: // line 1: # of lines
            {
                String[] strs = reply.split(" ");
                if ((strs.length == 1) && (Integer.parseInt(reply) == 3))
                    ++replyState;
                else replyFail = true;
                break;
            }
            case 2: // line 2: 4 device constants
            {
                String[] strs = reply.split(" ");
                if (strs.length == 4)
                {
                    countPixels = Integer.parseInt(strs[0]);
                    countLayers = Integer.parseInt(strs[1]);
                    countTracks = Integer.parseInt(strs[2]);
                    rangeDelay  = Integer.parseInt(strs[3]);
                    Log.d(LOGNAME, "Constants: Pixels=" + countPixels + " Layers=" + countLayers + " Tracks=" + countTracks + " RangeDelay=" + rangeDelay);

                    if (CheckValue(countPixels, 1, 0) &&
                        CheckValue(countLayers, 2, 0) &&
                        CheckValue(countTracks, 1, 0))
                        ++replyState;

                    else replyFail = true;
                }
                else replyFail = true;
                break;
            }
            case 3: // line 3: 4 extern mode values
            {
                String[] strs = reply.split(" ");
                if (strs.length == 4)
                {
                    xmodeEnabled = (Integer.parseInt(strs[0]) != 0);
                    xmodeHue     = Integer.parseInt(strs[1]);
                    xmodeWhite   = Integer.parseInt(strs[2]);
                    xmodePixCnt  = Integer.parseInt(strs[3]);
                    Log.d(LOGNAME, "Externs: Enable=" + xmodeEnabled + " Hue=" + xmodeHue + " White=" + xmodeWhite + " PixCnt=" + xmodePixCnt);

                    if (CheckValue(xmodeHue, 0, MAXVAL_HUE) &&
                        CheckValue(xmodeWhite, 0, MAXVAL_PERCENT) &&
                        CheckValue(xmodePixCnt, 0, MAXVAL_PERCENT))
                        ++replyState;

                    else replyFail = true;
                }
                else replyFail = true;
                break;
            }
            case 4: // line 4: 3 current settings
            {
                String[] strs = reply.split(" ");
                if (strs.length == 3)
                {
                    curPattern  = Integer.parseInt(strs[0]);
                    curDelay    = Integer.parseInt(strs[1]);
                    curBright   = Integer.parseInt(strs[2]);
                    Log.d(LOGNAME, "Current: Pattern=" + curPattern + " Delay=" + curDelay + " Bright=" + curBright);

                    if (CheckValue(curPattern, 1, MAXVAL_PATTERN) &&
                        CheckValue(curBright, 0, MAXVAL_PERCENT))
                    {
                        // allow for bad current delay value
                        if (curDelay < -rangeDelay) curDelay = -rangeDelay;
                        else if (curDelay > rangeDelay) curDelay = rangeDelay;

                        Log.i(LOGNAME, "Communication successful <<<<<<<<<<");
                        startActivity( new Intent(Devices.this, Controls.class) );
                    }
                    else replyFail = true;
                }
                else replyFail = true;
                break;
            }
        }

        if (replyFail)
        {
            Log.e(LOGNAME, "Read failed: state=" + replyState);
            DeviceFailed("Device Not Recognized: Try Again");
        }
    }

    private boolean CheckValue(int val, int min, int max)
    {
        if (val < min) return false;
        if ((0 < max) && (max < val)) return false;
        return true;
    }
}
