package com.devicenut.pixelnutctrl;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class FragAdv extends Fragment
{
    private final String LOGNAME = "Controls";
    private Activity context;

    public FragAdv() {}
    public static FragAdv newInstance() { return new FragAdv(); }

    @Override public void onCreate(Bundle savedInstanceState)
    {
        Log.d(LOGNAME, ">>onCreate");
        super.onCreate(savedInstanceState);
    }

    @Override public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        Log.d(LOGNAME, ">>onCreateView");
        context = this.getActivity(); // not valid until now

        return inflater.inflate(R.layout.fragment_adv, container, false);
    }

    @Override public void onAttach(Context context)
    {
        Log.d(LOGNAME, ">>onAttach");
        super.onAttach(context);
        //mListener = (FragListen)getActivity();
    }

    @Override public void onDetach()
    {
        Log.d(LOGNAME, ">>onDetach");
        super.onDetach();
        //mListener = null;
    }

}
