package com.peanut.xrpg;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import java.util.Random;

/** Lightweight AOSP 2D dice visual with rolling-number animation. */
public final class Dice2DView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Random random = new Random();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private int sides = 20, count = 1, tick = 0;
    private int[] values = {20}, shown = {20};
    private boolean rolling;

    public Dice2DView(Context context) { super(context); setLayerType(View.LAYER_TYPE_SOFTWARE, null); }

    public void setDice(int sides, int count) {
        this.sides = Math.max(2, sides); this.count = Math.max(1, Math.min(8, count));
        values = new int[this.count]; shown = new int[this.count];
        for (int i=0;i<this.count;i++) values[i]=shown[i]=1+random.nextInt(this.sides);
        invalidate();
    }

    public void roll(final int sides, final int[] result) {
        this.sides=sides; this.count=Math.max(1,Math.min(8,result.length)); values=result.clone(); shown=new int[count];
        for(int i=0;i<count;i++) shown[i]=1+random.nextInt(this.sides); tick=0; rolling=true; handler.removeCallbacksAndMessages(null);
        handler.post(new Runnable(){ @Override public void run(){
            tick++;
            if(tick<15){ for(int i=0;i<shown.length;i++) shown[i]=1+random.nextInt(Dice2DView.this.sides); invalidate(); handler.postDelayed(this,48+tick*3L); }
            else { shown=values.clone(); rolling=false; invalidate(); }
        }});
        invalidate();
    }

    @Override protected void onDetachedFromWindow(){ handler.removeCallbacksAndMessages(null); super.onDetachedFromWindow(); }

    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);
        int cols=count<=3?count:3, rows=(count+cols-1)/cols;
        float cellW=getWidth()/(float)cols, cellH=getHeight()/(float)Math.max(1,rows), size=Math.min(cellW,cellH)*(count==1?.68f:.56f);
        paint.setStyle(Paint.Style.FILL); paint.setTextAlign(Paint.Align.CENTER); paint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        for(int i=0;i<count;i++){
            int col=i%cols,row=i/cols; float cx=col*cellW+cellW/2f,cy=row*cellH+cellH/2f;
            RectF r=new RectF(cx-size/2f,cy-size/2f,cx+size/2f,cy+size/2f);
            canvas.save(); canvas.rotate((i%2==0?-2f:2f)*(rolling?1f:.35f),cx,cy);
            paint.setColor(Color.WHITE); paint.setShadowLayer(14f,0,5f,0x66000000); canvas.drawRoundRect(r,size*.16f,size*.16f,paint); paint.clearShadowLayer();
            paint.setColor(Color.BLACK); paint.setTextSize(size*(shown[i]>=100?.24f:.31f)); float baseline=cy-(paint.ascent()+paint.descent())/2f; canvas.drawText(String.valueOf(shown[i]),cx,baseline,paint);
            paint.setColor(Color.rgb(105,109,118)); paint.setTextSize(Math.max(10f,size*.075f)); canvas.drawText("D"+sides,cx,r.bottom+size*.15f,paint);
            canvas.restore();
        }
    }
}
