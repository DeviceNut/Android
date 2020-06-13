package com.devicenut.pixelnutctrl;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.core.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import static android.view.View.GONE;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static com.devicenut.pixelnutctrl.Main.CMD_BRIGHT;
import static com.devicenut.pixelnutctrl.Main.CMD_DELAY;
import static com.devicenut.pixelnutctrl.Main.CMD_EXTMODE;
import static com.devicenut.pixelnutctrl.Main.CMD_PAUSE;
import static com.devicenut.pixelnutctrl.Main.CMD_POP_PATTERN;
import static com.devicenut.pixelnutctrl.Main.CMD_PROPVALS;
import static com.devicenut.pixelnutctrl.Main.CMD_RESUME;
import static com.devicenut.pixelnutctrl.Main.CMD_SEGS_ENABLE;
import static com.devicenut.pixelnutctrl.Main.CMD_SEQ_END;
import static com.devicenut.pixelnutctrl.Main.CMD_START_END;
import static com.devicenut.pixelnutctrl.Main.CMD_TRIGGER;
import static com.devicenut.pixelnutctrl.Main.MAXVAL_FORCE;
import static com.devicenut.pixelnutctrl.Main.MAXVAL_HUE;
import static com.devicenut.pixelnutctrl.Main.MAXVAL_PERCENT;
import static com.devicenut.pixelnutctrl.Main.MAXVAL_WHT;
import static com.devicenut.pixelnutctrl.Main.NUM_FAVSTR_VALS;

import static com.devicenut.pixelnutctrl.Main.appContext;
import static com.devicenut.pixelnutctrl.Main.curBright;
import static com.devicenut.pixelnutctrl.Main.curDelay;
import static com.devicenut.pixelnutctrl.Main.curSegment;
import static com.devicenut.pixelnutctrl.Main.devicePatterns;
import static com.devicenut.pixelnutctrl.Main.multiStrands;
import static com.devicenut.pixelnutctrl.Main.basicPatternsCount;
import static com.devicenut.pixelnutctrl.Main.haveBasicSegs;
import static com.devicenut.pixelnutctrl.Main.numSegments;
import static com.devicenut.pixelnutctrl.Main.oldSegmentVals;
import static com.devicenut.pixelnutctrl.Main.pageFavorites;
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
import static com.devicenut.pixelnutctrl.Main.patternNames_All;
import static com.devicenut.pixelnutctrl.Main.patternNames_Basic;
import static com.devicenut.pixelnutctrl.Main.patternHelp_All;
import static com.devicenut.pixelnutctrl.Main.patternHelp_Basic;
import static com.devicenut.pixelnutctrl.Main.patternCmds_All;
import static com.devicenut.pixelnutctrl.Main.patternCmds_Basic;
import static com.devicenut.pixelnutctrl.Main.patternBits_All;
import static com.devicenut.pixelnutctrl.Main.patternBits_Basic;
import static com.devicenut.pixelnutctrl.Main.mapIndexToPattern_All;
import static com.devicenut.pixelnutctrl.Main.mapIndexToPattern_Basic;
import static com.devicenut.pixelnutctrl.Main.mapPatternToIndex_All;
import static com.devicenut.pixelnutctrl.Main.mapPatternToIndex_Basic;
import static com.devicenut.pixelnutctrl.Main.mapIndexToPattern;
import static com.devicenut.pixelnutctrl.Main.mapPatternToIndex;
import static com.devicenut.pixelnutctrl.Main.patternNames;
import static com.devicenut.pixelnutctrl.Main.patternHelp;
import static com.devicenut.pixelnutctrl.Main.patternCmds;
import static com.devicenut.pixelnutctrl.Main.patternBits;
import static com.devicenut.pixelnutctrl.Main.createViewCtrls;

public class FragCtrls extends Fragment implements SeekBar.OnSeekBarChangeListener
{
    private static final String LOGNAME = "Controls";

    private static ScrollView viewCtrls;
    private static ScrollView helpPage;
    private static TextView helpText;

    private static int helpMode = 0;
    private static boolean changePattern = true;

    private static LinearLayout llPatternHelp, llDelayControl;
    private static LinearLayout llProperties, llAutoControls;
    private static LinearLayout llPropColor, llPropWhite, llPropCount;
    private static LinearLayout llTrigControls, llTrigForce;

    private static Button segAddButton, favButton, helpButton, manualButton;
    private static TextView helpText2, textTrigger;
    private static Spinner selectPattern;

    private static SeekBar seekBright, seekDelay;
    private static SeekBar seekPropColor, seekPropWhite, seekPropCount;
    private static SeekBar seekTrigForce;

    private static EditText cmdText;

    // number of entries must be the same as MAXNUM_SEGMENTS
    private static final boolean[] segEnables = {false, false, false, false, false};
    private static final int[] segRadioIds =
            {
                    R.id.radio_1,
                    R.id.radio_2,
                    R.id.radio_3,
                    R.id.radio_4,
                    R.id.radio_5,
            };
    private static boolean doGroupSegments;

    private static RadioButton[] segRadioButtons;

    interface DeviceCommandInterface
    {
        void onDeviceCommand(String s);
    }

    private DeviceCommandInterface listenDeviceCommand;

    interface FavoriteDeselectInterface
    {
        void onFavoriteDeselect();
    }

    private FavoriteDeselectInterface listenFavoriteDeselect;

    interface FavoriteCreateInterface
    {
        void onFavoriteCreate(String name, int seg, int pnum, String vals);
    }

    private FavoriteCreateInterface listenFavoriteCreate;

    interface PatternSelectInterface
    {
        boolean onPatternSelect(String name, int seg, int pnum, String vals);
    }

    private PatternSelectInterface listenPatternSelect;

    public FragCtrls() {}

    static FragCtrls newInstance() { return new FragCtrls(); }

    @Override public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        Log.d(LOGNAME, ">>onCreateView");

        View v = inflater.inflate(R.layout.fragment_ctrls, container, false);

        viewCtrls       = v.findViewById(R.id.scroll_Controls);
        helpPage        = v.findViewById(R.id.scroll_HelpPage_Ctrls);
        helpText        = v.findViewById(R.id.view_HelpText_Ctrls);

        llPatternHelp   = v.findViewById(R.id.ll_PatternHelp);
        llDelayControl  = v.findViewById(R.id.ll_DelayControl);
        llProperties    = v.findViewById(R.id.ll_Properties);
        llAutoControls  = v.findViewById(R.id.ll_AutoControls);
        llPropColor     = v.findViewById(R.id.ll_PropColor);
        llPropWhite     = v.findViewById(R.id.ll_PropWhite);
        llPropCount     = v.findViewById(R.id.ll_PropCount);
        llTrigControls  = v.findViewById(R.id.ll_TrigControls);
        llTrigForce     = v.findViewById(R.id.ll_TrigForce);

        selectPattern   = v.findViewById(R.id.spinner_Pattern);
        textTrigger     = v.findViewById(R.id.text_Trigger);
        helpText2       = v.findViewById(R.id.text_PatternHelp);

        seekBright      = v.findViewById(R.id.seek_Bright);
        seekDelay       = v.findViewById(R.id.seek_Delay);
        seekPropColor   = v.findViewById(R.id.seek_PropColor);
        seekPropWhite   = v.findViewById(R.id.seek_PropWhite);
        seekPropCount   = v.findViewById(R.id.seek_PropCount);
        seekTrigForce   = v.findViewById(R.id.seek_TrigForce);

        seekBright.setOnSeekBarChangeListener(this);
        seekDelay.setOnSeekBarChangeListener(this);
        seekPropColor.setOnSeekBarChangeListener(this);
        seekPropWhite.setOnSeekBarChangeListener(this);
        seekPropCount.setOnSeekBarChangeListener(this);
        seekTrigForce.setOnSeekBarChangeListener(this);

        if (pageFavorites >= 0)
        {
            favButton = v.findViewById(R.id.button_Favorite);
            favButton.setOnClickListener(mClicker);
        }

        manualButton = v.findViewById(R.id.button_AutoProp);
        manualButton.setOnClickListener(mClicker);

        segAddButton = v.findViewById(R.id.button_SegAdd);
        segAddButton.setOnClickListener(mClicker);

        helpButton = v.findViewById(R.id.button_PatternHelp);
        helpButton.setOnClickListener(mClicker);

        Button triggerButton = v.findViewById(R.id.button_TrigAction);
        triggerButton.setOnClickListener(mClicker);

        if (BuildConfig.DEBUG)
        {
            cmdText = v.findViewById(R.id.edit_CmdStr);
            Button sendButton = v.findViewById(R.id.button_SendCmd);
            sendButton.setOnClickListener(mClicker);
        }
        else v.findViewById(R.id.rl_SendCmdStr).setVisibility(GONE);

        segRadioButtons = new RadioButton[segRadioIds.length];
        for (int i = 0; i < segRadioIds.length; ++i)
            segRadioButtons[i] = v.findViewById(segRadioIds[i]);

        LinearLayout llSelectSegs = v.findViewById(R.id.ll_SelectSegments);
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
                b.setOnClickListener(mClicker);

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
        }
        else llSelectSegs.setVisibility(GONE);

        doGroupSegments = false;
        AssignPatternArrays(curSegment, true);

        // cannot create these until have context
        if (haveBasicSegs)  CreateSpinnerAdapterBasic();
        CreateSpinnerAdapterAll();

        if (segBasicOnly[curSegment])
             selectPattern.setAdapter(spinnerArrayAdapter_Basic);
        else selectPattern.setAdapter(spinnerArrayAdapter_All);
        selectPattern.setOnItemSelectedListener(patternListener);

        SetPatternNameOnly();   // only set the pattern display name
        SetControlPositions();  // set controls display without sending commands
        CheckForFavorite();     // check if selected pattern is one of the favorites

        createViewCtrls = true;
        return v;
    }

    @Override public void onDestroyView()
    {
        Log.d(LOGNAME, ">>onDestroyView");
        super.onDestroyView();

        viewCtrls = null;
        helpPage = null;
        helpText = null;

        llPatternHelp = llDelayControl = null;
        llProperties = llAutoControls = null;
        llPropColor = llPropWhite = llPropCount = null;
        llTrigControls = llTrigForce = null;

        segAddButton = favButton = null;
        helpButton = manualButton = null;
        helpText2 = textTrigger = null;
        selectPattern = null;

        seekBright = seekDelay = seekTrigForce = null;
        seekPropColor = seekPropWhite = seekPropCount = null;

        createViewCtrls = false;
    }

    @Override public void onAttach(Context context)
    {
        Log.d(LOGNAME, ">>onAttach");
        super.onAttach(context);

        listenDeviceCommand = (DeviceCommandInterface) getActivity();
        listenFavoriteDeselect = (FavoriteDeselectInterface) getActivity();
        listenFavoriteCreate = (FavoriteCreateInterface) getActivity();
        listenPatternSelect = (PatternSelectInterface) getActivity();
    }

    @Override public void onDetach()
    {
        Log.d(LOGNAME, ">>onDetach");
        super.onDetach();

        listenDeviceCommand = null;
        listenFavoriteDeselect = null;
        listenFavoriteCreate = null;
        listenPatternSelect = null;
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
                    if (vi != null) v = vi.inflate(R.layout.layout_spinner, null);
                }

                if (v != null)
                {
                    TextView tv = v.findViewById(R.id.spinnerText);
                    tv.setText(listNames_Basic[position]);

                    if (!listEnables_Basic[position]) tv.setTextColor(Color.GRAY);
                    else tv.setTextColor(ContextCompat.getColor(appContext, R.color.UserChoice));
                }

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
                    if (vi != null) v = vi.inflate(R.layout.layout_spinner, null);
                }

                if (v != null)
                {
                    TextView tv = v.findViewById(R.id.spinnerText);
                    tv.setText(listNames_All[position]);

                    if (!listEnables_All[position]) tv.setTextColor(Color.GRAY);
                    else tv.setTextColor(ContextCompat.getColor(appContext, R.color.UserChoice));
                }

                return v;
            }
        };
    }

    private final AdapterView.OnItemSelectedListener patternListener = new AdapterView.OnItemSelectedListener()
    {
        @Override public void onNothingSelected(AdapterView<?> parent) {}

        @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
        {
            TextView v = (TextView) view;
            v.setTextColor(ContextCompat.getColor(appContext, R.color.UserChoice));
            v.setTextSize(18);

            if (changePattern)
            {
                Log.d(LOGNAME, "Pattern choice: " + parent.getItemAtPosition(position));

                int pnum = mapIndexToPattern[position];
                segPatterns[curSegment] = pnum;

                SelectPattern(pnum, true);  // send pattern commands
                SetControlPositions();      // set control positions without sending commands
                CheckForFavorite();         // check if this pattern is one of the favorites

                // change text for new pattern if pattern help is active, but keep it active
                if (helpMode > 0) SetPatternHelp(false, pnum);
            }
            else changePattern = true; // reset for next time TODO: explain this
        }
    };

    private void AssignPatternArrays(int seg, boolean doselect)
    {
        if (segBasicOnly[seg])
        {
            Log.d(LOGNAME, "Setting basic arrays...");
            mapIndexToPattern = mapIndexToPattern_Basic;
            mapPatternToIndex = mapPatternToIndex_Basic;
            patternNames      = patternNames_Basic;
            patternHelp       = patternHelp_Basic;
            patternCmds       = patternCmds_Basic;
            patternBits       = patternBits_Basic;

            if (doselect) selectPattern.setAdapter(spinnerArrayAdapter_Basic);
        }
        else
        {
            Log.d(LOGNAME, "Setting advanced arrays...");
            mapIndexToPattern = mapIndexToPattern_All;
            mapPatternToIndex = mapPatternToIndex_All;
            patternNames      = patternNames_All;
            patternHelp       = patternHelp_All;
            patternCmds       = patternCmds_All;
            patternBits       = patternBits_All;

            if (doselect) selectPattern.setAdapter(spinnerArrayAdapter_All);
        }
    }

    private void CheckForFavorite()
    {
        if (pageFavorites >= 0)
        {
            favButton.setVisibility(INVISIBLE);

            int pnum = segPatterns[curSegment];
            if (pnum >= devicePatterns)
            {
                String name = patternNames[pnum];
                if (listenPatternSelect.onPatternSelect(name, numSegments, 0, null))
                {
                    for (int i = 0; i < numSegments; ++i)
                    {
                        String vals = "";

                        vals += curBright[multiStrands ? i : 0] + " ";
                        vals += curDelay[ multiStrands ? i : 0] + " ";
                        vals += (segXmodeEnb[i] ? "1 " : "0 ");
                        vals += segXmodeHue[ i] + " ";
                        vals += segXmodeWht[ i] + " ";
                        vals += segXmodeCnt[ i] + " ";
                        vals += segTrigForce[i] + " ";

                        if (!listenPatternSelect.onPatternSelect(null, i, segPatterns[i], vals))
                        {
                            favButton.setVisibility(VISIBLE);
                            return;
                        }
                    }
                }
            }
        }
    }

    private void SelectPattern(int pnum, boolean doend)
    {
        if (numSegments > 1)
        {
            if (!multiStrands) // must send all segment patterns at once
            // note that device patterns are not supported for this case
            {
                SendString(CMD_START_END);
                SendString(CMD_POP_PATTERN);

                for (int i = 0; i < numSegments; ++i)
                {
                    SendString(" X" + segPosStart[i] + " Y" + segPosCount[i] + " ");
                    AssignPatternArrays(i, false);
                    SendString(patternCmds[segPatterns[i]]);
                }
                AssignPatternArrays(curSegment, false);

                SendString(CMD_START_END);

                int num = pnum+1;       // device pattern numbers start at 1
                SendString("" + num);   // store current pattern number

                if (doend)
                {
                    SendString(CMD_RESUME);
                    SendString(CMD_SEQ_END);
                }
                return;
            }
            // else physically separate segments

            if (doGroupSegments) // if more than one segment might be grouped together
            {
                for (int i = 0; i < numSegments; ++i)
                {
                    Log.d(LOGNAME, "Selecting pattern: seg=" + i + " enable=" + segEnables[i]);

                    if (segEnables[i])
                    {
                        segPatterns[i] = pnum;
                        ChangeSegment(i);

                        if (pnum >= devicePatterns)
                        {
                            SendString(CMD_START_END);
                            SendString(CMD_POP_PATTERN);
                            AssignPatternArrays(i, false);
                            SendString(patternCmds[pnum]);
                            SendString(CMD_START_END);
                        }

                        int num = pnum+1;       // device pattern numbers start at 1
                        SendString("" + num);   // store current pattern number
                    }
                }

                AssignPatternArrays(curSegment, false);
                ChangeSegment(curSegment);

                if (doend)
                {
                    SendString(CMD_RESUME);
                    SendString(CMD_SEQ_END);
                }
                return;
            }
        }
        // else a single segment, or multiple physical segments not grouped together

        if (pnum >= devicePatterns)
        {
            SendString(CMD_START_END);
            SendString(CMD_POP_PATTERN);
            SendString(patternCmds[pnum]);
            SendString(CMD_START_END);
        }

        int num = pnum+1;       // device pattern numbers start at 1
        SendString("" + num);   // store current pattern number

        if (doend)
        {
            SendString(CMD_RESUME);
            SendString(CMD_SEQ_END);
        }
    }

    // user just selected a favorite
    // this will be called once for every segment
    void ChangePattern(int seg, int pnum, String vals)
    {
        Log.d(LOGNAME, "ChangePattern: seg=" + seg + " pnum=" + pnum + " vals=" + vals);

        String[] strs = vals.split("\\s+");
        if (strs.length < NUM_FAVSTR_VALS) return;

        curBright[   seg] = Integer.parseInt(strs[0]);
        curDelay[    seg] = Integer.parseInt(strs[1]);
        segXmodeEnb[ seg] = Integer.parseInt(strs[2]) != 0;
        segXmodeHue[ seg] = Integer.parseInt(strs[3]);
        segXmodeWht[ seg] = Integer.parseInt(strs[4]);
        segXmodeCnt[ seg] = Integer.parseInt(strs[5]);
        segTrigForce[seg] = Integer.parseInt(strs[6]);

        if (segBasicOnly[seg] && (pnum > basicPatternsCount))
            pnum = 0; // cannot use this pattern, so set to Solid

        segPatterns[seg] = pnum;

        if (seg == 0) // first call here, might be more
        {
            favButton.setVisibility(INVISIBLE);

            // disable pattern help if currently displayed
            if (helpMode > 0) SetPatternHelp(true, 0);

            //doGroupSegments = false; // setting favorite disables segment grouping
            // FIXME: this should be kept as a config of the favorite ??
            //SetupSegEnable();

            SendString(CMD_PAUSE);
        }

        if (numSegments > 1)
            ChangeSegment(seg);

        if (segXmodeEnb[seg])
             SendString(CMD_EXTMODE + "1");
        else SendString(CMD_EXTMODE + "0");
        SendString(CMD_PROPVALS + segXmodeHue[seg] + " " + segXmodeWht[seg] + " " + segXmodeCnt[seg]);

        SendString(CMD_BRIGHT + curBright[seg]);
        SendString(CMD_DELAY + curDelay[seg]);

        SelectPattern(pnum, false);

        if (seg == numSegments-1)   // last call here for this favorite
        {
            SetPatternNameOnly();   // select the pattern name to be displayed
            SetControlPositions();  // set controls display without sending commands

            SendString(CMD_RESUME);

            if (numSegments > 1)
                ChangeSegment(curSegment);
        }

        SendString(CMD_SEQ_END);
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
            changePattern = true;
        }});
    }

    private void SetControlPositions()
    {
        int bits = patternBits[segPatterns[curSegment]];
        Log.d(LOGNAME, "SetControlPositions: bits=" + Integer.toHexString(bits));

        int index = multiStrands ? curSegment : 0;
        seekBright.setProgress(curBright[index]);
        seekDelay.setProgress(((rangeDelay - curDelay[index]) * MAXVAL_PERCENT) / (rangeDelay + rangeDelay));

        llDelayControl.setVisibility(((bits & 0x80) == 0) ? VISIBLE : GONE);

        if (segBasicOnly[curSegment])
        {
            // small segments with only basic patterns
            // always have the property controls displayed

            manualButton.setVisibility(GONE);

            if (!segXmodeEnb[curSegment])
            {
                segXmodeEnb[curSegment] = true;
                SendString(CMD_EXTMODE + "1");

                SendString(CMD_SEQ_END);
            }
        }
        else manualButton.setVisibility(VISIBLE);

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
                seekTrigForce.setProgress((segTrigForce[curSegment] * MAXVAL_PERCENT) / MAXVAL_FORCE);
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

    private void SetSegment(int seg)
    {
        boolean enable = segEnables[seg];
        Log.d(LOGNAME, "Segment " + seg + " enabled=" + enable);

        if (doGroupSegments) // allow multiple segment selections
        {
            if (!enable) // adding new segment to the group
            {
                segEnables[seg] = true;
                int pattern = segPatterns[curSegment];
                Log.d(LOGNAME, "Copying segment " + curSegment + "->" + seg + ", pattern=" + pattern);

                curDelay[    seg] = curDelay[    curSegment];
                curBright[   seg] = curBright[   curSegment];
                segXmodeEnb[ seg] = segXmodeEnb[ curSegment];
                segXmodeHue[ seg] = segXmodeHue[ curSegment];
                segXmodeWht[ seg] = segXmodeWht[ curSegment];
                segXmodeCnt[ seg] = segXmodeCnt[ curSegment];
                segTrigForce[seg] = segTrigForce[curSegment];
                segPatterns[ seg] = pattern;

                // change the pattern:
                ChangeSegment(seg);

                if (pattern >= devicePatterns)
                {
                    SendString(CMD_START_END);  // start sequence
                    SendString(CMD_POP_PATTERN);
                    SendString(patternCmds[pattern]);
                    SendString(CMD_START_END);  // end sequence
                }

                int pnum = pattern+1;
                SendString("" + pnum); // store pattern number

                // change brightness/delay:
                SendString(CMD_BRIGHT + curBright[seg]);
                SendString(CMD_DELAY + curDelay[seg]);

                // change properties:
                if (segXmodeEnb[seg])
                     SendString(CMD_EXTMODE + "1");
                else SendString(CMD_EXTMODE + "0");
                SendString(CMD_PROPVALS + segXmodeHue[seg] + " " + segXmodeWht[seg] + " " + segXmodeCnt[seg]);

                curSegment = seg; // change to this segment
                ChangeSegment(curSegment);
                SendString(CMD_SEQ_END);
            }
            else // try to disable this segment
            {
                int foundseg = -1; // set to first one enabled
                segEnables[seg] = false;

                // search for any other enabled segments
                for (int i = 0; i < numSegments; ++i)
                {
                    if (segEnables[i])
                    {
                        foundseg = i;
                        break;
                    }
                }

                if (foundseg < 0) // no other ones found
                {
                    segEnables[seg] = true;
                    Log.w(LOGNAME, "Cannot disable last segment");
                }
                else
                {
                    segRadioButtons[seg].setChecked(false);
                    if (curSegment == seg)
                    {
                        curSegment = foundseg;
                        ChangeSegment(curSegment);
                    }
                }
            }
        }
        else if (!enable && (seg != curSegment))
        {
            curSegment = seg;
            segEnables[curSegment] = true;
            Log.d(LOGNAME, "Switching to segment=" + curSegment + " pattern=" + segPatterns[curSegment]);
            ClearSegEnables();

            ChangeSegment(curSegment);
            SendString(CMD_SEQ_END);

            AssignPatternArrays(seg, true);
            SetPatternNameOnly();   // select the pattern name to be displayed
            SetControlPositions();  // set controls display without sending commands
        }
        // else already on this segment
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

    void setHelpMode(boolean enable)
    {
        if (enable)
        {
            viewCtrls.setVisibility(GONE);
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
            viewCtrls.setVisibility(VISIBLE);
        }
    }

    private void SetPatternHelp(boolean toggle, int pattern)
    {
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
            helpText2.setText( patternHelp[pattern]);
            helpMode = pattern+1;
        }
    }

    private void ChangeSegment(int seg)
    {
        if (oldSegmentVals) ++seg;
        SendString(CMD_SEGS_ENABLE + seg);
    }

    private void SendString(String str)
    {
        if (listenDeviceCommand != null)
            listenDeviceCommand.onDeviceCommand(str);
    }

    private final View.OnClickListener mClicker = new View.OnClickListener()
    {
        @Override public void onClick(View v)
        {
            switch (v.getId())
            {
                case R.id.button_SegAdd:
                {
                    doGroupSegments = !doGroupSegments;
                    segAddButton.setText(doGroupSegments ? "X" : "&");
                    if (!doGroupSegments) ClearSegEnables();
                    break;
                }
                case R.id.button_Favorite:
                {
                    String name = patternNames[segPatterns[curSegment]];
                    listenFavoriteCreate.onFavoriteCreate(name, numSegments, 0, null);

                    for (int i = 0; i < numSegments; ++i)
                    {
                        String vals = "";

                        vals += curBright[multiStrands ? i : 0] + " ";
                        vals += curDelay[ multiStrands ? i : 0] + " ";
                        vals += (segXmodeEnb[i] ? "1 " : "0 ");
                        vals += segXmodeHue[ i] + " ";
                        vals += segXmodeWht[ i] + " ";
                        vals += segXmodeCnt[ i] + " ";
                        vals += segTrigForce[i] + " ";

                        favButton.setVisibility(INVISIBLE);
                        listenFavoriteCreate.onFavoriteCreate(null, i, segPatterns[i], vals);
                    }
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

                    if (doGroupSegments)
                    {
                        for (int i = 0; i < numSegments; ++i)
                        {
                            if (segEnables[i])
                            {
                                ChangeSegment(i);

                                segXmodeEnb[i] = enable;
                                if (enable)
                                     SendString(CMD_EXTMODE + "1");
                                else SendString(CMD_EXTMODE + "0");
                                SendString(CMD_PROPVALS + segXmodeHue[i] + " " + segXmodeWht[i] + " " + segXmodeCnt[i]);
                            }
                        }

                        ChangeSegment(curSegment);
                    }
                    else
                    {
                        segXmodeEnb[curSegment] = enable;
                        if (enable)
                             SendString(CMD_EXTMODE + "1");
                        else SendString(CMD_EXTMODE + "0");
                        SendString(CMD_PROPVALS + segXmodeHue[curSegment] + " " + segXmodeWht[curSegment] + " " + segXmodeCnt[curSegment]);
                    }

                    SendString(CMD_SEQ_END);

                    SetControlPositions(); // set control positions without sending commands

                    if (pageFavorites >= 0)
                    {
                        favButton.setVisibility(VISIBLE);
                        listenFavoriteDeselect.onFavoriteDeselect(); // deselects current pattern
                    }
                    break;
                }
                case R.id.button_TrigAction:
                {
                    if (doGroupSegments)
                    {
                        for (int i = 0; i < numSegments; ++i)
                        {
                            if (segEnables[i])
                            {
                                int seg = i+1;
                                ChangeSegment(seg);

                                if ((patternBits[segPatterns[i]] & 0x20) != 0)
                                     SendString(CMD_TRIGGER + segTrigForce[i]);
                                else SendString(CMD_TRIGGER + 0);
                            }
                        }

                        ChangeSegment(curSegment);
                    }
                    else if ((patternBits[segPatterns[curSegment]] & 0x20) != 0)
                         SendString(CMD_TRIGGER + segTrigForce[curSegment]);
                    else SendString(CMD_TRIGGER + 0);

                    SendString(CMD_SEQ_END);
                    break;
                }
                case R.id.button_SendCmd:
                {
                    String cmdstr = cmdText.getText().toString();
                    Log.d(LOGNAME, "Command: \"" + cmdstr + "\"");
                    if (!cmdstr.isEmpty())
                    {
                        cmdText.setText("");
                        SendString(cmdstr);
                        SendString(CMD_SEQ_END);
                    }
                    break;
                }
            }
        }
    };

    @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
    {
        Log.v(LOGNAME, "SeekBar: val=" + progress + " user=" + fromUser);

        if (fromUser)
        {
            switch (seekBar.getId()) // ignore when setting initial values
            {
                case R.id.seek_Bright:
                {
                    if (doGroupSegments)
                    {
                        for (int i = 0; i < numSegments; ++i)
                        {
                            if (segEnables[i])
                            {
                                ChangeSegment(i);

                                curBright[i] = progress;
                                SendString(CMD_BRIGHT + curBright[i]);
                            }
                        }

                        ChangeSegment(curSegment);
                    }
                    else
                    {
                        curBright[curSegment] = progress;
                        SendString(CMD_BRIGHT + curBright[curSegment]);
                    }

                    SendString(CMD_SEQ_END);
                    break;
                }
                case R.id.seek_Delay:
                {
                    if (doGroupSegments)
                    {
                        for (int i = 0; i < numSegments; ++i)
                        {
                            if (segEnables[i])
                            {
                                ChangeSegment(i);

                                curDelay[i] = rangeDelay - (progress * 2 * rangeDelay)/100;
                                SendString(CMD_DELAY + curDelay[i]);
                            }
                        }

                        ChangeSegment(curSegment);
                    }
                    else
                    {
                        curDelay[curSegment] = rangeDelay - (progress * 2 * rangeDelay)/100;
                        SendString(CMD_DELAY + curDelay[curSegment]);
                    }

                    SendString(CMD_SEQ_END);
                    break;
                }
                case R.id.seek_PropColor:
                {
                    if (doGroupSegments)
                    {
                        for (int i = 0; i < numSegments; ++i)
                        {
                            if (segEnables[i])
                            {
                                ChangeSegment(i);

                                segXmodeHue[i] = (progress * MAXVAL_HUE) / MAXVAL_PERCENT;
                                SendString(CMD_PROPVALS + segXmodeHue[i] + " " + segXmodeWht[i] + " " + segXmodeCnt[i]);
                            }
                        }

                        ChangeSegment(curSegment);
                    }
                    else
                    {
                        Log.v(LOGNAME, "PropColor!");
                        segXmodeHue[curSegment] = (progress * MAXVAL_HUE) / MAXVAL_PERCENT;
                        SendString(CMD_PROPVALS + segXmodeHue[curSegment] + " " + segXmodeWht[curSegment] + " " + segXmodeCnt[curSegment]);
                    }

                    SendString(CMD_SEQ_END);
                    break;
                }
                case R.id.seek_PropWhite:
                {
                    if (doGroupSegments)
                    {
                        for (int i = 0; i < numSegments; ++i)
                        {
                            if (segEnables[i])
                            {
                                ChangeSegment(i);

                                segXmodeWht[i] = (progress * MAXVAL_WHT) / MAXVAL_PERCENT;
                                SendString(CMD_PROPVALS + segXmodeHue[i] + " " + segXmodeWht[i] + " " + segXmodeCnt[i]);
                            }
                        }

                        ChangeSegment(curSegment);
                    }
                    else
                    {
                        Log.v(LOGNAME, "PropWhite!");
                        segXmodeWht[curSegment] = (progress * MAXVAL_WHT) / MAXVAL_PERCENT;
                        SendString(CMD_PROPVALS + segXmodeHue[curSegment] + " " + segXmodeWht[curSegment] + " " + segXmodeCnt[curSegment]);
                    }

                    SendString(CMD_SEQ_END);
                    break;
                }
                case R.id.seek_PropCount:
                {
                    if (doGroupSegments)
                    {
                        for (int i = 0; i < numSegments; ++i)
                        {
                            if (segEnables[i])
                            {
                                ChangeSegment(i);

                                segXmodeCnt[i] = progress;
                                SendString(CMD_PROPVALS + segXmodeHue[i] + " " + segXmodeWht[i] + " " + segXmodeCnt[i]);
                            }
                        }

                        ChangeSegment(curSegment);
                    }
                    else
                    {
                        Log.v(LOGNAME, "PropCount!");
                        segXmodeCnt[curSegment] = progress;
                        SendString(CMD_PROPVALS + segXmodeHue[curSegment] + " " + segXmodeWht[curSegment] + " " + segXmodeCnt[curSegment]);
                    }

                    SendString(CMD_SEQ_END);
                    break;
                }
                case R.id.seek_TrigForce:
                {
                    int val = ((progress * MAXVAL_FORCE) / MAXVAL_PERCENT);
                    if (doGroupSegments)
                    {
                        for (int i = 0; i < numSegments; ++i)
                            if (segEnables[i])
                                segTrigForce[i] = val;
                    }
                    else segTrigForce[curSegment] = val;
                    break;
                }
            }

            if (fromUser)
            {
                if (pageFavorites >= 0)
                {
                    favButton.setVisibility(VISIBLE);
                    listenFavoriteDeselect.onFavoriteDeselect(); // deselects current pattern
                }
            }
        }
    }

    @Override public void onStartTrackingTouch(SeekBar seekBar) {}
    @Override public void onStopTrackingTouch(SeekBar seekBar) {}
}
