package com.ronda.calcul;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.LinearInterpolator;
import androidx.annotation.Nullable;

public class FloatingButton extends View {

    private Paint paint;
    private float centerX, centerY;
    private float radius;
    private ValueAnimator pulseAnimator;
    private float pulseScale = 1f;

    private OnClickListener clickListener;
    private OnLongClickListener longClickListener;

    public FloatingButton(Context context) {
        super(context);
        init();
    }

    public FloatingButton(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public FloatingButton(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL);

        // ⚡ Animation نبض
        pulseAnimator = ValueAnimator.ofFloat(1f, 1.15f, 1f);
        pulseAnimator.setDuration(1500);
        pulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        pulseAnimator.setInterpolator(new LinearInterpolator());
        pulseAnimator.addUpdateListener(animation -> {
            pulseScale = (float) animation.getAnimatedValue();
            invalidate();
        });
        pulseAnimator.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        centerX = getWidth() / 2f;
        centerY = getHeight() / 2f;
        radius = Math.min(centerX, centerY) * 0.85f * pulseScale;

        // ⚡ تدرج لوني (Gradient)
        RadialGradient gradient = new RadialGradient(
            centerX, centerY, radius,
            new int[]{
                Color.parseColor("#FF4488FF"),
                Color.parseColor("#FF2266DD"),
                Color.parseColor("#FF1155AA")
            },
            new float[]{0f, 0.7f, 1f},
            Shader.TileMode.CLAMP
        );
        paint.setShader(gradient);

        // ⚡ نبضو الدائرة
        canvas.drawCircle(centerX, centerY, radius, paint);

        // ⚡ الحلقة البيضا
        paint.setShader(null);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(6f);
        paint.setColor(Color.WHITE);
        canvas.drawCircle(centerX, centerY, radius, paint);

        // ⚡ الأيقونة (📌)
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        paint.setTextSize(radius * 1.0f);
        paint.setTextAlign(Paint.Align.CENTER);

        float textY = centerY - (paint.descent() + paint.ascent()) / 2;
        canvas.drawText("📌", centerX, textY, paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                pulseScale = 0.9f;
                invalidate();
                return true;
            case MotionEvent.ACTION_UP:
                pulseScale = 1f;
                invalidate();
                if (clickListener != null) {
                    clickListener.onClick(this);
                }
                return true;
        }
        return super.onTouchEvent(event);
    }

    @Override
    public void setOnClickListener(@Nullable OnClickListener listener) {
        this.clickListener = listener;
    }

    @Override
    public void setOnLongClickListener(@Nullable OnLongClickListener listener) {
        this.longClickListener = listener;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (pulseAnimator != null) {
            pulseAnimator.cancel();
        }
    }
}