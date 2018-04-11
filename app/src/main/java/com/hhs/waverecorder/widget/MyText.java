package com.hhs.waverecorder.widget;

import android.content.Context;
import android.util.AttributeSet;

import com.hhs.waverecorder.listener.CursorChangedListener;


public class MyText extends android.support.v7.widget.AppCompatEditText {
    private CursorChangedListener listener;
    public MyText(Context context) {
        super(context);
    }
    public MyText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    public MyText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setOnCursorChangedListener(CursorChangedListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onSelectionChanged(int selStart, int selEnd) {
        super.onSelectionChanged(selStart, selEnd);
        int pos = (selEnd - 1 >= 0) ? selEnd - 1 : 0;
        if (listener != null)
            listener.onCursorChanged(pos);
    }
}
