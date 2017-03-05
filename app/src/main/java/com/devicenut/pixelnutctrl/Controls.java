package com.devicenut.pixelnutctrl;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import static com.devicenut.pixelnutctrl.AApp.curBright;
import static com.devicenut.pixelnutctrl.AApp.curDelay;
import static com.devicenut.pixelnutctrl.AApp.curPattern;
import static com.devicenut.pixelnutctrl.AApp.rangeDelay;
import static com.devicenut.pixelnutctrl.AApp.xmodeEnabled;
import static com.devicenut.pixelnutctrl.AApp.xmodeHue;
import static com.devicenut.pixelnutctrl.AApp.xmodePixCnt;
import static com.devicenut.pixelnutctrl.AApp.xmodeWhite;

@SuppressWarnings("unchecked")
public class Controls extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener, Bluetooth.BleCallbacks
{
    private final String LOGNAME = "Controls";
    private Activity context = this;

    private EditText editName;
    private boolean isEditing = true;
    private String devName;

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

    private final String CMD_BEACON     = "*";
    private final String CMD_BLUENAME   = "@";
    private final String CMD_BRIGHT     = "%";
    private final String CMD_DELAY      = ":";
    private final String CMD_EXTMODE    = "_";
    private final String CMD_PROPVALS   = "=";
    private final String CMD_TRIGGER    = "!";

    private String[] titlesPatterns =
            {
                    "Rainbow Wipe     ",
                    "Rainbow Roll     ",
                    "Light Waves      ",
                    "Blue Twinkle     ",
                    "Twinkle Comets   ",
                    "Dueling Comets   ",
                    "Dueling Scanners ",
                    "Ferris Wheel     ",
                    "White Noise      ",
                    "Bright Blinks    ",
                    "Bright Swells    ",
                    "Color Smooth     ",
                    "All Together     ",
            };

    private int trigForce = 500;
    private boolean inHelpMode = false;

    private Bluetooth ble;

    private PCQueue<String> writeQueue = new PCQueue(50);
    private boolean isConnected = false;
    private boolean writeEnable = false;
    private boolean writeBusy = false;

    @Override protected void onCreate(Bundle savedInstanceState)
    {
        Log.d(LOGNAME, ">>onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_control);

        //getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN); // hides keyboard on entry?

        editName = (EditText) findViewById(R.id.edit_DevName);
        editName.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS); // prevents second underline!
        editName.setOnEditorActionListener(new TextView.OnEditorActionListener()
        {
            @Override public boolean onEditorAction(TextView v, int actionId, KeyEvent event)
            {
                if (actionId == EditorInfo.IME_ACTION_DONE)
                {
                    if (!devName.equals(editName.getText().toString()))
                    {
                        if (editName.length() > 0)
                            QueueCmdStr(CMD_BLUENAME + editName.getText());

                        else editName.setText(devName);

                        isEditing = false;
                    }

                    editName.post(new Runnable() {
                        @Override public void run() { editName.clearFocus(); }
                    });
                }
                return false;
            }
        });
        editName.setOnFocusChangeListener(new View.OnFocusChangeListener()
        {
            public void onFocusChange(View v, boolean hasFocus)
            {
                if (hasFocus)
                {
                    devName = editName.getText().toString();
                    Log.d(LOGNAME, "DevName=" + devName);
                    isEditing = true;
                }
                else if (isEditing && !devName.equals(editName.getText().toString()))
                {
                    ClearEditFocus();
                    editName.setText(devName);
                    isEditing = false;
                }
            }
        });

        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(this, R.layout.layout_spinner, titlesPatterns);
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

                // always reset the pattern from scratch
                Log.d(LOGNAME, "Pattern choice: " + parent.getItemAtPosition(position));
                curPattern = position+1; // curPattern starts at 1
                QueueCmdStr("" + curPattern);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
        selectPattern.setOnTouchListener(new View.OnTouchListener()
        {
            @Override public boolean onTouch(View v, MotionEvent event)
            {
                ClearEditFocus();
                return false;
            }
        });
        selectPattern.setOnKeyListener(new View.OnKeyListener()
        {
            @Override public boolean onKey(View v, int keyCode, KeyEvent event)
            {
                ClearEditFocus();
                return false;
            }
        });

        seekBright = (SeekBar) findViewById(R.id.seek_Bright);
        seekDelay = (SeekBar) findViewById(R.id.seek_Delay);
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

        layoutControls = (LinearLayout) findViewById(R.id.layout_Controls);
        helpButton = (Button) findViewById(R.id.button_Help);
        helpTitle = (TextView) findViewById(R.id.view_HelpTitle);
        helpText = (TextView) findViewById(R.id.view_HelpText);
    }

    @Override protected void onResume()
    {
        Log.d(LOGNAME, ">>onResume");
        super.onResume();

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
        editName.setText(devName);

        isConnected = true;
        writeEnable = false; // prevent following from writing commands

        SetManualControls();
        toggleAutoProp.setChecked(xmodeEnabled);
        selectPattern.setSelection(curPattern-1, true); // curPattern starts at 1
        seekBright.setProgress(curBright);
        seekDelay.setProgress(((rangeDelay - curDelay) * 100) / (rangeDelay + rangeDelay));
        seekPropColor.setProgress(((xmodeHue * 100) / 360));
        seekPropWhite.setProgress(xmodeWhite);
        seekPropCount.setProgress(xmodePixCnt);
        seekTrigForce.setProgress(trigForce / 10);

        writeEnable = true;
        writeBusy = false;

        threadSendCmd.start();
    }

    @Override protected void onPause()
    {
        Log.d(LOGNAME, ">>onPause");
        super.onPause();

        writeEnable = false;
        ble.disconnect();
    }

    @Override public void onBackPressed()
    {
        if (inHelpMode) SetHelpMode();

        else super.onBackPressed();
    }

    private void SetHelpMode()
    {
        if (inHelpMode)
        {
            helpText.setVisibility(View.GONE);
            helpTitle.setVisibility(View.GONE);
            layoutControls.setVisibility(View.VISIBLE);
            helpButton.setText("Help");
            inHelpMode = false;
        }
        else
        {
            layoutControls.setVisibility(View.GONE);
            helpTitle.setVisibility(View.VISIBLE);
            helpText.setVisibility(View.VISIBLE);
            helpButton.setText("Controls");
            inHelpMode = true;
        }
    }

    private void SetManualControls()
    {
        seekPropColor.setEnabled(xmodeEnabled);
        seekPropWhite.setEnabled(xmodeEnabled);
        seekPropCount.setEnabled(xmodeEnabled);
    }

    private void ClearEditFocus() // hack to hide cursor and keyboard from EditView
    {
        editName.clearFocus();
        InputMethodManager inputManager = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputManager.hideSoftInputFromWindow(selectPattern.getWindowToken(), 0);
    }

    public void onClick(View v)
    {
        ClearEditFocus();
        switch (v.getId())
        {
            case R.id.button_Help:
            {
                SetHelpMode();
                break;
            }
            case R.id.button_Beacon:
            {
                QueueCmdStr(CMD_BEACON);
                break;
            }
            case R.id.toggle_AutoProp:
            {
                xmodeEnabled = toggleAutoProp.isChecked();
                Log.d(LOGNAME, "AutoProp: manual=" + xmodeEnabled);
                SetManualControls();

                if (xmodeEnabled)
                     QueueCmdStr(CMD_EXTMODE + "1");
                else QueueCmdStr(CMD_EXTMODE + "0");
                break;
            }
            case R.id.button_TrigAction:
            {
                QueueCmdStr(CMD_TRIGGER + trigForce);
                break;
            }
        }
    }

    @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
    {
        ClearEditFocus();
        switch (seekBar.getId())
        {
            case R.id.seek_Bright:
            {
                curBright = progress;
                QueueCmdStr(CMD_BRIGHT + curBright);
                break;
            }
            case R.id.seek_Delay:
            {
                curDelay = rangeDelay - (progress * 2 * rangeDelay)/100;
                QueueCmdStr(CMD_DELAY + curDelay);
                break;
            }
            case R.id.seek_PropColor:
            {
                xmodeHue = (progress * 359) / 100;
                QueueCmdStr(CMD_PROPVALS + xmodeHue + " " + xmodeWhite + " " + xmodePixCnt);
                break;
            }
            case R.id.seek_PropWhite:
            {
                xmodeWhite = progress;
                QueueCmdStr(CMD_PROPVALS + xmodeHue + " " + xmodeWhite + " " + xmodePixCnt);
                break;
            }
            case R.id.seek_PropCount:
            {
                xmodePixCnt = progress;
                QueueCmdStr(CMD_PROPVALS + xmodeHue + " " + xmodeWhite + " " + xmodePixCnt);
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

    private void DeviceDisconnect()
    {
        if (isConnected)
        {
            isConnected = false;
            context.runOnUiThread(new Runnable()
            {
                public void run()
                {
                    Toast.makeText(context, "Device Disconnected", Toast.LENGTH_SHORT).show();
                    inHelpMode = false;
                    onBackPressed(); // FIXME: crashes when reloading...
                }
            });
        }
    }

    private void QueueCmdStr(String cmdstr)
    {
        if (writeEnable)
        {
            Log.v(LOGNAME, "Queuing=\"" + cmdstr + "\"");
            if (!writeQueue.put(cmdstr))
            {
                Log.e(LOGNAME, "Queue full: cmd=" + cmdstr);
                DeviceDisconnect();
            }
        }
    }

    private void SendCmdStr()
    {
        if (!writeQueue.empty() && !writeBusy)
        {
            String cmd1 = writeQueue.get();
            if (cmd1 != null)
            {
                while(true) // coalesce same commands
                {
                    String cmd2 = writeQueue.peek();
                    if ((cmd2 == null) || !cmd2.substring(0,0).equals(cmd1.substring(0,0)))
                        break;

                    Log.v(LOGNAME, "Skipping=\"" + cmd1 + "\"");
                    cmd1 = writeQueue.get();
                }
                Log.d(LOGNAME, "Command=\"" + cmd1 + "\"");

                writeBusy = true;
                ble.WriteString(cmd1);
            }
            else Log.e(LOGNAME, "Queue was empty");
        }
    }

    private Thread threadSendCmd = new Thread()
    {
        @Override public void run()
        {
            while (writeEnable)
            {
                SendCmdStr();
                yield();
            }
        }
    };

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
        Log.e(LOGNAME, "Received disconnect");
        DeviceDisconnect();
    }

    @Override public void onWrite(final int status)
    {
        if (status != 0)
        {
            writeEnable = false;
            Log.e(LOGNAME, "Write status: " + Integer.toHexString(status));
            DeviceDisconnect();
        }
        writeBusy = false;
    }

    @Override public void onRead(String reply)
    {
        Log.e(LOGNAME, "Unexpected onRead");
    }
}
