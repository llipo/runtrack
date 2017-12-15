package cz.tmartinik.runtrack.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Property;
import android.view.View;
import android.view.ViewGroup;

import cz.tmartinik.runtrack.R;


public class FitDoughnut extends ViewGroup {


    private FitDoughnutView fitDoughnutView;

    private Paint paintPrimary;
    private Paint paintSecondary;

    private final RectF oval = new RectF();
    private float width;

    private int colorPrimary;
    private int colorSecondary;

    // Percent
    // stored internally as float between 0..360 (degrees)
    // exposed through setter and getter as 0..100 (percent)
    // overflow with modulo allows for continuous looping animations
    private float percentDeg;
    public float getPercent() { return (percentDeg/ 360.f) * 100.f; }
    public void setPercent(float percent) { percentDeg = ((percent % 100)/ 100.f) * 360.f; }
    private final Property<FitDoughnut, Float> percentProperty = new Property<FitDoughnut, Float>(Float.class, "Percent") {
        @Override public Float get(FitDoughnut fd) { return fd.getPercent(); }
        @Override public void set(FitDoughnut fd, Float value) { fd.setPercent(value); }
    };


    // OriginAngle
    // used for animations
    // range 0..360
    // overflow loops with modulo
    // offset by 270deg i.e. 0deg is 12 o'clock, 90deg is 3 o'clock
    private float originAngle = 0;
    private float getOriginAngle() { return (originAngle + 270) % 360; }
    private void setOriginAngle(Float value) { originAngle = (value % 360); }
    private final Property<FitDoughnut, Float> originAngleProperty = new Property<FitDoughnut, Float>(Float.class, "OriginAngle") {
        @Override public Float get(FitDoughnut fd) { return fd.getOriginAngle(); }
        @Override public void set(FitDoughnut fd, Float value) { fd.setOriginAngle(value); }
    };

    public FitDoughnut(Context ctx) {
        super(ctx);
        init();
    }

    public FitDoughnut(Context ctx, AttributeSet attrs) {
        super(ctx, attrs);

        TypedArray a = ctx.getTheme().obtainStyledAttributes(attrs, R.styleable.FitDoughnut, 0, 0);

        // attempt to get any values from the xml
        try {
            colorPrimary = a.getColor(R.styleable.FitDoughnut_fdColorPrimary, Color.rgb(225, 140, 80));
            colorSecondary = a.getColor(R.styleable.FitDoughnut_fdColorSecondary, Color.rgb(200,200,200));
        } finally {
            a.recycle();
        }

        init();
    }



    private void init() {

        fitDoughnutView = new FitDoughnutView(getContext());
        addView(fitDoughnutView);

        // setup paint
        paintPrimary = new Paint();
        paintPrimary.setAntiAlias(true);
        paintPrimary.setColor(colorPrimary);
        paintPrimary.setStyle(Paint.Style.STROKE);
        paintPrimary.setStrokeCap(Paint.Cap.ROUND);

        paintSecondary = new Paint();
        paintSecondary.setAntiAlias(true);
        paintSecondary.setColor(colorSecondary);
        paintSecondary.setStyle(Paint.Style.STROKE);
    }

    @Override
    protected void onLayout(boolean b, int i, int i2, int i3, int i4) {
        // do nothing
        // don't call super.onLayout() -- this would cause a layout pass on the children
        // children will lay out in onSizeChanged()
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        // account for padding
        float xpad = (float) (getPaddingLeft() + getPaddingRight());
        float ypad = (float) (getPaddingTop() + getPaddingBottom());

        // figure out how big the doughnut can be
        float ww = (float) w - xpad;
        float hh = (float) h - ypad;
        float diameter = Math.max(ww, hh);

        oval.set(0.f, 0.f, diameter, diameter);
        oval.offsetTo(getPaddingLeft(), getPaddingTop());

        // set stroke width..
        width = 8;//diameter / 15.f;
        paintPrimary.setStrokeWidth(width);
        paintSecondary.setStrokeWidth(width);

        // lay out the child view that actually draws the doughnut
        fitDoughnutView.layout((int) oval.left, (int) oval.top, (int) oval.right, (int) oval.bottom);
    }



//    @Override
//    public WindowInsets onApplyWindowInsets(WindowInsets insets) {
//        int chin = insets.getSystemWindowInsetBottom();
//        setPadding(0, 0, 0, -chin);
//        return insets;
//    }

    //endregion


    //region FitDoughnutView

    /**
     * FitDoughnutView class
     */
    class FitDoughnutView extends View {

        private final RectF fdvOval = new RectF();

        public FitDoughnutView(Context ctx) {
            super(ctx);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            //canvas.drawText("PAINT TEXT PRIMARY", oval.left, oval.top, paintTextPrimary);

            // draw the background doughnut
            canvas.drawArc(fdvOval, 0, 360, false, paintSecondary);

            // draw the main ring on top
            canvas.drawArc(fdvOval, getOriginAngle(), percentDeg, false, paintPrimary);
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            fdvOval.set(width, width /*[SIC]*/, w - width, h - width);
        }
    }

    //endregion
}
