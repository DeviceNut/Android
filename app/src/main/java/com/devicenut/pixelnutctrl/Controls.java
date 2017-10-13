package com.devicenut.pixelnutctrl;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
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
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.devicenut.pixelnutctrl.Bluetooth.BLESTAT_DISCONNECTED;
import static com.devicenut.pixelnutctrl.Main.CMD_BLUENAME;
import static com.devicenut.pixelnutctrl.Main.CMD_BRIGHT;
import static com.devicenut.pixelnutctrl.Main.CMD_DELAY;
import static com.devicenut.pixelnutctrl.Main.CMD_EXTMODE;
import static com.devicenut.pixelnutctrl.Main.CMD_PAUSE;
import static com.devicenut.pixelnutctrl.Main.CMD_POP_PATTERN;
import static com.devicenut.pixelnutctrl.Main.CMD_PROPVALS;
import static com.devicenut.pixelnutctrl.Main.CMD_RESUME;
import static com.devicenut.pixelnutctrl.Main.CMD_SEGS_ENABLE;
import static com.devicenut.pixelnutctrl.Main.CMD_TRIGGER;
import static com.devicenut.pixelnutctrl.Main.MAXVAL_HUE;
import static com.devicenut.pixelnutctrl.Main.MAXVAL_PERCENT;
import static com.devicenut.pixelnutctrl.Main.basicPatternsCount;
import static com.devicenut.pixelnutctrl.Main.ble;
import static com.devicenut.pixelnutctrl.Main.curSegment;
import static com.devicenut.pixelnutctrl.Main.customPatterns;
import static com.devicenut.pixelnutctrl.Main.devPatternHelp;
import static com.devicenut.pixelnutctrl.Main.devPatternNames;
import static com.devicenut.pixelnutctrl.Main.devPatternCmds;
import static com.devicenut.pixelnutctrl.Main.devPatternBits;
import static com.devicenut.pixelnutctrl.Main.initPatterns;
import static com.devicenut.pixelnutctrl.Main.multiStrands;
import static com.devicenut.pixelnutctrl.Main.numSegments;
import static com.devicenut.pixelnutctrl.Main.numPatterns;
import static com.devicenut.pixelnutctrl.Main.curBright;
import static com.devicenut.pixelnutctrl.Main.curDelay;
import static com.devicenut.pixelnutctrl.Main.segPatterns;
import static com.devicenut.pixelnutctrl.Main.segPosStart;
import static com.devicenut.pixelnutctrl.Main.segPosCount;
import static com.devicenut.pixelnutctrl.Main.rangeDelay;
import static com.devicenut.pixelnutctrl.Main.segTrigForce;
import static com.devicenut.pixelnutctrl.Main.segXmodeCnt;
import static com.devicenut.pixelnutctrl.Main.segXmodeEnb;
import static com.devicenut.pixelnutctrl.Main.segXmodeHue;
import static com.devicenut.pixelnutctrl.Main.segXmodeWht;
import static com.devicenut.pixelnutctrl.Main.stdPatternsCount;
import static com.devicenut.pixelnutctrl.Main.devName;
import static com.devicenut.pixelnutctrl.Main.useAdvPatterns;

@SuppressWarnings("unchecked")
public class Controls extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener, Bluetooth.BleCallbacks
{
    private final String LOGNAME = "Controls";
    private final Activity context = this;

    private TextView nameText;
    private Button segAddButton;
    private Button pauseButton, helpButton, helpButton2, manualButton;
    private TextView helpText, helpText2, helpTitle, textTrigger;
    private LinearLayout outerControls, innerControls, patternHelp;
    private LinearLayout autoControls, llPropColor, llPropWhite, llPropCount;
    private LinearLayout trigControls, llTrigForce;
    private Spinner selectPattern;
    private SeekBar seekBright, seekDelay;
    private SeekBar seekPropColor, seekPropWhite, seekPropCount;
    private SeekBar seekTrigForce;

    private int helpMode = 0;
    private boolean doUpdate = true;

    private boolean isConnected = false;
    private boolean sendEnable = false;
    private boolean isEditing = false;
    private boolean changePattern = true;

    private boolean useSegEnables = false;
    private final boolean segEnables[] = { false, false, false, false, false };

    private final int segRadioIds[] =
            {
                    R.id.radio_1,
                    R.id.radio_2,
                    R.id.radio_3,
                    R.id.radio_4,
                    R.id.radio_5,
            };

    private String[] patternNames;
    private boolean[] listEnables;
    private int[] mapPatternToIndex;
    private int[] mapIndexToPattern;

    @Override protected void onCreate(Bundle savedInstanceState)
    {
        Log.d(LOGNAME, ">>onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_controls);

        int j = 0;
        int extra = (customPatterns > 0) ? 1 : 0;
        if (stdPatternsCount > 0) extra += 2;
        if ((customPatterns == 0) && !useAdvPatterns)
        {
            extra = 0;
            numPatterns = basicPatternsCount;
        }
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
        else if (useAdvPatterns)
        {
            patternNames[j] = "Basic Patterns";
            listEnables[j] = false;
            mapIndexToPattern[j] = 0;
            ++j;
        }

        for (int i = 0; i < numPatterns; ++i)
        {
            if ((i > 0) && (i == customPatterns) && useAdvPatterns)
            {
                patternNames[j] = "Basic Patterns";
                listEnables[j] = false;
                mapIndexToPattern[j] = 0;
                ++j;
            }

            if ((i > customPatterns) && (i == basicPatternsCount) && useAdvPatterns)
            {
                patternNames[j] = "Advanced Patterns";
                listEnables[j] = false;
                mapIndexToPattern[j] = 0;
                ++j;
            }

            Log.v(LOGNAME, "Adding pattern i=" + i + " j=" + j + " => " + devPatternNames[i]);

            patternNames[j] = devPatternNames[i];
            listEnables[j] = true;
            mapIndexToPattern[j] = i;
            mapPatternToIndex[i] = j;
            ++j;
        }

        SetupSpinnerLayout();

        seekBright    = (SeekBar) findViewById(R.id.seek_Bright);
        seekDelay     = (SeekBar) findViewById(R.id.seek_Delay);
        seekPropColor = (SeekBar) findViewById(R.id.seek_PropColor);
        seekPropWhite = (SeekBar) findViewById(R.id.seek_PropWhite);
        seekPropCount = (SeekBar) findViewById(R.id.seek_PropCount);
        seekTrigForce = (SeekBar) findViewById(R.id.seek_TrigForce);

        segAddButton = (Button)findViewById(R.id.button_SegAdd);

        manualButton = (Button) findViewById(R.id.button_AutoProp);
        if (!useAdvPatterns) manualButton.setVisibility(GONE);

        seekBright.setOnSeekBarChangeListener(this);
        seekDelay.setOnSeekBarChangeListener(this);
        seekPropColor.setOnSeekBarChangeListener(this);
        seekPropWhite.setOnSeekBarChangeListener(this);
        seekPropCount.setOnSeekBarChangeListener(this);
        seekTrigForce.setOnSeekBarChangeListener(this);

        curSegment = 0; // always start with first segment
        SetupSegments();

        /*
        segmentGroup = (RadioGroup) findViewById(R.id.radioGroup);
        segmentGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener()
        {
            @Override public void onCheckedChanged(RadioGroup group, int checkedId) { SetSegment(checkedId); }
        });
        */

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

        if (isEditing && (ble != null))
        {
            isEditing = false;
            Log.d(LOGNAME, "Renaming device: " + devName);
            SendString(CMD_BLUENAME + devName);

            if (Build.VERSION.SDK_INT < 23)
            {
                //ble.refreshDeviceCache(); // doesn't work

                Toast.makeText(context, "Rescan from Settings to see name change", Toast.LENGTH_SHORT).show();
            }
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

            isConnected = true;
            sendEnable = true; // allow controls to work

            devName = devName.substring(2);
            Log.d(LOGNAME, "Device name: " + devName);

            // if only using basic patterns, then the properties will always be displayed
            // so enable the properties for any segment which is currently disabled
            if (!useAdvPatterns)
            {
                Log.d(LOGNAME, "Enabling Properties:");
                for (int i = 0; i < numSegments; ++i)
                {
                    if (!segXmodeEnb[i])
                    {
                        int seg = i + 1;
                        segXmodeEnb[i] = true;
                        SendString(CMD_SEGS_ENABLE + seg);
                        SendString(CMD_EXTMODE + "1");
                    }
                }
            }

            if (initPatterns && multiStrands) // initialize all of physical strands
            {
                Log.d(LOGNAME, "Initializing Patterns:");
                for (int i = 0; i < numSegments; ++i)
                {
                    int seg = i+1;
                    int pnum = segPatterns[i]+1;
                    Log.d(LOGNAME, "  segment=" + seg + " pattern==" + pnum);

                    SendString(CMD_SEGS_ENABLE + seg);
                    SendString("."); // start sequence
                    SendString(devPatternCmds[pnum-1]);
                    SendString("."); // end sequence
                    SendString("" + pnum); // store pattern number
                }
                changePattern = false;
            }
            else
            {
                changePattern = initPatterns;
                if (changePattern) Log.d(LOGNAME, "Initializing: pattern=" + segPatterns[curSegment]);
            }

            curSegment = 0; // always start with first segment
            if (numSegments > 1) SendString(CMD_SEGS_ENABLE + "1"); // segment numbers start at 1 on device

            SelectPattern();    // select the pattern to be displayed
            SetupControls();    // set control positions without sending commands
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

    private void SetupSpinnerLayout()
    {
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

                    int curPattern = mapIndexToPattern[position];
                    segPatterns[curSegment] = curPattern;

                    if (curPattern >= customPatterns)
                    {
                        if (numSegments == 1)
                        {
                            SendString("."); // start sequence
                            SendString(CMD_POP_PATTERN);
                            SendString(devPatternCmds[curPattern]);
                            SendString("."); // end sequence

                            int num = curPattern+1; // device pattern numbers start at 1
                            SendString("" + num);   // store current pattern number
                        }
                        else if (!multiStrands) // must send all segment patterns at once
                        {
                            SendString("."); // start sequence
                            SendString(CMD_POP_PATTERN);

                            for (int i = 0; i < numSegments; ++i)
                            {
                                if (i == curSegment) segPatterns[i] = curPattern;

                                SendString("X" + segPosStart[i] + " Y" + segPosCount[i]);
                                SendString(devPatternCmds[ segPatterns[i] ]);
                            }

                            SendString("."); // end sequence

                            int num = curPattern+1; // device pattern numbers start at 1
                            SendString("" + num);   // store current pattern number
                        }
                        // else physically separate segments, so can treat them as such
                        else if (useSegEnables)
                        {
                            for (int i = 0; i < numSegments; ++i)
                            {
                                if (segEnables[i])
                                {
                                    int seg = i+1;
                                    SendString(CMD_SEGS_ENABLE + seg);
                                    SendString("."); // start sequence
                                    SendString(CMD_POP_PATTERN);
                                    SendString(devPatternCmds[ segPatterns[i] ]);
                                    SendString("."); // end sequence

                                    int num = curPattern+1; // device pattern numbers start at 1
                                    SendString("" + num);   // store current pattern number
                                }
                            }

                            int seg = curSegment+1;
                            SendString(CMD_SEGS_ENABLE + seg);
                        }
                        else
                        {
                            SendString("."); // start sequence
                            SendString(CMD_POP_PATTERN);
                            SendString(devPatternCmds[ segPatterns[ curSegment ] ]);
                            SendString("."); // end sequence

                            int num = curPattern+1; // device pattern numbers start at 1
                            SendString("" + num);   // store current pattern number
                        }
                    }
                    else
                    {
                        int num = curPattern+1; // device pattern numbers start at 1
                        SendString("" + num);   // store current pattern number
                    }

                    SetupControls();    // set control positions without sending commands

                    if (helpMode > 0) SetHelpMode(false, curPattern);
                }
                else changePattern = true; // reset for next time
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        };

        selectPattern = (Spinner) findViewById(R.id.spinner_Pattern);
        selectPattern.setAdapter(spinnerArrayAdapter);
        selectPattern.setOnItemSelectedListener(patternListener);
    }

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
            helpButton2.setText("?");
            //helpButton2.setTextSize(22);
            patternHelp.setVisibility(GONE);
            helpMode = 0;
        }
        else
        {
            patternHelp.setVisibility(VISIBLE);
            helpButton2.setText("x");
            //helpButton2.setTextSize(18);
            helpText2.setText( devPatternHelp[newmode]);
            helpMode = newmode;
        }
    }

    private void SetupControls()
    {
        sendEnable = false; // prevent following from writing commands

        int index = multiStrands ? curSegment : 0;
        seekBright.setProgress(curBright[index]);
        seekDelay.setProgress(((rangeDelay - curDelay[index]) * MAXVAL_PERCENT) / (rangeDelay + rangeDelay));

        int bits = devPatternBits[segPatterns[curSegment]];

        if ((bits & 0x07) != 0) // enable properties
        {
            innerControls.setVisibility(VISIBLE);

            if (segXmodeEnb[curSegment])
            {
                autoControls.setVisibility(VISIBLE);
                manualButton.setText(getResources().getString(R.string.name_disable));

                llPropColor.setVisibility(((bits & 1) != 0) ? VISIBLE : GONE);
                llPropWhite.setVisibility(((bits & 2) != 0) ? VISIBLE : GONE);
                llPropCount.setVisibility(((bits & 4) != 0) ? VISIBLE : GONE);

                seekPropColor.setProgress(((segXmodeHue[ curSegment] * MAXVAL_PERCENT) / MAXVAL_HUE));
                seekPropWhite.setProgress(  segXmodeWht[ curSegment]);
                seekPropCount.setProgress(  segXmodeCnt[ curSegment]);
                seekTrigForce.setProgress(  segTrigForce[curSegment] / 10);
            }
            else
            {
                manualButton.setText(getResources().getString(R.string.name_enable));
                autoControls.setVisibility(GONE);
            }
        }
        else innerControls.setVisibility(GONE);

        if ((bits & 0x10) != 0) // enable triggering
        {
            trigControls.setVisibility(VISIBLE);

            if ((bits & 0x20) != 0) // enable force control
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

        sendEnable = true; // allow controls to work now
    }

    private void SetupSegments()
    {
        LinearLayout ll = (LinearLayout) findViewById(R.id.ll_SelectSegments);
        if (numSegments > 1)
        {
            ll.setVisibility(VISIBLE);
            if (multiStrands) segAddButton.setVisibility(VISIBLE);

            for (int i = 0; i < numSegments; ++i)
            {
                RadioButton b = (RadioButton)findViewById(segRadioIds[i]);
                b.setVisibility(VISIBLE);
                b.setEnabled(true);
                b.setFocusable(true);
                b.setClickable(true);

                if (i == curSegment)
                {
                    b.setChecked(true);
                    segEnables[i] = true;
                }
                else
                {
                    b.setChecked(false);
                    segEnables[i] = false;
                }
            }

            useSegEnables = false;
        }
        else ll.setVisibility(GONE);

        /* debug: show all segment buttons
        for (int i = 0; i < segRadioIds.length; ++i)
        {
            RadioButton b = (RadioButton) findViewById(segRadioIds[i]);
            b.setVisibility(VISIBLE);
        }
        */
    }

    private void SetSegment(int index)
    {
        if (index == curSegment)
        {
            Log.w(LOGNAME, "Segment not changed");
            return;
        }

        boolean enable = ((RadioButton)findViewById(segRadioIds[index])).isChecked();

        if (useSegEnables) // allow multiple segment selections
        {
            if (enable && !segEnables[index]) // adding new segment to the chain
            {
                Log.d(LOGNAME, "Copying all values to segment=" + index);
                segEnables[index] = true;

                curDelay[    index] = curDelay[    curSegment];
                curBright[   index] = curBright[   curSegment];
                segXmodeEnb[ index] = segXmodeEnb[ curSegment];
                segXmodeHue[ index] = segXmodeHue[ curSegment];
                segXmodeWht[ index] = segXmodeWht[ curSegment];
                segXmodeCnt[ index] = segXmodeCnt[ curSegment];
                segTrigForce[index] = segTrigForce[curSegment];
                segPatterns[ index] = segPatterns[ curSegment];

                int seg = index+1;
                int pnum = segPatterns[index]+1;
                Log.d(LOGNAME, "  segment=" + seg + " pattern=" + pnum);

                // change the pattern:
                SendString(CMD_SEGS_ENABLE + seg);
                SendString("."); // start sequence
                SendString(devPatternCmds[ segPatterns[index] ]);
                SendString("."); // end sequence
                SendString("" + pnum); // store pattern number

                // change brightness/delay:
                SendString(CMD_BRIGHT + curBright[index]);
                SendString(CMD_DELAY + curDelay[index]);

                // change properties:
                if (segXmodeEnb[index])
                     SendString(CMD_EXTMODE + "1");
                else SendString(CMD_EXTMODE + "0");
                SendString(CMD_PROPVALS + segXmodeHue[index] + " " + segXmodeWht[index] + " " + segXmodeCnt[index]);

                // switch back to current segment
                seg = curSegment+1;
                SendString(CMD_SEGS_ENABLE + seg);
            }
            else segEnables[index] = enable;
        }
        else
        {
            curSegment = index;
            segEnables[curSegment] = enable;
            Log.d(LOGNAME, "Switching to segment=" + curSegment + " pattern=" + segPatterns[curSegment]);
            ClearSegEnables();

            int num = curSegment+1; // device segment numbers start at 1
            SendString(CMD_SEGS_ENABLE + num); // restricts subsequent controls to this segment

            changePattern = false;  // don't need to resend the pattern
            SelectPattern();        // select the pattern to be displayed
            SetupControls();        // set controls display without sending commands
        }
    }

    private void ClearSegEnables()
    {
        for (int i = 0; i < numSegments; ++i)
        {
            if ((i != curSegment) && segEnables[i])
            {
                segEnables[i] = false;
                ((RadioButton)findViewById(segRadioIds[i])).setChecked(false);
            }
        }
    }

    public void SelectPattern()
    {
        selectPattern.setSelection(mapPatternToIndex[segPatterns[curSegment]], false);
        // changePattern doesn't get reset if the pattern didn't change (same for both segments)
        // and since the selection is asynchronous, we cannot just set it after this call, and
        // if we don't reset it you cannot ever change the pattern again, so post a call to do it
        selectPattern.post(new Runnable() { @Override public void run()
        {
            //Log.v(LOGNAME, "Resetting 'changePattern' here, value=" + changePattern);
            changePattern = true;
        }});
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
                if (doUpdate)
                {
                    SendString(CMD_PAUSE);
                    pauseButton.setText(getResources().getString(R.string.name_resume));
                }
                else
                {
                    SendString(CMD_RESUME);
                    pauseButton.setText(getResources().getString(R.string.name_pause));
                }
                doUpdate = !doUpdate;
                break;
            }
            case R.id.button_PatternHelp:
            {
                SetHelpMode(true, segPatterns[curSegment]);
                break;
            }
            case R.id.button_ControlsHelp:
            {
                SetHelpMode(true, -1);
                break;
            }
            case R.id.button_AutoProp:
            {
                boolean enable = !segXmodeEnb[curSegment];
                Log.d(LOGNAME, "AutoProps: enable=" + enable);

                if (useSegEnables)
                {
                    for (int i = 0; i < numSegments; ++i)
                    {
                        if (segEnables[i])
                        {
                            int seg = i+1;
                            SendString(CMD_SEGS_ENABLE + seg);

                            segXmodeEnb[i] = enable;
                            if (enable)
                                 SendString(CMD_EXTMODE + "1");
                            else SendString(CMD_EXTMODE + "0");
                        }
                    }

                    int seg = curSegment+1;
                    SendString(CMD_SEGS_ENABLE + seg);
                }
                else
                {
                    segXmodeEnb[curSegment] = enable;
                    if (enable)
                         SendString(CMD_EXTMODE + "1");
                    else SendString(CMD_EXTMODE + "0");
                }

                SetupControls();    // set control positions without sending commands
                break;
            }
            case R.id.button_TrigAction:
            {
                if (useSegEnables)
                {
                    for (int i = 0; i < numSegments; ++i)
                    {
                        if (segEnables[i])
                        {
                            int seg = i+1;
                            SendString(CMD_SEGS_ENABLE + seg);

                            if ((devPatternBits[segPatterns[curSegment]] & 0x20) != 0)
                                SendString(CMD_TRIGGER + segTrigForce[i]);
                            else SendString(CMD_TRIGGER + 0);
                        }
                    }

                    int seg = curSegment+1;
                    SendString(CMD_SEGS_ENABLE + seg);
                }
                else if ((devPatternBits[segPatterns[curSegment]] & 0x20) != 0)
                     SendString(CMD_TRIGGER + segTrigForce[curSegment]);
                else SendString(CMD_TRIGGER + 0);
                break;
            }
            case R.id.button_SegAdd:
            {
                useSegEnables = !useSegEnables;
                segAddButton.setText(useSegEnables ? "x" : "+");
                if (!useSegEnables) ClearSegEnables();
                break;
            }
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
        }
    }

    @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
    {
        switch (seekBar.getId())
        {
            case R.id.seek_Bright:
            {
                if (useSegEnables)
                {
                    for (int i = 0; i < numSegments; ++i)
                    {
                        if (segEnables[i])
                        {
                            int seg = i+1;
                            SendString(CMD_SEGS_ENABLE + seg);

                            curBright[i] = progress;
                            SendString(CMD_BRIGHT + curBright[i]);
                        }
                    }

                    int seg = curSegment+1;
                    SendString(CMD_SEGS_ENABLE + seg);
                }
                else
                {
                    int index = multiStrands ? curSegment : 0;
                    curBright[index] = progress;
                    SendString(CMD_BRIGHT + curBright[index]);
                }
                break;
            }
            case R.id.seek_Delay:
            {
                if (useSegEnables)
                {
                    for (int i = 0; i < numSegments; ++i)
                    {
                        if (segEnables[i])
                        {
                            int seg = i+1;
                            SendString(CMD_SEGS_ENABLE + seg);

                            curDelay[i] = rangeDelay - (progress * 2 * rangeDelay)/100;
                            SendString(CMD_DELAY + curDelay[i]);
                        }
                    }

                    int seg = curSegment+1;
                    SendString(CMD_SEGS_ENABLE + seg);
                }
                else
                {
                    int index = multiStrands ? curSegment : 0;
                    curDelay[index] = rangeDelay - (progress * 2 * rangeDelay)/100;
                    SendString(CMD_DELAY + curDelay[index]);
                }
                break;
            }
            case R.id.seek_PropColor:
            {
                if (useSegEnables)
                {
                    for (int i = 0; i < numSegments; ++i)
                    {
                        if (segEnables[i])
                        {
                            int seg = i+1;
                            SendString(CMD_SEGS_ENABLE + seg);

                            segXmodeHue[i] = (progress * MAXVAL_HUE) / MAXVAL_PERCENT;
                            SendString(CMD_PROPVALS + segXmodeHue[i] + " " + segXmodeWht[i] + " " + segXmodeCnt[i]);
                        }
                    }

                    int seg = curSegment+1;
                    SendString(CMD_SEGS_ENABLE + seg);
                }
                else
                {
                    segXmodeHue[curSegment] = (progress * MAXVAL_HUE) / MAXVAL_PERCENT;
                    SendString(CMD_PROPVALS + segXmodeHue[curSegment] + " " + segXmodeWht[curSegment] + " " + segXmodeCnt[curSegment]);
                }
                break;
            }
            case R.id.seek_PropWhite:
            {
                if (useSegEnables)
                {
                    for (int i = 0; i < numSegments; ++i)
                    {
                        if (segEnables[i])
                        {
                            int seg = i+1;
                            SendString(CMD_SEGS_ENABLE + seg);

                            segXmodeWht[i] = progress;
                            SendString(CMD_PROPVALS + segXmodeHue[i] + " " + segXmodeWht[i] + " " + segXmodeCnt[i]);
                        }
                    }

                    int seg = curSegment+1;
                    SendString(CMD_SEGS_ENABLE + seg);
                }
                else
                {
                    segXmodeWht[curSegment] = progress;
                    SendString(CMD_PROPVALS + segXmodeHue[curSegment] + " " + segXmodeWht[curSegment] + " " + segXmodeCnt[curSegment]);
                }
                break;
            }
            case R.id.seek_PropCount:
            {
                if (useSegEnables)
                {
                    for (int i = 0; i < numSegments; ++i)
                    {
                        if (segEnables[i])
                        {
                            int seg = i+1;
                            SendString(CMD_SEGS_ENABLE + seg);

                            segXmodeCnt[curSegment] = progress;
                            SendString(CMD_PROPVALS + segXmodeHue[i] + " " + segXmodeWht[i] + " " + segXmodeCnt[i]);
                        }
                    }

                    int seg = curSegment+1;
                    SendString(CMD_SEGS_ENABLE + seg);
                }
                else
                {
                    segXmodeCnt[curSegment] = progress;
                    SendString(CMD_PROPVALS + segXmodeHue[curSegment] + " " + segXmodeWht[curSegment] + " " + segXmodeCnt[curSegment]);
                }
                break;
            }
            case R.id.seek_TrigForce:
            {
                int val = (10 * progress);
                if (useSegEnables)
                {
                    for (int i = 0; i < numSegments; ++i)
                    {
                        if (segEnables[i]) segTrigForce[i] = val;
                    }
                }
                else segTrigForce[curSegment] = val;
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

/* use in onCreate to inflate layout after adjusting parameters

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
