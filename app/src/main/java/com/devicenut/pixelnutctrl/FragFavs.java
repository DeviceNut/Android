package com.devicenut.pixelnutctrl;

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
import static com.devicenut.pixelnutctrl.Main.numSegments;
import static com.devicenut.pixelnutctrl.Main.numFavorites;
import static com.devicenut.pixelnutctrl.Main.curFavorite;
import static com.devicenut.pixelnutctrl.Main.listFavorites;

public class FragFavs extends Fragment
{
    private static final String LOGNAME = "Favorites";

    private static LinearLayout llViewFavs;
    private static ScrollView helpPage;
    private static TextView helpText;

    private final int[] idsLayout =
            {
                    R.id.ll_Favorite1,
                    R.id.ll_Favorite2,
                    R.id.ll_Favorite3,
                    R.id.ll_Favorite4,
                    R.id.ll_Favorite5,
                    R.id.ll_Favorite6,
            };
    private static LinearLayout[] objsLayout;

    private final int[] idsChoose =
            {
                    R.id.button_Favorite1,
                    R.id.button_Favorite2,
                    R.id.button_Favorite3,
                    R.id.button_Favorite4,
                    R.id.button_Favorite5,
                    R.id.button_Favorite6,
            };
    private static Button[] objsChoose;

    private final int[] idsCancel =
            {
                    R.id.button_Favorite1,
                    R.id.button_Favorite2,
                    R.id.button_Favorite3,
                    R.id.button_Favorite4,
                    R.id.button_Favorite5,
                    R.id.button_Favorite6,
            };

    interface FavoriteSelectInterface
    {
        void onFavoriteSelect(int seg, int pnum, String vals);
    }
    private FavoriteSelectInterface listenFavoriteSelect;

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

        llViewFavs  = (LinearLayout) v.findViewById(R.id.ll_Favorites);
        helpPage    = (ScrollView)   v.findViewById(R.id.ll_HelpPage_Favs);
        helpText    = (TextView)     v.findViewById(R.id.view_HelpText_Favs);

        objsChoose = new Button[numFavorites];
        objsLayout = new LinearLayout[numFavorites];

        for (int i = 0; i < numFavorites; ++i)
        {
            if (i >= idsChoose.length) break; // insure button is in the layout
            Log.w(LOGNAME, "Setting favorite: " + listFavorites[i].getPatternName());

            Button b = (Button)v.findViewById(idsChoose[i]);
            b.setText(listFavorites[i].getPatternName());
            b.setOnClickListener(mClicker);
            objsChoose[i] = b;

            b = (Button)v.findViewById(idsCancel[i]);
            b.setOnClickListener(mClicker);

            LinearLayout ll = (LinearLayout)v.findViewById(idsLayout[i]);
            ll.setVisibility(VISIBLE);
            objsLayout[i] = ll;
        }

        return v;
    }

    @Override public void onDestroyView()
    {
        Log.d(LOGNAME, ">>onDestroyView");
        super.onDestroyView();

        llViewFavs = null;
        helpPage = null;
        helpText = null;
    }

    @Override public void onAttach(Context context)
    {
        Log.d(LOGNAME, ">>onAttach");
        super.onAttach(context);
        listenFavoriteSelect = (FavoriteSelectInterface)getActivity();
    }

    @Override public void onDetach()
    {
        Log.d(LOGNAME, ">>onDetach");
        super.onDetach();
        listenFavoriteSelect = null;
    }

    public void setHelpMode(boolean enable)
    {
        if (enable)
        {
            llViewFavs.setVisibility(GONE);
            helpPage.setVisibility(VISIBLE);

            String str = appContext.getResources().getString(R.string.text_help_head);
            str += appContext.getResources().getString(R.string.text_help_favs);
            helpText.setText(str);
        }
        else
        {
            helpPage.setVisibility(GONE);
            llViewFavs.setVisibility(VISIBLE);
        }
    }

    private final View.OnClickListener mClicker = new View.OnClickListener()
    {
        @Override public void onClick(View v)
        {
            int id = v.getId();
            for (int i = 0; i < numFavorites; ++i)
            {
                if (id == idsChoose[i])
                {
                    DeselectChoice();
                    if (curFavorite != i)
                    {
                        for (int j = 0; j < numSegments; ++j)
                            listenFavoriteSelect.onFavoriteSelect(j,
                                listFavorites[i].getPatternNum(j),
                                listFavorites[i].getPatternVals(j));

                        objsChoose[i].setText(">>> " + listFavorites[i].getPatternName() + " <<<");
                        objsChoose[i].setTextColor(ContextCompat.getColor(appContext, R.color.HighLight));
                        curFavorite = i;
                    }
                    break;
                }
                else if (id == idsCancel[i])
                {
                    break;
                }
            }
        }
    };

    private void DeselectChoice()
    {
        if (curFavorite >= 0)
        {
            objsChoose[curFavorite].setText(listFavorites[curFavorite].getPatternName());
            objsChoose[curFavorite].setTextColor(ContextCompat.getColor(appContext, R.color.UserChoice));
            curFavorite = -1;
        }
    }

    public void onPatternSelect()
    {
        DeselectChoice(); // deselect current choice
    }
}
