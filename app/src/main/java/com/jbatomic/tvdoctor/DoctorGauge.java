package com.jbatomic.tvdoctor;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

public final class DoctorGauge extends View {
    private float value=0;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF arc=new RectF();
    public DoctorGauge(Context c){super(c);}
    public void setPercent(float p){value=Math.min(100,Math.max(0,p));invalidate();}
    @Override protected void onDraw(Canvas c){
        super.onDraw(c);
        float w=getWidth(),h=getHeight(),d=Math.min(w,h)-14;
        float x=(w-d)/2,y=(h-d)/2;
        arc.set(x+6,y+6,x+d-6,y+d-6);
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeCap(Paint.Cap.ROUND);paint.setStrokeWidth(11);
        paint.setColor(0xff2b4357);c.drawArc(arc,135,270,false,paint);
        paint.setColor(value>88?0xffffad67:0xff00d9ec);c.drawArc(arc,135,270*value/100,false,paint);
        paint.setStyle(Paint.Style.FILL);paint.setColor(0xffe6f9ff);
        paint.setTextAlign(Paint.Align.CENTER);paint.setFakeBoldText(true);paint.setTextSize(d*.22f);
        c.drawText(Math.round(value)+"%",w/2,h/2+7,paint);
    }
}
