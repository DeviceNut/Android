package com.devicenut.pixelnutctrl;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.CountDownTimer;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.devicenut.pixelnutctrl.Main.DEVSTAT_FAILED;
import static com.devicenut.pixelnutctrl.Main.DEVSTAT_SUCCESS;
import static com.devicenut.pixelnutctrl.Main.MINLEN_REPLYSTR;
import static com.devicenut.pixelnutctrl.Main.SendCommandString;
import static com.devicenut.pixelnutctrl.Main.WCMD_CONNECT_CLOUD;
import static com.devicenut.pixelnutctrl.Main.WCMD_GET_NETACTIVE;
import static com.devicenut.pixelnutctrl.Main.WCMD_GET_NETSTORE;
import static com.devicenut.pixelnutctrl.Main.WCMD_SET_NETSTORE;
import static com.devicenut.pixelnutctrl.Main.WCMD_SET_SOFTAP;
import static com.devicenut.pixelnutctrl.Main.appContext;
import static com.devicenut.pixelnutctrl.Main.helpActive;
import static com.devicenut.pixelnutctrl.Main.isConnected;
import static com.devicenut.pixelnutctrl.Main.msgWriteEnable;
import static com.devicenut.pixelnutctrl.Main.resumeScanning;
import static com.devicenut.pixelnutctrl.Main.wifi;

public class Network extends AppCompatActivity implements Wifi.WifiCallbacks
{
    private final String LOGNAME = "Network";
    private final Activity context = this;

    private EditText passEdit;
    private TextView title, networkSelect;
    private ProgressBar waitRefresh;
    private ScrollView scrollNet, helpPage;
    private Button doClearButton, doAddButton, doCntButton;
    private boolean doStoredNetworks = false;
    private boolean doAvailableNetworks = false;
    private boolean doAddNetwork = false;
    private boolean keepWaiting = false;
    private boolean isRefreshing = false;
    private boolean doFirstScan = false;
    private final StringBuilder replyString = new StringBuilder(MINLEN_REPLYSTR);

    private int storedCount = 0;
    private int availableCount = 0;
    private Spinner networkList;
    private String listNames[] = new String[0];

    private final int netNameIds1[] =
            {
                    R.id.text_NetName1_1,
                    R.id.text_NetName1_2,
                    R.id.text_NetName1_3,
            };
    private final int netNameIds2[] =
            {
                    R.id.text_NetName2_1,
                    R.id.text_NetName2_2,
                    R.id.text_NetName2_3,
            };
    private final int netNameIds3[] =
            {
                    R.id.text_NetName3_1,
                    R.id.text_NetName3_2,
                    R.id.text_NetName3_3,
            };
    private final int netNameIds4[] =
            {
                    R.id.text_NetName4_1,
                    R.id.text_NetName4_2,
                    R.id.text_NetName4_3,
            };
    private final int netNameIds5[] =
            {
                    R.id.text_NetName5_1,
                    R.id.text_NetName5_2,
                    R.id.text_NetName5_3,
            };
    private final int[] netNameIds[] =
            {
                 netNameIds1, netNameIds2, netNameIds3, netNameIds4, netNameIds5
            };

    private int netNameIndex = 0;
    private String listSecurity[] = new String[0];
    private String listCipher[] = new String[0];

    @Override protected void onCreate(Bundle savedInstanceState)
    {
        Log.d(LOGNAME, ">>onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_network);

        title = findViewById(R.id.title_Networks);
        scrollNet  = findViewById(R.id.scroll_Network);
        helpPage   = findViewById(R.id.scroll_HelpPage_Network);

        networkSelect = findViewById(R.id.text_NetworkSelect);
        waitRefresh = findViewById(R.id.progress_Refresh);
        networkList = findViewById(R.id.spinner_Networks);

        passEdit = findViewById(R.id.edit_NetPass);
        passEdit.setText(""); // clear input field

        doClearButton = findViewById(R.id.button_ClearStore);
        doAddButton   = findViewById(R.id.button_AddToDevice);
        doCntButton   = findViewById(R.id.button_ConnectToNetwork);
        SetConnectButton(false);

        wifi.setCallbacks(this);

        CreateSpinnerAdapterNetworks();
        networkList.setAdapter(arrayAdapter_Networks);
        networkList.setOnItemSelectedListener(netnameListener);

        doFirstScan = true;
        GetStoredNetworks(true);
    }

    @Override protected void onResume()
    {
        Log.d(LOGNAME, ">>onResume");
        super.onResume();
    }

    @Override protected void onPause()
    {
        super.onPause();
        Log.i(LOGNAME, ">>Pause");
        keepWaiting = false;
    }

    @Override public void onBackPressed()
    {
        if (helpActive) SetHelpMode(false);

        else if (isRefreshing) SetupDisplay(false);

        else super.onBackPressed();

        keepWaiting = false;
    }

    private ArrayAdapter<String> arrayAdapter_Networks;

    private void CreateSpinnerAdapterNetworks()
    {
        arrayAdapter_Networks = new ArrayAdapter<String>(appContext, R.layout.layout_spinner,
                                    new ArrayList<>(Arrays.asList(listNames)))
        {
            @Override public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent)
            {
                View v = convertView;
                if (v == null)
                {
                    Log.v(LOGNAME, "Inflating list view...");
                    Context mContext = this.getContext();
                    LayoutInflater vi = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    if (vi != null) v = vi.inflate(R.layout.layout_spinner, null);
                }

                if (v != null)
                {
                    Log.v(LOGNAME, "Setting list text...");
                    TextView tv = v.findViewById(R.id.spinnerText);
                    tv.setText(listNames[position]);
                    tv.setTextColor(ContextCompat.getColor(appContext, R.color.UserChoice));
                }

                return v;
            }
        };
    }

    private final AdapterView.OnItemSelectedListener netnameListener = new AdapterView.OnItemSelectedListener()
    {
        @Override public void onNothingSelected(AdapterView<?> parent) {}

        @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
        {
            Log.d(LOGNAME, "Network choice: " + parent.getItemAtPosition(position));
            netNameIndex = position;
        }
    };

    private void ClearDisplayedNetworks()
    {
        Log.d(LOGNAME, "Clear Displayed Networks");
        for (int i = 0; i < 5; ++i)
        {
            int[] netids = netNameIds[i];
            ((TextView)findViewById(netids[0])).setText("");
            ((TextView)findViewById(netids[1])).setText("");
            ((TextView)findViewById(netids[2])).setText("");
        }
    }

    private void SetConnectButton(boolean enabled)
    {
        if (enabled)
        {
            doCntButton.setEnabled(true);
            doCntButton.setAlpha(((float)1.0));
        }
        else
        {
            doCntButton.setEnabled(false);
            doCntButton.setAlpha((float)0.5);
        }
    }

    private void SetAddClrButtons()
    {
        if ((availableCount > 0) && (storedCount < 5))
        {
            doAddButton.setEnabled(true);
            doAddButton.setAlpha((float)1.0);
        }
        else
        {
            doAddButton.setEnabled(false);
            doAddButton.setAlpha((float)0.5);
        }

        if (storedCount > 0)
        {
            doClearButton.setEnabled(true);
            doClearButton.setAlpha((float)1.0);
        }
        else
        {
            doClearButton.setEnabled(false);
            doClearButton.setAlpha((float)0.5);
        }
    }

    private void SetupDisplay(boolean dostart)
    {
        helpPage.setVisibility(GONE);

        if (dostart)
        {
            title.setAlpha((float)0.3);
            scrollNet.setAlpha((float)0.3);
            waitRefresh.setVisibility(VISIBLE);
            isRefreshing = true;
        }
        else
        {
            title.setAlpha((float)1.0);
            scrollNet.setAlpha((float)1.0);
            waitRefresh.setVisibility(GONE);
            isRefreshing = false;
        }

        if (availableCount > 0)
        {
            networkSelect.setText("Select from available");
            networkList.setVisibility(VISIBLE);
        }
        else
        {
            networkSelect.setText("No networks found");
            networkList.setVisibility(GONE);
        }

        SetAddClrButtons();
    }

    private void SetHelpMode(boolean enable)
    {
        if (enable)
        {
            helpActive = true;
            scrollNet.setVisibility(GONE);
            helpPage.setVisibility(VISIBLE);
        }
        else
        {
            helpActive = false;
            helpPage.setVisibility(GONE);
            scrollNet.setVisibility(VISIBLE);
        }
    }

    public void onClick(View v)
    {
        if (!keepWaiting) switch (v.getId())
        {
            case R.id.button_ClearStore:
            {
                Log.d(LOGNAME, "Clear Stored Networks...");
                Intent i = new Intent(this, Confirm.class);
                String str = getResources().getString(R.string.text_addnetwork);
                i.putExtra("Text", str);
                startActivityForResult(i, 0);
                break;
            }
            case R.id.button_NetRefresh:
            {
                Log.d(LOGNAME, "Network List Refresh...");
                GetAvaiableNetworks(true);
                break;
            }
            case R.id.button_AddToDevice:
            {
                Log.d(LOGNAME, "Add Network to Device...");

                String passtext =  passEdit.getText().toString();
                if (passtext.trim().length() == 0) passtext = null;

                SetStoredNetwork(listNames[netNameIndex], passtext,
                                listSecurity[netNameIndex],
                                listCipher[netNameIndex]);

                passEdit.setText(""); // clear input field]
                break;
            }
            case R.id.button_ConnectToNetwork:
            {
                Log.d(LOGNAME, "Connect Device to Network...");
                Intent i = new Intent(this, Confirm.class);
                String str = getResources().getString(R.string.text_connectnetwork);
                i.putExtra("Text", str);
                startActivityForResult(i, 1);
                break;
            }
            case R.id.button_HelpPage2:
            {
                SetHelpMode(true);
                break;
            }
        }
    }

    private void GetStoredNetworks(boolean dowait)
    {
        Log.d(LOGNAME, "Retrieving Stored Networks...");

        doStoredNetworks = true;
        doAvailableNetworks = false;
        doAddNetwork = false;
        replyString.setLength(0);

        SendCommandString(WCMD_GET_NETSTORE);
        if (dowait) WaitForResponse();
    }

    private void GetAvaiableNetworks(boolean dowait)
    {
        Log.d(LOGNAME, "Retrieving Available Networks");

        doStoredNetworks = false;
        doAvailableNetworks = true;
        doAddNetwork = false;
        replyString.setLength(0);

        SendCommandString(WCMD_GET_NETACTIVE);
        if (dowait) WaitForResponse();
    }

    private void SetStoredNetwork(String netname, String password, String security, String cipher)
    {
        StringBuilder bstr = new StringBuilder(50);

        bstr.append(WCMD_SET_NETSTORE);
        if (netname != null)
        {
            bstr.append(" ").append(netname);

            if (security != null)
            {
                password = (password == null) ? "*" : password; // use special placeholder for empty password
                bstr.append(" ").append(password);

                bstr.append(" ").append(security);

                if (cipher != null)
                    bstr.append(" ").append(cipher);
            }
        }
        String s = bstr.toString();

        Log.d(LOGNAME, "Set Stored Network: " + s);

        doStoredNetworks = false;
        doAvailableNetworks = false;
        doAddNetwork = true;

        SendCommandString(s);
        WaitForResponse();
    }

    private void WaitForResponse()
    {
        keepWaiting = true;
        SetupDisplay(true);

        new CountDownTimer(10000, 125)
        {
            public void onTick(long millisUntilFinished)
            {
                if (!keepWaiting)
                {
                    cancel();
                    SetupDisplay(false);
                }
            }
            public void onFinish()
            {
                Toast.makeText(getApplicationContext(), "Press Back to cancel waiting...", Toast.LENGTH_SHORT).show();
                start();
            }

        }.start();
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (resultCode == Activity.RESULT_OK)
        {
            if (requestCode == 0) // ClearStore
            {
                Log.d(LOGNAME, "Clearing Stored Networks");
                SetStoredNetwork(null, null, null, null); // this clears all stored networks
            }
            else // ConnectToDevice
            {
                doStoredNetworks = false;
                doAvailableNetworks = false;
                doAddNetwork = false;

                SendCommandString(WCMD_SET_SOFTAP + "0"); // first disable SoftAP mode
                SendCommandString(WCMD_CONNECT_CLOUD);    // then connect to the cloud
                WaitForResponse();
            }
        }
    }

    private void DeviceDisconnect(final String reason)
    {
        Log.v(LOGNAME, "Device disconnect: reason=" + reason + " connected=" + isConnected);
        if (isConnected)
        {
            context.runOnUiThread(new Runnable()
            {
                public void run()
                {
                    Toast.makeText(context, "Disconnect: " + reason, Toast.LENGTH_SHORT).show();
                }
            });

            isConnected = false;
        }
        else Log.w(LOGNAME, "Device not connected!");

        Log.d(LOGNAME, "Finishing network activity...");
        finish();
    }

    @Override public void onScan(String name, int id, boolean isble)
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
        if (status == DEVSTAT_SUCCESS)
            msgWriteEnable = true;

        else if (status == DEVSTAT_FAILED)
        {
            Log.e(LOGNAME, "OnWrite: failed");
            DeviceDisconnect("Write");
        }
        else Log.w(LOGNAME, "OnWrite: bad device state");
    }

    @Override public void onRead(final String reply)
    {
        if (reply != null)
        {
            Log.v(LOGNAME, "WiFi reply: " + reply);
            replyString.append(reply);
            replyString.append("\n");
        }
        else context.runOnUiThread(new Runnable()
        {
            public void run()
            {
                if (doStoredNetworks) // display stored networks
                {
                    Log.d(LOGNAME, "Display Stored Networks: " + replyString.toString());
                    doStoredNetworks = false;

                    ClearDisplayedNetworks();
                    storedCount = 0;

                    if (replyString.length() > 0)
                    {
                        String strs[] = replyString.toString().split("\n");
                        for (int i = 0; i < strs.length; ++i)
                        {
                            Log.v(LOGNAME, "  " + i + ") " + strs[i]);
                            int[] netids = netNameIds[i];
                            String items[] = strs[i].split(" ");

                            if (items.length > 0) ((TextView)findViewById(netids[0])).setText(items[0]);
                            if (items.length > 1) ((TextView)findViewById(netids[1])).setText(items[1]);
                            if (items.length > 2) ((TextView)findViewById(netids[2])).setText(items[2]);

                            if (items.length > 0) ++storedCount;
                        }

                        replyString.setLength(0);
                    }

                    SetConnectButton(storedCount > 0);

                    if (doFirstScan)
                    {
                        doFirstScan = false;
                        GetAvaiableNetworks(false);
                    }
                    else
                    {
                        SetAddClrButtons();
                        keepWaiting = false;
                    }
                }
                else if (doAvailableNetworks) // create list of available networks
                {
                    Log.d(LOGNAME, "Display Available Networks: " + replyString.toString());
                    doAvailableNetworks = false;

                    if (replyString.length() > 0)
                    {
                        String strs[] = replyString.toString().split("\n");
                        listNames = new String[strs.length];
                        listSecurity = new String[strs.length];
                        listCipher = new String[strs.length];

                        arrayAdapter_Networks.clear();
                        availableCount = 0;

                        for (int i = 0; i < strs.length; ++i)
                        {
                            Log.d(LOGNAME, "Adding network: " + strs[i]);
                            String items[] = strs[i].split(" ");

                            Log.v(LOGNAME, "#strs=" + strs.length + " #items=" + items.length);

                            if (items.length > 0)
                            {
                                ++availableCount;
                                listNames[i] = items[0];
                            }
                            listSecurity[i] = (items.length > 1) ? items[1] : null;
                            listCipher[i]   = (items.length > 2) ? items[2] : null;
                        }

                        arrayAdapter_Networks.addAll(listNames);
                        arrayAdapter_Networks.notifyDataSetChanged();

                        replyString.setLength(0);
                    }

                    SetAddClrButtons();
                    keepWaiting = false;
                }
                else if (doAddNetwork)
                {
                    doAddNetwork = false;

                    if (!replyString.toString().startsWith("ok")) // return value from Add Network failed
                    {
                        keepWaiting = false;
                        Toast.makeText(context, "Add Network: " + replyString.toString().trim(), Toast.LENGTH_LONG).show();
                    }
                    else GetStoredNetworks(false); // now retrieve all stored networks to update display
                }
                else // return value from Connect Device (ignore errors)
                {
                    isConnected = false;
                    resumeScanning = false;
                    wifi.disconnect();
                    finish();
                }
            }
        });
    }
}
