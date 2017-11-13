package com.devicenut.pixelnutctrl;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class FragCtrls extends Fragment
{
    private OnFragmentInteractionListener mListener;

    public FragCtrls() {} // Required empty public constructor

    public static FragCtrls newInstance()
    {
        FragCtrls fragment = new FragCtrls();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
    }

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        return inflater.inflate(R.layout.fragment_ctrls, container, false);
    }

    public void onButtonPressed()
    {
        if (mListener != null)
        {
            mListener.onFragmentInteraction("Hi");
        }
    }

    @Override public void onAttach(Context context)
    {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener)
        {
            mListener = (OnFragmentInteractionListener) context;
        }
        else throw new RuntimeException(context.toString() + " OnFragmentInteractionListener!");
    }

    @Override public void onDetach()
    {
        super.onDetach();
        mListener = null;
    }

    public interface OnFragmentInteractionListener
    {
        void onFragmentInteraction(String s);
    }
}
