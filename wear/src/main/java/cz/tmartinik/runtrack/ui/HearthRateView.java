package cz.tmartinik.runtrack.ui;


import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.view.WindowInsets;

/**
 * Created by tmartinik on 15.12.2017.
 */

public class HearthRateView extends View {
    public HearthRateView(Context context) {
        super(context);
    }

    public HearthRateView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public HearthRateView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public HearthRateView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    public WindowInsets onApplyWindowInsets(WindowInsets insets) {
        insets.getSystemWindowInsetBottom();
        return super.onApplyWindowInsets(insets);
    }

    @Override
    protected void onDraw(Canvas canvas) {

        int centerX = getWidth() / 2;
        int centerY = getHeight() / 2;

        Paint paint = new Paint();
        RectF rectF = new RectF(50, 20, 100, 80);
        paint.setColor(Color.WHITE);
        canvas.drawArc (rectF, 90, 45, true, paint);
    }
}
