package com.hhs.waverecorder.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.DisplayMetrics;
import android.view.View;


public class VolumeCircle extends View {
    int level = 0; // max 100
    float scale;
    public VolumeCircle(Context context, int level) {
        super(context);
        scale = getDensity(context);
        setLevel(level);
    }

    public VolumeCircle(Context context) {
        super(context);
        scale = getDensity(context);
    }

    @SuppressLint("DrawAllocation")
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        Paint pen = new Paint();
        pen.setStyle(Paint.Style.FILL);
        pen.setColor(Color.GRAY);
        pen.setAlpha(64);

        float x = convertDpToPixel((float) (55. + 64.));
        float r = convertDpToPixel((float) (34 + 0.8 * level));

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

    public float convertDpToPixel(float dp){
        return dp * scale;
    }

    /**
     * Covert dp to px
     * @param dp
     * @param context
     * @return pixel
     */
    public static float convertDpToPixel(float dp, Context context){
        float px = dp * getDensity(context);
        return px;
    }

    /**
     * Covert px to dp
     * @param px
     * @param context
     * @return dp
     */
    public static float convertPixelToDp(float px, Context context){
        float dp = px / getDensity(context);
        return dp;
    }

    /**
     * 取得螢幕密度
     * 120dpi = 0.75
     * 160dpi = 1 (default)
     * 240dpi = 1.5
     * @param context
     * @return
     */
    public static float getDensity(Context context){
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return metrics.density;
    }
}
