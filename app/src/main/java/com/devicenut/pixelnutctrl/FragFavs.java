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
import static com.devicenut.pixelnutctrl.Main.masterPager;
import static com.devicenut.pixelnutctrl.Main.numSegments;
import static com.devicenut.pixelnutctrl.Main.numsFavorites;

public class FragFavs extends Fragment
{
    private final String LOGNAME = "Favorites";
    private final Activity context = this.getActivity();

    private int[] idsButton =
            {
                    R.id.button_Pattern1,
                    R.id.button_Pattern2,
                    R.id.button_Pattern3,
                    R.id.button_Pattern4,
                    R.id.button_Pattern5,
                    R.id.button_Pattern6,
                    R.id.button_Pattern7,
            };
    private Button[] pattButtons = new Button[7];

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
        View v = inflater.inflate(R.layout.fragment_favs, container, false);

        for (int i = 0; i < 7; ++i)
        {
            pattButtons[i] = (Button)v.findViewById(idsButton[i]);
            pattButtons[i].setText(advPatternNames[numsFavorites[i]]);
            pattButtons[i].setOnClickListener(mClicker);
        }

        return v;
    }

    @Override public void onAttach(Context context)
    {
        Log.d(LOGNAME, ">>onAttach");
        super.onAttach(context);
        mListener = (OnFragmentInteractionListener)context;

        SendString(CMD_EXTMODE + "0"); // turn off external properties mode
    }

    @Override public void onDetach()
    {
        Log.d(LOGNAME, ">>onDetach");
        super.onDetach();
        mListener = null;
    }

    View.OnClickListener mClicker = new View.OnClickListener()
    {
        @Override public void onClick(View v)
        {
            Log.d(LOGNAME, "Clicking Favorites Id=" + v.getId());
            //SendPattern(v.getId());
            masterPager.setCurrentItem(1);
        }
    };

    public void onClickFavs(View v)
    {
        Log.d(LOGNAME, "Clicking Favorites Id=" + v.getId());
        //SendPattern(v.getId());
        masterPager.setCurrentItem(1);
    }

    private void SendPattern(int id)
    {
        for (int i = 0; i < 7; ++i)
        {
            if (id == idsButton[i])
            {
                int num = numsFavorites[i] + basicPatternsCount + 1;
                if (numSegments == 1) SendSegPat(num);
                else for (int seg = 1; seg <= numSegments; ++seg)
                {
                    SendString(CMD_SEGS_ENABLE + seg);
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
