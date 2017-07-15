package com.devicenut.pixelnutctrl;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import static com.devicenut.pixelnutctrl.Main.CMD_BLUENAME;
import static com.devicenut.pixelnutctrl.Main.CMD_BRIGHT;
import static com.devicenut.pixelnutctrl.Main.CMD_DELAY;
import static com.devicenut.pixelnutctrl.Main.CMD_EXTMODE;
import static com.devicenut.pixelnutctrl.Main.CMD_PAUSE;
import static com.devicenut.pixelnutctrl.Main.CMD_PROPVALS;
import static com.devicenut.pixelnutctrl.Main.CMD_RESUME;
import static com.devicenut.pixelnutctrl.Main.CMD_TRIGGER;
import static com.devicenut.pixelnutctrl.Main.ble;
import static com.devicenut.pixelnutctrl.Main.internalPatterns;
import static com.devicenut.pixelnutctrl.Main.maxlenSendStrs;
import static com.devicenut.pixelnutctrl.Main.patternNames;
import static com.devicenut.pixelnutctrl.Main.curBright;
import static com.devicenut.pixelnutctrl.Main.curDelay;
import static com.devicenut.pixelnutctrl.Main.curPattern;
import static com.devicenut.pixelnutctrl.Main.patternStrs;
import static com.devicenut.pixelnutctrl.Main.rangeDelay;
import static com.devicenut.pixelnutctrl.Main.xmodeEnabled;
import static com.devicenut.pixelnutctrl.Main.xmodeHue;
import static com.devicenut.pixelnutctrl.Main.xmodePixCnt;
import static com.devicenut.pixelnutctrl.Main.xmodeWhite;
import static com.devicenut.pixelnutctrl.Main.devName;

@SuppressWarnings("unchecked")
public class Controls extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener, Bluetooth.BleCallbacks
{
    private final String LOGNAME = "Controls";
    private final Activity context = this;

    private TextView nameText;
    private Button pauseButton;
    private Button helpButton;
    private TextView helpText;
    private TextView helpTitle;
    private LinearLayout layoutControls;
    private Spinner selectPattern;
    private SeekBar seekBright;
    private SeekBar seekDelay;
    private SeekBar seekPropColor;
    private SeekBar seekPropWhite;
    private SeekBar seekPropCount;
    private SeekBar seekTrigForce;
    private ToggleButton toggleAutoProp;

    private int trigForce = 500;
    private boolean inHelpMode = false;
    private boolean doUpdate = true;

    private boolean isConnected = false;
    private boolean sendEnable = false;
    private boolean isEditing = false;
    private boolean firstPattern = true;

    @Override protected void onCreate(Bundle savedInstanceState)
    {
        Log.d(LOGNAME, ">>onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_control);

        //getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN); // hides keyboard on entry?

        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<>(this, R.layout.layout_spinner, patternNames);
        spinnerArrayAdapter.setDropDownViewResource(R.layout.layout_spinner);

        selectPattern = (Spinner) findViewById(R.id.spinner_Pattern);
        selectPattern.setAdapter(spinnerArrayAdapter);
        selectPattern.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
            {
                //TextView v = (TextView)view;
                //v.setTextColor(ContextCompat.getColor(context, R.color.UserChoice));
                //v.setTextSize(18);

                if (!firstPattern || (internalPatterns == 0))
                {
                    // always reset the pattern from scratch
                    Log.d(LOGNAME, "Pattern choice: " + parent.getItemAtPosition(position));
                    curPattern = position+1; // curPattern starts at 1

                    SendString("P");
                    if (internalPatterns == 0)
                    {
                        SendString(".");
                        SendString(patternStrs[position]);
                        SendString(".");
                    }
                    SendString("" + curPattern);
                }
                else firstPattern = false;
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        seekBright    = (SeekBar) findViewById(R.id.seek_Bright);
        seekDelay     = (SeekBar) findViewById(R.id.seek_Delay);
        seekPropColor = (SeekBar) findViewById(R.id.seek_PropColor);
        seekPropWhite = (SeekBar) findViewById(R.id.seek_PropWhite);
        seekPropCount = (SeekBar) findViewById(R.id.seek_PropCount);
        seekTrigForce = (SeekBar) findViewById(R.id.seek_TrigForce);

        toggleAutoProp = (ToggleButton) findViewById(R.id.toggle_AutoProp);

        seekBright.setOnSeekBarChangeListener(this);
        seekDelay.setOnSeekBarChangeListener(this);
        seekPropColor.setOnSeekBarChangeListener(this);
        seekPropWhite.setOnSeekBarChangeListener(this);
        seekPropCount.setOnSeekBarChangeListener(this);
        seekTrigForce.setOnSeekBarChangeListener(this);

        layoutControls  = (LinearLayout) findViewById(R.id.layout_Controls);
        nameText        = (TextView)     findViewById(R.id.text_Devname);
        pauseButton     = (Button)       findViewById(R.id.button_Pause);
        helpButton      = (Button)       findViewById(R.id.button_Help);
        helpTitle       = (TextView)     findViewById(R.id.view_HelpTitle);
        helpText        = (TextView)     findViewById(R.id.view_HelpText);
    }

    @Override protected void onResume()
    {
        Log.d(LOGNAME, ">>onResume");
        super.onResume();

        if (isEditing)
        {
            Log.d(LOGNAME, "Renaming device: " + devName);
            SendString(CMD_BLUENAME + devName);
            isEditing = false;
        }
        else
        {
            ble = new Bluetooth(this);
            ble.setCallbacks(this);

            devName = ble.getCurDevName();
            if ((devName == null) || (devName.length() < 3)) // have disconnected
            {
                Log.w(LOGNAME, "Lost connection (no device name)");
                Toast.makeText(context, "Lost connection", Toast.LENGTH_SHORT).show();
                onBackPressed();
                return;
            }
            devName = devName.substring(2);
            Log.d(LOGNAME, "Device name: " + devName);

            isConnected = true;
            sendEnable = false; // prevent following from writing commands
            firstPattern = true; // prevent sending pattern on initial selection

            SetManualControls();
            toggleAutoProp.setChecked(xmodeEnabled);
            selectPattern.setSelection(curPattern-1, false); // curPattern starts at 1
            seekBright.setProgress(curBright);
            seekDelay.setProgress(((rangeDelay - curDelay) * 100) / (rangeDelay + rangeDelay));
            seekPropColor.setProgress(((xmodeHue * 100) / 360));
            seekPropWhite.setProgress(xmodeWhite);
            seekPropCount.setProgress(xmodePixCnt);
            seekTrigForce.setProgress(trigForce / 10);

            sendEnable = true; // allow controls to work now
        }
        nameText.setText(devName);
    }

    @Override protected void onPause()
    {
        Log.d(LOGNAME, ">>onPause");
        super.onPause();

        if (!isEditing) ble.disconnect();
    }

    @Override public void onBackPressed()
    {
        if (inHelpMode) SetHelpMode();

        else super.onBackPressed();
    }

    private void SendString(String str)
    {
        if (sendEnable) ble.WriteString(str);
    }

    private void SetHelpMode()
    {
        if (inHelpMode)
        {
            helpText.setVisibility(View.GONE);
            helpTitle.setVisibility(View.GONE);
            layoutControls.setVisibility(View.VISIBLE);
            helpButton.setText(getResources().getString(R.string.name_help));
            inHelpMode = false;
        }
        else
        {
            layoutControls.setVisibility(View.GONE);
            helpTitle.setVisibility(View.VISIBLE);
            helpText.setVisibility(View.VISIBLE);
            helpButton.setText(getResources().getString(R.string.name_controls));
            inHelpMode = true;
        }
    }

    private void SetManualControls()
    {
        seekPropColor.setEnabled(xmodeEnabled);
        seekPropWhite.setEnabled(xmodeEnabled);
        seekPropCount.setEnabled(xmodeEnabled);
    }

    public void onClick(View v)
    {
        switch (v.getId())
        {
            case R.id.text_Devname:
            {
                isEditing = true;
                startActivity( new Intent(Controls.this, EditName.class) );
                break;
            }
            case R.id.button_Pause:
            {
                SendString(doUpdate ? CMD_PAUSE : CMD_RESUME);
                doUpdate = !doUpdate;
                pauseButton.setText(doUpdate ? "Pause" : "Resume");
                break;
            }
            case R.id.button_Help:
            {
                SetHelpMode();
                break;
            }
            case R.id.toggle_AutoProp:
            {
                xmodeEnabled = toggleAutoProp.isChecked();
                Log.d(LOGNAME, "AutoProp: manual=" + xmodeEnabled);
                SetManualControls();

                if (xmodeEnabled)
                     SendString(CMD_EXTMODE + "1");
                else SendString(CMD_EXTMODE + "0");
                break;
            }
            case R.id.button_TrigAction:
            {
                SendString(CMD_TRIGGER + trigForce);
                break;
            }
        }
    }

    @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
    {
        switch (seekBar.getId())
        {
            case R.id.seek_Bright:
            {
                curBright = progress;
                SendString(CMD_BRIGHT + curBright);
                break;
            }
            case R.id.seek_Delay:
            {
                curDelay = rangeDelay - (progress * 2 * rangeDelay)/100;
                SendString(CMD_DELAY + curDelay);
                break;
            }
            case R.id.seek_PropColor:
            {
                xmodeHue = (progress * 359) / 100;
                SendString(CMD_PROPVALS + xmodeHue + " " + xmodeWhite + " " + xmodePixCnt);
                break;
            }
            case R.id.seek_PropWhite:
            {
                xmodeWhite = progress;
                SendString(CMD_PROPVALS + xmodeHue + " " + xmodeWhite + " " + xmodePixCnt);
                break;
            }
            case R.id.seek_PropCount:
            {
                xmodePixCnt = progress;
                SendString(CMD_PROPVALS + xmodeHue + " " + xmodeWhite + " " + xmodePixCnt);
                break;
            }
            case R.id.seek_TrigForce:
            {
                trigForce = (10 * progress);
                break;
            }
        }
    }

    @Override public void onStartTrackingTouch(SeekBar seekBar) {}
    @Override public void onStopTrackingTouch(SeekBar seekBar)  {}

    private void DeviceDisconnect(final String reason)
    {
        if (isConnected)
        {
            isConnected = false;
            context.runOnUiThread(new Runnable()
            {
                public void run()
                {
                    Toast.makeText(context, "Disconnect: " + reason, Toast.LENGTH_SHORT).show();
                    inHelpMode = false;

                    if (!isFinishing()) onBackPressed();
                }
            });
        }
    }

    @Override public void onScan(String name, int id)
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
        if (status != 0)
        {
            Log.e(LOGNAME, "Write status: " + status); //Integer.toHexString(status));
            DeviceDisconnect("Write");
        }
    }

    @Override public void onRead(String reply)
    {
        Log.e(LOGNAME, "Unexpected onRead");
    }
}
