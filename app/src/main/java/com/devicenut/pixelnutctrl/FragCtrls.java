package com.devicenut.pixelnutctrl;

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
import static com.devicenut.pixelnutctrl.Main.createViewFavs;
import static com.devicenut.pixelnutctrl.Main.curBright;
import static com.devicenut.pixelnutctrl.Main.curDelay;
import static com.devicenut.pixelnutctrl.Main.curSegment;
import static com.devicenut.pixelnutctrl.Main.devicePatterns;
import static com.devicenut.pixelnutctrl.Main.initPatterns;
import static com.devicenut.pixelnutctrl.Main.isConnected;
import static com.devicenut.pixelnutctrl.Main.multiStrands;
import static com.devicenut.pixelnutctrl.Main.basicPatternsCount;
import static com.devicenut.pixelnutctrl.Main.haveBasicSegs;
import static com.devicenut.pixelnutctrl.Main.useAdvPatterns;
import static com.devicenut.pixelnutctrl.Main.numSegments;
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

    private static boolean useSegEnables = false;
    private static final boolean segEnables[] = {false, false, false, false, false};

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

    public static FragCtrls newInstance() { return new FragCtrls(); }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        Log.d(LOGNAME, ">>onCreateView");

        View v = inflater.inflate(R.layout.fragment_ctrls, container, false);

        viewCtrls = (ScrollView) v.findViewById(R.id.scroll_Controls);
        helpPage = (ScrollView) v.findViewById(R.id.ll_HelpPage_Ctrls);
        helpText = (TextView) v.findViewById(R.id.view_HelpText_Ctrls);

        llPatternHelp = (LinearLayout) v.findViewById(R.id.ll_PatternHelp);
        llDelayControl = (LinearLayout) v.findViewById(R.id.ll_DelayControl);
        llProperties = (LinearLayout) v.findViewById(R.id.ll_Properties);
        llAutoControls = (LinearLayout) v.findViewById(R.id.ll_AutoControls);
        llPropColor = (LinearLayout) v.findViewById(R.id.ll_PropColor);
        llPropWhite = (LinearLayout) v.findViewById(R.id.ll_PropWhite);
        llPropCount = (LinearLayout) v.findViewById(R.id.ll_PropCount);
        llTrigControls = (LinearLayout) v.findViewById(R.id.ll_TrigControls);
        llTrigForce = (LinearLayout) v.findViewById(R.id.ll_TrigForce);

        selectPattern = (Spinner) v.findViewById(R.id.spinner_Pattern);
        textTrigger = (TextView) v.findViewById(R.id.text_Trigger);
        helpText2 = (TextView) v.findViewById(R.id.text_PatternHelp);

        seekBright = (SeekBar) v.findViewById(R.id.seek_Bright);
        seekDelay = (SeekBar) v.findViewById(R.id.seek_Delay);
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

        if (pageFavorites >= 0)
        {
            favButton = (Button) v.findViewById(R.id.button_Favorite);
            favButton.setOnClickListener(mClicker);
        }

        manualButton = (Button) v.findViewById(R.id.button_AutoProp);
        manualButton.setOnClickListener(mClicker);

        segAddButton = (Button) v.findViewById(R.id.button_SegAdd);
        segAddButton.setOnClickListener(mClicker);

        helpButton = (Button) v.findViewById(R.id.button_PatternHelp);
        helpButton.setOnClickListener(mClicker);

        Button triggerButton = (Button) v.findViewById(R.id.button_TrigAction);
        triggerButton.setOnClickListener(mClicker);

        segRadioButtons = new RadioButton[segRadioIds.length];
        for (int i = 0; i < segRadioIds.length; ++i)
            segRadioButtons[i] = (RadioButton) v.findViewById(segRadioIds[i]);

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

            useSegEnables = false;
        }
        else llSelectSegs.setVisibility(GONE);

        SetupPatternArraysForSegment(curSegment, true);

        // cannot create these until have context
        if (haveBasicSegs)  CreateSpinnerAdapterBasic();
        if (useAdvPatterns) CreateSpinnerAdapterAll();

        if (segBasicOnly[curSegment] || !useAdvPatterns)
             selectPattern.setAdapter(spinnerArrayAdapter_Basic);
        else selectPattern.setAdapter(spinnerArrayAdapter_All);
        selectPattern.setOnItemSelectedListener(patternListener);

        if (initPatterns)
        {
            Log.d(LOGNAME, "Initializing all patterns...");
            SelectPattern(segPatterns[0], true);
            initPatterns = false;
        }

        SetPatternNameOnly();   // only set the pattern display name
        SetControlPositions();  // set controls display without sending commands
        CheckForFavorite();     // check if selected pattern is one of the favorites

        createViewCtrls = true;
        return v;
    }

    @Override
    public void onDestroyView()
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

    @Override
    public void onAttach(Context context)
    {
        Log.d(LOGNAME, ">>onAttach");
        super.onAttach(context);

        listenDeviceCommand = (DeviceCommandInterface) getActivity();
        listenFavoriteDeselect = (FavoriteDeselectInterface) getActivity();
        listenFavoriteCreate = (FavoriteCreateInterface) getActivity();
        listenPatternSelect = (PatternSelectInterface) getActivity();
    }

    @Override
    public void onDetach()
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
            @Override
            public boolean areAllItemsEnabled() { return false; }

            @Override
            public boolean isEnabled(int position) { return listEnables_Basic[position]; }

            @Override
            public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent)
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
            @Override
            public boolean areAllItemsEnabled() { return false; }

            @Override
            public boolean isEnabled(int position) { return listEnables_All[position]; }

            @Override
            public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent)
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
        @Override
        public void onNothingSelected(AdapterView<?> parent) {}

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
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
            else changePattern = true; // reset for next time
        }
    };

    private void SetupPatternArraysForSegment(int seg, boolean doselect)
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
                    SendString("X" + segPosStart[i] + " Y" + segPosCount[i]);
                    SetupPatternArraysForSegment(i, false);
                    SendString(patternCmds[segPatterns[i]]);
                }
                SetupPatternArraysForSegment(curSegment, false);

                SendString(CMD_START_END);

                int num = pnum+1;       // device pattern numbers start at 1
                SendString("" + num);   // store current pattern number

                SendString(CMD_RESUME);
                SendString(CMD_SEQ_END);
                return;
            }

            // physically separate segments, so can treat them as such
            if (initPatterns || useSegEnables)
            {
                for (int i = 0; i < numSegments; ++i)
                {
                    Log.d(LOGNAME, "Selecting pattern: seg=" + i + " enable=" + segEnables[i] + " init=" + initPatterns);

                    if (initPatterns || segEnables[i])
                    {
                        if (initPatterns) pnum = segPatterns[i];
                        else segPatterns[i] = pnum;

                        int seg = i+1;
                        SendString(CMD_SEGS_ENABLE + seg);

                        if (segPatterns[i] >= devicePatterns)
                        {
                            SendString(CMD_START_END);
                            SendString(CMD_POP_PATTERN);
                            SetupPatternArraysForSegment(i, false);
                            SendString(patternCmds[segPatterns[i]]);
                            SendString(CMD_START_END);
                        }

                        int num = pnum+1;       // device pattern numbers start at 1
                        SendString("" + num);   // store current pattern number
                    }
                }
                SetupPatternArraysForSegment(curSegment, false);

                int seg = curSegment+1;
                SendString(CMD_SEGS_ENABLE + seg);

                SendString(CMD_RESUME);
                SendString(CMD_SEQ_END);
                return;
            }
        }
        // else a single segment, or multiple physical segments not grouped together and not initializing

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
    public void ChangePattern(int seg, int pnum, String vals)
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

        if (seg == 0)
        {
            favButton.setVisibility(INVISIBLE);

            // disable pattern help if currently displayed
            if (helpMode > 0) SetPatternHelp(true, 0);

            useSegEnables = false; // setting favorite disables segment grouping
            SetupSegEnable();

            SendString(CMD_PAUSE);
        }

        if (numSegments > 1)
        {
            int devseg = seg+1;
            SendString(CMD_SEGS_ENABLE + devseg);
        }

        if (segXmodeEnb[seg])
             SendString(CMD_EXTMODE + "1");
        else SendString(CMD_EXTMODE + "0");
        SendString(CMD_PROPVALS + segXmodeHue[seg] + " " + segXmodeWht[seg] + " " + segXmodeCnt[seg]);

        SendString(CMD_BRIGHT + curBright[seg]);
        SendString(CMD_DELAY + curDelay[seg]);

        SelectPattern(pnum, false);

        if (seg == numSegments-1)
        {
            SetPatternNameOnly();   // select the pattern name to be displayed
            SetControlPositions();  // set controls display without sending commands

            SendString(CMD_RESUME);

            if (numSegments > 1)
                SendString(CMD_SEGS_ENABLE + "1");
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
                Log.d(LOGNAME, "Copying values to segment=" + index + " pattern=" + segPatterns[curSegment]);
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
                Log.d(LOGNAME, "  pattern=" + segPatterns[index]);

                // change the pattern:
                SendString(CMD_SEGS_ENABLE + seg);

                if (segPatterns[index] >= devicePatterns)
                {
                    SendString(CMD_START_END);; // start sequence
                    SendString(CMD_POP_PATTERN);
                    SendString(patternCmds[ segPatterns[index] ]);
                    SendString(CMD_START_END);; // end sequence
                }
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

                SendString(CMD_SEQ_END);
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

            SendString(CMD_SEQ_END);

            SetupPatternArraysForSegment(index, true);
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

    public void setHelpMode(boolean enable)
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

    private void SendString(String str)
    {
        if (listenDeviceCommand != null)
            listenDeviceCommand.onDeviceCommand(str);
    }

    private void SetupSegEnable()
    {
        segAddButton.setText(useSegEnables ? "X" : "&");
        if (!useSegEnables) ClearSegEnables();
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
                    SetupSegEnable();
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
                                SendString(CMD_PROPVALS + segXmodeHue[i] + " " + segXmodeWht[i] + " " + segXmodeCnt[i]);
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
                    if (useSegEnables)
                    {
                        for (int i = 0; i < numSegments; ++i)
                        {
                            if (segEnables[i])
                            {
                                int seg = i+1;
                                SendString(CMD_SEGS_ENABLE + seg);

                                if ((patternBits[segPatterns[i]] & 0x20) != 0)
                                     SendString(CMD_TRIGGER + segTrigForce[i]);
                                else SendString(CMD_TRIGGER + 0);
                            }
                        }

                        int seg = curSegment+1;
                        SendString(CMD_SEGS_ENABLE + seg);
                    }
                    else if ((patternBits[segPatterns[curSegment]] & 0x20) != 0)
                         SendString(CMD_TRIGGER + segTrigForce[curSegment]);
                    else SendString(CMD_TRIGGER + 0);

                    SendString(CMD_SEQ_END);
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

                    SendString(CMD_SEQ_END);
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

                    SendString(CMD_SEQ_END);
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

                    SendString(CMD_SEQ_END);
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

                    SendString(CMD_SEQ_END);
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

                    SendString(CMD_SEQ_END);
                    break;
                }
                case R.id.seek_TrigForce:
                {
                    int val = ((progress * MAXVAL_FORCE) / MAXVAL_PERCENT);
                    if (useSegEnables)
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
