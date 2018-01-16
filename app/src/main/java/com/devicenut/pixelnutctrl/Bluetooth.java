package com.devicenut.pixelnutctrl;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static android.bluetooth.le.ScanSettings.MATCH_MODE_AGGRESSIVE;
import static android.bluetooth.le.ScanSettings.MATCH_NUM_ONE_ADVERTISEMENT;
import static android.bluetooth.le.ScanSettings.SCAN_MODE_LOW_LATENCY;
import static com.devicenut.pixelnutctrl.Main.DEVSTAT_DISCONNECTED;
import static com.devicenut.pixelnutctrl.Main.DEVSTAT_FAILED;
import static com.devicenut.pixelnutctrl.Main.DEVSTAT_SUCCESS;
import static com.devicenut.pixelnutctrl.Main.SleepMsecs;
import static com.devicenut.pixelnutctrl.Main.appContext;
import static com.devicenut.pixelnutctrl.Main.deviceID;
import static com.devicenut.pixelnutctrl.Main.doRefreshCache;

class Bluetooth
{
    private static final String LOGNAME = "Bluetooth";

    private static final String UUID_UART   = "6e400001-b5a3-f393-e0a9-e50e24dcca9e";
    private static final String UUID_TX     = "6e400002-b5a3-f393-e0a9-e50e24dcca9e";
    private static final String UUID_RX     = "6e400003-b5a3-f393-e0a9-e50e24dcca9e";
    private static final String CH_CONFIG   = "00002902-0000-1000-8000-00805f9b34fb";

    private static BluetoothAdapter bleAdapter = null;
    private static final List<BluetoothDevice> bleDevList = new ArrayList<>();
    private static BluetoothGatt bleGatt = null;
    private static BluetoothGattCharacteristic bleTx, bleRx;
    private static String strLine = "";

    interface BleCallbacks
    {
        void onScan(String name, int id, boolean isble);
        void onConnect(final int status);
        void onDisconnect();
        void onWrite(final int status);
        void onRead(String reply);
    }
    private static BleCallbacks bleCB;

    Bluetooth()
    {
        bleCB = null;

        if (appContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE))
        {
            BluetoothManager manager = (BluetoothManager) appContext.getSystemService(Context.BLUETOOTH_SERVICE);
            if (manager != null) bleAdapter = manager.getAdapter();
        }
    }

    boolean checkForPresence()
    {
        return (bleAdapter != null);
    }
    boolean checkIfEnabled()
    {
        if (bleAdapter == null) return false;
        /*
        if (!bleAdapter.isEnabled())
        {
            Log.w(LOGNAME, "Enabling Bluetooth now...");
            bleAdapter.enable(); // NOT supposed to do this without user permission

            BluetoothManager manager = (BluetoothManager) appContext.getSystemService(Context.BLUETOOTH_SERVICE);
            if (manager == null) return false;

            bleAdapter = manager.getAdapter();
            if (bleAdapter == null) return false;

            Log.w(LOGNAME, "...Bluetooth is now enabled");
        }
        */
        return bleAdapter.isEnabled();
    }

    void setCallbacks(BleCallbacks cb) { bleCB = cb; }

    void startScanning()
    {
        if (bleAdapter == null) return; // sanity check
        /*
        if (Build.VERSION.SDK_INT < 23)
        {
            Log.w(LOGNAME, "Reset Adapter to capture name changes");
            if (bleAdapter.disable() && bleAdapter.enable()) // this doesn't work (asynchronous) as well as against rules
            {
                BluetoothManager manager = (BluetoothManager) appContext.getSystemService(Context.BLUETOOTH_SERVICE);
                if (manager == null) bleAdapter = null;
                else bleAdapter = manager.getAdapter();
                if (bleAdapter == null) Log.e(LOGNAME, "Failed to get BLE adapter");
            }
            else Log.e(LOGNAME, "Failed to reset BLE adapter");
        }
        */
        bleDevList.clear();
        ScanSettings.Builder builder = new ScanSettings.Builder();
        builder.setScanMode(SCAN_MODE_LOW_LATENCY);
        if (Build.VERSION.SDK_INT >= 23)
        {
            builder.setMatchMode(MATCH_MODE_AGGRESSIVE);
            builder.setNumOfMatches(MATCH_NUM_ONE_ADVERTISEMENT);
        }

        Log.i(LOGNAME, "Start scanning...");
        bleAdapter.getBluetoothLeScanner().startScan(null, builder.build(), bleScanDevicesCB);
    }

    void stopScanning()
    {
        if (bleAdapter == null) return; // sanity check

        Log.i(LOGNAME, "Stop scanning...");
        bleAdapter.getBluetoothLeScanner().stopScan(bleScanDevicesCB);
    }

    boolean connect()
    {
        strLine = ""; // clear any read data

        BluetoothDevice bdev = bleDevList.get(deviceID);
        if (bdev == null) return false;

        Log.i(LOGNAME, "Connecting to GATT...");
        SleepMsecs(100); // hack to prevent disconnect

        //bleAdapter.cancelDiscovery();
        bleGatt = bdev.connectGatt(appContext, false, bleGattCB);
        if (bleGatt == null) return false;

        if (doRefreshCache)
        {
            doRefreshCache = false;
            /*
            try // this doesn't seem to work
            {
                Method localMethod = bleGatt.getClass().getMethod("refresh");
                if (localMethod != null)
                {
                    boolean result = (Boolean) localMethod.invoke(bleGatt);
                    if (result) Log.d(LOGNAME, "Bluetooth refresh cache");
                    return result;
                }
            }
            catch (Exception localException) { Log.e(LOGNAME, "Failed refreshing device cache"); }
            */
        }
        return true;
    }

    void disconnect()
    {
        if (bleGatt != null)
        {
            Log.i(LOGNAME, "Disconnecting from GATT");
            //bleGatt.disconnect(); // unnecessary if close(), can cause issues?
            bleGatt.close();
        }
        else Log.w(LOGNAME, "No GATT to disconnect");
    }

    // this must be called on a non-UI thread
    void WriteString(String str)
    {
        if ((bleTx != null) && (bleGatt != null))
        {
            bleTx.setValue(str);
            if (!bleGatt.writeCharacteristic(bleTx))
            {
                Log.e(LOGNAME, "Write failed: \"" + str + "\"");
                bleCB.onWrite(DEVSTAT_FAILED);
            }
        }
        else bleCB.onWrite(DEVSTAT_DISCONNECTED);
    }

    /*
    private void ShowProperties(String type, BluetoothGattCharacteristic ch)
    {
        int props = ch.getProperties();
        if ((props & BluetoothGattCharacteristic.PROPERTY_READ)     != 0) Log.v(LOGNAME, type + "=PropRead");
        if ((props & BluetoothGattCharacteristic.PROPERTY_WRITE)    != 0) Log.v(LOGNAME, type + "=PropWrite");
        if ((props & BluetoothGattCharacteristic.PROPERTY_NOTIFY)   != 0) Log.v(LOGNAME, type + "=PropNotify");
        if ((props & BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0) Log.v(LOGNAME, type + "=PropIndicate");

        int perms = ch.getPermissions();
        if ((perms & BluetoothGattCharacteristic.PERMISSION_READ)   != 0) Log.v(LOGNAME, type + "=PermRead");
        if ((perms & BluetoothGattCharacteristic.PERMISSION_WRITE)  != 0) Log.v(LOGNAME, type + "=PermRead");
    }
    */

    private final ScanCallback bleScanDevicesCB = new ScanCallback()
    {
        public void onScanResult(int callbackType, ScanResult result)
        {
            BluetoothDevice dev = result.getDevice();
            String name = dev.getName();

            if ((name != null) && !bleDevList.contains(dev))
            {
                bleDevList.add(dev);
                int id = bleDevList.indexOf(dev);
                Log.d(LOGNAME, "Found #" + id + ": " + name);

                if (bleCB == null) Log.e(LOGNAME, "BLE CB is null!!!"); // have seen this happen, but how???
                else bleCB.onScan(name, id, true);
            }
        }

        public void onBatchScanResults(List<ScanResult> results) {}

        // @param errorCode Error code (one of SCAN_FAILED_*)
        public void onScanFailed(int errorCode)
        {
            // never seen this happen
            Log.e(LOGNAME, "Scan error=" + errorCode);
        }
    };

    private final BluetoothGattCallback bleGattCB = new BluetoothGattCallback()
    {
        @Override public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState)
        {
            if (newState == BluetoothProfile.STATE_CONNECTED)
            {
                if (bleGatt != null)
                {
                    Log.i(LOGNAME, "...GATT now connected");
                    bleGatt.discoverServices();
                }
                else Log.w(LOGNAME, "No GATT on connect");
            }
            else if (newState == BluetoothProfile.STATE_DISCONNECTED)
            {
                Log.i(LOGNAME, "GATT now disconnected");
                bleCB.onDisconnect();
                bleGatt.close();
                bleGatt = null;
            }
            else Log.i(LOGNAME, "GATT state=" + newState);
        }

        @Override public void onServicesDiscovered(BluetoothGatt gatt, int status)
        {
            if (status != BluetoothGatt.GATT_SUCCESS)
            {
                bleCB.onConnect(DEVSTAT_FAILED);
                return;
            }

            for (BluetoothGattService service : bleGatt.getServices())
            {
                Log.v(LOGNAME, "Service=" + service.getUuid());
                if (service.getUuid().toString().equals(UUID_UART))
                {
                    Log.d(LOGNAME, "Found UART Service");

                    bleTx = bleRx = null;
                    for (BluetoothGattCharacteristic ch : service.getCharacteristics())
                    {
                             if (ch.getUuid().toString().equals(UUID_TX)) bleTx = ch;
                        else if (ch.getUuid().toString().equals(UUID_RX)) bleRx = ch;
                    }

                    if ((bleTx == null) || (bleRx == null))
                    {
                        Log.e(LOGNAME, "Rx/Tx characteristic is null");
                        bleCB.onConnect(DEVSTAT_FAILED);
                        return;
                    }

                    Log.v(LOGNAME, "Found RX and TX Chars");
                    if (BuildConfig.DEBUG)
                    {
                        //ShowProperties("TX", bleTx);
                        //ShowProperties("RX", bleRx);
                    }

                    BluetoothGattDescriptor config = bleRx.getDescriptor(UUID.fromString(CH_CONFIG));
                    if (config == null)
                    {
                        Log.e(LOGNAME, "Config descriptor is null");
                        bleCB.onConnect(DEVSTAT_FAILED);
                        return;
                    }

                    Log.v(LOGNAME, "Found Config Descriptor");

                    bleGatt.setCharacteristicNotification(bleRx, true);
                    config.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    bleGatt.writeDescriptor(config);

                    bleCB.onConnect(DEVSTAT_SUCCESS);
                }
            }
        }

        @Override public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status)
        {
            if (status == BluetoothGatt.GATT_SUCCESS)
            {
                Log.v(LOGNAME, "Write completed");
                bleCB.onWrite(DEVSTAT_SUCCESS);
            }
            else bleCB.onWrite(DEVSTAT_FAILED);
        }

        @Override public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic)
        {
            if (bleRx == null) Log.w(LOGNAME, "Read characteristic is null");
            if (bleRx != characteristic) Log.i(LOGNAME, "Not read characteristic");
            if ((bleRx == null) || (bleRx != characteristic)) return;

            bleGatt.readCharacteristic(bleRx);
            byte[] bytes = bleRx.getValue();
            if (bytes == null)
            {
                Log.w(LOGNAME, "Read characteristic bytes is null");
                return;
            }

            String str = new String(bytes, Charset.forName("UTF-8"));
            Log.v(LOGNAME, "ReadBytes=\"" + str + "\"");

            while(true)
            {
                int i = str.indexOf("\n");
                if (i < 0)
                {
                    strLine += str;
                    break;
                }

                strLine += str.substring(0,i);
                bleCB.onRead(strLine);
                strLine = "";
                str = str.substring(i+1);
            }
        }

        @Override public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {}
    };
}
