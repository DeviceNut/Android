package com.devicenut.pixelnutctrl;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import static android.view.View.GONE;
import static com.devicenut.pixelnutctrl.Main.devIsBLE;
import static com.devicenut.pixelnutctrl.Main.devName;

public class EditName extends AppCompatActivity
{
    private final String LOGNAME = "EditName";
    private EditText editName;
    private String saveName;

    @Override protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_name);

        editName = findViewById(R.id.edit_DevName);
        editName.setOnEditorActionListener(new TextView.OnEditorActionListener()
        {
            @Override public boolean onEditorAction(TextView v, int actionId, KeyEvent event)
            {
                if (actionId == EditorInfo.IME_ACTION_DONE) SaveName();
                finish();
                return false;
            }
        });

        saveName = devName;
        editName.setText(devName);

        if (devIsBLE) findViewById(R.id.ll_AddNetwork).setVisibility(GONE);

        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
    }

    private void SaveName()
    {
        if ((editName.length() > 0) && !devName.equals(editName.getText().toString()))
            devName = editName.getText().toString();
    }

    private void DoExit()
    {
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.toggleSoftInput(InputMethodManager.RESULT_HIDDEN, 0);
        finish();
    }

    public void onClick(View v)
    {
        switch (v.getId())
        {
            case R.id.button_EditCancel:
            {
                devName = saveName;
                DoExit();
                break;
            }
            case R.id.button_EditDone:
            {
                SaveName();
                DoExit();
                break;
            }
            case R.id.button_AddNetwork:
            {
                Log.d(LOGNAME, "Add network SSID and Passphrase");
                break;
            }
        }
    }

}
