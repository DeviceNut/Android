package com.devicenut.pixelnutctrl;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.devicenut.pixelnutctrl.Main.appContext;
import static com.devicenut.pixelnutctrl.Main.customPatterns;
import static com.devicenut.pixelnutctrl.Main.basicPatternsCount;
import static com.devicenut.pixelnutctrl.Main.devPatternNames;
import static com.devicenut.pixelnutctrl.Main.numsFavorites;
import static com.devicenut.pixelnutctrl.Main.curFavorite;

public class FragFavs extends Fragment
{
    private static final String LOGNAME = "Favorites";

    private static LinearLayout view_Favs;
    private static ScrollView helpPage;
    private static TextView helpText;

    private final int[] idsButton =
            {
                    R.id.button_Pattern1,
                    R.id.button_Pattern2,
                    R.id.button_Pattern3,
                    R.id.button_Pattern4,
                    R.id.button_Pattern5,
                    R.id.button_Pattern6,
            };
    private static Button[] objsButton;

    interface FavoriteSelectInterface
    {
        void onFavoriteSelect(int favnum);
    }
    private FavoriteSelectInterface mListener;

    public FragFavs() {}
    public static FragFavs newInstance() { return new FragFavs(); }

    @Override public void onCreate(Bundle savedInstanceState)
    {
        Log.d(LOGNAME, ">>onCreate");
        super.onCreate(savedInstanceState);
    }

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        Log.d(LOGNAME, ">>onCreateView");

        View v = inflater.inflate(R.layout.fragment_favs, container, false);

        view_Favs   = (LinearLayout) v.findViewById(R.id.ll_Favorites);
        helpPage    = (ScrollView)   v.findViewById(R.id.ll_HelpPage_Favs);
        helpText    = (TextView)     v.findViewById(R.id.view_HelpText_Favs);

        objsButton = new Button[idsButton.length];
        for (int i = 0; i < idsButton.length; ++i)
        {
            Log.w(LOGNAME, "Setting favorite: " + devPatternNames[ numsFavorites[i] + customPatterns + basicPatternsCount ]);
            Button b = (Button)v.findViewById(idsButton[i]);
            b.setText(devPatternNames[ numsFavorites[i] + customPatterns + basicPatternsCount ]);
            b.setOnClickListener(mClicker);
            objsButton[i] = b;
        }

        return v;
    }

    @Override public void onDestroyView()
    {
        Log.d(LOGNAME, ">>onDestroyView");
        super.onDestroyView();

        view_Favs = null;
        helpPage = null;
        helpText = null;
    }

    @Override public void onAttach(Context context)
    {
        Log.d(LOGNAME, ">>onAttach");
        super.onAttach(context);
        mListener = (FavoriteSelectInterface)getActivity();
    }

    @Override public void onDetach()
    {
        Log.d(LOGNAME, ">>onDetach");
        super.onDetach();
        mListener = null;
    }

    public void setHelpMode(boolean enable)
    {
        if (enable)
        {
            view_Favs.setVisibility(GONE);
            helpPage.setVisibility(VISIBLE);

            String str = appContext.getResources().getString(R.string.text_help_head);
            str += appContext.getResources().getString(R.string.text_help_favs);
            helpText.setText(str);
        }
        else
        {
            helpPage.setVisibility(GONE);
            view_Favs.setVisibility(VISIBLE);
        }
    }

    private final View.OnClickListener mClicker = new View.OnClickListener()
    {
        @Override public void onClick(View v)
        {
            int id = v.getId();
            for (int i = 0; i < idsButton.length; ++i)
            {
                if (id == idsButton[i])
                {
                    if (curFavorite == i) break;

                    int pnum = numsFavorites[i] + customPatterns + basicPatternsCount;

                    if (mListener != null)
                        mListener.onFavoriteSelect(pnum);

                    ChangeSelection(i, pnum);
                    break;
                }
            }
        }
    };

    private void ChangeSelection(int findex, int pnum)
    {
        Log.d(LOGNAME, "FavSelect findex=" + findex + " pnum=" + pnum);
        if (curFavorite >= 0)
        {
            int prev = numsFavorites[curFavorite] + customPatterns + basicPatternsCount;
            objsButton[curFavorite].setText(devPatternNames[prev]);
            objsButton[curFavorite].setTextColor(ContextCompat.getColor(appContext, R.color.UserChoice));
        }

        if (findex >= 0)
        {
            objsButton[findex].setText(">>> " + devPatternNames[pnum] + " <<<");
            objsButton[findex].setTextColor(ContextCompat.getColor(appContext, R.color.HighLight));
        }

        curFavorite = findex;
    }

    public void onPatternSelect(int pnum)
    {
        for (int i = 0; i < numsFavorites.length; ++i)
        {
            if (numsFavorites[i] == pnum)
            {
                ChangeSelection(i, pnum + customPatterns + basicPatternsCount);
                return;
            }
        }

        ChangeSelection(-1,0); // deselect current choice
    }
}
