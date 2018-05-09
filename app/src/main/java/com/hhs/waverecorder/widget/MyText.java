package com.hhs.waverecorder.widget;

import android.content.Context;
import android.util.AttributeSet;

import com.hhs.waverecorder.listener.OnCursorChangedListener;


public class MyText extends android.support.v7.widget.AppCompatEditText {
    private OnCursorChangedListener listener;
    public MyText(Context context) {
        super(context);
    }
    public MyText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    public MyText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setOnCursorChangedListener(OnCursorChangedListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onSelectionChanged(int selStart, int selEnd) {
        super.onSelectionChanged(selStart, selEnd);
        if (selStart == selEnd) {
            if (listener != null)
                listener.onCursorChanged(this);
        }
    }
}
