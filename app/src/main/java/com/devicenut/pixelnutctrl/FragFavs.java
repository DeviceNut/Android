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
import static com.devicenut.pixelnutctrl.Main.FAVTYPE_ADV;
import static com.devicenut.pixelnutctrl.Main.FAVTYPE_BASIC;
import static com.devicenut.pixelnutctrl.Main.advPatternsCount;
import static com.devicenut.pixelnutctrl.Main.appContext;
import static com.devicenut.pixelnutctrl.Main.basicPatternsCount;
import static com.devicenut.pixelnutctrl.Main.numSegments;
import static com.devicenut.pixelnutctrl.Main.numFavorites;
import static com.devicenut.pixelnutctrl.Main.curFavorite;
import static com.devicenut.pixelnutctrl.Main.listFavorites;

public class FragFavs extends Fragment
{
    private static final String LOGNAME = "Favorites";

    private static View masterView;
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
                    R.id.button_FavCancel1,
                    R.id.button_FavCancel2,
                    R.id.button_FavCancel3,
                    R.id.button_FavCancel4,
                    R.id.button_FavCancel5,
                    R.id.button_FavCancel6,
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

        masterView = inflater.inflate(R.layout.fragment_favs, container, false);

        llViewFavs  = (LinearLayout) masterView.findViewById(R.id.ll_Favorites);
        helpPage    = (ScrollView)   masterView.findViewById(R.id.ll_HelpPage_Favs);
        helpText    = (TextView)     masterView.findViewById(R.id.view_HelpText_Favs);

        CreateList();
        return masterView;
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

    private void CreateList()
    {
        objsChoose = new Button[numFavorites];

        for (int i = 0; i < idsLayout.length; ++i)
        {
            LinearLayout ll = (LinearLayout)masterView.findViewById(idsLayout[i]);

            if (i < numFavorites)
            {
                Log.w(LOGNAME, "Setting favorite: " + listFavorites[i].getPatternName());

                Button b = (Button)masterView.findViewById(idsChoose[i]);
                b.setTextColor(ContextCompat.getColor(appContext, R.color.UserChoice));
                b.setText(listFavorites[i].getPatternName());
                b.setOnClickListener(mClicker);
                objsChoose[i] = b;

                b = (Button)masterView.findViewById(idsCancel[i]);
                b.setOnClickListener(mClicker);

                ll.setVisibility(VISIBLE);
            }
            else ll.setVisibility(GONE);
        }
    }

    private void ReadList()
    {

    }

    private void WriteList()
    {

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
                    if (curFavorite != i)
                    {
                        FavoriteDeselect();

                        for (int j = 0; j < numSegments; ++j)
                            listenFavoriteSelect.onFavoriteSelect(j,
                                listFavorites[i].getPatternNum(j),
                                listFavorites[i].getPatternVals(j));

                        FavoriteSelect(i);
                    }
                    break;
                }
                else if (id == idsCancel[i]) // remove choice from list of favorites
                {
                    listFavorites[i] = null;
                    if (i == curFavorite) curFavorite = -1;

                    while (++i < numFavorites)
                        listFavorites[i-1] = listFavorites[i];

                    --numFavorites;
                    CreateList();
                    FavoriteSelect(curFavorite);
                    break;
                }
            }
        }
    };

    private void FavoriteSelect(int index)
    {
        if (index >= 0)
        {
            Log.d(LOGNAME, "Select Favorite #" + index);
            objsChoose[index].setText(">>> " + listFavorites[index].getPatternName() + " <<<");
            objsChoose[index].setTextColor(ContextCompat.getColor(appContext, R.color.HighLight));
            curFavorite = index;
        }
    }

    // deselect current choice
    public void FavoriteDeselect()
    {
        if (curFavorite >= 0)
        {
            Log.d(LOGNAME, "Deselect Favorite #" + curFavorite);
            objsChoose[curFavorite].setText(listFavorites[curFavorite].getPatternName());
            objsChoose[curFavorite].setTextColor(ContextCompat.getColor(appContext, R.color.UserChoice));
            curFavorite = -1;
        }
    }

    // insert new choice into favorites
    public void FavoriteCreate(String name, int seg, int pnum, String vals)
    {
        if (name != null) // first time only
        {
            // insert choice into list of favorites
            for (int i = numFavorites; i > 0; --i)
                listFavorites[i] = listFavorites[i-1];

            Log.d(LOGNAME, "Create Favorite: name=" + name);
            listFavorites[0] = new Main.FavoriteInfo(name, numSegments);
        }
        else
        {
            int type = FAVTYPE_BASIC;
            if (pnum < (basicPatternsCount + advPatternsCount))
            {
                type = FAVTYPE_ADV;
                pnum -= basicPatternsCount;
            }
            else if (pnum >= basicPatternsCount)
                throw new NullPointerException("Invalid pattern number=" + pnum);

            Log.d(LOGNAME, "Update Favorite: seg=" + seg + " type.pnum=" + type + "." + pnum + " vals=" + vals);
            listFavorites[0].addValue(seg, type, pnum, vals);

            if (seg == numSegments-1) // last one
            {
                ++numFavorites;
                CreateList();
                FavoriteSelect(0);
                WriteList();
            }
        }
    }
}
