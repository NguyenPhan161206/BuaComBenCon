package vn.edu.buacombencon;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

/** Small consistent line icons, independent of device emoji fonts. */
public class UiIcon extends View {
    private final Paint p=new Paint(3);
    private final String kind;
    public UiIcon(Context c,String kind,int color) { super(c);this.kind=kind;p.setColor(color);p.setStrokeWidth(1.7f);p.setStyle(Paint.Style.STROKE);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO); }
    private void line(Canvas c,float...points) {Path path=new Path();path.moveTo(points[0],points[1]);for(int i=2;i<points.length;i+=2)path.lineTo(points[i],points[i+1]);c.drawPath(path,p);}
    @Override protected void onDraw(Canvas c) {
        c.save();c.scale(getWidth()/24f,getHeight()/24f);
        switch(kind) {
            case "home":line(c,3,10,12,3,21,10);line(c,5,9,5,21,10,21,10,14,14,14,14,21,19,21,19,9);break;
            case "ticket":c.drawRoundRect(3,5,21,19,3,3,p);line(c,8,5,8,8);line(c,8,11,8,13);line(c,8,16,8,19);line(c,12,10,17,10);line(c,12,14,16,14);break;
            case "heart":Path h=new Path();h.moveTo(12,21);h.cubicTo(-4,10,4,-1,12,7);h.cubicTo(20,-1,28,10,12,21);c.drawPath(h,p);break;
            case "chart":line(c,4,3,4,21,22,21);line(c,9,17,9,11);line(c,14,17,14,6);line(c,19,17,19,9);break;
            case "user":c.drawCircle(12,7,4,p);c.drawArc(4,14,20,26,180,180,false,p);break;
            case "pin":c.drawCircle(12,9,2.4f,p);Path pin=new Path();pin.moveTo(12,22);pin.cubicTo(-2,8,6,2,12,2);pin.cubicTo(18,2,26,8,12,22);c.drawPath(pin,p);break;
            case "clock":c.drawCircle(12,12,9,p);line(c,12,6,12,12,16,14);break;
            case "check":c.drawCircle(12,12,9,p);line(c,7,12,10,15,17,8);break;
            case "scan":line(c,3,8,3,3,8,3);line(c,16,3,21,3,21,8);line(c,3,16,3,21,8,21);line(c,16,21,21,21,21,16);line(c,5,12,19,12);break;
            default:c.drawRoundRect(4,5,20,21,3,3,p);line(c,4,10,20,10);line(c,8,2,8,7);line(c,16,2,16,7);break;
        }
        c.restore();
    }
}
