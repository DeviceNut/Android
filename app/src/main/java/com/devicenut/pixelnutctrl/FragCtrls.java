package com.devicenut.pixelnutctrl;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
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
import android.widget.ScrollView;
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

import static com.devicenut.pixelnutctrl.Main.appContext;
import static com.devicenut.pixelnutctrl.Main.curBright;
import static com.devicenut.pixelnutctrl.Main.curDelay;
import static com.devicenut.pixelnutctrl.Main.curSegment;
import static com.devicenut.pixelnutctrl.Main.customPatterns;
import static com.devicenut.pixelnutctrl.Main.haveBasicSegs;
import static com.devicenut.pixelnutctrl.Main.initPatterns;
import static com.devicenut.pixelnutctrl.Main.multiStrands;
import static com.devicenut.pixelnutctrl.Main.numSegments;
import static com.devicenut.pixelnutctrl.Main.rangeDelay;
import static com.devicenut.pixelnutctrl.Main.segBasicOnly;
import static com.devicenut.pixelnutctrl.Main.segPatterns;
import static com.devicenut.pixelnutctrl.Main.segPosCount;
import static com.devicenut.pixelnutctrl.Main.segPosStart;
import static com.devicenut.pixelnutctrl.Main.segTrigForce;
import static com.devicenut.pixelnutctrl.Main.segXmodeCnt;
import static com.devicenut.pixelnutctrl.Main.segXmodeEnb;
import static com.devicenut.pixelnutctrl.Main.segXmodeHue;
import static com.devicenut.pixelnutctrl.Main.segXmodeWht;

import static com.devicenut.pixelnutctrl.Main.listNames_All;
import static com.devicenut.pixelnutctrl.Main.listNames_Basic;
import static com.devicenut.pixelnutctrl.Main.listEnables_All;
import static com.devicenut.pixelnutctrl.Main.listEnables_Basic;
import static com.devicenut.pixelnutctrl.Main.devPatternNames_All;
import static com.devicenut.pixelnutctrl.Main.devPatternNames_Basic;
import static com.devicenut.pixelnutctrl.Main.devPatternHelp_All;
import static com.devicenut.pixelnutctrl.Main.devPatternHelp_Basic;
import static com.devicenut.pixelnutctrl.Main.devPatternCmds_All;
import static com.devicenut.pixelnutctrl.Main.devPatternCmds_Basic;
import static com.devicenut.pixelnutctrl.Main.devPatternBits_All;
import static com.devicenut.pixelnutctrl.Main.devPatternBits_Basic;
import static com.devicenut.pixelnutctrl.Main.mapIndexToPattern_All;
import static com.devicenut.pixelnutctrl.Main.mapIndexToPattern_Basic;
import static com.devicenut.pixelnutctrl.Main.mapPatternToIndex_All;
import static com.devicenut.pixelnutctrl.Main.mapPatternToIndex_Basic;
import static com.devicenut.pixelnutctrl.Main.mapIndexToPattern;
import static com.devicenut.pixelnutctrl.Main.mapPatternToIndex;
import static com.devicenut.pixelnutctrl.Main.devPatternNames;
import static com.devicenut.pixelnutctrl.Main.devPatternHelp;
import static com.devicenut.pixelnutctrl.Main.devPatternCmds;
import static com.devicenut.pixelnutctrl.Main.devPatternBits;

public class FragCtrls extends Fragment implements SeekBar.OnSeekBarChangeListener
{
    private static final String LOGNAME = "Controls";

    private static ScrollView view_Ctrls;
    private static ScrollView helpPage;
    private static TextView helpText;

    private static int helpMode = 0;
    private static boolean changePattern = true;

    private static LinearLayout llProperties, llPatternHelp;
    private static LinearLayout llAutoControls, llPropColor, llPropWhite, llPropCount;
    private static LinearLayout llTrigControls, llTrigForce;

    private static Button segAddButton, helpButton, manualButton;
    private static TextView helpText2, textTrigger;
    private static Spinner selectPattern;

    private static SeekBar seekBright, seekDelay;
    private static SeekBar seekPropColor, seekPropWhite, seekPropCount;
    private static SeekBar seekTrigForce;

    private static boolean useSegEnables = false;
    private static final boolean segEnables[] = { false, false, false, false, false };

    private static final int segRadioIds[] =
            {
                    R.id.radio_1,
                    R.id.radio_2,
                    R.id.radio_3,
                    R.id.radio_4,
                    R.id.radio_5,
            };

    private static RadioButton segRadioButtons[];

    interface DeviceCommandInterface
    {
        void onDeviceCommand(String s);
    }
    private DeviceCommandInterface listenSendCommand;

    interface PatternSelectionInterface
    {
        void onPatternSelect(int pnum);
    }
    private PatternSelectionInterface listenPatternSelect;

    public FragCtrls() {}
    public static FragCtrls newInstance() { return new FragCtrls(); }

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        Log.d(LOGNAME, ">>onCreateView");

        View v = inflater.inflate(R.layout.fragment_ctrls, container, false);

        view_Ctrls     = (ScrollView) v.findViewById(R.id.scroll_Controls);
        helpPage       = (ScrollView) v.findViewById(R.id.ll_HelpPage_Ctrls);
        helpText       = (TextView)   v.findViewById(R.id.view_HelpText_Ctrls);

        llPatternHelp  = (LinearLayout) v.findViewById(R.id.ll_PatternHelp);
        llProperties   = (LinearLayout) v.findViewById(R.id.ll_Properties);
        llAutoControls = (LinearLayout) v.findViewById(R.id.ll_AutoControls);
        llPropColor    = (LinearLayout) v.findViewById(R.id.ll_PropColor);
        llPropWhite    = (LinearLayout) v.findViewById(R.id.ll_PropWhite);
        llPropCount    = (LinearLayout) v.findViewById(R.id.ll_PropCount);
        llTrigControls = (LinearLayout) v.findViewById(R.id.ll_TrigControls);
        llTrigForce    = (LinearLayout) v.findViewById(R.id.ll_TrigForce);

        selectPattern = (Spinner)  v.findViewById(R.id.spinner_Pattern);
        textTrigger   = (TextView) v.findViewById(R.id.text_Trigger);
        helpText2     = (TextView) v.findViewById(R.id.text_PatternHelp);

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
        manualButton.setOnClickListener(mClicker);

        segAddButton = (Button)v.findViewById(R.id.button_SegAdd);
        segAddButton.setOnClickListener(mClicker);

        helpButton = (Button)v.findViewById(R.id.button_PatternHelp);
        helpButton.setOnClickListener(mClicker);

        Button triggerButton = (Button)v.findViewById(R.id.button_TrigAction);
        triggerButton.setOnClickListener(mClicker);

        segRadioButtons = new RadioButton[ segRadioIds.length ];
        for (int i = 0; i < segRadioIds.length; ++i)
            segRadioButtons[i] = (RadioButton)v.findViewById(segRadioIds[i]);

        LinearLayout llSelectSegs = (LinearLayout) v.findViewById(R.id.ll_SelectSegments);
        if (numSegments > 1)
        {
            llSelectSegs.setVisibility(VISIBLE);
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
        else llSelectSegs.setVisibility(GONE);

        // cannot create these until have context
        if (haveBasicSegs) CreateSpinnerAdapterBasic();
        CreateSpinnerAdapterAll();

        if (segBasicOnly[curSegment])
             selectPattern.setAdapter(spinnerArrayAdapter_Basic);
        else selectPattern.setAdapter(spinnerArrayAdapter_All);
        selectPattern.setOnItemSelectedListener(patternListener);

        SetPatternNameOnly();   // select the pattern name to be displayed
        SetControlPositions();  // set controls display without sending commands

        return v;
    }

    @Override public void onDestroyView()
    {
        Log.d(LOGNAME, ">>onDestroyView");
        super.onDestroyView();

        view_Ctrls = null;
        helpPage = null;
        helpText = null;
    }

    @Override public void onAttach(Context context)
    {
        Log.d(LOGNAME, ">>onAttach");
        super.onAttach(context);
        listenSendCommand = (DeviceCommandInterface)getActivity();
        listenPatternSelect = (PatternSelectionInterface)getActivity();
    }

    @Override public void onDetach()
    {
        Log.d(LOGNAME, ">>onDetach");
        super.onDetach();
        listenSendCommand = null;
        listenPatternSelect = null;
    }

    public void setHelpMode(boolean enable)
    {
        if (enable)
        {
            view_Ctrls.setVisibility(GONE);
            helpPage.setVisibility(VISIBLE);

            String str = appContext.getResources().getString(R.string.text_help_head);

            if (numSegments > 1)
            {
                if (multiStrands)
                     str += appContext.getResources().getString(R.string.text_help_segs_physical);
                else str += appContext.getResources().getString(R.string.text_help_segs_logical);
            }

            str += appContext.getResources().getString(R.string.text_help_tail);
            helpText.setText(str);
        }
        else
        {
            helpPage.setVisibility(GONE);
            view_Ctrls.setVisibility(VISIBLE);
        }
    }

    private ArrayAdapter<String> spinnerArrayAdapter_Basic;
    private void CreateSpinnerAdapterBasic()
    {
        spinnerArrayAdapter_Basic = new ArrayAdapter<String>(appContext, R.layout.layout_spinner, listNames_Basic)
        {
            @Override public boolean areAllItemsEnabled() { return false; }

            @Override public boolean isEnabled(int position) { return listEnables_Basic[position]; }

            @Override public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent)
            {
                View v = convertView;
                if (v == null)
                {
                    Context mContext = this.getContext();
                    LayoutInflater vi = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    v = vi.inflate(R.layout.layout_spinner, null);
                }

                TextView tv = (TextView) v.findViewById(R.id.spinnerText);
                tv.setText(listNames_Basic[position]);

                if (!listEnables_Basic[position]) tv.setTextColor(Color.GRAY);
                else tv.setTextColor(ContextCompat.getColor(appContext, R.color.UserChoice));

                return v;
            }
        };
    }

    private ArrayAdapter<String> spinnerArrayAdapter_All;
    private void CreateSpinnerAdapterAll()
    {
        spinnerArrayAdapter_All = new ArrayAdapter<String>(appContext, R.layout.layout_spinner, listNames_All)
        {
            @Override public boolean areAllItemsEnabled() { return false; }

            @Override public boolean isEnabled(int position) { return listEnables_All[position]; }

            @Override public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent)
            {
                View v = convertView;
                if (v == null)
                {
                    Context mContext = this.getContext();
                    LayoutInflater vi = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    v = vi.inflate(R.layout.layout_spinner, null);
                }

                TextView tv = (TextView) v.findViewById(R.id.spinnerText);
                tv.setText(listNames_All[position]);

                if (!listEnables_All[position]) tv.setTextColor(Color.GRAY);
                else tv.setTextColor(ContextCompat.getColor(appContext, R.color.UserChoice));

                return v;
            }
        };
    }

    private AdapterView.OnItemSelectedListener patternListener = new AdapterView.OnItemSelectedListener()
    {
        @Override public void onNothingSelected(AdapterView<?> parent) {}

        @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
        {
            TextView v = (TextView)view;
            v.setTextColor(ContextCompat.getColor(appContext, R.color.UserChoice));
            v.setTextSize(18);

            if (initPatterns || changePattern)
            {
                Log.d(LOGNAME, "Pattern choice: " + parent.getItemAtPosition(position));

                int curPattern = mapIndexToPattern[position];
                segPatterns[curSegment] = curPattern;

                if (curPattern < customPatterns)
                {
                    int num = curPattern+1; // device pattern numbers start at 1
                    SendString("" + num);   // store current pattern number
                }
                else if (numSegments == 1)
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
                else if (initPatterns || useSegEnables)
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

                SetControlPositions(); // set control positions without sending commands

                // change text for new pattern if pattern help is active, but keep it active
                if (helpMode > 0) SetPatternHelp(false, curPattern);

                initPatterns = false; // end of one-time initialization

                if (listenPatternSelect != null)
                    listenPatternSelect.onPatternSelect(curPattern);
            }
            else changePattern = true; // reset for next time
        }
    };

    public void ChangePattern(int num)
    {
        segPatterns[curSegment] = num;
        Log.v(LOGNAME, "SelectPattern=" + selectPattern);
        selectPattern.setSelection(mapPatternToIndex[segPatterns[curSegment]], false);
    }
    private void SetPatternNameOnly()
    {
        changePattern = false; // don't need to resend the pattern
        selectPattern.setSelection(mapPatternToIndex[segPatterns[curSegment]], false);
        // changePattern doesn't get reset if the pattern didn't change (same for segments)
        // and since the selection is asynchronous, we cannot just set it after this call, and
        // if we don't reset it you cannot ever change the pattern again, so post a call to do it,
        // which of course assumes that the post will execute after the selection
        selectPattern.post(new Runnable() { @Override public void run()
        {
            //Log.v(LOGNAME, "Resetting 'changePattern' here, value=" + changePattern);
            changePattern = true;
        }});
    }

    private void SetPatternHelp(boolean toggle, int pattern)
    {
        //Log.d(LOGNAME, "PatternHelp: toggle=" + toggle + " pattern=" + pattern);
        if ((helpMode > 0) && toggle)
        {
            helpButton.setText("?");
            llPatternHelp.setVisibility(GONE);
            helpMode = 0;
        }
        else
        {
            llPatternHelp.setVisibility(VISIBLE);
            helpButton.setText("x");
            helpText2.setText( devPatternHelp[pattern]);
            helpMode = pattern+1;
        }
    }

    private void SetupPatternArraysForSegment(int seg)
    {
        if (segBasicOnly[seg])
        {
            mapIndexToPattern   = mapIndexToPattern_Basic;
            mapPatternToIndex   = mapPatternToIndex_Basic;
            devPatternNames     = devPatternNames_Basic;
            devPatternHelp      = devPatternHelp_Basic;
            devPatternCmds      = devPatternCmds_Basic;
            devPatternBits      = devPatternBits_Basic;

            selectPattern.setAdapter(spinnerArrayAdapter_Basic);
        }
        else
        {
            mapIndexToPattern   = mapIndexToPattern_All;
            mapPatternToIndex   = mapPatternToIndex_All;
            devPatternNames     = devPatternNames_All;
            devPatternHelp      = devPatternHelp_All;
            devPatternCmds      = devPatternCmds_All;
            devPatternBits      = devPatternBits_All;

            selectPattern.setAdapter(spinnerArrayAdapter_All);
        }
    }

    private void SetControlPositions()
    {
        int index = multiStrands ? curSegment : 0;
        seekBright.setProgress(curBright[index]);
        seekDelay.setProgress(((rangeDelay - curDelay[index]) * MAXVAL_PERCENT) / (rangeDelay + rangeDelay));

        if (segBasicOnly[curSegment])
        {
            // small segments with only basic patterns
            // always have the property controls displayed

            manualButton.setVisibility(GONE);

            if (!segXmodeEnb[curSegment])
            {
                Log.d(LOGNAME, "Enabling Properties:");
                int seg = curSegment + 1;
                segXmodeEnb[curSegment] = true;
                SendString(CMD_SEGS_ENABLE + seg);
                SendString(CMD_EXTMODE + "1");
            }
        }
        else
        {
            manualButton.setVisibility(VISIBLE);

            int bits = devPatternBits[segPatterns[curSegment]];

            if ((bits & 0x07) != 0) // enable properties
            {
                llProperties.setVisibility(VISIBLE);

                if (segXmodeEnb[curSegment])
                {
                    llAutoControls.setVisibility(VISIBLE);
                    manualButton.setText(appContext.getResources().getString(R.string.name_disable));

                    llPropColor.setVisibility(((bits & 1) != 0) ? VISIBLE : GONE);
                    llPropWhite.setVisibility(((bits & 2) != 0) ? VISIBLE : GONE);
                    llPropCount.setVisibility(((bits & 4) != 0) ? VISIBLE : GONE);

                    seekPropColor.setProgress(((segXmodeHue[ curSegment] * MAXVAL_PERCENT) / MAXVAL_HUE));
                    seekPropWhite.setProgress(((segXmodeWht[ curSegment] * MAXVAL_PERCENT) / MAXVAL_WHT));
                    seekPropCount.setProgress(  segXmodeCnt[ curSegment]);
                }
                else
                {
                    manualButton.setText(appContext.getResources().getString(R.string.name_enable));
                    llAutoControls.setVisibility(GONE);
                }
            }
            else llProperties.setVisibility(GONE);

            if ((bits & 0x10) != 0) // enable triggering
            {
                llTrigControls.setVisibility(VISIBLE);

                if ((bits & 0x20) != 0) // enable force control
                {
                    llTrigForce.setVisibility(VISIBLE);
                    seekTrigForce.setProgress(segTrigForce[curSegment] / 10);
                    textTrigger.setText(appContext.getResources().getString(R.string.title_trigforce));
                }
                else
                {
                    llTrigForce.setVisibility(GONE);
                    textTrigger.setText(appContext.getResources().getString(R.string.title_dotrigger));
                }
            }
            else llTrigControls.setVisibility(GONE);
        }
    }

    private void SetSegment(int index)
    {
        if (index == curSegment)
        {
            Log.w(LOGNAME, "Segment not changed");
            return;
        }

        SetupPatternArraysForSegment(index);

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

            SetPatternNameOnly();   // select the pattern name to be displayed
            SetControlPositions();  // set controls display without sending commands
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

    private void SendString(String str)
    {
        if (listenSendCommand != null)
            listenSendCommand.onDeviceCommand(str);
    }

    private final View.OnClickListener mClicker = new View.OnClickListener()
    {
        @Override public void onClick(View v)
        {
            switch (v.getId())
            {
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

                    SetControlPositions(); // set control positions without sending commands
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
        if (fromUser) switch (seekBar.getId()) // ignore when setting initial values
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
                    Log.v(LOGNAME, "PropColor!");
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
                    Log.v(LOGNAME, "PropWhite!");
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
                    Log.v(LOGNAME, "PropCount!");
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
