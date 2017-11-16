package com.devicenut.pixelnutctrl;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import static com.devicenut.pixelnutctrl.Bluetooth.BLESTAT_SUCCESS;
import static com.devicenut.pixelnutctrl.Main.CMD_EXTMODE;
import static com.devicenut.pixelnutctrl.Main.CMD_GET_INFO;
import static com.devicenut.pixelnutctrl.Main.CMD_PAUSE;
import static com.devicenut.pixelnutctrl.Main.CMD_POP_PATTERN;
import static com.devicenut.pixelnutctrl.Main.CMD_RESUME;
import static com.devicenut.pixelnutctrl.Main.CMD_SEGS_ENABLE;
import static com.devicenut.pixelnutctrl.Main.CMD_START_END;
import static com.devicenut.pixelnutctrl.Main.TITLE_ADAFRUIT;
import static com.devicenut.pixelnutctrl.Main.TITLE_NONAME;
import static com.devicenut.pixelnutctrl.Main.TITLE_PIXELNUT;
import static com.devicenut.pixelnutctrl.Main.URL_PIXELNUT;
import static com.devicenut.pixelnutctrl.Main.basicPatternsCount;
import static com.devicenut.pixelnutctrl.Main.ble;
import static com.devicenut.pixelnutctrl.Main.curSegment;
import static com.devicenut.pixelnutctrl.Main.customPatterns;
import static com.devicenut.pixelnutctrl.Main.devName;
import static com.devicenut.pixelnutctrl.Main.devPatternCmds;
import static com.devicenut.pixelnutctrl.Main.devPatternNames;
import static com.devicenut.pixelnutctrl.Main.initPatterns;
import static com.devicenut.pixelnutctrl.Main.listEnables;
import static com.devicenut.pixelnutctrl.Main.mapIndexToPattern;
import static com.devicenut.pixelnutctrl.Main.mapPatternToIndex;
import static com.devicenut.pixelnutctrl.Main.multiStrands;
import static com.devicenut.pixelnutctrl.Main.numPatterns;
import static com.devicenut.pixelnutctrl.Main.numSegments;
import static com.devicenut.pixelnutctrl.Main.pageControls;
import static com.devicenut.pixelnutctrl.Main.pageDetails;
import static com.devicenut.pixelnutctrl.Main.pageFavorites;
import static com.devicenut.pixelnutctrl.Main.patternNames;
import static com.devicenut.pixelnutctrl.Main.pixelDensity;
import static com.devicenut.pixelnutctrl.Main.pixelLength;
import static com.devicenut.pixelnutctrl.Main.pixelWidth;
import static com.devicenut.pixelnutctrl.Main.segPatterns;
import static com.devicenut.pixelnutctrl.Main.segXmodeEnb;
import static com.devicenut.pixelnutctrl.Main.stdPatternsCount;
import static com.devicenut.pixelnutctrl.Main.useAdvPatterns;
import static com.devicenut.pixelnutctrl.Main.haveFavorites;
import static com.devicenut.pixelnutctrl.Main.pageCurrent;

public class Devices extends AppCompatActivity implements Bluetooth.BleCallbacks
{
    private final String LOGNAME = "Devices";
    private final Activity context = this;

    private TextView textConnecting;
    private TextView textSelectDevice;
    private Button buttonScan;
    private ProgressBar progressBar;
    private ProgressBar progressLine;
    private ScrollView scrollDevices;
    private final ArrayList<Integer> bleDevIDs = new ArrayList<>();

    private ReplyStrs doReply;

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
    private boolean blePresentAndEnabled = true;
    private boolean bleEnabled = false;
    private boolean isScanning = false;
    private boolean isConnecting = false;
    private boolean isConnected = false;
    private boolean resumeScanning = true;
    private boolean didFail = false;
    private boolean isDone = false;

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
        progressLine = (ProgressBar) findViewById(R.id.progress_Loader);

        blePresentAndEnabled = true;

        ble = new Bluetooth();
        if (!ble.checkForBlePresent())
        {
            Toast.makeText(this, "Cannot find Bluetooth LE", Toast.LENGTH_LONG).show();
            blePresentAndEnabled = false;
            DoFinish();
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)   != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            Log.w(LOGNAME, "Asking for location permission...");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 0);
        }

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        pixelLength = metrics.heightPixels;
        pixelWidth = metrics.widthPixels;

        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        pixelDensity = dm.densityDpi;

        Log.w(LOGNAME, "Screen: width=" + pixelWidth + " length=" + pixelLength + " density=" + pixelDensity);

        if (getResources().getBoolean(R.bool.portrait_only))
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    @Override protected void onResume()
    {
        Log.i(LOGNAME, ">>onResume");
        super.onResume();

        if (!blePresentAndEnabled) return;

        bleEnabled = ble.checkForBleEnabled();
        if (!bleEnabled)
        {
            blePresentAndEnabled = false;
            Toast.makeText(this, "Bluetooth LE not enabled", Toast.LENGTH_LONG).show();
            DoFinish();
            return;
        }

        isConnecting = false;
        isConnected = false;
        ble.setCallbacks(this);

        if (resumeScanning) StartScanning();
        else resumeScanning = true;
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

    private void DoFinish()
    {
        new Thread()
        {
            @Override public void run()
            {
                SleepMsecs(2000);
                Devices.this.finish();
            }
        }.start();
    }

    public void onClick(View v)
    {
        switch (v.getId())
        {
            case R.id.text_Header:
            {
                if (isScanning) StopScanning();
                else resumeScanning = false;

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
                if (isScanning) StopScanning();
                else resumeScanning = false;

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

                        doReply = new ReplyStrs();

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
        progressLine.setVisibility(View.GONE);

        if (isConnecting)
        {
            HideButtons();
            textSelectDevice.setVisibility(View.GONE);
            textConnecting.setVisibility(View.VISIBLE);
            textConnecting.setText(getResources().getString(R.string.title_connecting));
            progressBar.setVisibility(View.VISIBLE);
            buttonScan.setText(getResources().getString(R.string.name_cancel));
        }
        else if (isScanning)
        {
            HideButtons();
            textSelectDevice.setVisibility(View.VISIBLE);
            textConnecting.setVisibility(View.GONE);
            progressBar.setVisibility(View.VISIBLE);
            buttonScan.setText(getResources().getString(R.string.name_stop));
        }
        else // scan stopped, waiting for user
        {
            textSelectDevice.setVisibility(View.VISIBLE);
            textConnecting.setVisibility(View.GONE);
            progressBar.setVisibility(View.GONE);
            buttonScan.setText(getResources().getString(R.string.name_scan));
        }

        buttonScan.setTextColor(ContextCompat.getColor(context, R.color.UserChoice));
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
        StopScanning();
        ble.disconnect();
    }

    private synchronized void DeviceFailed(final String errstr)
    {
        if (didFail) return;
        didFail = true;

        Log.w(LOGNAME, errstr);

        DoDisconnect();

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
            String dspname = "";
            boolean haveone = false;

            if (name.startsWith(TITLE_PIXELNUT))
            {
                haveone = true;
                dspname = name.substring(2);
            }
            else if (name.startsWith(TITLE_ADAFRUIT))
            {
                haveone = true;
                dspname = TITLE_NONAME;
            }

            if (haveone)
            {
                if (buttonCount < listButtons.length)
                {
                    Button b = (Button) findViewById(listButtons[buttonCount]);
                    b.setText(dspname);
                    b.setVisibility(View.VISIBLE);

                    ++buttonCount;
                    bleDevIDs.add(id);

                    scrollDevices.post(new Runnable()
                    {
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
            isConnected = true; // actually connected now
            // note: still have isConnecting set as well

            new Thread()
            {
                @Override public void run()
                {

                    isDone = false;
                    SleepMsecs(500); // don't send too soon...hack!
                    Log.d(LOGNAME, "Sending command: " + CMD_PAUSE);
                    ble.WriteString(CMD_PAUSE);
                    Log.d(LOGNAME, "Sending command: " + CMD_GET_INFO);
                    ble.WriteString(CMD_GET_INFO);
                }

            }.start();
        }
        else DeviceFailed("Connection Failed: Try Again");
    }

    @Override public void onDisconnect()
    {
        final String errstr = "Device Disconnected: Try Again";
        Log.w(LOGNAME, errstr);

        isConnecting = false;
        isConnected = false;

        context.runOnUiThread(new Runnable()
        {
            public void run()
            {
                Toast.makeText(context, errstr, Toast.LENGTH_SHORT).show();
                StartScanning(); // return to scanning
            }
        });
    }

    @Override public void onWrite(final int status)
    {
        if (status != 0)
        {
            Log.e(LOGNAME, "Write status: " + status);
            DeviceFailed("Write Failed: Try Again");
        }
    }

    @Override public void onRead(String rstr)
    {
        rstr = rstr.trim();
        Log.v(LOGNAME, "Reply=" + rstr);

        if (isConnecting)
        {
            isConnecting = false;
            //isConnected = true; // now set in onConnect()

            // no longer able to Cancel now
            buttonScan.post(new Runnable()
            {
                @Override public void run()
                {
                    progressBar.setVisibility(View.INVISIBLE);
                    textConnecting.setText(getResources().getString(R.string.title_configuring));
                    progressLine.setProgress(0);
                    progressLine.setVisibility(View.VISIBLE);

                    buttonScan.setEnabled(false);
                    buttonScan.setText(getResources().getString(R.string.name_wait));
                    buttonScan.setTextColor(Color.GRAY);
                }
            });
        }

        int reply;
        try { reply = doReply.Next(rstr); }
        catch(Exception e)
        {
            e.printStackTrace();
            DeviceFailed("Device Config Failed");
            return;
        }
        switch (reply)
        {
            case -1:
            {
                DeviceFailed("Device Not Recognized: Try Again");
                break;
            }
            case 1:
            {
                UpdateProgress();
                break;
            }
            case 2:
            {
                UpdateProgress();
                SendCommand();
                break;
            }
            case 3:
            {
                isDone = true;
                UpdateProgress();
                break;
            }
        }
    }

    private void SendCommand()
    {
        new Thread()
        {
            @Override public void run()
            {
                SleepMsecs(500); // don't send too soon...hack!
                Log.d(LOGNAME, "Sending command: " + doReply.sendCmdStr);
                ble.WriteString(doReply.sendCmdStr);
            }

        }.start();
    }

    private void UpdateProgress()
    {
        progressLine.post(new Runnable()
        {
            @Override public void run()
            {
                doReply.progressPercent += doReply.progressPcentInc;
                progressLine.setProgress((int)doReply.progressPercent);
                Log.v(LOGNAME, "Progress=" + (int)doReply.progressPercent);

                if (isDone)
                {
                    new Thread()
                    {
                        @Override public void run()
                        {
                            SleepMsecs(500); // don't send too soon...hack!
                            Log.d(LOGNAME, "Sending command: " + CMD_RESUME);
                            ble.WriteString(CMD_RESUME);
                        }

                    }.start();

                    Log.i(LOGNAME, ">>> Device Setup Successful <<<");
                    SleepMsecs(250); // allow time for display update

                    SetupDeviceControls();
                    startActivity( new Intent(Devices.this, Master.class) );
                }
            }
        });
    }

    private void SendString(String str)
    {
        ble.WriteString(str);
    }

    private void SetupDeviceControls()
    {
        if ((customPatterns != 0) || !useAdvPatterns || (numSegments > 1)) // ((numSegments > 1) && !multiStrands))
            haveFavorites = false;

        devName = ble.getCurDevName();
        if ((devName == null) || (devName.length() < 3)) // have disconnected
        {
            Log.w(LOGNAME, "Lost connection (no device name)");
            Toast.makeText(context, "Lost connection", Toast.LENGTH_SHORT).show();
            onBackPressed();
            return;
        }

        if (devName.startsWith(TITLE_PIXELNUT))
             devName = devName.substring(2);
        else devName = TITLE_NONAME;
        Log.d(LOGNAME, "Device name=" + devName);

        CreatePatternArrays();

        // if only using basic patterns, then the properties will always be displayed
        // so enable the properties for any segment which is currently disabled
        if (!useAdvPatterns)
        {
            Log.d(LOGNAME, "Enabling Properties:");
            for (int i = 0; i < numSegments; ++i)
            {
                if (!segXmodeEnb[i])
                {
                    int seg = i + 1;
                    segXmodeEnb[i] = true;
                    SendString(CMD_SEGS_ENABLE + seg);
                    SendString(CMD_EXTMODE + "1");
                }
            }
        }

        if (initPatterns && multiStrands) // initialize all of physical strands
        {
            Log.d(LOGNAME, "Initializing Patterns:");
            for (int i = 0; i < numSegments; ++i)
            {
                int seg = i+1; // segment numbers start at 1 on device
                int pnum = segPatterns[i]+1; // same with pattern numbers
                Log.d(LOGNAME, "  segment=" + seg + " pattern==" + pnum);

                SendString(CMD_SEGS_ENABLE + seg);
                SendString(CMD_START_END);; // start sequence
                SendString(CMD_POP_PATTERN);
                SendString(devPatternCmds[pnum-1]);
                SendString(CMD_START_END);; // end sequence
                SendString("" + pnum); // store pattern number
            }
        }

        curSegment = 0; // always start with first segment
        if (numSegments > 1) SendString(CMD_SEGS_ENABLE + "1");

        if (haveFavorites)
        {
            pageFavorites = 0;
            pageControls = 1;
            pageDetails = 2;
        }
        else
        {
            pageFavorites = -1;
            pageControls = 0;
            pageDetails = 1;
        }
        pageCurrent = 0;
    }

    private void CreatePatternArrays()
    {
        int j = 0;
        int extra = (customPatterns > 0) ? 1 : 0;
        if (stdPatternsCount > 0) extra += 2;
        if ((customPatterns == 0) && !useAdvPatterns)
        {
            extra = 0;
            numPatterns = basicPatternsCount;
        }
        patternNames = new String[numPatterns + extra];
        listEnables = new boolean[numPatterns + extra];
        mapIndexToPattern = new int[numPatterns + extra];
        mapPatternToIndex = new int[numPatterns];

        if (customPatterns > 0)
        {
            patternNames[j] = "Custom Patterns";
            listEnables[j] = false;
            mapIndexToPattern[j] = 0;
            ++j;
        }
        else if (useAdvPatterns)
        {
            patternNames[j] = "Basic Patterns";
            listEnables[j] = false;
            mapIndexToPattern[j] = 0;
            ++j;
        }

        for (int i = 0; i < numPatterns; ++i)
        {
            if ((i > 0) && (i == customPatterns) && useAdvPatterns)
            {
                patternNames[j] = "Basic Patterns";
                listEnables[j] = false;
                mapIndexToPattern[j] = 0;
                ++j;
            }

            if ((i > customPatterns) && (i == basicPatternsCount) && useAdvPatterns)
            {
                patternNames[j] = "Advanced Patterns";
                listEnables[j] = false;
                mapIndexToPattern[j] = 0;
                ++j;
            }

            Log.v(LOGNAME, "Adding pattern i=" + i + " j=" + j + " => " + devPatternNames[i]);

            patternNames[j] = devPatternNames[i];
            listEnables[j] = true;
            mapIndexToPattern[j] = i;
            mapPatternToIndex[i] = j;
            ++j;
        }
    }
}
