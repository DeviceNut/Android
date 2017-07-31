package com.devicenut.pixelnutctrl;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
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
import static com.devicenut.pixelnutctrl.Main.CMD_GETINFO;
import static com.devicenut.pixelnutctrl.Main.MAXVAL_HUE;
import static com.devicenut.pixelnutctrl.Main.MAXVAL_PATTERN;
import static com.devicenut.pixelnutctrl.Main.MAXVAL_PERCENT;
import static com.devicenut.pixelnutctrl.Main.TITLE_PIXELNUT;
import static com.devicenut.pixelnutctrl.Main.URL_PIXELNUT;
import static com.devicenut.pixelnutctrl.Main.ble;
import static com.devicenut.pixelnutctrl.Main.countLayers;
import static com.devicenut.pixelnutctrl.Main.countPixels;
import static com.devicenut.pixelnutctrl.Main.countTracks;
import static com.devicenut.pixelnutctrl.Main.curBright;
import static com.devicenut.pixelnutctrl.Main.curDelay;
import static com.devicenut.pixelnutctrl.Main.curPattern;
import static com.devicenut.pixelnutctrl.Main.customPatterns;
import static com.devicenut.pixelnutctrl.Main.customPlugins;
import static com.devicenut.pixelnutctrl.Main.editPatterns;
import static com.devicenut.pixelnutctrl.Main.stdPatternNames;
import static com.devicenut.pixelnutctrl.Main.stdPatternCmds;
import static com.devicenut.pixelnutctrl.Main.stdPatternBits;
import static com.devicenut.pixelnutctrl.Main.stdPatternsCount;
import static com.devicenut.pixelnutctrl.Main.devPatternNames;
import static com.devicenut.pixelnutctrl.Main.devPatternCmds;
import static com.devicenut.pixelnutctrl.Main.devPatternBits;
import static com.devicenut.pixelnutctrl.Main.maxlenCmdStrs;
import static com.devicenut.pixelnutctrl.Main.numSegments;
import static com.devicenut.pixelnutctrl.Main.numPatterns;
import static com.devicenut.pixelnutctrl.Main.pixelDensity;
import static com.devicenut.pixelnutctrl.Main.pixelLength;
import static com.devicenut.pixelnutctrl.Main.pixelWidth;
import static com.devicenut.pixelnutctrl.Main.posSegStart;
import static com.devicenut.pixelnutctrl.Main.posSegCount;
import static com.devicenut.pixelnutctrl.Main.rangeDelay;
import static com.devicenut.pixelnutctrl.Main.xmodeEnabled;
import static com.devicenut.pixelnutctrl.Main.xmodeHue;
import static com.devicenut.pixelnutctrl.Main.xmodePixCnt;
import static com.devicenut.pixelnutctrl.Main.xmodeWhite;

public class Devices extends AppCompatActivity implements Bluetooth.BleCallbacks
{
    private final String LOGNAME = "Devices";
    private final Activity context = this;

    private TextView textConnecting;
    private TextView textSelectDevice;
    private Button buttonScan;
    private ProgressBar progressBar;
    private ProgressBar progressLine;
    private Handler progressHandler = new Handler();
    private ProgressDialog progressDialog;
    private ScrollView scrollDevices;
    private final ArrayList<Integer> bleDevIDs = new ArrayList<>();

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

    private int replyState = 0;
    private int optionLines = 0;
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
        progressLine = (ProgressBar) findViewById(R.id.progress_Loader);

        /*
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Reading device configuration...");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setProgress(0);
        progressDialog.setMax(100);
        */

        blePresentAndEnabled = true;

        ble = new Bluetooth(this);
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
    }

    @Override protected void onResume()
    {
        Log.i(LOGNAME, ">>onResume");
        super.onResume();

        if (!blePresentAndEnabled) return;

        bleEnabled = ble.checkForBleEnabled();
        if (!bleEnabled)
        {
            Toast.makeText(this, "Bluetooth LE not enabled", Toast.LENGTH_LONG).show();
            DoFinish();
            blePresentAndEnabled = false;
        }
        else
        {
            isConnecting = false;
            isConnected = false;
            ble.setCallbacks(this);

            if (resumeScanning) StartScanning();
            else resumeScanning = true;
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
            if (name.startsWith("P!"))
            {
                if (buttonCount < listButtons.length)
                {
                    Button b = (Button) findViewById(listButtons[buttonCount]);
                    b.setText(name.substring(2));
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
            isConnecting = false;
            isConnected = true;

            // no longer able to Cancel now
            buttonScan.post(new Runnable()
            {
                @Override public void run()
                {
                    progressBar.setVisibility(View.INVISIBLE);
                    textConnecting.setText(getResources().getString(R.string.title_configuring));
                    progressLine.setProgress(0);
                    progressLine.setVisibility(View.VISIBLE);

                    buttonScan.setText(getResources().getString(R.string.name_wait));
                    buttonScan.setEnabled(false);
                }
            });

            /*
            progressPercent = 0;
            progressHandler.post(new Runnable()
            {
                @Override public void run()
                {
                    progressDialog.show();
                }
            });
            */

            new Thread()
            {
                @Override public void run()
                {

                    SleepMsecs(500); // don't send too soon...hack!
                    didFinishReading = false;
                    Log.d(LOGNAME, "Sending command: " + CMD_GETINFO);
                    ble.WriteString(CMD_GETINFO);
                }

            }.start();
        }
        else DeviceFailed("Connection Failed: Try Again");
    }

    @Override public void onDisconnect()
    {
        final String errstr = "Device Disconnected: Try Again";
        Log.w(LOGNAME, errstr);

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

    @Override public void onWrite(final int status)
    {
        if (status != 0)
        {
            Log.e(LOGNAME, "Write status: " + status);
            DeviceFailed("Write Failed: Try Again");
        }
    }

    @Override public void onRead(String reply)
    {
        reply = reply.trim();
        Log.v(LOGNAME, "Reply=" + reply);

        if (!replyFail)
        {
            if (didFinishReading)
            {
                Log.e(LOGNAME, "Reply after finish: " + reply);
                replyFail = true;
                return;
            }
            else if (!isConnected || ((replyState > 1) && (optionLines <= 0)))
            {
                Log.w(LOGNAME, "Unexpected reply: " + reply);
                replyFail = true;
            }
            else if (getSegments)
            {
                if (replyState == 1)
                {
                    String[] strs = reply.split(" ");
                    for (int i = 0, j = 0; i < strs.length; ++j)
                    {
                        int val1 = Integer.parseInt(strs[i++]);
                        int val2 = Integer.parseInt(strs[i++]);
                        Log.v(LOGNAME, "Segment " + i + ": " + val2 + "." + val2);

                        if ((val1 < 0) || (val1 >= countPixels-1) ||
                            (val2 < 0) || (val2 >= (countPixels-val1)))
                        {
                            replyFail = true;
                            break;
                        }

                        posSegStart[j] = val1;
                        posSegCount[j] = val2;

                        if (i >= 12) break; // only support for 6 segments, just ignore if more
                    }

                    getSegments = false; // only single line to read
                    replyState = 2; // trigger completion
                    optionLines = 0;
                }
                else replyFail = true;
            }
            else if (getPatterns)
            {
                int index = (replyState-1)/2;
                if (index < customPatterns)
                {
                    if (((replyState-1) & 1) == 0)
                    {
                        devPatternNames[index] = new String(reply);
                        ++devPatternOffset;
                    }
                    else
                    {
                        devPatternCmds[index] = new String(reply);
                        devPatternBits[index] = 0;

                        String[] strs = reply.split(" ");
                        for (int i = 0; i < strs.length; ++i)
                        {
                            if ((strs[i].charAt(0) == 'Q') && (strs[i].length() > 1))
                            {
                                int val = Integer.parseInt(strs[i].substring(1));
                                devPatternBits[index] |= val;
                            }
                        }
                    }

                    if (--optionLines == 0) getPatterns = false; // finished with patterns

                    else ++replyState;
                }
                else replyFail = true;
            }
            else if (getPlugins)
            {
                throw new NullPointerException("Custom Plugins Not Supported Yet");
            }
            else switch(replyState)
            {
                case 0: // first line: title
                {
                    if (reply.contains(TITLE_PIXELNUT)) ++replyState;
                    else Log.w(LOGNAME, "Expected title: " + reply);
                    progressPcentInc = 5;
                    break;
                }
                case 1: // second line: # of additional lines
                {
                    String[] strs = reply.split(" ");
                    optionLines = Integer.parseInt(reply);
                    if ((strs.length == 1) && (optionLines >= 3))
                        ++replyState;

                    else replyFail = true;

                    progressPercent = 0;
                    progressPcentInc = 100/(optionLines+1);
                    break;
                }
                case 2: // additional line 1: 4 device constants
                {
                    String[] strs = reply.split(" ");
                    if (strs.length == 4)
                    {
                        countPixels = Integer.parseInt(strs[0]);
                        countLayers = Integer.parseInt(strs[1]);
                        countTracks = Integer.parseInt(strs[2]);
                        rangeDelay = Integer.parseInt(strs[3]);
                        Log.d(LOGNAME, "Constants: Pixels=" + countPixels + " Layers=" + countLayers + " Tracks=" + countTracks + " RangeDelay=" + rangeDelay);

                        if (!CheckValue(countPixels, 1, 0) ||
                                !CheckValue(countLayers, 2, 0) ||
                                !CheckValue(countTracks, 1, 0))
                            replyFail = true;
                    }
                    else replyFail = true;

                    ++replyState;
                    --optionLines;
                    break;
                }
                case 3: // additional line 2: 4 extern mode values
                {
                    String[] strs = reply.split(" ");
                    if (strs.length == 4)
                    {
                        xmodeEnabled = (Integer.parseInt(strs[0]) != 0);
                        xmodeHue = Integer.parseInt(strs[1]);
                        xmodeWhite = Integer.parseInt(strs[2]);
                        xmodePixCnt = Integer.parseInt(strs[3]);
                        Log.d(LOGNAME, "Externs: Enable=" + xmodeEnabled + " Hue=" + xmodeHue + " White=" + xmodeWhite + " PixCnt=" + xmodePixCnt);

                        if (!CheckValue(xmodeHue, 0, MAXVAL_HUE) ||
                                !CheckValue(xmodeWhite, 0, MAXVAL_PERCENT) ||
                                !CheckValue(xmodePixCnt, 0, MAXVAL_PERCENT))
                            replyFail = true;
                    }
                    else replyFail = true;

                    ++replyState;
                    --optionLines;
                    break;
                }
                case 4: // additional line 3: 3 current settings
                {
                    String[] strs = reply.split(" ");
                    if (strs.length == 3)
                    {
                        curPattern = Integer.parseInt(strs[0]);
                        curDelay = Integer.parseInt(strs[1]);
                        curBright = Integer.parseInt(strs[2]);
                        Log.d(LOGNAME, "Current: Pattern=" + curPattern + " Delay=" + curDelay + " Bright=" + curBright);

                        if (curPattern == 0) curPattern = 1; // ok to reset to default
                        if (CheckValue(curPattern, 1, MAXVAL_PATTERN) &&
                                CheckValue(curBright, 0, MAXVAL_PERCENT))
                        {
                            // allow for bad current delay value
                            if (curDelay < -rangeDelay) curDelay = -rangeDelay;
                            else if (curDelay > rangeDelay) curDelay = rangeDelay;
                        }
                        else replyFail = true;
                    }
                    else replyFail = true;

                    ++replyState;
                    --optionLines;
                    break;
                }
                case 5: // additional line 4: 4 current settings
                {
                    String[] strs = reply.split(" ");
                    if (strs.length == 4)
                    {
                        numSegments = Integer.parseInt(strs[0]);
                        customPatterns = Integer.parseInt(strs[1]);
                        customPlugins = Integer.parseInt(strs[2]);
                        maxlenCmdStrs = Integer.parseInt(strs[3]);
                        Log.d(LOGNAME, "Xinfo: Segments=" + numSegments + " XPatterns=" + customPatterns + " XPlugins=" + customPlugins + " MaxCmdStr=" + maxlenCmdStrs);

                        if ((numSegments < 1)    ||
                            (customPlugins < 0)  ||
                            (maxlenCmdStrs < 80))
                            replyFail = true;

                        else if (customPatterns < 0) // indicates fixed internal device patterns
                        {
                            editPatterns = false;
                            customPatterns = -customPatterns;
                            stdPatternsCount = 0;
                        }
                        else stdPatternsCount = stdPatternCmds.length;

                        if (!replyFail)
                        {
                            getSegments = (numSegments > 1);
                            getPatterns = (customPatterns > 0);
                            getPlugins = (customPlugins > 0);

                            if (!getPatterns)
                            {
                                int count = stdPatternNames.length;
                                devPatternNames = new String[count];
                                devPatternCmds = new String[count];
                                devPatternBits = new int[count];
                            }

                            devPatternOffset = 0;
                            numPatterns = customPatterns + stdPatternsCount;

                            progressPercent = 0;
                            progressPcentInc = 100 / ((numSegments-1) + (customPatterns*2) + (customPlugins*2));
                        }
                    }
                    else replyFail = true;

                    ++replyState;
                    --optionLines;
                    break;
                }
                default: // ignore for forward compatibility
                {
                    Log.w(LOGNAME, "Unknown settings: " + reply);
                    --optionLines;
                    break;
                }
            }

            SleepMsecs(100);
            //progressHandler.post(new Runnable()
            progressLine.post(new Runnable()
            {
                @Override public void run()
                {
                    Log.v(LOGNAME, "Progress=" + progressPercent);
                    //progressDialog.setProgress(progressPercent);
                    progressLine.setProgress(progressPercent);
                    progressPercent += progressPcentInc;
                }
            });

            if (replyFail)
            {
                //progressDialog.dismiss();
                progressLine.post(new Runnable()
                {
                    @Override public void run()
                    {
                        progressLine.setVisibility(View.GONE);
                    }
                });

                Log.e(LOGNAME, "Read failed: state=" + replyState);
                DeviceFailed("Device Not Recognized: Try Again");
            }
            else if ((replyState > 1) && (optionLines == 0))
            {
                boolean moreinfo = false;

                if (getSegments)
                {
                    infostr = "?S";
                    moreinfo = true;
                    optionLines = 1;
                }
                else if (getPatterns)
                {
                    infostr = "?P";
                    moreinfo = true;
                    optionLines = customPatterns*2;

                    devPatternNames = new String[numPatterns];
                    devPatternCmds = new String[numPatterns];
                    devPatternBits = new int[numPatterns];
                }
                else if (getPlugins)
                {
                    infostr = "?X";
                    moreinfo = true;
                    optionLines = customPlugins*2;
                }

                if (moreinfo)
                {
                    replyState = 1;
                    new Thread()
                    {
                        @Override public void run()
                        {
                            SleepMsecs(250); // don't send too soon...hack!
                            Log.d(LOGNAME, "Sending command: " + infostr);
                            ble.WriteString(infostr);
                        }

                    }.start();
                }
                else
                {
                    if (stdPatternsCount > 0)
                    {
                        for (int i = 0; i < stdPatternNames.length; ++i)
                        {
                            devPatternNames[devPatternOffset+i] = stdPatternNames[i];
                            devPatternCmds[ devPatternOffset+i] = stdPatternCmds[i];
                            devPatternBits[ devPatternOffset+i] = stdPatternBits[i];
                        }
                    }

                    //progressDialog.dismiss();
                    didFinishReading = true;
                    Log.i(LOGNAME, ">>>>>>>>>> Device Setup Successful <<<<<<<<<<");
                    startActivity( new Intent(Devices.this, Controls.class) );
                }
            }
        }
    }
    private int progressPercent = 0;
    private int progressPcentInc = 0;
    private boolean getSegments = false;
    private boolean getPatterns = false;
    private boolean getPlugins = false;
    private int devPatternOffset = 0;
    private static String infostr = null;
    private boolean didFinishReading = false;

    private boolean CheckValue(int val, int min, int max)
    {
        if (val < min) return false;
        if ((0 < max) && (max < val)) return false;
        return true;
    }
}
