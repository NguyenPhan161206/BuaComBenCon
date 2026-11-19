package vn.edu.buacombencon;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;
public final class QrView extends View {
    private final QrCode code;
    private final Paint p=new Paint();
    public QrView(Context c,String payload) {super(c);code=new QrCode(payload);setContentDescription("Mã QR nhận suất. Có thể dùng mã ngắn "+payload);}
    @Override protected void onDraw(Canvas c) {
        c.drawColor(Color.WHITE);p.setColor(Color.BLACK);
        float unit=Math.min(getWidth(),getHeight())/29f;
        float left=(getWidth()-29*unit)/2,top=(getHeight()-29*unit)/2;
        for(int y=0;y<21;y++)for(int x=0;x<21;x++)if(code.modules[y][x])c.drawRect(left+(x+4)*unit,top+(y+4)*unit,left+(x+5)*unit,top+(y+5)*unit,p);
    }
}
