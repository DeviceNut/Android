package com.devicenut.pixelnutctrl;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
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

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.devicenut.pixelnutctrl.Main.CMD_BRIGHT;
import static com.devicenut.pixelnutctrl.Main.CMD_DELAY;
import static com.devicenut.pixelnutctrl.Main.CMD_EXTMODE;
import static com.devicenut.pixelnutctrl.Main.CMD_POP_PATTERN;
import static com.devicenut.pixelnutctrl.Main.CMD_PROPVALS;
import static com.devicenut.pixelnutctrl.Main.CMD_SEGS_ENABLE;
import static com.devicenut.pixelnutctrl.Main.CMD_START_END;
import static com.devicenut.pixelnutctrl.Main.CMD_TRIGGER;
import static com.devicenut.pixelnutctrl.Main.MAXVAL_HUE;
import static com.devicenut.pixelnutctrl.Main.MAXVAL_PERCENT;
import static com.devicenut.pixelnutctrl.Main.MAXVAL_WHT;
import static com.devicenut.pixelnutctrl.Main.basicPatternsCount;
import static com.devicenut.pixelnutctrl.Main.curBright;
import static com.devicenut.pixelnutctrl.Main.curDelay;
import static com.devicenut.pixelnutctrl.Main.curSegment;
import static com.devicenut.pixelnutctrl.Main.customPatterns;
import static com.devicenut.pixelnutctrl.Main.devPatternBits;
import static com.devicenut.pixelnutctrl.Main.devPatternCmds;
import static com.devicenut.pixelnutctrl.Main.devPatternHelp;
import static com.devicenut.pixelnutctrl.Main.devPatternNames;
import static com.devicenut.pixelnutctrl.Main.multiStrands;
import static com.devicenut.pixelnutctrl.Main.numPatterns;
import static com.devicenut.pixelnutctrl.Main.numSegments;
import static com.devicenut.pixelnutctrl.Main.rangeDelay;
import static com.devicenut.pixelnutctrl.Main.segPatterns;
import static com.devicenut.pixelnutctrl.Main.segPosCount;
import static com.devicenut.pixelnutctrl.Main.segPosStart;
import static com.devicenut.pixelnutctrl.Main.segTrigForce;
import static com.devicenut.pixelnutctrl.Main.segXmodeCnt;
import static com.devicenut.pixelnutctrl.Main.segXmodeEnb;
import static com.devicenut.pixelnutctrl.Main.segXmodeHue;
import static com.devicenut.pixelnutctrl.Main.segXmodeWht;
import static com.devicenut.pixelnutctrl.Main.stdPatternsCount;
import static com.devicenut.pixelnutctrl.Main.useAdvPatterns;

public class FragCtrls extends Fragment implements SeekBar.OnSeekBarChangeListener
{
    private final String LOGNAME = "Controls";
    private final Activity context = this.getActivity();

    private String[] patternNames;
    private boolean[] listEnables;
    private int[] mapPatternToIndex;
    private int[] mapIndexToPattern;

    private boolean sendEnable = false;
    private boolean changePattern = true;

    private Button segAddButton, triggerButton;
    private Button helpButton, manualButton;
    private TextView helpText, textTrigger;
    private LinearLayout innerControls, patternHelp;
    private LinearLayout autoControls, llPropColor, llPropWhite, llPropCount;
    private LinearLayout trigControls, llTrigForce;
    private Spinner selectPattern;

    private SeekBar seekBright, seekDelay;
    private SeekBar seekPropColor, seekPropWhite, seekPropCount;
    private SeekBar seekTrigForce;

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

    private final RadioButton segRadioButtons[] = {};

    private OnFragmentInteractionListener mListener;

    public interface OnFragmentInteractionListener
    {
        void onFragmentInteraction(String s);
    }

    public FragCtrls() {}

    public static FragCtrls newInstance()
    {
        FragCtrls fragment = new FragCtrls();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override public void onCreate(Bundle savedInstanceState)
    {
        Log.d(LOGNAME, ">>onCreate");
        super.onCreate(savedInstanceState);
    }

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        Log.d(LOGNAME, ">>onCreateView");
        View v = inflater.inflate(R.layout.fragment_ctrls, container, false);

        innerControls   = (LinearLayout) v.findViewById(R.id.ll_InnerControls);
        patternHelp     = (LinearLayout) v.findViewById(R.id.ll_PatternHelp);
        autoControls    = (LinearLayout) v.findViewById(R.id.auto_Controls);
        llPropColor     = (LinearLayout) v.findViewById(R.id.ll_PropColor);
        llPropWhite     = (LinearLayout) v.findViewById(R.id.ll_PropWhite);
        llPropCount     = (LinearLayout) v.findViewById(R.id.ll_PropCount);
        trigControls    = (LinearLayout) v.findViewById(R.id.trig_Controls);
        llTrigForce     = (LinearLayout) v.findViewById(R.id.ll_TrigForce);
        textTrigger     = (TextView)     v.findViewById(R.id.text_Trigger);
        helpText        = (TextView)     v.findViewById(R.id.text_PatternHelp);

        seekBright    = (SeekBar) v.findViewById(R.id.seek_Bright);
        seekDelay     = (SeekBar) v.findViewById(R.id.seek_Delay);
        seekPropColor = (SeekBar) v.findViewById(R.id.seek_PropColor);
        seekPropWhite = (SeekBar) v.findViewById(R.id.seek_PropWhite);
        seekPropCount = (SeekBar) v.findViewById(R.id.seek_PropCount);
        seekTrigForce = (SeekBar) v.findViewById(R.id.seek_TrigForce);

        seekBright.setOnSeekBarChangeListener(this);
        seekDelay.setOnSeekBarChangeListener(this);
        seekPropColor.setOnSeekBarChangeListener(this);
        seekPropWhite.setOnSeekBarChangeListener(this);
        seekPropCount.setOnSeekBarChangeListener(this);
        seekTrigForce.setOnSeekBarChangeListener(this);

        manualButton = (Button)v.findViewById(R.id.button_AutoProp);
        if (useAdvPatterns) manualButton.setOnClickListener(mClicker);
        else manualButton.setVisibility(GONE);

        segAddButton  = (Button)v.findViewById(R.id.button_SegAdd);
        helpButton    = (Button)v.findViewById(R.id.button_PatternHelp);
        triggerButton = (Button)v.findViewById(R.id.button_TrigAction);

        segAddButton.setOnClickListener(mClicker);
        helpButton.setOnClickListener(mClicker);
        triggerButton.setOnClickListener(mClicker);

        for (int i = 0; i < segRadioIds.length; ++i)
            segRadioButtons[i] = (RadioButton)v.findViewById(segRadioIds[i]);

        curSegment = 0; // always start with first segment
        SetupSegments();

        return v;
    }

    @Override public void onAttach(Context context)
    {
        Log.d(LOGNAME, ">>onAttach");
        super.onAttach(context);
        mListener = (OnFragmentInteractionListener)context;
    }

    @Override public void onDetach()
    {
        Log.d(LOGNAME, ">>onDetach");
        super.onDetach();
        mListener = null;
    }

    private void CreatePatternArrays()
    {
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
                            SendString(CMD_START_END);; // start sequence
                            SendString(CMD_POP_PATTERN);
                            SendString(devPatternCmds[curPattern]);
                            SendString(CMD_START_END);; // end sequence

                            int num = curPattern+1; // device pattern numbers start at 1
                            SendString("" + num);   // store current pattern number
                        }
                        else if (!multiStrands) // must send all segment patterns at once
                        {
                            SendString(CMD_START_END);; // start sequence
                            SendString(CMD_POP_PATTERN);

                            for (int i = 0; i < numSegments; ++i)
                            {
                                if (i == curSegment) segPatterns[i] = curPattern;

                                SendString("X" + segPosStart[i] + " Y" + segPosCount[i]);
                                SendString(devPatternCmds[ segPatterns[i] ]);
                            }

                            SendString(CMD_START_END);; // end sequence

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
                                    SendString(CMD_START_END);; // start sequence
                                    SendString(CMD_POP_PATTERN);
                                    segPatterns[i] = curPattern;
                                    SendString(devPatternCmds[ segPatterns[i] ]);
                                    SendString(CMD_START_END);; // end sequence

                                    int num = curPattern+1; // device pattern numbers start at 1
                                    SendString("" + num);   // store current pattern number
                                }
                            }

                            int seg = curSegment+1;
                            SendString(CMD_SEGS_ENABLE + seg);
                        }
                        else
                        {
                            SendString(CMD_START_END);; // start sequence
                            SendString(CMD_POP_PATTERN);
                            SendString(devPatternCmds[ segPatterns[ curSegment ] ]);
                            SendString(CMD_START_END);; // end sequence

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

                    // change text for new pattern if pattern help is active, but keep it active
                    if (helpMode > 0) SetPatternHelp(false, curPattern);
                }
                else changePattern = true; // reset for next time
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        };

        selectPattern = (Spinner) findViewById(R.id.spinner_Pattern);
        selectPattern.setAdapter(spinnerArrayAdapter);
        selectPattern.setOnItemSelectedListener(patternListener);
    }

    private void SetPatternHelp(boolean toggle, int pattern)
    {
        //Log.d(LOGNAME, "PatternHelp: toggle=" + toggle + " pattern=" + pattern);
        if ((helpMode > 0) && toggle)
        {
            helpButton2.setText("?");
            patternHelp.setVisibility(GONE);
            helpMode = 0;
        }
        else
        {
            patternHelp.setVisibility(VISIBLE);
            helpButton.setText("x");
            helpText.setText( devPatternHelp[pattern]);
            helpMode = pattern+1;
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
                seekPropWhite.setProgress(((segXmodeWht[ curSegment] * MAXVAL_PERCENT) / MAXVAL_WHT));
                seekPropCount.setProgress(  segXmodeCnt[ curSegment]);
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
                seekTrigForce.setProgress(segTrigForce[curSegment] / 10);
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
                RadioButton b = segRadioButtons[i];
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

        boolean enable = segRadioButtons[index].isChecked();

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
                SendString(CMD_START_END);; // start sequence
                SendString(CMD_POP_PATTERN);
                SendString(devPatternCmds[ segPatterns[index] ]);
                SendString(CMD_START_END);; // end sequence
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
                segRadioButtons[i].setChecked(false);
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

    private void SendString(String str)
    {
        if (mListener != null)
            mListener.onFragmentInteraction(str);
    }

    View.OnClickListener mClicker = new View.OnClickListener()
    {
        @Override public void onClick(View v)
        {
            switch (v.getId())
            {
                case R.id.button_PatternHelp:
                {
                    SetPatternHelp(true, segPatterns[curSegment]);
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

                                if ((devPatternBits[segPatterns[i]] & 0x20) != 0)
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
                    segAddButton.setText(useSegEnables ? "X" : "&");
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
    };

    @Override public void onStartTrackingTouch(SeekBar seekBar)
    {
        /*
        Log.v(LOGNAME, "Disable swiping...");
        ViewParent parent = (ViewParent)masterPager;
        parent.requestDisallowInterceptTouchEvent(true);
        */
    }
    @Override public void onStopTrackingTouch(SeekBar seekBar)
    {
        /*
        Log.v(LOGNAME, "Enable swiping...");
        ViewParent parent = (ViewParent)masterPager;
        parent.requestDisallowInterceptTouchEvent(false);
        */
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

                            segXmodeWht[i] = (progress * MAXVAL_WHT) / MAXVAL_PERCENT;
                            SendString(CMD_PROPVALS + segXmodeHue[i] + " " + segXmodeWht[i] + " " + segXmodeCnt[i]);
                        }
                    }

                    int seg = curSegment+1;
                    SendString(CMD_SEGS_ENABLE + seg);
                }
                else
                {
                    segXmodeWht[curSegment] = (progress * MAXVAL_WHT) / MAXVAL_PERCENT;
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

                            segXmodeCnt[i] = progress;
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
}
