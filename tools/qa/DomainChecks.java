import vn.edu.buacombencon.*;
import android.content.*;
import java.lang.reflect.*;
import java.util.*;
import java.nio.file.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import static vn.edu.buacombencon.DemoStore.*;

public class DomainChecks {
    static final Map<String,Object> values=new HashMap<>();
    static int checks=0;
    static void check(boolean condition,String what){if(!condition)throw new AssertionError(what);System.out.println("PASS "+what);checks++;}
    static Object prefProxy(Class<?> type){return Proxy.newProxyInstance(type.getClassLoader(),new Class[]{type},(p,m,a)->{
        String n=m.getName();
        if(n.equals("edit"))return prefProxy(SharedPreferences.Editor.class);
        if(n.startsWith("put")){values.put((String)a[0],a[1]);return p;}
        if(n.equals("getString"))return values.getOrDefault(a[0],a[1]);
        if(n.equals("apply"))return null;if(n.equals("commit"))return true;
        if(n.equals("clear")){values.clear();return p;}
        if(n.equals("remove")){values.remove(a[0]);return p;}return null;
    });}
    public static void main(String[] args)throws Exception {
        Context ctx=new ContextWrapper(null){@Override public SharedPreferences getSharedPreferences(String n,int mode){return (SharedPreferences)prefProxy(SharedPreferences.class);}};
        DemoStore s=new DemoStore(ctx);
        boolean closed=false;try{s.register("c1","Test family","0900000000","",false,true);}catch(IllegalStateException e){closed=true;}
        check(closed,"reject registration when campaign is closed");
        check(s.assigned("c1")==30&&s.count("c1",WAITING)==5,"30 allocated or received and 5 waiting in seeded campaign");
        Registration own=s.registration("R0013");check(own.code.equals("BC0013"),"sample short code maps to the intended registration");
        check(s.claimError(own,"c2").contains("không thuộc"),"reject code belonging to another campaign");
        check(s.claimError(own,"c1")==null,"allocated current-day code accepted");
        int prepared=s.campaign("c1").prepared;s.campaign("c1").prepared=s.received(s.campaign("c1"));
        check(s.claimError(own,"c1")!=null,"reject handover beyond actual prepared supply");s.campaign("c1").prepared=prepared;
        own.status=RECEIVED;s.save();check(s.claimError(own,"c1").contains("đã được sử dụng"),"reject repeated handover");
        check(s.claimError(null,"c1")!=null,"reject unknown code");
        Registration r=s.register("c2","Test family","0900000000","",true,false);
        check(r.status.equals(PENDING),"submission does not grant a meal automatically");
        boolean duplicate=false;try{s.register("c2","Test family","0900000000","",true,false);}catch(IllegalStateException e){duplicate=true;}
        check(duplicate,"reject a second active self registration for same campaign");
        s.allocate(r);check(r.status.equals(ALLOCATED),"coordinator allocates using confirmed supply");
        check(s.claimError(r,"c2")!=null,"reject handover before campaign day");
        r.status=CANCELLED;s.save();check(s.claimError(r,"c2")!=null,"cancelled code cannot be redeemed");
        Registration wait=s.registration("R0031");s.allocate(wait);check(wait.status.equals(WAITING),"full confirmed supply keeps applicant waiting");
        s.registration("R0014").status=CANCELLED;s.allocate(wait);check(wait.status.equals(ALLOCATED)&&s.assigned("c1")==30,"released allocation can be reassigned without exceeding capacity");
        DemoStore reloaded=new DemoStore(ctx);check(reloaded.registration(r.id).status.equals(CANCELLED),"local changes survive store recreation");
        Campaign archived=s.campaign("c0");check(archived.prepared==30&&s.received(archived)==28&&archived.prepared-s.received(archived)==2,"historical reconciliation keeps unhanded meals visible");
        int cf=s.campaign("c2").confirmed;s.campaign("c2").pledged+=20;s.save();check(s.campaign("c2").confirmed==cf,"pledged meals remain separate from confirmed supply");
        QrCode qr=new QrCode("BC0013");BufferedImage im=new BufferedImage(290,290,BufferedImage.TYPE_INT_RGB);
        for(int y=0;y<290;y++)for(int x=0;x<290;x++){int xx=x/10-4,yy=y/10-4;boolean dark=xx>=0&&xx<21&&yy>=0&&yy<21&&qr.modules[yy][xx];im.setRGB(x,y,dark?0:0xffffff);}
        ImageIO.write(im,"png",Path.of(args[0],"qr-native-encoder.png").toFile());
        System.out.println("TOTAL "+checks+" checks passed");
    }
}
