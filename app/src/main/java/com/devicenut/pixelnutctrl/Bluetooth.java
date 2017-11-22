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

import static com.devicenut.pixelnutctrl.Main.appContext;

class Bluetooth
{
    private static final String LOGNAME = "Bluetooth";

    private static final String UUID_UART   = "6e400001-b5a3-f393-e0a9-e50e24dcca9e";
    private static final String UUID_TX     = "6e400002-b5a3-f393-e0a9-e50e24dcca9e";
    private static final String UUID_RX     = "6e400003-b5a3-f393-e0a9-e50e24dcca9e";
    private static final String CH_CONFIG   = "00002902-0000-1000-8000-00805f9b34fb";
    private static final int MAXLEN_CHUNK   = 20;

    private static BluetoothAdapter bleAdapter = null;
    private static final List<BluetoothDevice> bleDevList = new ArrayList<>();
    private static BluetoothDevice bleDevice = null;
    private static BluetoothGatt bleGatt = null;
    private static BluetoothGattCharacteristic bleTx, bleRx;

    private static final PCQueue<String> writeQueue = new PCQueue<>(50);
    private static String strLine = "";
    private static boolean writeEnable = false;
    private static boolean doNextChunk = true;
    private static String[] writeChunks = null;
    private static int nextChunk = 0;

    static final int BLESTAT_SUCCESS        =  0;
    static final int BLESTAT_CALL_FAILED    = -1;
    static final int BLESTAT_DISCONNECTED   = -2;
    static final int BLESTAT_NO_SERVICES    = -3;

    interface BleCallbacks
    {
        void onScan(String name, int id);
        void onConnect(final int status);
        void onDisconnect();
        void onWrite(final int status);
        void onRead(String reply);
    }
    private static BleCallbacks bleCB;

    Bluetooth()
    {
        if (appContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE))
        {
            BluetoothManager manager = (BluetoothManager) appContext.getSystemService(Context.BLUETOOTH_SERVICE);
            if (manager != null) bleAdapter = manager.getAdapter();
        }
    }

    boolean checkForBlePresent()
    {
        return (bleAdapter != null);
    }
    boolean checkForBleEnabled()
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
        bleAdapter.getBluetoothLeScanner().startScan(null, builder.build(), bleScanDevicesCB);
    }

    void stopScanning()
    {
        bleAdapter.getBluetoothLeScanner().stopScan(bleScanDevicesCB);
    }

    boolean connect(int devid)
    {
        strLine = ""; // clear any read data

        BluetoothDevice bdev = bleDevList.get(devid);
        if (bdev == null) return false;

        Log.i(LOGNAME, "Connecting to GATT...");

        bleDevice = bdev;
        bleGatt = bdev.connectGatt(appContext, false, bleGattCB);

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

    /*
    boolean refreshDeviceCache() // this doesn't work to clear cached name
    {
        if (bleGatt == null) return false;
        try
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
        return false;
    }
    */

    void disconnect()
    {
        writeEnable = false; // stop write sender thread

        if (bleGatt != null)
        {
            Log.i(LOGNAME, "Disconnecting from GATT");
            bleGatt.disconnect();
        }
        else
        {
            Log.w(LOGNAME, "No GATT to disconnect");
            bleDevice = null;
        }
    }

    void WriteString(String str)
    {
        if (!writeEnable) Log.e(LOGNAME, "Write disabled: str=" + str + "\"");
        else if (!writeQueue.put(str)) Log.e(LOGNAME, "Queue full: str=" + str + "\"");
    }

    private void SendNextChunk()
    {
        String str = writeChunks[nextChunk++] + " ";

        int len = str.length();
        if (len >= MAXLEN_CHUNK) // cannot support chunks that are too large
        {
            Log.e(LOGNAME, "Chunk too large: str=\"" + str + "\"");
            bleCB.onWrite(BLESTAT_CALL_FAILED);
            writeEnable = false;
            return;
        }

        while ((nextChunk < writeChunks.length))
        {
            len += writeChunks[nextChunk].length() + 1; // 1 for space separator
            if (len >= MAXLEN_CHUNK) break;
            str += writeChunks[nextChunk++] + " ";
        }

        Log.v(LOGNAME, "Sending chunk: \"" + str + "\"");

        bleTx.setValue(str + "\n"); // MUST add newline to end of each string
        if (!bleGatt.writeCharacteristic(bleTx))
        {
            Log.e(LOGNAME, "Chunk write failed: \"" + str + "\"");
            bleCB.onWrite(BLESTAT_CALL_FAILED);
            writeEnable = false;
            return;
        }
    }

    private void StartSender()
    {
        new Thread()
        {
            @Override public void run()
            {
                Log.i(LOGNAME, "SenderThread starting...");
                while (writeEnable)
                {
                    yield();

                    if ((bleTx == null) || (bleGatt == null))
                    {
                        bleCB.onWrite(BLESTAT_DISCONNECTED);
                        writeEnable = false;
                    }
                    else if (doNextChunk)
                    {
                        if ((writeChunks != null) && (nextChunk < writeChunks.length))
                        {
                            doNextChunk = false; // wait for completion
                            SendNextChunk();
                        }
                        else if (!writeQueue.empty())
                        {
                            Log.v(LOGNAME, "Getting next command from queue");
                            String cmd1 = writeQueue.get();
                            if (cmd1 != null)
                            {
                                while(true) // coalesce same commands
                                {
                                    String cmd2 = writeQueue.peek();
                                    if ((cmd2 == null) || !cmd2.substring(0,1).equals(cmd1.substring(0,1)))
                                        break;

                                    Log.v(LOGNAME, "Skipping=\"" + cmd1 + "\" (\"" + cmd2 + "\")");
                                    cmd1 = writeQueue.get();
                                }
                                Log.d(LOGNAME, "Command=\"" + cmd1 + "\"");

                                writeChunks = cmd1.split("\\s+"); // remove ALL spaces
                                nextChunk = 0;

                                doNextChunk = false; // wait for completion
                                SendNextChunk();
                            }
                            else Log.e(LOGNAME, "Queue was empty");
                        }
                        // else wait for next string in queue
                    }
                }
                Log.i(LOGNAME, "SenderThread has ended");
            }
        }.start();
    }

    private void ShowProperties(String type, BluetoothGattCharacteristic ch)
    {
        int props = ch.getProperties();
        if ((props & BluetoothGattCharacteristic.PROPERTY_READ) != 0)       Log.v(LOGNAME, type + "=PropRead");
        if ((props & BluetoothGattCharacteristic.PROPERTY_WRITE) != 0)      Log.v(LOGNAME, type + "=PropWrite");
        if ((props & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0)     Log.v(LOGNAME, type + "=PropNotify");
        if ((props & BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0)   Log.v(LOGNAME, type + "=PropIndicate");

        int perms = ch.getPermissions();
        if ((perms & BluetoothGattCharacteristic.PERMISSION_READ) != 0)     Log.v(LOGNAME, type + "=PermRead");
        if ((perms & BluetoothGattCharacteristic.PERMISSION_WRITE) != 0)    Log.v(LOGNAME, type + "=PermRead");
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
            Log.e(LOGNAME, "Scan error=" + errorCode); // TODO: do something here!
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
                bleDevice = null;
            }
        }

        @Override public void onServicesDiscovered(BluetoothGatt gatt, int status)
        {
            if (status == BluetoothGatt.GATT_SUCCESS)
            {
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

                        if ((bleTx != null) && (bleRx != null))
                        {
                            Log.v(LOGNAME, "Found RX and TX Chars");
                            if (BuildConfig.DEBUG)
                            {
                                ShowProperties("TX", bleTx);
                                ShowProperties("RX", bleRx);
                            }

                            BluetoothGattDescriptor config = bleRx.getDescriptor(UUID.fromString(CH_CONFIG));
                            if (config != null)
                            {
                                Log.v(LOGNAME, "Found Config Descriptor");
                                bleGatt.setCharacteristicNotification(bleRx, true);
                                config.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                                bleGatt.writeDescriptor(config);

                                writeEnable = true;
                                StartSender();
                                bleCB.onConnect(BLESTAT_SUCCESS);
                                return;
                            }
                            else
                            {
                                Log.e(LOGNAME, "Config descriptor is null");
                                bleCB.onConnect(BLESTAT_CALL_FAILED);
                                return;
                            }
                        }
                        else
                        {
                            Log.e(LOGNAME, "Rx/Tx characteristic is null");
                            bleCB.onConnect(BLESTAT_CALL_FAILED);
                            return;
                        }
                    }
                }
            }
            bleCB.onConnect(BLESTAT_NO_SERVICES);
        }

        @Override public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status)
        {
            if (status == BluetoothGatt.GATT_SUCCESS)
            {
                if (nextChunk >= writeChunks.length)
                {
                    Log.v(LOGNAME, "Write string completed");
                    writeChunks = null;
                    nextChunk = 0;
                    bleCB.onWrite(BLESTAT_SUCCESS);
                }
                else Log.v(LOGNAME, "Write chunk finished");

                doNextChunk = true;
            }
            else bleCB.onWrite(BLESTAT_CALL_FAILED);
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

            //Log.v(LOGNAME, "StrLine=" + strLine);
        }

        @Override public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {}
    };
}
