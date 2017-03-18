package com.devicenut.pixelnutctrl;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import static com.devicenut.pixelnutctrl.Main.devName;

public class EditName extends AppCompatActivity
{
    private EditText editName;
    private String saveName;

    @Override protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_name);

        editName = (EditText) findViewById(R.id.edit_DevName);
        editName.setOnEditorActionListener(new TextView.OnEditorActionListener()
        {
            @Override public boolean onEditorAction(TextView v, int actionId, KeyEvent event)
            {
                if (actionId == EditorInfo.IME_ACTION_DONE) SaveName();
                return false;
            }
        });

        saveName = devName;
        editName.setText(devName);

        ((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE)).toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
    }

    private void SaveName()
    {
        if ((editName.length() > 0) && !devName.equals(editName.getText().toString()))
        {
            devName = editName.getText().toString();
            onBackPressed();
        }
    }

    public void onClick(View v)
    {
        switch (v.getId())
        {
            case R.id.button_EditCancel:
            {
                devName = saveName;
                onBackPressed();
                break;
            }
            case R.id.button_EditDone:
            {
                SaveName();
                break;
            }
        }
    }

}
