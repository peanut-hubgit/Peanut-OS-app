package com.peanut.xrpg;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.animation.ValueAnimator;

/** Lightweight pseudo-3D dice renderer using only Android Canvas. */
public class Dice3DView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float rotation = 0f;
    private float scale = 1f;
    private String value = "?";
    private String die = "D20";

    public Dice3DView(Context context) {
        super(context);
        paint.setStyle(Paint.Style.FILL);
        textPaint.setTypeface(android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD));
    }

    public void setResult(String dieName, int result) {
        this.die = dieName;
        this.value = String.valueOf(result);
        animateRoll();
    }

    public void animateRoll() {
        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(520);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(a -> {
            float t = (Float) a.getAnimatedValue();
            rotation = t * 540f;
            scale = 0.88f + (float) Math.sin(t * Math.PI) * 0.14f;
            invalidate();
        });
        animator.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float size = Math.min(getWidth(), getHeight()) * 0.34f * scale;

        canvas.save();
        canvas.rotate(rotation * 0.12f, cx, cy);

        float skew = (float) Math.sin(Math.toRadians(rotation)) * size * 0.35f;

        Path top = new Path();
        top.moveTo(cx - size, cy - size * 0.75f);
        top.lineTo(cx + skew * 0.2f, cy - size);
        top.lineTo(cx + size, cy - size * 0.55f);
        top.lineTo(cx, cy - size * 0.28f);
        top.close();
        paint.setColor(Color.rgb(84, 88, 104));
        canvas.drawPath(top, paint);

        Path left = new Path();
        left.moveTo(cx - size, cy - size * 0.75f);
        left.lineTo(cx, cy - size * 0.28f);
        left.lineTo(cx, cy + size);
        left.lineTo(cx - size, cy + size * 0.62f);
        left.close();
        paint.setColor(Color.rgb(52, 56, 70));
        canvas.drawPath(left, paint);

        Path right = new Path();
        right.moveTo(cx, cy - size * 0.28f);
        right.lineTo(cx + size, cy - size * 0.55f);
        right.lineTo(cx + size, cy + size * 0.62f);
        right.lineTo(cx, cy + size);
        right.close();
        paint.setColor(Color.rgb(32, 36, 46));
        canvas.drawPath(right, paint);

        paint.setColor(Color.argb(90, 0, 0, 0));
        canvas.drawOval(new RectF(cx - size * 0.90f, cy + size * 0.68f, cx + size * 0.90f, cy + size * 0.95f), paint);

        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(size * 0.54f);
        textPaint.setColor(Color.rgb(245, 247, 250));
        canvas.drawText(value, cx, cy + size * 0.20f, textPaint);

        textPaint.setTextSize(size * 0.22f);
        textPaint.setColor(Color.rgb(105, 226, 174));
        canvas.drawText(die, cx, cy + size * 0.56f, textPaint);

        canvas.restore();
    }
}
