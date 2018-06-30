package com.devicenut.pixelnutctrl;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class MyPager extends ViewPager
{
    private boolean enabled;

    public MyPager(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        this.enabled = false;
    }

    @Override public boolean onTouchEvent(MotionEvent event)
    {
        return this.enabled && super.onTouchEvent(event);
    }

    @Override public boolean onInterceptTouchEvent(MotionEvent event)
    {
        return this.enabled && super.onInterceptTouchEvent(event);
    }

    public void setPagingEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }
}
