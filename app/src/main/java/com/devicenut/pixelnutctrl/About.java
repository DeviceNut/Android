package com.devicenut.pixelnutctrl;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

public class About extends AppCompatActivity
{
    @Override protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_about);

        TextView mVersion = findViewById(R.id.Text_Version);
        mVersion.setText( BuildConfig.VERSION_NAME );
    }
}
