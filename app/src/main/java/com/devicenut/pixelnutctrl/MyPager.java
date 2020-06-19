package com.devicenut.pixelnutctrl;

import android.content.Context;
import androidx.viewpager.widget.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class MyPager extends ViewPager
{
    private final boolean enabled;

    public MyPager(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        this.enabled = false;
    }

    @Override public boolean performClick()
    {
        return this.enabled && super.performClick();
    }

    @Override public boolean onTouchEvent(MotionEvent event)
    {
        return this.enabled && super.onTouchEvent(event);
    }

    @Override public boolean onInterceptTouchEvent(MotionEvent event)
    {
        return this.enabled && super.onInterceptTouchEvent(event);
    }
}
