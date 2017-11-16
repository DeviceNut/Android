package com.devicenut.pixelnutctrl;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import static com.devicenut.pixelnutctrl.Main.CMD_EXTMODE;
import static com.devicenut.pixelnutctrl.Main.CMD_POP_PATTERN;
import static com.devicenut.pixelnutctrl.Main.CMD_SEGS_ENABLE;
import static com.devicenut.pixelnutctrl.Main.CMD_START_END;
import static com.devicenut.pixelnutctrl.Main.advPatternNames;
import static com.devicenut.pixelnutctrl.Main.basicPatternsCount;
import static com.devicenut.pixelnutctrl.Main.devPatternCmds;
import static com.devicenut.pixelnutctrl.Main.numSegments;
import static com.devicenut.pixelnutctrl.Main.numsFavorites;
import static com.devicenut.pixelnutctrl.Main.masterPager;
import static com.devicenut.pixelnutctrl.Main.pageControls;

public class FragFavs extends Fragment
{
    private final String LOGNAME = "Favorites";
    private final int numButtons = 7;
    private Activity context;

    private final int[] idsButton =
            {
                    R.id.button_Pattern1,
                    R.id.button_Pattern2,
                    R.id.button_Pattern3,
                    R.id.button_Pattern4,
                    R.id.button_Pattern5,
                    R.id.button_Pattern6,
                    R.id.button_Pattern7,
            };

    private OnFragmentInteractionListener mListener;

    public interface OnFragmentInteractionListener
    {
        void onFragmentInteraction(String s);
    }

    public FragFavs() {}

    public static FragFavs newInstance()
    {
        FragFavs fragment = new FragFavs();
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
        context = this.getActivity(); // not valid until now

        View v = inflater.inflate(R.layout.fragment_favs, container, false);

        for (int i = 0; i < numButtons; ++i)
        {
            Button b = (Button)v.findViewById(idsButton[i]);
            b.setText(advPatternNames[numsFavorites[i]]);
            b.setOnClickListener(mClicker);
        }

        (v.findViewById(R.id.text_GoToControls)).setOnClickListener(mClicker);

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

    private final View.OnClickListener mClicker = new View.OnClickListener()
    {
        @Override public void onClick(View v)
        {
            if (v.getId() == R.id.text_GoToControls)
                masterPager.setCurrentItem(pageControls);
            else SendPattern(v.getId());
        }
    };

    private void SendPattern(int id)
    {
        for (int i = 0; i < numButtons; ++i)
        {
            if (id == idsButton[i])
            {
                int num = numsFavorites[i] + basicPatternsCount + 1;
                for (int seg = 1; seg <= numSegments; ++seg)
                {
                    if (numSegments > 1) SendString(CMD_SEGS_ENABLE + seg);
                    SendString(CMD_EXTMODE + "0"); // turn off external properties mode
                    SendSegPat(num);
                }
                break;
            }
        }
    }

    private void SendSegPat(int num)
    {
        SendString(CMD_START_END);; // start sequence
        SendString(CMD_POP_PATTERN);
        SendString(devPatternCmds[num-1]);
        SendString(CMD_START_END);; // end sequence
        SendString("" + num);   // store current pattern number
    }

    private void SendString(String str)
    {
        if (mListener != null)
            mListener.onFragmentInteraction(str);
    }
}
