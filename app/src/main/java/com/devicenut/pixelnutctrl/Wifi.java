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
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static android.os.Process.setThreadPriority;
import static com.devicenut.pixelnutctrl.Main.DEVSTAT_BADSTATE;
import static com.devicenut.pixelnutctrl.Main.DEVSTAT_FAILED;
import static com.devicenut.pixelnutctrl.Main.DEVSTAT_SUCCESS;
import static com.devicenut.pixelnutctrl.Main.POSTFIX_WIFI;
import static com.devicenut.pixelnutctrl.Main.PREFIX_PHOTON;
import static com.devicenut.pixelnutctrl.Main.PREFIX_PIXELNUT;
import static com.devicenut.pixelnutctrl.Main.SleepMsecs;
import static com.devicenut.pixelnutctrl.Main.appContext;
import static com.devicenut.pixelnutctrl.Main.deviceID;

class Wifi
{
    private static final String LOGNAME = "WiFi";

    private static WifiManager wifiManager = null;
    private static WifiReceiver wifiReceiver;
    private static boolean stopScan = false;
    private static boolean stopConnect = false;
    private static final List<String> wifiNameList = new ArrayList<>();

    private static final String DEVICE_URL = "http://192.168.0.1/command";

    private static int saveDeviceID;

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
        saveDeviceID = 0;

        if (appContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_WIFI))
        {
            wifiManager = (WifiManager) appContext.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if ((wifiManager != null) && wifiManager.isWifiEnabled())
            {
                WifiInfo info = wifiManager.getConnectionInfo();
                ShowConnectionInfo(info);

                saveDeviceID = info.getNetworkId();
                Log.d(LOGNAME, "Saving previous network ID=" + saveDeviceID);
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

        stopConnecting();
    }

    void stopConnecting()
    {
        stopConnect = true;
    }

    private void ShowConnectionInfo(WifiInfo info)
    {
        String ssid = info.getSSID();
        int ip = info.getIpAddress();
        int p4 = (ip >> 24) & 0xFF;
        int p3 = (ip >> 16) & 0xFF;
        int p2 = (ip >> 8)  & 0xFF;
        int p1 = (ip)       & 0xFF;
        Log.d(LOGNAME, "Info: SSID=" + ssid + " IP=" + p1 + "." + p2 + "." + p3 + "." + p4);
    }

    private boolean IsConnected()
    {
        WifiInfo info = wifiManager.getConnectionInfo();
        ShowConnectionInfo(info);

        if (info.getIpAddress() == 0) return false;

        if (info.getNetworkId() == deviceID) return true;

        // if deviceID doesn't match then need to rescan
        Log.w(LOGNAME, "DeviceID mismatch: " + info.getNetworkId() + " != " + deviceID);
        stopConnect = true;
        wifiCB.onConnect(DEVSTAT_FAILED);
        return false;
    }

    boolean connect()
    {
        boolean success = wifiManager.enableNetwork(deviceID, true);
        Log.i(LOGNAME, "Connection to network: ID=" + deviceID + ": " + (success ? "success" : "failed"));
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
                        SleepMsecs(2000); // hack to help prevent reset on first write?
                        wifiCB.onConnect(DEVSTAT_SUCCESS);
                        break;
                    }

                    // timeout after 10 seconds
                    if (++count >= 10)
                    {
                        Log.d(LOGNAME, "Network connection failed");
                        wifiCB.onConnect(DEVSTAT_FAILED);
                        break;
                    }
                }

                Log.d(LOGNAME, "Connect thread ending...");
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

        if (saveDeviceID != 0)
        {
            boolean success = wifiManager.enableNetwork(saveDeviceID, true);
            Log.i(LOGNAME, "Reconnect to previous network: ID=" + saveDeviceID + ": " + (success ? "success" : "failed"));
            saveDeviceID = 0;
        }
    }

    // this must be called on a non-UI thread
    void WriteString(String str)
    {
        Log.d(LOGNAME, "Wifi write: " + str);
        for (int i = 0; i < 3; ++i)
        {
            try
            {
                Log.v(LOGNAME, DEVICE_URL);
                URL wifiURL = new URL(DEVICE_URL);

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
                    wifiCB.onRead(inline);
                }
                wifiCB.onRead(null); // indicate done reading

                devReader.close();
                Log.v(LOGNAME, "Wifi finished reading");

                devConnection.disconnect();
                wifiCB.onWrite(DEVSTAT_SUCCESS);
                return;
            }
            catch (FileNotFoundException e)
            {
                Log.w(LOGNAME, "Device not listening!");
                wifiCB.onWrite(DEVSTAT_BADSTATE);
                return;
            }
            catch (Exception e)
            {
                Log.e(LOGNAME, "Write failed: \"" + str + "\"");
                e.printStackTrace();
                // retry up to 3 times
            }
        }

        wifiCB.onWrite(DEVSTAT_FAILED);
    }

    class WifiReceiver extends BroadcastReceiver
    {
        // called when number of wifi connections changed
        public void onReceive(Context c, Intent intent)
        {
            if (stopScan) return;
            if (wifiCB == null) return;

            // lists all networks ever configured (connected to)
            List<WifiConfiguration> configlist = wifiManager.getConfiguredNetworks();

            // this gets results from the latest scan
            List<ScanResult> listActive = wifiManager.getScanResults();

            for (int i = 0; i < listActive.size(); ++i)
            {
                ScanResult result = listActive.get(i);
                // result.SSID does *not* include beginning/ending quotes
                String matchstr = "\"" + result.SSID + "\"";
                Log.v(LOGNAME, "Result " + i + ") SSID=" + matchstr);

                String name = result.SSID.trim(); // remove spaces first
                String dspname = "";
                boolean haveone = false;

                int index = name.indexOf(POSTFIX_WIFI);
                if (name.startsWith(PREFIX_PIXELNUT) && (index > 0))
                {
                    haveone = true;
                    dspname = name.substring(PREFIX_PIXELNUT.length(), index);
                }
                else if (name.toUpperCase().startsWith(PREFIX_PHOTON))
                {
                    haveone = true;
                    dspname = name.substring( PREFIX_PHOTON.length() );
                }

                if (haveone && !wifiNameList.contains(dspname)) // have not already seen this
                {
                    Log.d(LOGNAME, "Found device: " + dspname);
                    int id = 0; // needed for connection call
                    boolean haveid = false;
                    boolean didadd = false;

                    while (!haveid)
                    {
                        for (WifiConfiguration entry: configlist)
                        {
                            // the config SSID string *includes* beginning/ending quotes
                            //Log.v(LOGNAME, "  Entry " + j + ") SSID=" + entry.SSID + " ID=" + entry.networkId);

                            if ((entry.SSID != null) && (entry.SSID.equals(matchstr)))
                            {
                                haveid = true;
                                id = entry.networkId;
                                Log.v(LOGNAME, "==> matches ID=" + id);
                                break;
                            }
                        }

                        if (haveid)
                        {
                            Log.d(LOGNAME, "Success: ID=" + id + " Name=" + dspname);
                            wifiNameList.add(dspname); // add to "have seen" list

                            if (wifiCB != null)
                            {
                                wifiCB.onScan(dspname, id, false);
                            }
                            else Log.e(LOGNAME, "WiFi CB is null!!!");
                        }
                        else if (!didadd)
                        {
                            Log.v(LOGNAME, "Adding configuration...");
                            WifiConfiguration conf = new WifiConfiguration();
                            conf.SSID = matchstr;
                            conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                            wifiManager.addNetwork(conf);
                            configlist = wifiManager.getConfiguredNetworks(); // refresh list
                            didadd = true;
                        }
                        else
                        {
                            Log.w(LOGNAME, "Failed to add configuration!");
                            break;
                        }
                    }
                }
            }
        }
    }
}
