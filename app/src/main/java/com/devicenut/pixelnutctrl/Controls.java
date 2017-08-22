package com.devicenut.pixelnutctrl;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.devicenut.pixelnutctrl.Bluetooth.BLESTAT_DISCONNECTED;
import static com.devicenut.pixelnutctrl.Main.CMD_BLUENAME;
import static com.devicenut.pixelnutctrl.Main.CMD_BRIGHT;
import static com.devicenut.pixelnutctrl.Main.CMD_DELAY;
import static com.devicenut.pixelnutctrl.Main.CMD_EXTMODE;
import static com.devicenut.pixelnutctrl.Main.CMD_PAUSE;
import static com.devicenut.pixelnutctrl.Main.CMD_PROPVALS;
import static com.devicenut.pixelnutctrl.Main.CMD_RESUME;
import static com.devicenut.pixelnutctrl.Main.CMD_TRIGGER;
import static com.devicenut.pixelnutctrl.Main.basicPatternsCount;
import static com.devicenut.pixelnutctrl.Main.ble;
import static com.devicenut.pixelnutctrl.Main.customPatterns;
import static com.devicenut.pixelnutctrl.Main.devPatternHelp;
import static com.devicenut.pixelnutctrl.Main.devPatternNames;
import static com.devicenut.pixelnutctrl.Main.devPatternCmds;
import static com.devicenut.pixelnutctrl.Main.devPatternBits;
import static com.devicenut.pixelnutctrl.Main.numSegments;
import static com.devicenut.pixelnutctrl.Main.numPatterns;
import static com.devicenut.pixelnutctrl.Main.curBright;
import static com.devicenut.pixelnutctrl.Main.curDelay;
import static com.devicenut.pixelnutctrl.Main.curPattern;
import static com.devicenut.pixelnutctrl.Main.posSegStart;
import static com.devicenut.pixelnutctrl.Main.posSegCount;
import static com.devicenut.pixelnutctrl.Main.rangeDelay;
import static com.devicenut.pixelnutctrl.Main.stdPatternsCount;
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
    private Button pauseButton, helpButton, helpButton2;
    private TextView helpText, helpText2, helpTitle, textTrigger;
    private LinearLayout outerControls, innerControls, patternHelp;
    private LinearLayout autoControls, llPropColor, llPropWhite, llPropCount;
    private LinearLayout trigControls, llTrigForce;
    private Spinner selectPattern;
    private SeekBar seekBright, seekDelay;
    private SeekBar seekPropColor, seekPropWhite, seekPropCount;
    private SeekBar seekTrigForce;
    private ToggleButton toggleAutoProp;

    private int trigForce = 500;
    private int helpMode = 0;
    private boolean doUpdate = true;

    private boolean isConnected = false;
    private boolean sendEnable = false;
    private boolean isEditing = false;
    private boolean changePattern = true;

    private final int segRadioIds[] =
            {
                    R.id.radio_1,
                    R.id.radio_2,
                    R.id.radio_3,
                    R.id.radio_4,
                    R.id.radio_5,
                    R.id.radio_6,
            };

    private String[] patternNames;
    private boolean[] listEnables;
    private int[] mapPatternToIndex;
    private int[] mapIndexToPattern;
    private final boolean segEnables[] = { true, false, false, false, false, false };
    private final int segPatterns[] = { 0,0,0,0,0,0 };

    @Override protected void onCreate(Bundle savedInstanceState)
    {
        Log.d(LOGNAME, ">>onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_controls);

        /*
        final LinearLayout mainLayout = (LinearLayout) this.getLayoutInflater().inflate(R.layout.activity_controls, null);

        // set a global layout listener which will be called when the layout pass is completed and the view is drawn
        mainLayout.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener()
                {
                    public void onGlobalLayout()
                    {
                        //Remove the listener before proceeding
                        mainLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                        ScrollView v = (ScrollView) findViewById(R.id.scroll_Controls);
                        Rect r = new Rect();

                        v.getGlobalVisibleRect(r);
                        Log.w(LOGNAME, "ScrollView top=" + String.valueOf(r.top));
                        Log.w(LOGNAME, "ScrollView bottom=" + String.valueOf(r.bottom));

                        int height = v.getChildAt(0).getHeight();
                        Log.w(LOGNAME, "Scroll height=" + height);

                        if (height <= (r.bottom - r.top))
                        {
                            int extra = (pixelLength - r.top - height);
                            Log.w(LOGNAME, "Excess pixels=" + extra);
                        }

                        LinearLayout ll = (LinearLayout) findViewById(R.id.auto_Controls);
                        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) ll.getLayoutParams();
                        layoutParams.topMargin = 200;
                        ll.setLayoutParams(layoutParams);
                    }
                }
        );
        setContentView(mainLayout);
        */

        //getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN); // hides keyboard on entry?

        SetupSegments(numSegments);

        int j = 0;
        int extra = (customPatterns > 0) ? 1 : 0;
        if (stdPatternsCount > 0) extra += 2;
        patternNames = new String[numPatterns + extra];
        listEnables = new boolean[numPatterns + extra];
        mapIndexToPattern = new int[numPatterns + extra];
        mapPatternToIndex = new int[numPatterns];

        if (customPatterns > 0)
        {
            patternNames[j] = "Custom Patterns";
            listEnables[j] = false;
            mapIndexToPattern[j] = 0;
            ++j;
        }
        else
        {
            patternNames[j] = "Basic Patterns";
            listEnables[j] = false;
            mapIndexToPattern[j] = 0;
            ++j;
        }

        for (int i = 0; i < numPatterns; ++i)
        {
            if ((i > 0) && (i == customPatterns))
            {
                patternNames[j] = "Basic Patterns";
                listEnables[j] = false;
                mapIndexToPattern[j] = 0;
                ++j;
            }

            if ((i > customPatterns) && (i == basicPatternsCount))
            {
                patternNames[j] = "Advanced Patterns";
                listEnables[j] = false;
                mapIndexToPattern[j] = 0;
                ++j;
            }

            Log.v(LOGNAME, "Adding pattern: " + devPatternNames[i]);

            patternNames[j] = devPatternNames[i];
            listEnables[j] = true;
            mapIndexToPattern[j] = i + 1; // curPattern starts at 1;
            mapPatternToIndex[i] = j;
            ++j;
        }

        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(this, R.layout.layout_spinner, patternNames)
        {
            @Override public boolean isEnabled(int position)
            {
                return listEnables[position];
            }

            @Override public boolean areAllItemsEnabled()
            {
                return ((customPatterns == 0) || (stdPatternsCount == 0));
            }

            @Override public View getDropDownView(int position, View convertView, ViewGroup parent)
            {
                View v = convertView;
                if (v == null)
                {
                    Context mContext = this.getContext();
                    LayoutInflater vi = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    v = vi.inflate(R.layout.layout_spinner, null);
                }

                TextView tv = (TextView) v.findViewById(R.id.spinnerText);
                tv.setText(patternNames[position]);

                if (!listEnables[position]) tv.setTextColor(Color.GRAY);
                else tv.setTextColor(ContextCompat.getColor(context, R.color.UserChoice));

                return v;
            }
        };
        //spinnerArrayAdapter.setDropDownViewResource(R.layout.layout_spinner);

        selectPattern = (Spinner) findViewById(R.id.spinner_Pattern);
        selectPattern.setAdapter(spinnerArrayAdapter);
        selectPattern.setOnItemSelectedListener(patternListener);

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

        outerControls   = (LinearLayout) findViewById(R.id.ll_OuterControls);
        innerControls   = (LinearLayout) findViewById(R.id.ll_InnerControls);
        patternHelp     = (LinearLayout) findViewById(R.id.ll_PatternHelp);
        autoControls    = (LinearLayout) findViewById(R.id.auto_Controls);
        llPropColor     = (LinearLayout) findViewById(R.id.ll_PropColor);
        llPropWhite     = (LinearLayout) findViewById(R.id.ll_PropWhite);
        llPropCount     = (LinearLayout) findViewById(R.id.ll_PropCount);
        trigControls    = (LinearLayout) findViewById(R.id.trig_Controls);
        llTrigForce     = (LinearLayout) findViewById(R.id.ll_TrigForce);
        textTrigger     = (TextView)     findViewById(R.id.text_Trigger);
        nameText        = (TextView)     findViewById(R.id.text_Devname);
        pauseButton     = (Button)       findViewById(R.id.button_Pause);
        helpButton      = (Button)       findViewById(R.id.button_ControlsHelp);
        helpButton2     = (Button)       findViewById(R.id.button_PatternHelp);
        helpTitle       = (TextView)     findViewById(R.id.title_ControlsHelp);
        helpText        = (TextView)     findViewById(R.id.text_ControlsHelp);
        helpText2       = (TextView)     findViewById(R.id.text_PatternHelp);
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
            changePattern = (curPattern == 0); // set initial selection if one not already set
            if (changePattern) curPattern = 1; // default to first pattern

            CheckBox b = (CheckBox) findViewById(segRadioIds[0]);
            segEnables[0] = true;
            b.setChecked(true);

            SetupControls();
            toggleAutoProp.setChecked(xmodeEnabled);
            selectPattern.setSelection(mapPatternToIndex[curPattern-1], false);

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

        if (!isEditing && isConnected)
            ble.disconnect();
    }

    @Override public void onBackPressed()
    {
        if (helpMode < 0) SetHelpMode(true, 0);

        else super.onBackPressed();
    }

    AdapterView.OnItemSelectedListener patternListener = new AdapterView.OnItemSelectedListener()
    {
        @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
        {
            TextView v = (TextView)view;
            v.setTextColor(ContextCompat.getColor(context, R.color.UserChoice));
            v.setTextSize(18);

            if (changePattern)
            {
                Log.d(LOGNAME, "Pattern choice: " + parent.getItemAtPosition(position));

                curPattern = mapIndexToPattern[position];
                int index = curPattern-1;

                if (curPattern > customPatterns)
                {
                    if (numSegments == 1)
                    {
                        if (xmodeEnabled)
                        {
                            xmodeEnabled = false;
                            SendString(CMD_EXTMODE + "0");
                            toggleAutoProp.setChecked(false);
                        }
                        SendString(".");
                        SendString(devPatternCmds[index]);
                        SendString(".");
                        SendString("" + curPattern); // this clears by default
                    }
                    else
                    {
                        SendString(".");
                        SendString("X Y"); // clear segment info

                        for (int i = 0; i < numSegments; ++i)
                        {
                            if (segEnables[i])
                            {
                                SendString("X" + posSegStart[i] + " Y" + posSegCount[i]);
                                SendString(devPatternCmds[ segPatterns[i] ]);
                                segPatterns[i] = position;
                            }
                        }
                        SendString(".");

                        SendString("1"); // FIXME??? pattern=0 indicates custom pattern command
                    }
                }
                else SendString("" + curPattern); // just send device specific custom pattern number

                SetupControls();
                if (helpMode > 0) SetHelpMode(false, curPattern);
            }
            else changePattern = true;
        }
        @Override public void onNothingSelected(AdapterView<?> parent) {}
    };

    private void SendString(String str)
    {
        if (sendEnable) ble.WriteString(str);
    }

    private void SetHelpMode(boolean toggle, int newmode)
    {
        if ((helpMode < 0) || (newmode < 0))
        {
            if ((newmode == 0) || (helpMode < 0))
            {
                helpText.setVisibility(GONE);
                helpTitle.setVisibility(GONE);
                outerControls.setVisibility(VISIBLE);
                helpButton.setText(getResources().getString(R.string.name_help));
                helpMode = 0;
            }
            else
            {
                outerControls.setVisibility(GONE);
                helpTitle.setVisibility(VISIBLE);
                helpText.setVisibility(VISIBLE);
                helpButton.setText(getResources().getString(R.string.name_controls));
                helpMode = newmode;
            }
        }
        else if (((newmode == 0) || (helpMode > 0)) && toggle)
        {
            //innerControls.setVisibility(VISIBLE);
            helpButton2.setText("?");
            //helpButton2.setTextSize(22);
            patternHelp.setVisibility(GONE);
            helpMode = 0;
        }
        else
        {
            //innerControls.setVisibility(GONE);
            patternHelp.setVisibility(VISIBLE);
            helpButton2.setText("x");
            //helpButton2.setTextSize(18);
            helpText2.setText( devPatternHelp[newmode-1]);
            helpMode = newmode;
        }
    }

    private void SetupControls()
    {
        int bits = devPatternBits[curPattern-1];
        boolean domode = ((bits & 7) != 0);

        if (xmodeEnabled && domode)
        {
            autoControls.setVisibility(VISIBLE);
            toggleAutoProp.setEnabled(true);

            //seekPropColor.setEnabled((bits & 1) != 0);
            //seekPropWhite.setEnabled((bits & 2) != 0);
            //seekPropCount.setEnabled((bits & 4) != 0);
            llPropColor.setVisibility(((bits & 1) != 0) ? VISIBLE : GONE);
            llPropWhite.setVisibility(((bits & 2) != 0) ? VISIBLE : GONE);
            llPropCount.setVisibility(((bits & 4) != 0) ? VISIBLE : GONE);
        }
        else
        {
            autoControls.setVisibility(GONE);
            toggleAutoProp.setEnabled(domode);
        }

        if ((bits & 0x10) != 0) // enable triggering
        {
            trigControls.setVisibility(VISIBLE);
            //seekTrigForce.setEnabled((bits & 0x20) != 0);

            if ((bits & 0x20) != 0)
            {
                llTrigForce.setVisibility(VISIBLE);
                textTrigger.setText(getResources().getString(R.string.title_trigforce));
            }
            else
            {
                llTrigForce.setVisibility(GONE);
                textTrigger.setText(getResources().getString(R.string.title_dotrigger));
            }
        }
        else trigControls.setVisibility(GONE);
    }

    private void SetupSegments(int count)
    {
        LinearLayout ll = (LinearLayout) findViewById(R.id.ll_SelectSegments);
        if (count > 1)
        {
            ll.setVisibility(VISIBLE);

            for (int i = 0; i < segRadioIds.length; ++i)
            {
                boolean doshow = (i < count);
                CheckBox b = (CheckBox) findViewById(segRadioIds[i]);
                b.setVisibility(doshow ? VISIBLE : GONE);
                b.setEnabled(doshow);
                b.setFocusable(doshow);
                b.setClickable(doshow);
                b.setChecked(doshow);
                segEnables[i] = doshow;
            }
        }
        else ll.setVisibility(GONE);
    }

    private void SetSegment(int n)
    {
        segEnables[n] = ((CheckBox)findViewById(segRadioIds[n])).isChecked();

        boolean haveseg = false;
        for (int i = 0; i < numSegments; ++i)
            if (segEnables[i]) haveseg = true;

        //selectPattern.setEnabled(haveseg);
        //selectPattern.setFocusable(haveseg);

        LinearLayout ll = (LinearLayout) findViewById(R.id.ll_SelectPattern);
        ll.setVisibility(haveseg ? VISIBLE : GONE);
    }

    public void onClick(View v)
    {
        switch (v.getId())
        {
            case R.id.radio_1:
            {
                SetSegment(0);
                break;
            }
            case R.id.radio_2:
            {
                SetSegment(1);
                break;
            }
            case R.id.radio_3:
            {
                SetSegment(2);
                break;
            }
            case R.id.radio_4:
            {
                SetSegment(3);
                break;
            }
            case R.id.radio_5:
            {
                SetSegment(4);
                break;
            }
            case R.id.radio_6:
            {
                SetSegment(5);
                break;
            }
            case R.id.text_Devname:
            {
                isEditing = true;
                startActivity( new Intent(Controls.this, EditName.class) );
                break;
            }
            case R.id.button_Pause:
            {
                if (doUpdate)
                {
                    SendString(CMD_PAUSE);
                    pauseButton.setText(getResources().getString(R.string.name_pause));
                }
                else
                {
                    SendString(CMD_RESUME);
                    pauseButton.setText(getResources().getString(R.string.name_resume));
                }
                doUpdate = !doUpdate;
                break;
            }
            case R.id.button_PatternHelp:
            {
                SetHelpMode(true, curPattern);
                break;
            }
            case R.id.button_ControlsHelp:
            {
                SetHelpMode(true, -1);
                break;
            }
            case R.id.toggle_AutoProp:
            {
                xmodeEnabled = toggleAutoProp.isChecked();
                Log.d(LOGNAME, "AutoProp: manual=" + xmodeEnabled);
                SetupControls();

                if (xmodeEnabled)
                     SendString(CMD_EXTMODE + "1");
                else SendString(CMD_EXTMODE + "0");
                break;
            }
            case R.id.button_TrigAction:
            {
                if ((devPatternBits[curPattern-1] & 0x30) != 0)
                     SendString(CMD_TRIGGER + trigForce);
                else SendString(CMD_TRIGGER + 0);
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
        Log.v(LOGNAME, "Device disconnect: reason=" + reason + " connected=" + isConnected);
        if (isConnected)
        {
            isConnected = false;
            context.runOnUiThread(new Runnable()
            {
                public void run()
                {
                    Toast.makeText(context, "Disconnect: " + reason, Toast.LENGTH_SHORT).show();
                    helpMode = 0;

                    Log.v(LOGNAME, "Finishing controls activity...");
                    finish();
                    //if (!isFinishing()) onBackPressed();
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
        if ((status != 0) && (status != BLESTAT_DISCONNECTED))
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
