package com.devicenut.pixelnutctrl;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import static com.devicenut.pixelnutctrl.Main.CMD_RESUME;
import static com.devicenut.pixelnutctrl.Main.CMD_SEQ_END;
import static com.devicenut.pixelnutctrl.Main.DEVSTAT_BADSTATE;
import static com.devicenut.pixelnutctrl.Main.DEVSTAT_SUCCESS;
import static com.devicenut.pixelnutctrl.Main.CMD_GET_INFO;
import static com.devicenut.pixelnutctrl.Main.URL_DEVICENUT;
import static com.devicenut.pixelnutctrl.Main.appContext;
import static com.devicenut.pixelnutctrl.Main.blePresentAndEnabled;
import static com.devicenut.pixelnutctrl.Main.cmdPauseEnable;
import static com.devicenut.pixelnutctrl.Main.deviceID;
import static com.devicenut.pixelnutctrl.Main.msgThread;
import static com.devicenut.pixelnutctrl.Main.pixelDensity;
import static com.devicenut.pixelnutctrl.Main.pixelHeight;
import static com.devicenut.pixelnutctrl.Main.pixelWidth;
import static com.devicenut.pixelnutctrl.Main.InitVarsForDevice;
import static com.devicenut.pixelnutctrl.Main.SleepMsecs;

import static com.devicenut.pixelnutctrl.Main.ble;
import static com.devicenut.pixelnutctrl.Main.resumeScanning;
import static com.devicenut.pixelnutctrl.Main.wifi;
import static com.devicenut.pixelnutctrl.Main.devName;
import static com.devicenut.pixelnutctrl.Main.devIsBLE;
import static com.devicenut.pixelnutctrl.Main.isConnected;
import static com.devicenut.pixelnutctrl.Main.wifiPresentAndEnabled;
import static com.devicenut.pixelnutctrl.Main.msgWriteEnable;
import static com.devicenut.pixelnutctrl.Main.msgWriteQueue;

public class Devices extends AppCompatActivity implements Bluetooth.BleCallbacks, Wifi.WifiCallbacks
{
    private final String LOGNAME = "Devices";
    private final Activity context = this;

    private TextView textConnecting;
    private TextView textSelectDevice;
    private Button buttonScan;
    private ProgressBar progressBar;
    private ProgressBar progressLine;
    private ScrollView scrollDevices;

    private final ArrayList<Integer> deviceIDs = new ArrayList<>();
    private final ArrayList<String> deviceNames = new ArrayList<>();
    private final ArrayList<Boolean> deviceIsBLE = new ArrayList<>();

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
    private boolean isScanning = false;
    private boolean isConnecting = false;
    private boolean didFail = false;
    private boolean isDone = false;

    @Override protected void onCreate(Bundle savedInstanceState)
    {
        Log.i(LOGNAME, ">>onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_devices);

        textSelectDevice = findViewById(R.id.text_SelectDevice);
        textConnecting = findViewById(R.id.text_Connecting);
        buttonScan = findViewById(R.id.button_ScanStop);
        scrollDevices = findViewById(R.id.scroll_Devices);

        progressBar = findViewById(R.id.progress_Scanner);
        progressLine = findViewById(R.id.progress_Loader);

        ble = new Bluetooth();
        blePresentAndEnabled = ble.checkForPresence();

        wifi = new Wifi();
        wifiPresentAndEnabled = wifi.checkForPresence();

        if (!blePresentAndEnabled && !wifiPresentAndEnabled)
        {
            Toast.makeText(this, "Must have Bluetooth or WiFi", Toast.LENGTH_LONG).show();
            WaitAndFinish();
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
        pixelHeight = metrics.heightPixels;
        pixelWidth = metrics.widthPixels;

        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        pixelDensity = dm.densityDpi;

        Log.w(LOGNAME, "Screen: width=" + pixelWidth + " height=" + pixelHeight + " density=" + pixelDensity);

        if (getResources().getBoolean(R.bool.portrait_only))
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    @Override protected void onResume()
    {
        Log.i(LOGNAME, ">>onResume");
        super.onResume();

        blePresentAndEnabled = ble.checkIfEnabled();
        wifiPresentAndEnabled = wifi.checkIfEnabled();

        if (!blePresentAndEnabled && !wifiPresentAndEnabled)
        {
            if (ble.checkForPresence() && wifi.checkForPresence())
                 Toast.makeText(this, "Enable Bluetooth and/or WiFi", Toast.LENGTH_LONG).show();
            else if (ble.checkForPresence())
                 Toast.makeText(this, "Must enable Bluetooth", Toast.LENGTH_LONG).show();
            else if (wifi.checkForPresence())
                 Toast.makeText(this, "Must enable WiFi", Toast.LENGTH_LONG).show();
            else Toast.makeText(this, "No Bluetooth or WiFi available", Toast.LENGTH_LONG).show();

            WaitAndFinish();
            return;
        }

        if (ble.checkForPresence() && !blePresentAndEnabled)
            Toast.makeText(this, "Warning: Bluetooth disabled", Toast.LENGTH_SHORT).show();

        isConnecting = false;
        isConnected = false;

        if (blePresentAndEnabled)  ble.setCallbacks(this);
        if (wifiPresentAndEnabled) wifi.setCallbacks(this);

        if (resumeScanning)
             StartScanning();
        else SetupUserDisplay();

        resumeScanning = true;
    }

    @Override protected void onPause()
    {
        Log.i(LOGNAME, ">>onPause");
        super.onPause();

        StopScanning();

        if (wifiPresentAndEnabled && !devIsBLE)
            wifi.stopConnecting();

        //if (myToast != null) myToast.cancel();
    }

    @Override public void onBackPressed()
    {
        if (isConnected) DoDisconnect();

        super.onBackPressed();
    }

    private void WaitAndFinish()
    {
        new Thread()
        {
            @Override public void run()
            {
                SleepMsecs(2000); // allow time for user to see our screen
                Devices.this.finish();
            }
        }
        .start();
    }

    public void onClick(View v)
    {
        switch (v.getId())
        {
            case R.id.text_Header:
            {
                // if not scanning now, don't resume after return
                if (!StopScanning()) resumeScanning = false;

                startActivity(new Intent(this, About.class));
                break;
            }
            case R.id.button_ScanStop:
            {
                if (!isConnected)
                {
                    if (isConnecting)
                    {
                        Log.d(LOGNAME, "Cancel connecting...");
                        DoDisconnect();
                        StartScanning();
                    }
                    else if (!StopScanning()) StartScanning();
                }
                else if (isConnecting)
                {
                    Log.d(LOGNAME, "Cancel connection...");
                    DoDisconnect();
                    StartScanning();
                }
                break;
            }
            case R.id.button_Website:
            {
                // if not scanning now, don't resume after return
                if (!StopScanning()) resumeScanning = false;

                Uri uri = Uri.parse(URL_DEVICENUT);
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
                        deviceID = deviceIDs.get(i);
                        devName = deviceNames.get(i);
                        devIsBLE = deviceIsBLE.get(i);

                        Log.d(LOGNAME, "ButtonID=" + i + " DeviceID=" + deviceID + " DevName=" + devName + " BLE=" + devIsBLE);

                        doReply = new ReplyStrs();

                        boolean success;
                        if (devIsBLE) success = ble.connect();
                        else          success = wifi.connect();
                        if (success)
                        {
                            isConnecting = true;
                            SetupUserDisplay();
                            break;
                        }

                        Toast.makeText(context, "Cannot connect: retry", Toast.LENGTH_SHORT).show();
                        deviceID = 0;
                        devName = null;
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
            Button b = findViewById(listButton);
            b.setVisibility(View.GONE);
        }

        deviceIDs.clear();
        deviceNames.clear();
        deviceIsBLE.clear();

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
        buttonScan.setTextColor(ContextCompat.getColor(appContext, R.color.UserChoice));
    }

    private void StartScanning()
    {
        if (!isScanning)
        {
            Log.d(LOGNAME, "Start scanning...");
            isScanning = true;
            SetupUserDisplay();

            didFail = false; // reset for next scan/connect attempt

            if (blePresentAndEnabled)
                ble.startScanning();

            if (wifiPresentAndEnabled)
                wifi.startScanning();
        }
    }

    private boolean StopScanning()
    {
        if (isScanning)
        {
            Log.d(LOGNAME, "Stop scanning...");
            isScanning = false;

            if (blePresentAndEnabled)
                ble.stopScanning();

            if (wifiPresentAndEnabled)
                wifi.stopScanning();

            context.runOnUiThread(new Runnable()
            {
                public void run()
                {
                    SetupUserDisplay();
                }
            });

            return true;
        }
        return false;
    }

    private void DoDisconnect()
    {
        isConnecting = false;
        isConnected = false;

        StopScanning();

        Log.d(LOGNAME, "Disconnecting...");
        if (devIsBLE) ble.disconnect();
        else          wifi.disconnect();
    }

    private synchronized void DeviceFailed(final String errstr)
    {
        if (!didFail)
        {
            didFail = true;
            msgWriteEnable = false;

            Log.e(LOGNAME, errstr);

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
        else Log.w(LOGNAME, "Already failed once...");
    }

    @Override public void onScan(final String name, int id, boolean isble)
    {
        if (isScanning && !isConnecting && !isConnected && (name != null))
        {
            Log.v(LOGNAME, "OnScan: name=" + name + " id=" + id + " isble=" + isble);

            if (buttonCount < listButtons.length)
            {
                Button b = findViewById(listButtons[buttonCount]);
                b.setText(name);
                b.setVisibility(View.VISIBLE);

                ++buttonCount;
                deviceIDs.add(id);
                deviceNames.add(name);
                deviceIsBLE.add(isble);

                scrollDevices.post(new Runnable()
                {
                    @Override public void run() {
                        scrollDevices.scrollTo(0, scrollDevices.getBottom());
                    }
                });
            }
            else StopScanning();
        }
    }

    @Override public void onConnect(final int status)
    {
        if (status == DEVSTAT_SUCCESS)
        {
            Log.i(LOGNAME, "Connected to our device <<<<<<<<<<");
            isConnected = true; // actually connected now
            // note: still have isConnecting set as well
            // until finished retrieving configuration
            isDone = false;

            msgWriteQueue.clear();
            msgWriteEnable = true;
            cmdPauseEnable = true;

            msgThread = new MsgQueue();
            msgThread.start();

            SendString(CMD_GET_INFO);
            SendString(CMD_SEQ_END);
        }
        else DeviceFailed("Connection Failed: Try Again");
    }

    @Override public void onDisconnect()
    {
        isConnecting = false;
        isConnected = false;

        final String errstr = "Device Disconnected: Try Again";
        if (!didFail) Log.e(LOGNAME, errstr);

        context.runOnUiThread(new Runnable()
        {
            public void run()
            {
                if (!didFail) Toast.makeText(context, errstr, Toast.LENGTH_SHORT).show();
                StartScanning(); // return to scanning
            }
        });
    }

    @Override public void onWrite(final int status)
    {
        if (status == DEVSTAT_SUCCESS)
            msgWriteEnable = true;

        else if (status == DEVSTAT_BADSTATE)
        {
            Log.w(LOGNAME, "OnWrite: invalid state");
            DeviceFailed("Device waiting for setup");
        }
        else
        {
            Log.e(LOGNAME, "OnWrite: status=" + status);
            DeviceFailed("Write Failed: Try Again");
        }
    }

    @Override public void onRead(String rstr)
    {
        if (rstr == null) return; // end of read

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
                SendString(doReply.sendCmdStr);
                SendString(CMD_SEQ_END);
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

    private void UpdateProgress()
    {
        progressLine.post(new Runnable()
        {
            @Override public void run()
            {
                doReply.progressPercent += doReply.progressPcentInc;
                progressLine.setProgress((int)doReply.progressPercent);
                Log.v(LOGNAME, "Progress=" + (int)doReply.progressPercent);
            }
        });

        if (isDone)
        {
            Log.i(LOGNAME, ">>> Device Setup Successful <<<");
            SendString(CMD_RESUME); // insure device is not paused
            SendString(CMD_SEQ_END);

            InitVarsForDevice();
            cmdPauseEnable = false;

            startActivity( new Intent(Devices.this, Master.class) );
        }
    }

    private void SendString(String str)
    {
        Log.v(LOGNAME, "Queue command: " + str);
        if (!msgWriteQueue.put(str))
            Log.e(LOGNAME, "Msg queue full: str=" + str + "\"");
    }
}
