package com.devicenut.pixelnutctrl;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class Confirm extends AppCompatActivity
{

    @Override protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_confirm);

        Bundle extras = getIntent().getExtras();
        if (extras != null)
        {
            String str = extras.getString("Text");
            TextView tv = findViewById(R.id.text_Confirm);
            tv.setText(str);
        }
    }

    public void onClick(View v)
    {
        switch (v.getId())
        {
            case R.id.button_ClearCancel:
            {
                finish();
                break;
            }
            case R.id.button_ClearContinue:
            {
                break;
            }
        }
    }

}
