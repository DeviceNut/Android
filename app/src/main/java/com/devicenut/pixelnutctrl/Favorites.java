package com.devicenut.pixelnutctrl;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import static com.devicenut.pixelnutctrl.Bluetooth.BLESTAT_DISCONNECTED;
import static com.devicenut.pixelnutctrl.Main.CMD_BLUENAME;
import static com.devicenut.pixelnutctrl.Main.CMD_EXTMODE;
import static com.devicenut.pixelnutctrl.Main.CMD_PAUSE;
import static com.devicenut.pixelnutctrl.Main.CMD_POP_PATTERN;
import static com.devicenut.pixelnutctrl.Main.CMD_RESUME;
import static com.devicenut.pixelnutctrl.Main.CMD_SEGS_ENABLE;
import static com.devicenut.pixelnutctrl.Main.CMD_START_END;
import static com.devicenut.pixelnutctrl.Main.TITLE_NONAME;
import static com.devicenut.pixelnutctrl.Main.TITLE_PIXELNUT;
import static com.devicenut.pixelnutctrl.Main.advPatternNames;
import static com.devicenut.pixelnutctrl.Main.basicPatternsCount;
import static com.devicenut.pixelnutctrl.Main.ble;
import static com.devicenut.pixelnutctrl.Main.devName;
import static com.devicenut.pixelnutctrl.Main.devPatternCmds;
import static com.devicenut.pixelnutctrl.Main.doUpdate;
import static com.devicenut.pixelnutctrl.Main.numSegments;
import static com.devicenut.pixelnutctrl.Main.numsFavorites;

public class Favorites extends AppCompatActivity implements Bluetooth.BleCallbacks
{
    private final String LOGNAME = "Favorites";
    private final Activity context = this;

    private boolean isConnected = false;
    private boolean isEditing = false;

    private TextView nameText;
    private Button pauseButton;

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

    @Override protected void onCreate(Bundle savedInstanceState)
    {
        Log.d(LOGNAME, ">>onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);

        nameText = (TextView)findViewById(R.id.text_Devname1);
        pauseButton = (Button)findViewById(R.id.button_Pause1);

        for (int i = 0; i < 7; ++i)
        {
            pattButtons[i] = (Button)findViewById(idsButton[i]);
            pattButtons[i].setText(advPatternNames[numsFavorites[i]]);
        }
    }

    @Override protected void onResume()
    {
        Log.d(LOGNAME, ">>onResume");
        super.onResume();

        if (isEditing && (ble != null))
        {
            isEditing = false;
            Log.d(LOGNAME, "Renaming device: " + devName);
            SendString(CMD_BLUENAME + devName);

            if (Build.VERSION.SDK_INT < 23)
            {
                //ble.refreshDeviceCache(); // doesn't work FIXME

                Toast.makeText(context, "Rescan from Settings to see name change", Toast.LENGTH_SHORT).show();
            }
        }
        else
        {
            ble = new Bluetooth(this);
            ble.setCallbacks(this);

            devName = ble.getCurDevName();
            if ((devName == null) || (devName.length() < 3)) // have disconnected
            {
                Log.w(LOGNAME, "Lost connection (no device name)");
                Toast.makeText(context, "Lost connection", Toast.LENGTH_SHORT).show();
                onBackPressed();
                return;
            }

            isConnected = true;

            if (devName.startsWith(TITLE_PIXELNUT))
                 devName = devName.substring(2);
            else devName = TITLE_NONAME;
            Log.d(LOGNAME, "Device name: " + devName);

            // set pause button to correct state
            pauseButton.setText(getResources().getString(doUpdate ? R.string.name_pause : R.string.name_resume));

            SendString(CMD_EXTMODE + "0"); // turn off external properties mode
        }
        nameText.setText(devName);
    }

    @Override public void onBackPressed()
    {
        super.onBackPressed();
    }

    public void onClick(View v)
    {
        switch (v.getId())
        {
            case R.id.text_Devname:
            {
                isEditing = true;
                startActivity( new Intent(Favorites.this, EditName.class) );
                break;
            }
            case R.id.button_Pause1:
            {
                if (doUpdate)
                {
                    SendString(CMD_PAUSE);
                    pauseButton.setText(getResources().getString(R.string.name_resume));
                }
                else
                {
                    SendString(CMD_RESUME);
                    pauseButton.setText(getResources().getString(R.string.name_pause));
                }
                doUpdate = !doUpdate;
                break;
            }
            default:
            {
                SendPattern(v.getId());
                break;
            }
        }
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
        ble.WriteString(str);
    }

    private void DeviceDisconnect(final String reason)
    {
        Log.v(LOGNAME, "Device disconnect: reason=" + reason + " connected=" + isConnected);
        if (isConnected)
        {
            isConnected = false;
            context.runOnUiThread(new Runnable()
            {
                public void run()
                {
                    Toast.makeText(context, "Disconnect: " + reason, Toast.LENGTH_SHORT).show();

                    Log.v(LOGNAME, "Finishing controls activity...");
                    finish();
                }
            });
        }
    }

    @Override public void onScan(String name, int id)
    {
        Log.e(LOGNAME, "Unexpected callback: onScan");
    }

    @Override public void onConnect(final int status)
    {
        Log.e(LOGNAME, "Unexpected callback: onConnect");
    }

    @Override public void onDisconnect()
    {
        Log.i(LOGNAME, "Received disconnect");
        DeviceDisconnect("Request");
    }

    @Override public void onWrite(final int status)
    {
        if ((status != 0) && (status != BLESTAT_DISCONNECTED))
        {
            Log.e(LOGNAME, "Write status: " + status); //Integer.toHexString(status));
            DeviceDisconnect("Write");
        }
    }

    @Override public void onRead(String reply)
    {
        Log.e(LOGNAME, "Unexpected onRead");
    }
}
