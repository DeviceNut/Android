package com.devicenut.pixelnutctrl;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;

import static com.devicenut.pixelnutctrl.Main.CMD_BRIGHT;
import static com.devicenut.pixelnutctrl.Main.CMD_DELAY;
import static com.devicenut.pixelnutctrl.Main.CMD_PROPVALS;
import static com.devicenut.pixelnutctrl.Main.CMD_SEGS_ENABLE;
import static com.devicenut.pixelnutctrl.Main.MAXVAL_HUE;
import static com.devicenut.pixelnutctrl.Main.MAXVAL_PERCENT;
import static com.devicenut.pixelnutctrl.Main.MAXVAL_WHT;
import static com.devicenut.pixelnutctrl.Main.curBright;
import static com.devicenut.pixelnutctrl.Main.curDelay;
import static com.devicenut.pixelnutctrl.Main.curSegment;
import static com.devicenut.pixelnutctrl.Main.multiStrands;
import static com.devicenut.pixelnutctrl.Main.numSegments;
import static com.devicenut.pixelnutctrl.Main.rangeDelay;
import static com.devicenut.pixelnutctrl.Main.segTrigForce;
import static com.devicenut.pixelnutctrl.Main.segXmodeCnt;
import static com.devicenut.pixelnutctrl.Main.segXmodeHue;
import static com.devicenut.pixelnutctrl.Main.segXmodeWht;

public class FragCtrls extends Fragment implements SeekBar.OnSeekBarChangeListener
{
    private boolean useSegEnables = false;
    private final boolean segEnables[] = { false, false, false, false, false };

    private OnFragmentInteractionListener mListener;

    public interface OnFragmentInteractionListener
    {
        void onFragmentInteraction(String s);
    }

    public FragCtrls() {}

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

    private void SendString(String str)
    {
        if (mListener != null)
            mListener.onFragmentInteraction(str);
    }

    @Override public void onStartTrackingTouch(SeekBar seekBar) {}
    @Override public void onStopTrackingTouch(SeekBar seekBar)  {}

    @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
    {
        switch (seekBar.getId())
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
                    segXmodeHue[curSegment] = (progress * MAXVAL_HUE) / MAXVAL_PERCENT;
                    SendString(CMD_PROPVALS + segXmodeHue[curSegment] + " " + segXmodeWht[curSegment] + " " + segXmodeCnt[curSegment]);
                }
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
                    segXmodeWht[curSegment] = (progress * MAXVAL_WHT) / MAXVAL_PERCENT;
                    SendString(CMD_PROPVALS + segXmodeHue[curSegment] + " " + segXmodeWht[curSegment] + " " + segXmodeCnt[curSegment]);
                }
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
                    segXmodeCnt[curSegment] = progress;
                    SendString(CMD_PROPVALS + segXmodeHue[curSegment] + " " + segXmodeWht[curSegment] + " " + segXmodeCnt[curSegment]);
                }
                break;
            }
            case R.id.seek_TrigForce:
            {
                int val = (10 * progress);
                if (useSegEnables)
                {
                    for (int i = 0; i < numSegments; ++i)
                    {
                        if (segEnables[i]) segTrigForce[i] = val;
                    }
                }
                else segTrigForce[curSegment] = val;
                break;
            }
        }
    }

}
