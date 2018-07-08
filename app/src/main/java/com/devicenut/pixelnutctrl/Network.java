package com.devicenut.pixelnutctrl;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import static com.devicenut.pixelnutctrl.Main.DEVSTAT_FAILED;
import static com.devicenut.pixelnutctrl.Main.DEVSTAT_SUCCESS;
import static com.devicenut.pixelnutctrl.Main.MINLEN_REPLYSTR;
import static com.devicenut.pixelnutctrl.Main.appContext;
import static com.devicenut.pixelnutctrl.Main.msgWriteEnable;
import static com.devicenut.pixelnutctrl.Main.wifi;

public class Network extends AppCompatActivity implements Wifi.WifiCallbacks
{
    private final String LOGNAME = "Network";
    private final Activity context = this;

    private EditText pass;
    private TextView title;
    private ProgressBar waitRefresh;
    private ScrollView scrollView;
    private boolean isRefreshing = false;
    private Button doAdd, doConnect;
    private boolean haveNetworks = false;
    private boolean isConfirming = false;
    private StringBuilder replyString = new StringBuilder(MINLEN_REPLYSTR);

    private String listNames[] =
            {
                    "NetworkName1",
                    "NetworkName2",
                    "NetworkName3",
                    "NetworkName4",
                    "NetworkName5",
            };

    @Override protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_network);

        title = findViewById(R.id.title_Networks);
        scrollView = findViewById(R.id.scroll_networks);
        waitRefresh = findViewById(R.id.progress_Refresh);

        DoRefresh(true);

        doAdd = findViewById(R.id.button_AddToDevice);
        doConnect = findViewById(R.id.button_ConnectToNetwork);
        //doConnect.setEnabled(false);
        //doConnect.setAlpha((float)0.5);

        pass = findViewById(R.id.edit_NetPass);
        //pass.setOnEditorActionListener(SaveNetworkOnDone);

        CreateSpinnerAdapterNetworks();
        Spinner selectNetwork = findViewById(R.id.spinner_Networks);
        selectNetwork.setAdapter(arrayAdapter_Networks);

        //InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        //if (imm != null) imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
    }

    @Override protected void onResume()
    {
        Log.d(LOGNAME, ">>onResume");
        super.onResume();

        if (isConfirming)
        {
            Log.d(LOGNAME, "Returning from confirmation...");
            isConfirming = false;
        }
        else
        {
            replyString.setLength(0);
            wifi.setCallbacks(this);
        }
    }

    @Override public void onBackPressed()
    {
        if (isRefreshing) DoRefresh(false);

        else super.onBackPressed();
    }

    private ArrayAdapter<String> arrayAdapter_Networks;

    private void CreateSpinnerAdapterNetworks()
    {
        arrayAdapter_Networks = new ArrayAdapter<String>(appContext, R.layout.layout_spinner, listNames)
        {
            @Override public boolean areAllItemsEnabled()    { return true; }
            @Override public boolean isEnabled(int position) { return true; }

            @Override
            public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent)
            {
                View v = convertView;
                if (v == null)
                {
                    Context mContext = this.getContext();
                    LayoutInflater vi = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    if (vi != null) v = vi.inflate(R.layout.layout_spinner, null);
                }

                if (v != null)
                {
                    TextView tv = (TextView) v.findViewById(R.id.spinnerText);
                    tv.setText(listNames[position]);
                    tv.setTextColor(ContextCompat.getColor(appContext, R.color.UserChoice));
                }

                return v;
            }
        };
    }

    /*
    TextView.OnEditorActionListener SaveNetworkOnDone = new TextView.OnEditorActionListener()
    {
        @Override public boolean onEditorAction(TextView v, int actionId, KeyEvent event)
        {
            if (actionId == EditorInfo.IME_ACTION_DONE) SaveNetwork();
            return false;
        }
    };
    */

    private void DoRefresh(boolean dostart)
    {
        if (dostart)
        {
            title.setAlpha((float)0.3);
            scrollView.setAlpha((float)0.3);
            waitRefresh.setVisibility(View.VISIBLE);
            isRefreshing = true;
        }
        else
        {
            title.setAlpha((float)1.0);
            scrollView.setAlpha((float)1.0);
            waitRefresh.setVisibility(View.GONE);
            isRefreshing = false;
        }
    }

    private void SaveNetwork()
    {

    }

    private void DoExit()
    {
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.toggleSoftInput(InputMethodManager.RESULT_HIDDEN, 0);
        finish();
    }

    public void onClick(View v)
    {
        switch (v.getId())
        {
            case R.id.button_ClearStore:
            {
                Log.d(LOGNAME, "Clear Stored Networks...");
                isConfirming = true;
                Intent i = new Intent(this, Confirm.class);
                String str = getResources().getString(R.string.text_addnetwork);
                i.putExtra("Text", str);
                startActivity(i);
                break;
            }
            case R.id.button_NetRefresh:
            {
                Log.d(LOGNAME, "Network List Refresh...");
                DoRefresh(true);
                break;
            }
            case R.id.button_AddToDevice:
            {
                Log.d(LOGNAME, "Add Network to Device...");
                SaveNetwork();
                pass.setText("");
                break;
            }
            case R.id.button_ConnectToNetwork:
            {
                Log.d(LOGNAME, "Connect Device to Network...");
                isConfirming = true;
                Intent i = new Intent(this, Confirm.class);
                String str = getResources().getString(R.string.text_connectnetwork);
                i.putExtra("Text", str);
                startActivity(i);
                break;
            }
        }
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
        //DeviceDisconnect("Request");
    }

    @Override public void onWrite(final int status)
    {
        if (status == DEVSTAT_SUCCESS)
            msgWriteEnable = true;

        else if (status == DEVSTAT_FAILED)
        {
            Log.e(LOGNAME, "OnWrite: failed");
            //DeviceDisconnect("Write");
        }
        else Log.w(LOGNAME, "OnWrite: bad device state");
    }

    @Override public void onRead(String reply)
    {
        Log.d(LOGNAME, "WiFi reply: " + reply);
        replyString.append(reply);
    }

}
