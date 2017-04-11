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
import android.util.Log;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

class Bluetooth
{
    private static final String LOGNAME = "Bluetooth";

    private static final String UUID_UART   = "6e400001-b5a3-f393-e0a9-e50e24dcca9e";
    private static final String UUID_TX     = "6e400002-b5a3-f393-e0a9-e50e24dcca9e";
    private static final String UUID_RX     = "6e400003-b5a3-f393-e0a9-e50e24dcca9e";
    private static final String CH_CONFIG   = "00002902-0000-1000-8000-00805f9b34fb";

    private Context context = null;
    private static BluetoothAdapter bleAdapter = null;
    private static final List<BluetoothDevice> bleDevList = new ArrayList<>();
    private static BluetoothDevice bleDevice = null;
    private static BluetoothGatt bleGatt = null;
    private static BluetoothGattCharacteristic bleTx, bleRx;

    private String strLine = "";

    private static final int BLESTAT_SUCCESS        =  0;
    private static final int BLESTAT_CALL_FAILED    = -1;
    private static final int BLESTAT_DISCONNECTED   = -2;
    private static final int BLESTAT_NO_SERVICES    = -3;

    interface BleCallbacks
    {
        void onScan(String name, int id);
        void onConnect(final int status);
        void onDisconnect();
        void onWrite(final int status);
        void onRead(String reply);
    }
    private static BleCallbacks bleCB;

    Bluetooth(Context ctx)
    {
        context = ctx;

        if (bleAdapter == null)
        {
            if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE))
            {
                BluetoothManager manager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
                if (manager != null) bleAdapter = manager.getAdapter();
            }
        }
    }

    boolean checkForBlePresent()
    {
        return (bleAdapter != null);
    }
    boolean checkForBleEnabled()
    {
        return ((bleAdapter != null) && bleAdapter.isEnabled());
    }

    void setCallbacks(BleCallbacks cb) { bleCB = cb; }

    void startScanning()
    {
        bleDevList.clear();
        ScanSettings.Builder builder = new ScanSettings.Builder();
        bleAdapter.getBluetoothLeScanner().startScan(null, builder.build(), bleScanDevicesCB);
    }

    void stopScanning()
    {
        bleAdapter.getBluetoothLeScanner().stopScan(bleScanDevicesCB);
    }

    boolean connect(int devid)
    {
        BluetoothDevice bdev = bleDevList.get(devid);
        if (bdev == null) return false;

        Log.i(LOGNAME, "Connecting to GATT...");

        bleDevice = bdev;
        bleGatt = bdev.connectGatt(context, false, bleGattCB);
        return (bleGatt != null);
    }

    String getDevNameFromID(int devid)
    {
        BluetoothDevice bdev = bleDevList.get(devid);
        if (bdev == null) return null;
        return bdev.getName();
    }

    String getCurDevName()
    {
        if (bleDevice == null) return null;
        return bleDevice.getName();
    }

    void disconnect()
    {
        if (bleGatt != null)
        {
            Log.i(LOGNAME, "Disconnecting from GATT");
            bleGatt.disconnect();
            //bleGatt.close(); //this crashes in BluetoothGatt
            bleGatt = null;
        }
        else Log.w(LOGNAME, "No GATT to disconnect");

        bleDevice = null;
    }

    void WriteString(String str)
    {
        if ((bleTx != null) && (bleGatt != null))
        {
            bleTx.setValue(str + "\n"); // MUST add newline to end of each string
            if (!bleGatt.writeCharacteristic(bleTx))
                bleCB.onWrite(BLESTAT_CALL_FAILED);
        }
        else bleCB.onWrite(BLESTAT_DISCONNECTED);
    }

    private void ShowProperties(String type, BluetoothGattCharacteristic ch)
    {
        int props = ch.getProperties();
        if ((props & BluetoothGattCharacteristic.PROPERTY_READ) != 0)
            Log.v(LOGNAME, type + "=PropRead");
        if ((props & BluetoothGattCharacteristic.PROPERTY_WRITE) != 0)
            Log.v(LOGNAME, type + "=PropWrite");
        if ((props & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0)
            Log.v(LOGNAME, type + "=PropNotify");
        if ((props & BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0)
            Log.v(LOGNAME, type + "=PropIndicate");

        int perms = ch.getPermissions();
        if ((perms & BluetoothGattCharacteristic.PERMISSION_READ) != 0)
            Log.v(LOGNAME, type + "=PermRead");
        if ((perms & BluetoothGattCharacteristic.PERMISSION_WRITE) != 0)
            Log.v(LOGNAME, type + "=PermRead");
    }

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
                bleCB.onScan(name, id);
            }
        }

        public void onBatchScanResults(List<ScanResult> results) {}

        // @param errorCode Error code (one of SCAN_FAILED_*)
        public void onScanFailed(int errorCode)
        {
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
                    Log.i(LOGNAME, "GATT now connected!");
                    bleGatt.discoverServices();
                }
                else Log.w(LOGNAME, "No GATT on connect");
            }
            else if (newState == BluetoothProfile.STATE_DISCONNECTED)
            {
                Log.i(LOGNAME, "GATT now disconnected");
                bleCB.onDisconnect();
            }
        }

        @Override public void onServicesDiscovered(BluetoothGatt gatt, int status)
        {
            if (status == BluetoothGatt.GATT_SUCCESS)
            {
                for (BluetoothGattService service : bleGatt.getServices())
                {
                    Log.d(LOGNAME, "Service=" + service.getUuid());
                    if (service.getUuid().toString().equals(UUID_UART))
                    {
                        Log.v(LOGNAME, "Found UART Service");

                        bleTx = bleRx = null;
                        for (BluetoothGattCharacteristic ch : service.getCharacteristics())
                        {
                                 if (ch.getUuid().toString().equals(UUID_TX)) bleTx = ch;
                            else if (ch.getUuid().toString().equals(UUID_RX)) bleRx = ch;
                        }

                        if ((bleTx != null) && (bleRx != null))
                        {
                            Log.v(LOGNAME, "Found RX and TX Chars!");
                            ShowProperties("TX", bleTx);
                            ShowProperties("RX", bleRx);

                            BluetoothGattDescriptor config = bleRx.getDescriptor(UUID.fromString(CH_CONFIG));
                            if (config != null)
                            {
                                Log.v(LOGNAME, "Found Config Descriptor");
                                bleGatt.setCharacteristicNotification(bleRx, true);
                                config.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                                bleGatt.writeDescriptor(config);

                                bleCB.onConnect(BLESTAT_SUCCESS);
                                return;
                            }
                        }
                    }
                }
            }
            bleCB.onConnect(BLESTAT_NO_SERVICES);
        }

        @Override public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status)
        {
            if (status == BluetoothGatt.GATT_SUCCESS)
                 bleCB.onWrite(BLESTAT_SUCCESS);
            else bleCB.onWrite(BLESTAT_CALL_FAILED);
        }

        @Override public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic)
        {
            bleGatt.readCharacteristic(bleRx);
            byte[] bytes = bleRx.getValue();

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

                //String s = str.substring(0,i);
                //Log.v(LOGNAME, "Substr=" + s);
                //strLine += s;

                strLine += str.substring(0,i);
                bleCB.onRead(strLine);
                strLine = "";
                str = str.substring(i+1);
            }

            //Log.v(LOGNAME, "StrLine=" + strLine);
        }

        @Override public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {}
    };
}
