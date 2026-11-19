package vn.edu.buacombencon;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/** Local UI demonstration data only. Never sends data to any service. */
public final class DemoStore {
    public static final String PENDING="PENDING", ALLOCATED="ALLOCATED", WAITING="WAITING", RECEIVED="RECEIVED", CANCELLED="CANCELLED";
    public static class Campaign {
        public String id,title,meal,location,time,deadline,rules,note;
        public int day,confirmed,pledged,prepared,archivedReceived;
        public boolean archived,registrationOpen;
        Campaign(String id,String title,String meal,int day,int confirmed,String time) {
            this.id=id; this.title=title; this.meal=meal; this.day=day; this.confirmed=confirmed;
            this.time=time; pledged=confirmed+10; prepared=day<=0?confirmed:0; registrationOpen=day>0;
            location="Điểm phát An Tâm · khu sinh hoạt chung (mẫu)";
            deadline=day==0?"10:00 hôm nay":"18:00 ngày trước buổi phát";
            rules="Dành cho người chăm sóc bệnh nhi ung thư được nhóm hỗ trợ xác nhận. Mỗi người chăm sóc đăng ký 1 suất mỗi đợt. Sắp xếp theo thứ tự đăng ký hợp lệ; trường hợp ngoại lệ do điều phối viên xem xét.";
            note="Chưa có ghi chú xử lý suất còn lại.";
        }
        public String date() {
            Calendar c=Calendar.getInstance(); c.add(Calendar.DAY_OF_YEAR,day);
            return new SimpleDateFormat("dd/MM/yyyy",new Locale("vi","VN")).format(c.getTime());
        }
        public String dayLabel() { return day==0?"Hôm nay":day==1?"Ngày mai":date().substring(0,5); }
    }
    public static class Registration {
        public String id,campaignId,name,phone,note,status,code;
        public boolean mine,assisted;
        Registration(String id,String cid,String name,String phone,boolean mine,boolean assisted,String status) {
            this.id=id; campaignId=cid; this.name=name; this.phone=phone; this.mine=mine;
            this.assisted=assisted; this.status=status; code="BC"+id.substring(1); note="";
        }
    }
    public final List<Campaign> campaigns=new ArrayList<>();
    public final List<Registration> registrations=new ArrayList<>();
    private final SharedPreferences prefs;
    public int donorPledged=0;
    public String donorCampaign="c2",donorCadence="Theo đợt";
    public DemoStore(Context context) { prefs=context.getSharedPreferences("ui_demo_v1",Context.MODE_PRIVATE); load(); }
    private void seed() {
        campaigns.clear(); registrations.clear(); donorPledged=0;
        campaigns.add(new Campaign("c1","Bữa trưa ấm lòng","Bữa trưa",0,30,"11:00 – 12:00"));
        campaigns.add(new Campaign("c2","Thêm một bữa cơm lành","Bữa trưa",1,40,"11:00 – 12:00"));
        campaigns.add(new Campaign("c3","Bữa chiều sẻ chia","Bữa chiều",2,25,"16:30 – 17:30"));
        Campaign past=new Campaign("c0","Bữa cơm ngày hôm qua","Bữa trưa",-1,30,"11:00 – 12:00");
        past.archived=true; past.archivedReceived=28; past.note="2 suất chưa trao được nhóm chuyển tới điểm hỗ trợ theo quy tắc của đợt (minh họa).";
        campaigns.add(past);
        for(int i=1;i<=35;i++) {
            String status=i<=12?RECEIVED:i<=30?ALLOCATED:WAITING;
            registrations.add(new Registration(String.format(Locale.US,"R%04d",i),"c1",i==13?"Người chăm sóc (mẫu)":"Người chăm sóc "+String.format(Locale.US,"%02d",i),"0900000000",i==13,i%4==0,status));
        }
        registrations.add(new Registration("R0036","c0","Người chăm sóc (mẫu)","0900000000",true,false,RECEIVED));
    }
    public void reset() { seed(); save(); }
    private void load() {
        seed(); String raw=prefs.getString("state",null); if(raw==null)return;
        try {
            JSONObject root=new JSONObject(raw); JSONArray ca=root.getJSONArray("campaigns"); campaigns.clear();
            for(int i=0;i<ca.length();i++) {
                JSONObject o=ca.getJSONObject(i); Campaign c=new Campaign(o.getString("id"),o.getString("title"),o.getString("meal"),o.getInt("day"),o.getInt("confirmed"),o.getString("time"));
                c.pledged=o.getInt("pledged");c.prepared=o.getInt("prepared");c.location=o.getString("location");c.archived=o.getBoolean("archived");c.archivedReceived=o.getInt("received");c.note=o.getString("note");
                c.deadline=o.getString("deadline");c.rules=o.getString("rules");c.registrationOpen=o.optBoolean("open",c.day>0);campaigns.add(c);
            }
            registrations.clear(); JSONArray ra=root.getJSONArray("registrations");
            for(int i=0;i<ra.length();i++) {
                JSONObject o=ra.getJSONObject(i);Registration r=new Registration(o.getString("id"),o.getString("cid"),o.getString("name"),o.getString("phone"),o.getBoolean("mine"),o.getBoolean("assisted"),o.getString("status"));
                r.note=o.getString("note");registrations.add(r);
            }
            donorPledged=root.optInt("donor",0);donorCampaign=root.optString("donorCampaign","c2");donorCadence=root.optString("cadence","Theo đợt");
        } catch(Exception e) { seed(); }
    }
    public void save() {
        try {
            JSONObject root=new JSONObject();JSONArray ca=new JSONArray(),ra=new JSONArray();
            for(Campaign c:campaigns) ca.put(new JSONObject().put("id",c.id).put("title",c.title).put("meal",c.meal).put("day",c.day).put("confirmed",c.confirmed).put("time",c.time).put("pledged",c.pledged).put("prepared",c.prepared).put("location",c.location).put("archived",c.archived).put("received",c.archivedReceived).put("note",c.note).put("deadline",c.deadline).put("rules",c.rules).put("open",c.registrationOpen));
            for(Registration r:registrations)ra.put(new JSONObject().put("id",r.id).put("cid",r.campaignId).put("name",r.name).put("phone",r.phone).put("mine",r.mine).put("assisted",r.assisted).put("status",r.status).put("note",r.note));
            root.put("campaigns",ca).put("registrations",ra).put("donor",donorPledged).put("donorCampaign",donorCampaign).put("cadence",donorCadence);
            prefs.edit().putString("state",root.toString()).apply();
        } catch(Exception e) { throw new IllegalStateException("Cannot save demo",e); }
    }
    public Campaign campaign(String id) { for(Campaign c:campaigns)if(c.id.equals(id))return c;return campaigns.get(0); }
    public Registration registration(String id) { for(Registration r:registrations)if(r.id.equals(id))return r;return null; }
    public List<Registration> inCampaign(String cid) { List<Registration> out=new ArrayList<>();for(Registration r:registrations)if(r.campaignId.equals(cid))out.add(r);return out; }
    public Registration mine(String cid) {for(Registration r:registrations)if(r.mine&&r.campaignId.equals(cid)&&!r.status.equals(CANCELLED))return r;return null;}
    public int count(String cid,String status) { int n=0;for(Registration r:inCampaign(cid))if(r.status.equals(status))n++;return n; }
    public int assigned(String cid) { return count(cid,ALLOCATED)+count(cid,RECEIVED); }
    public int received(Campaign c) {return c.archived?c.archivedReceived:count(c.id,RECEIVED);}
    public Registration register(String cid,String name,String phone,String note,boolean mine,boolean assisted) {
        Campaign c=campaign(cid);if(c.archived||!c.registrationOpen)throw new IllegalStateException("Đợt này đã đóng đăng ký.");
        if(mine&&mine(cid)!=null)throw new IllegalStateException("Bạn đã đăng ký đợt này.");
        Registration r=new Registration(String.format(Locale.US,"R%04d",registrations.size()+1),cid,name,phone,mine,assisted,PENDING);
        r.note=note;registrations.add(r);save();return r;
    }
    public String allocate(Registration r) {
        if(!r.status.equals(PENDING)&&!r.status.equals(WAITING))return "Trạng thái không thể sắp xếp.";
        Campaign c=campaign(r.campaignId);
        if(assigned(c.id)<c.confirmed) {r.status=ALLOCATED;save();return "Đã sắp xếp 1 suất và cấp mã nhận.";}
        r.status=WAITING;save();return "Đã đủ suất xác nhận. Hồ sơ được đưa vào danh sách chờ.";
    }
    public String claimError(Registration r,String cid) {
        if(r==null)return "Không tìm thấy mã. Vui lòng kiểm tra hoặc nhờ điều phối viên hỗ trợ.";
        if(!r.campaignId.equals(cid))return "Mã không thuộc đợt đang chọn.";
        if(r.status.equals(RECEIVED))return "Mã đã được sử dụng. Không giao thêm suất; liên hệ điều phối viên nếu cần.";
        if(!r.status.equals(ALLOCATED))return "Đăng ký chưa được sắp xếp hoặc đã hủy. Liên hệ điều phối viên.";
        Campaign c=campaign(cid);
        if(c.day!=0||c.archived)return "Đợt chưa đến ngày phát hoặc đã kết thúc.";
        if(received(c)>=c.prepared)return "Số suất đã trao đã bằng số thực chuẩn bị. Cần điều phối viên kiểm tra.";
        return null;
    }
    public Registration findCode(String code) {for(Registration r:registrations)if(r.code.equalsIgnoreCase(code.trim()))return r;return null;}
    public static String statusLabel(String status) {
        switch(status) {case ALLOCATED:return "Được nhận";case WAITING:return "Danh sách chờ";case RECEIVED:return "Đã nhận";case CANCELLED:return "Đã hủy";default:return "Đã tiếp nhận";}
    }
}
