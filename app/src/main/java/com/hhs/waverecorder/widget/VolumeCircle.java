package com.hhs.waverecorder.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;


public class VolumeCircle extends View {
    int level = 0; // max 100
    float scale = (float) 1.0;
    public VolumeCircle(Context context, int level, int dpi) {
        super(context);
        scale = dpi / 160;
        setLevel(level);
    }

    public VolumeCircle(Context context) {
        super(context);
    }

    @SuppressLint("DrawAllocation")
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        Paint pen = new Paint();
        pen.setStyle(Paint.Style.FILL);
        pen.setColor(Color.GRAY);
        pen.setAlpha(64);

        float x = (55 + 64) * scale;
        float r = (float) ((34 + 0.8 * level) * scale);

        canvas.drawCircle(x, x, r, pen);
    }

    public void setLevel(int level) {
        this.level = (level < 0) ? 0 : level;
        this.level = (level > 100) ? 100 : level;

        // re-draw
        invalidate();
    }

    public int getLevel() {
        return level;
    }
}
