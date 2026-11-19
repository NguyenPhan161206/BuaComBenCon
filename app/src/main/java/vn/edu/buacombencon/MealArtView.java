package vn.edu.buacombencon;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

/** Original vector illustration, drawn by Android. No network/image dependency. */
public class MealArtView extends View {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    public MealArtView(Context c) { super(c); }
    public MealArtView(Context c, AttributeSet a) { super(c,a); }
    private void oval(Canvas c, int color, float x, float y, float w, float h) {
        p.setColor(color); p.setStyle(Paint.Style.FILL); c.drawOval(x,y,x+w,y+h,p);
    }
    private int color(String s) { return Color.parseColor(s); }
    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        float unit = Math.min(getWidth(),getHeight()) / 200f;
        c.save(); c.translate((getWidth()-200*unit)/2,(getHeight()-200*unit)/2); c.scale(unit,unit);
        oval(c,color("#DAE6CE"),18,16,180,180);
        oval(c,color("#CAD7BD"),26,43,164,144);
        oval(c,color("#FFFEF5"),20,34,164,144);
        oval(c,color("#E9E8D7"),32,46,140,120);
        oval(c,color("#F8F5E6"),36,50,132,112);
        // A mound of rice with individual grains.
        oval(c,color("#FFFFFF"),83,63,68,80);
        p.setColor(color("#E3E0CD")); p.setStrokeWidth(2.1f); p.setStrokeCap(Paint.Cap.ROUND);
        for (int i=0;i<19;i++) {
            float x=97+(i*17%43), y=76+(i*13%55); c.drawLine(x,y,x+3,y+2,p);
        }
        oval(c,color("#C47741"),52,93,37,45);
        oval(c,color("#D99353"),49,89,37,38);
        oval(c,color("#E4AD65"),56,94,22,24);
        oval(c,color("#397450"),46,57,22,27);
        oval(c,color("#4D8654"),63,53,24,26);
        oval(c,color("#588F58"),75,66,21,24);
        oval(c,color("#2F6749"),57,74,25,24);
        oval(c,color("#EB9846"),83,131,24,12);
        oval(c,color("#EFAC55"),101,137,24,12);
        oval(c,color("#DF8041"),71,139,22,11);
        // Chopsticks.
        p.setColor(color("#A97C45")); p.setStrokeWidth(4); c.drawLine(157,20,182,166,p);
        p.setColor(color("#BF9458")); c.drawLine(166,19,190,165,p);
        p.setColor(color("#85A16A")); p.setStrokeWidth(2.5f);
        Path steam=new Path(); steam.moveTo(108,42); steam.cubicTo(95,26,123,27,110,10); c.drawPath(steam,stroke());
        p.setStyle(Paint.Style.FILL);
        oval(c,color("#F7CF85"),26,158,9,9); oval(c,color("#83A571"),10,62,7,7);
        c.restore();
    }
    private Paint stroke() { p.setStyle(Paint.Style.STROKE); return p; }
}
