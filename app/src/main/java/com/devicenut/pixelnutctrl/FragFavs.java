package com.devicenut.pixelnutctrl;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
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
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static com.devicenut.pixelnutctrl.Main.MAXNUM_FAVORITIES;
import static com.devicenut.pixelnutctrl.Main.appContext;
import static com.devicenut.pixelnutctrl.Main.numPatterns;
import static com.devicenut.pixelnutctrl.Main.numSegments;
import static com.devicenut.pixelnutctrl.Main.numFavorites;
import static com.devicenut.pixelnutctrl.Main.curFavorite;
import static com.devicenut.pixelnutctrl.Main.listFavorites;
import static com.devicenut.pixelnutctrl.Main.createViewFavs;

public class FragFavs extends Fragment
{
    private static final String LOGNAME = "Favorites";

    private static View masterView;
    private static LinearLayout llViewFavs;
    private static ScrollView helpPage;
    private static TextView helpText;

    private static SharedPreferences mySettings = null;
    private static final String prefBaseName = "Favorite";

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

        mySettings = appContext.getSharedPreferences(getString(R.string.app_name), 0);
    }

    @Override public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        Log.d(LOGNAME, ">>onCreateView");

        masterView = inflater.inflate(R.layout.fragment_favs, container, false);

        llViewFavs  = masterView.findViewById(R.id.ll_Favorites);
        helpPage    = masterView.findViewById(R.id.ll_HelpPage_Favs);
        helpText    = masterView.findViewById(R.id.view_HelpText_Favs);

        curFavorite = -1; // clear current choice

        ReadAllFavorites();
        CreateList();

        createViewFavs = true;

        return masterView;
    }

    @Override public void onDestroyView()
    {
        Log.d(LOGNAME, ">>onDestroyView");
        super.onDestroyView();

        llViewFavs = null;
        helpPage = null;
        helpText = null;

        createViewFavs = false;
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
            LinearLayout ll = masterView.findViewById(idsLayout[i]);

            if (i < numFavorites)
            {
                Log.d(LOGNAME, "Listing favorite: " + listFavorites[i].getPatternName());

                Button b = masterView.findViewById(idsChoose[i]);
                b.setTextColor(ContextCompat.getColor(appContext, R.color.UserChoice));
                b.setText(listFavorites[i].getPatternName());
                b.setOnClickListener(mClicker);
                objsChoose[i] = b;

                b = masterView.findViewById(idsCancel[i]);
                if (listFavorites[i].userCreated())
                {
                    b.setVisibility(VISIBLE);
                    b.setOnClickListener(mClicker);
                }
                else b.setVisibility(INVISIBLE);

                ll.setVisibility(VISIBLE);
            }
            else ll.setVisibility(GONE);
        }
    }

    private static void SaveString(int index, String value)
    {
        SharedPreferences.Editor editor = mySettings.edit();
        editor.putString(prefBaseName + index, value);
        editor.apply();
    }

    private boolean HasValidPatternNum(Main.FavoriteInfo f)
    {
        for (int j = 0; j < f.getSegmentCount(); ++j)
        {
            Log.v(LOGNAME, "Checking favorite=" + f.getPatternName() + " segment=" + j + " pattern=" + f.getPatternNum(j));
            if (f.getPatternNum(j) >= numPatterns)
            {
                Log.w(LOGNAME, "Cannot use favorite=" + f.getPatternName());
                return false;
            }
        }
        return true;
    }

    private void ReadAllFavorites()
    {
        for (int i = MAXNUM_FAVORITIES-1; i >= 0 ; --i)
        {
            String s = mySettings.getString(prefBaseName + i, null);
            Main.FavoriteInfo f = new Main.FavoriteInfo(s);

            if (!HasValidPatternNum(f)) continue;

            if (f.data != null)
            {
                Log.d(LOGNAME, "Add favorite: name=" + f.name);
                FavoriteInsert(f);
            }
            else if (s != null) // remove invalid string
            {
                Log.w(LOGNAME, "Removing favorite #" + i + " str=" + s);
                SharedPreferences.Editor editor = mySettings.edit();
                editor.clear();
                editor.apply();
            }
        }
    }

    private void WriteAllFavorites()
    {
        for (int i = 0; i < MAXNUM_FAVORITIES; ++i)
        {
            Main.FavoriteInfo f = listFavorites[i];
            if ((f != null) && !f.builtin)
            {
                Log.d(LOGNAME, "Save favorite: name=" + f.name);
                SaveString(i, f.makeString(null));
            }
            else SaveString(i, null);
        }
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

                    if (curFavorite == i) curFavorite = -1;
                    else if (curFavorite > i) --curFavorite;

                    while (++i < numFavorites)
                        listFavorites[i-1] = listFavorites[i];

                    --numFavorites;

                    CreateList();
                    FavoriteSelect(curFavorite);
                    WriteAllFavorites();
                    break;
                }
            }
        }
    };

    private void FavoriteSelect(int index)
    {
        if (index >= 0)
        {
            Log.d(LOGNAME, "Select favorite #" + index);
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
            Log.d(LOGNAME, "Deselect favorite #" + curFavorite);
            objsChoose[curFavorite].setText(listFavorites[curFavorite].getPatternName());
            objsChoose[curFavorite].setTextColor(ContextCompat.getColor(appContext, R.color.UserChoice));
            curFavorite = -1;
        }
    }

    private void FavoriteInsert(Main.FavoriteInfo f)
    {
        // first remove any other favorite if it matches the name
        String name = f.getPatternName();
        for (int i = 0; i < numFavorites; ++i)
        {
            if (listFavorites[i].getPatternName().equals(name))
            {
                if (i == curFavorite) curFavorite = -1;

                while (++i < numFavorites)
                    listFavorites[i-1] = listFavorites[i];

                --numFavorites;
                break;
            }
        }

        // then insert choice into list of favorites
        for (int i = numFavorites; i > 0; --i)
            listFavorites[i] = listFavorites[i-1];

        listFavorites[0] = f;

        if (numFavorites < MAXNUM_FAVORITIES)
            ++numFavorites;
    }

    // check if newly selected pattern is the same as one of the favorites
    // if so, then select that favorite, otherwise deselect the current one
    // return false if failed to match any of the current favorites
    private static Main.FavoriteInfo savedFavorite;
    public boolean IsFavoritePattern(String name, int seg, int pnum, String vals)
    {
        Log.d(LOGNAME, "IsFavoritePattern: name=" + name + " seg=" + seg + " pnm=" + pnum + " vals=" + vals);

        if (name != null) savedFavorite = new Main.FavoriteInfo(name, seg);
        else
        {
            if (!savedFavorite.addValue(seg, pnum, vals))
                throw new NullPointerException("Favorite Check Failed");

            if (seg == numSegments-1)
            {
                String str1 = savedFavorite.makeString(null);
                for (int i = 0; i < numFavorites; ++i)
                {
                    String str2 = listFavorites[i].makeString(savedFavorite.getPatternName());
                    Log.v(LOGNAME, "Matching str=" + str1 + "\nWith fav=" + str2);
                    if (str1.equals(str2))
                    {
                        FavoriteSelect(i);
                        return true;
                    }
                }

                FavoriteDeselect();
                return false;
            }
        }

        return true;
    }

    // insert new choice into favorites
    public void FavoriteCreate(String name, int seg, int pnum, String vals)
    {
        if (name != null) // first time only
        {
            Log.d(LOGNAME, "Create favorite: name=" + name);
            Main.FavoriteInfo f = new Main.FavoriteInfo(name, numSegments);
            FavoriteInsert(f);
        }
        else
        {
            Log.d(LOGNAME, "Update favorite: seg=" + seg + " pnum=" + pnum + " vals=" + vals);

            if (!listFavorites[0].addValue(seg, pnum, vals))
                throw new NullPointerException("Favorite Create Failed");

            if (seg == numSegments-1) // last one
            {
                CreateList();
                FavoriteSelect(0);
                WriteAllFavorites();
            }
        }
    }
}
