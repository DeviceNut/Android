package com.devicenut.pixelnutctrl;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static android.os.Process.setThreadPriority;
import static com.devicenut.pixelnutctrl.Main.DEVSTAT_FAILED;
import static com.devicenut.pixelnutctrl.Main.DEVSTAT_SUCCESS;
import static com.devicenut.pixelnutctrl.Main.SleepMsecs;
import static com.devicenut.pixelnutctrl.Main.appContext;
import static com.devicenut.pixelnutctrl.Main.deviceID;

class Wifi
{
    private static final String LOGNAME = "WiFi";

    private static WifiManager wifiManager = null;
    private static List<WifiConfiguration> listAll;
    private static WifiReceiver wifiReceiver;
    private static boolean stopScan = false;
    private static boolean stopConnect = false;
    private static final List<String> wifiNameList = new ArrayList<>();

    //private static final String DEVICE_URL = "http://192.168.0.1:80/index";
    private static final String DEVICE_URL = "http://192.168.0.1/command";
    private static URL wifiURL;

    interface WifiCallbacks
    {
        void onScan(String name, int id, boolean isble);
        void onConnect(final int status);
        void onDisconnect();
        void onWrite(final int status);
        void onRead(String reply);
    }
    private static WifiCallbacks wifiCB;

    Wifi()
    {
        wifiCB = null;
        wifiReceiver = new WifiReceiver();

        if (appContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_WIFI))
        {
            wifiManager = (WifiManager) appContext.getSystemService(Context.WIFI_SERVICE);
            if (wifiManager != null)
            {
                // This list all of the networks every configured (connected to?):
                listAll = wifiManager.getConfiguredNetworks();

                /*
                if (!wifiManager.isWifiEnabled())
                {
                    Toast.makeText(appContext, "Enabling WiFi now", Toast.LENGTH_LONG).show();
                    wifiManager.setWifiEnabled(true);
                }
                */
            }
        }
    }

    boolean checkForPresence()
    {
        return (wifiManager != null);
    }

    boolean checkIfEnabled()
    {
        return ((wifiManager != null) && wifiManager.isWifiEnabled());
    }

    void setCallbacks(WifiCallbacks cb) { wifiCB = cb; }

    void startScanning()
    {
        if (wifiManager == null) return; // sanity check

        stopScan = false;
        wifiNameList.clear();

        Log.i(LOGNAME, "Start scanning...");
        appContext.registerReceiver(wifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        wifiManager.startScan(); // always returns true (or throws exception)
    }

    void stopScanning()
    {
        if (wifiManager == null) return; // sanity check

        Log.i(LOGNAME, "Stop scanning...");
        appContext.unregisterReceiver(wifiReceiver);
    }

    void stopConnecting()
    {
        stopConnect = true;
    }

    private boolean IsConnected()
    {
        WifiInfo info = wifiManager.getConnectionInfo();
        String ssid = info.getSSID();
        int ip = info.getIpAddress();
        int p4 = (ip >> 24) & 0xFF;
        int p3 = (ip >> 16) & 0xFF;
        int p2 = (ip >> 8)  & 0xFF;
        int p1 = (ip >> 0)  & 0xFF;
        Log.d(LOGNAME, "Info: SSID=" + ssid + " IP=" + p1 + "." + p2 + "." + p3 + "." + p4);

        return((info.getNetworkId() == deviceID) && (ip != 0));
    }

    boolean connect()
    {
        boolean success = wifiManager.enableNetwork(deviceID, true);
        Log.i(LOGNAME, "Connection to NetID=" + deviceID + ": " + (success ? "success" : "failed"));

        if (!success) return false;

        stopConnect = false;
        new Thread()
        {
            @Override public void run()
            {
                int count = 0;
                Log.i(LOGNAME, "Connect thread starting...");
                setThreadPriority(-10);

                while (!stopConnect)
                {
                    SleepMsecs(1000);

                    if (IsConnected())
                    {
                        Log.d(LOGNAME, "Network connection complete");
                        wifiCB.onConnect(DEVSTAT_SUCCESS);
                        break;
                    }

                    // timeout after 30 seconds
                    if (++count >= 30)
                    {
                        Log.d(LOGNAME, "Network connection failed");
                        wifiCB.onConnect(DEVSTAT_FAILED);
                        break;
                    }
                }
            }
        }
        .start();

        stopScan = true;
        return true;
    }

    void disconnect()
    {
        Log.d(LOGNAME, "Disconnect network");
        stopConnect = true;
        wifiManager.disconnect();
    }

    // this must be called on a non-UI thread
    void WriteString(String str)
    {
        Log.d(LOGNAME, "Wifi write: " + str);
        try
        {
            Log.v(LOGNAME, DEVICE_URL);
            wifiURL = new URL(DEVICE_URL);

            Log.v(LOGNAME, "Opening connection...");
            HttpURLConnection devConnection = (HttpURLConnection) wifiURL.openConnection();

            Log.v(LOGNAME, "Using HTTP POST");
            devConnection.setRequestMethod("POST");
            devConnection.setRequestMethod("GET");

            devConnection.setDoInput(true);
            devConnection.setDoOutput(true);
            devConnection.setReadTimeout(0);     // wait forever for connection/data
            devConnection.setConnectTimeout(0);

            Log.v(LOGNAME, "Connecting to device...");
            devConnection.connect();

            Log.v(LOGNAME, "Sending message to device...");
            BufferedWriter devWriter = new BufferedWriter(new OutputStreamWriter(devConnection.getOutputStream()));
            devWriter.write(str);
            devWriter.flush();
            devWriter.close();

            Log.v(LOGNAME, "Reading response from device...");
            BufferedReader devReader = new BufferedReader(new InputStreamReader(devConnection.getInputStream()));
            String inline;

            while ((inline = devReader.readLine()) != null) // this will block
            {
                inline = inline.trim();
                Log.v(LOGNAME, "DeviceSays: " + inline);
                if (inline.equals("ok")) break;
                wifiCB.onRead(inline);
            }

            devReader.close();
            Log.v(LOGNAME, "Wifi finished reading");

            devConnection.disconnect();
            wifiCB.onWrite(DEVSTAT_SUCCESS);
        }
        catch (Exception e)
        {
            Log.e(LOGNAME, "Write failed: \"" + str + "\"");
            e.printStackTrace();

            wifiCB.onWrite(DEVSTAT_FAILED);
        }
    }

    class WifiReceiver extends BroadcastReceiver
    {
        // called when number of wifi connections changed
        public void onReceive(Context c, Intent intent)
        {
            if (stopScan) return;
            if (wifiCB == null) return;

            List<ScanResult> listActive = wifiManager.getScanResults();

            for (int i = 0; i < listActive.size(); ++i)
            {
                ScanResult result = listActive.get(i);
                String ssid = "\"" + result.SSID + "\"";
                Log.v(LOGNAME, "Result " + i + ") SSID=" + ssid);
                // This SSID string does *not* include beginning/ending quotes

                for (int j = 0; j < listAll.size(); ++j)
                {
                    WifiConfiguration entry = listAll.get(j);
                    //Log.v(LOGNAME, "  Entry " + j + ") SSID=" + entry.SSID + " ID=" + entry.networkId);
                    // Note: SSID string *includes* the beginning/ending quotes

                    if (entry.SSID.equals(ssid)) // matches active SSID
                    {
                        Log.v(LOGNAME, "==> matches ID=" + entry.networkId);

                        ssid = result.SSID.trim();
                        if (ssid.startsWith("P!") && ssid.endsWith("-!")) // one or our devices
                        {
                            int id = entry.networkId; // needed for connection call
                            String name = ssid.substring(0, ssid.length()-2); // remove ending crap
                            Log.d(LOGNAME, "Found #" + id + ": " + name);

                            if (!wifiNameList.contains(name))
                            {
                                wifiNameList.add(name);
                                wifiCB.onScan(name, id, false);
                            }
                        }
                    }
                }
            }
        }
    }
}
