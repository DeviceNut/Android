package com.devicenut.pixelnutctrl;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class FragFavs extends Fragment
{
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
        super.onCreate(savedInstanceState);
    }

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        return inflater.inflate(R.layout.fragment_favs, container, false);
    }

    @Override public void onAttach(Context context)
    {
        super.onAttach(context);
        mListener = (OnFragmentInteractionListener)context;
    }

    @Override public void onDetach()
    {
        super.onDetach();
        mListener = null;
    }

    public void onButtonPressed()
    {
        if (mListener != null)
        {
            mListener.onFragmentInteraction("Hi");
        }
    }
}
