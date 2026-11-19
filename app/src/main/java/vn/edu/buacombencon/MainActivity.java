package vn.edu.buacombencon;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import static vn.edu.buacombencon.DemoStore.*;

/** Native Android Views and XML UI. All mutations stay in the local demo store. */
public class MainActivity extends Activity {
    private static final int INK=0xFF203F35,GREEN=0xFF23664F,MUTED=0xFF69776E,
        PALE=0xFFEAF2E8,LINE=0xFFE2E7DF,WHITE=Color.WHITE,ORANGE=0xFFAA5426;
    private final String[] roles={"Người chăm sóc","Điều phối viên","Tình nguyện viên","Nhà hảo tâm"};
    private final String[] shortRoles={"Gia đình","Điều phối","Phát suất","Đồng hành"};
    private DemoStore store;
    private LinearLayout content,nav;
    private ScrollView scroll;
    private int role=0,tab=0,filter=0;
    private String screen="tab",campaignId="c1",registrationId="R0013",listFilter="Tất cả";
    private ArrayList<String> back=new ArrayList<>();
    private Bundle drafts=new Bundle();
    private final Map<String,EditText> fields=new LinkedHashMap<>();
    private final Map<String,Spinner> spinners=new LinkedHashMap<>();
    private CheckBox consent;
    private boolean connected=true;
    private android.window.OnBackInvokedCallback modernBack;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);store=new DemoStore(this);
        if(state!=null) {
            role=state.getInt("role");tab=state.getInt("tab");screen=state.getString("screen","tab");
            campaignId=state.getString("campaign","c1");registrationId=state.getString("registration","R0013");
            drafts=state.getBundle("drafts");if(drafts==null)drafts=new Bundle();
            back=state.getStringArrayList("back");if(back==null)back=new ArrayList<>();
            filter=state.getInt("filter");connected=state.getBoolean("connected",true);listFilter=state.getString("listFilter","Tất cả");
        }
        setContentView(R.layout.activity_main);
        content=findViewById(R.id.content);nav=findViewById(R.id.bottom_nav);scroll=findViewById(R.id.scroll);
        findViewById(R.id.role_switch).setOnClickListener(v->chooseRole());
        View root=findViewById(R.id.app_root);
        root.setOnApplyWindowInsetsListener((v,insets)->{
            if(Build.VERSION.SDK_INT>=30) {
                android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());
                android.graphics.Insets ime=insets.getInsets(WindowInsets.Type.ime());
                v.setPadding(bars.left,bars.top,bars.right,Math.max(bars.bottom,ime.bottom));
                nav.setVisibility(insets.isVisible(WindowInsets.Type.ime())?View.GONE:View.VISIBLE);
            } else v.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());
            return insets;
        });
        root.requestApplyInsets();render();
    }
    private String draftKey() {return role+":"+screen+":"+campaignId;}
    private void capture() {
        Bundle b=new Bundle();for(Map.Entry<String,EditText> f:fields.entrySet())b.putString(f.getKey(),f.getValue().getText().toString());
        for(Map.Entry<String,Spinner> s:spinners.entrySet())b.putInt("spinner:"+s.getKey(),s.getValue().getSelectedItemPosition());
        if(consent!=null)b.putBoolean("consent",consent.isChecked());
        if(!fields.isEmpty()||!spinners.isEmpty()||consent!=null)drafts.putBundle(draftKey(),b);
    }
    @Override protected void onSaveInstanceState(Bundle out) {
        capture();super.onSaveInstanceState(out);out.putInt("role",role);out.putInt("tab",tab);out.putString("screen",screen);
        out.putString("campaign",campaignId);out.putString("registration",registrationId);out.putBundle("drafts",drafts);out.putStringArrayList("back",back);
        out.putInt("filter",filter);out.putBoolean("connected",connected);out.putString("listFilter",listFilter);
    }
    private void go(String next) {capture();back.add(screen);screen=next;hideKeyboard();render();}
    private void switchTab(int next) {capture();tab=next;if(role==0&&next==0&&filter==9)filter=0;screen="tab";back.clear();hideKeyboard();render();}
    @Override public void onBackPressed() {navigateBack();}
    private void navigateBack() {capture();if(!back.isEmpty()){screen=back.remove(back.size()-1);hideKeyboard();render();}else if(!screen.equals("tab")){screen="tab";render();}else if(tab!=0)switchTab(0);else finish();}
    private void updateBackHandler() {
        if(Build.VERSION.SDK_INT<33)return;
        boolean intercept=!back.isEmpty()||!screen.equals("tab")||tab!=0;
        if(intercept&&modernBack==null){modernBack=this::navigateBack;getOnBackInvokedDispatcher().registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT,modernBack);}
        else if(!intercept&&modernBack!=null){getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback(modernBack);modernBack=null;}
    }
    private void hideKeyboard() {View f=getCurrentFocus();if(f!=null)((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(f.getWindowToken(),0);}
    private void chooseRole() {
        new AlertDialog.Builder(this).setTitle("Chọn vai trò trình diễn").setSingleChoiceItems(roles,role,(dialog,which)->{
            capture();role=which;tab=0;screen="tab";back.clear();dialog.dismiss();render();
        }).setNegativeButton("Đóng",null).show();
    }
    private void render() {
        fields.clear();spinners.clear();consent=null;content.removeAllViews();
        ((TextView)findViewById(R.id.role_switch)).setText(shortRoles[role]+" ⌄");drawNav();
        if(!screen.equals("tab")) {TextView b=text("‹  Quay lại",14,GREEN,true);b.setMinHeight(dp(48));b.setGravity(Gravity.CENTER_VERTICAL);content.addView(b);b.setOnClickListener(v->onBackPressed());gap(content,8);}
        switch(screen) {
            case "detail":detail();break;
            case "form":registrationForm(false);break;
            case "assisted":registrationForm(true);break;
            case "success":success();break;
            case "ticket":ticket();break;
            case "manage":manage();break;
            case "create":createCampaign();break;
            case "pledge":pledge();break;
            default:
                if(tab==3)profile();
                else if(role==0) {if(tab==0)home();else if(tab==1)myRegistrations();else impact();}
                else if(role==1) {if(tab==0)dashboard();else if(tab==1)campaigns();else reports();}
                else if(role==2) {if(tab==0)distribution();else if(tab==1)handedOver();else help();}
                else {if(tab==0)donorHome();else if(tab==1)commitments();else reports();}
        }
        updateBackHandler();
        scroll.post(()->scroll.scrollTo(0,0));
    }
    private void drawNav() {
        nav.removeAllViews();String[][] labels={{"Trang chủ","Đăng ký","Kết quả","Tài khoản"},{"Tổng quan","Đợt hỗ trợ","Báo cáo","Tài khoản"},{"Phát suất","Đã trao","Hỗ trợ","Tài khoản"},{"Đồng hành","Cam kết","Kết quả","Tài khoản"}};
        String[] icons=role==2?new String[]{"scan","check","heart","user"}:new String[]{"home","ticket","chart","user"};
        for(int i=0;i<4;i++) {
            final int t=i;LinearLayout item=vertical();item.setGravity(Gravity.CENTER);item.setPadding(dp(2),dp(5),dp(2),dp(5));
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);nav.addView(item,lp);
            UiIcon icon=new UiIcon(this,icons[i],i==tab?GREEN:MUTED);item.addView(icon,new LinearLayout.LayoutParams(dp(22),dp(22)));
            TextView label=text(labels[role][i],10,i==tab?GREEN:MUTED,i==tab);label.setGravity(Gravity.CENTER);label.setPadding(0,dp(6),0,0);item.addView(label);
            item.setContentDescription(labels[role][i]);item.setBackground(round(i==tab?0xFFF1F5EF:WHITE,14));item.setOnClickListener(v->switchTab(t));item.setMinimumHeight(dp(56));
        }
    }
    private void home() {
        View home=getLayoutInflater().inflate(R.layout.screen_home,content,false);content.addView(home);
        if(getResources().getConfiguration().screenWidthDp<360)home.findViewById(R.id.hero_art).setTranslationX(dp(68));
        home.findViewById(R.id.find_meal).setOnClickListener(v->{View list=home.findViewById(R.id.campaign_list);scroll.smoothScrollTo(0,list.getTop()+home.getTop()-dp(20));});
        LinearLayout pickup=home.findViewById(R.id.home_pickup);
        for(Registration r:store.registrations)if(r.mine&&r.status.equals(ALLOCATED)) {
            Campaign c=store.campaign(r.campaignId);LinearLayout card=card(pickup,WHITE);pill(card,"ĐÃ CÓ SUẤT DÀNH CHO BẠN",PALE,GREEN);
            label(card,c.dayLabel()+" · "+c.time,17,INK,true,10);label(card,c.title+" · 1 suất",13,MUTED,false,5);
            action(card,"Mở mã nhận suất  →",()->openTicket(r),false);break;
        }
        LinearLayout filters=home.findViewById(R.id.filter_row),list=home.findViewById(R.id.campaign_list);
        String[] options={"Tất cả","Hôm nay","Ngày mai","Bữa chiều"};
        for(int i=0;i<options.length;i++){final int f=i;chip(filters,options[i],filter==i,()->{filter=f;render();});}
        int count=0;for(Campaign c:store.campaigns)if(!c.archived&&(filter==0||filter==1&&c.day==0||filter==2&&c.day==1||filter==3&&c.meal.equals("Bữa chiều"))){campaignCard(list,c,false);count++;}
        if(count==0)empty(list,"Chưa có đợt phù hợp","Bạn có thể chọn bộ lọc khác.");
        label(list,"Một điểm phát minh họa · Thông tin và số liệu đều là mẫu.",11,MUTED,false,8);
    }
    private void campaignCard(LinearLayout parent,Campaign c,boolean management) {
        LinearLayout card=card(parent,WHITE),row=horizontal();card.addView(row);
        LinearLayout date=vertical();date.setPadding(dp(10),dp(10),dp(10),dp(10));date.setGravity(Gravity.CENTER);date.setBackground(round(PALE,14));
        TextView day=text(c.date().substring(0,2),24,GREEN,true);day.setGravity(Gravity.CENTER);date.addView(day);
        TextView month=text("THÁNG "+c.date().substring(3,5),8,GREEN,true);month.setGravity(Gravity.CENTER);date.addView(month);row.addView(date,new LinearLayout.LayoutParams(dp(62),-2));
        LinearLayout info=vertical();LinearLayout.LayoutParams ilp=new LinearLayout.LayoutParams(0,-2,1);ilp.leftMargin=dp(14);row.addView(info,ilp);
        label(info,c.dayLabel()+" · "+c.meal,11,ORANGE,true,0);label(info,c.title,17,INK,true,6);label(info,c.time,12,MUTED,false,6);
        gap(card,14);iconLine(card,"pin",c.location);
        if(management)label(card,c.archived?"Đã kết thúc · "+store.received(c)+" suất đã trao":store.assigned(c.id)+" / "+c.confirmed+" suất đã sắp xếp · "+store.count(c.id,WAITING)+" chờ",12,GREEN,true,12);
        else {Registration r=store.mine(c.id);label(card,r!=null?statusLabel(r.status):c.registrationOpen?"Tiếp nhận đăng ký · Kết quả do đơn vị xác nhận":"Đã đóng đăng ký",12,GREEN,true,12);}
        action(card,management?(c.archived?"Xem báo cáo  →":"Quản lý đợt  →"):"Xem chi tiết  →",()->{campaignId=c.id;go(management&&!c.archived?"manage":"detail");},false);
    }
    private void detail() {
        Campaign c=store.campaign(campaignId);
        pill(content,c.archived?"ĐÃ KẾT THÚC":"ĐỢT HỖ TRỢ · "+c.meal.toUpperCase(new java.util.Locale("vi")),PALE,GREEN);
        heading(c.title,"Một bữa ăn ấm áp để bạn an tâm bên con.");
        MealArtView art=new MealArtView(this);art.setBackground(round(PALE,24));content.addView(art,new LinearLayout.LayoutParams(-1,dp(170)));gap(content,20);
        LinearLayout details=card(content,WHITE);iconLine(details,"calendar",c.dayLabel()+" · "+c.date());gap(details,14);iconLine(details,"clock",c.time);gap(details,14);iconLine(details,"pin",c.location);
        section("Thông tin đăng ký");body("Hạn đăng ký: "+c.deadline+".\nĐơn vị tổ chức: Nhóm hỗ trợ An Tâm (mẫu).\nSố suất xác nhận cung cấp: "+c.confirmed+".");
        section("Đối tượng và cách sắp xếp");body(c.rules);
        section("Hướng dẫn nhận suất");body("Đến đúng khung giờ, xuất trình mã QR hoặc đọc mã ngắn cho tình nguyện viên. Nếu không tìm thấy mã hoặc cần đăng ký hộ, gặp điều phối viên tại bàn hỗ trợ. Không cần cung cấp bệnh án của trẻ.");
        action(content,"Liên hệ đơn vị tổ chức",()->info("Đầu mối hỗ trợ","Bàn điều phối tại điểm phát An Tâm (mẫu). Chưa có số điện thoại thật được xác nhận."),false);
        Registration r=store.mine(c.id);
        if(role==0&&!c.archived) {
            if(r==null&&c.registrationOpen){body(getString(R.string.register_note));action(content,"Đăng ký 1 suất ăn",()->go("form"),true);}
            else if(r==null)body("Đợt này đã đóng đăng ký. Bạn có thể xem các đợt đang tiếp nhận trên Trang chủ.");
            else {pill(content,statusLabel(r.status),PALE,GREEN);action(content,r.status.equals(ALLOCATED)?"Xem mã nhận suất":"Xem đăng ký của tôi",()->openTicket(r),true);}
        } else if(role==3&&!c.archived)action(content,"Đồng hành cùng đợt này",()->go("pledge"),true);
        if(c.archived){section("Kết quả đợt hỗ trợ");reportCard(c);}
    }
    private void registrationForm(boolean assisted) {
        Campaign c=store.campaign(campaignId);View form=getLayoutInflater().inflate(R.layout.screen_form,content,false);content.addView(form);
        ((TextView)form.findViewById(R.id.form_title)).setText(assisted?"Đăng ký hộ gia đình":"Đăng ký nhận suất");
        ((TextView)form.findViewById(R.id.form_context)).setText(c.title+"\n"+c.date()+" · "+c.time+"\n1 suất / người chăm sóc");
        EditText name=form.findViewById(R.id.input_name),phone=form.findViewById(R.id.input_phone),note=form.findViewById(R.id.input_note);
        bindField("name",name,"");bindField("phone",phone,"");bindField("note",note,"");
        consent=form.findViewById(R.id.consent);Bundle draft=drafts.getBundle(draftKey());if(draft!=null)consent.setChecked(draft.getBoolean("consent"));
        if(assisted)consent.setText("Tôi đã trao đổi với người chăm sóc và được đồng ý ghi nhận thông tin liên hệ.");
        form.findViewById(R.id.submit_form).setOnClickListener(v->{
            if(!validName(name)||!validPhone(phone))return;
            if(!consent.isChecked()){info("Cần sự đồng ý","Vui lòng đọc và đồng ý phần cung cấp thông tin trước khi gửi.");return;}
            try {
                Registration r=store.register(c.id,name.getText().toString().trim(),phone.getText().toString().trim(),note.getText().toString().trim(),!assisted,assisted);
                registrationId=r.id;drafts.remove(draftKey());fields.clear();consent=null;screen="success";back.clear();if(assisted)back.add("manage");hideKeyboard();render();
            }catch(IllegalStateException e){info("Chưa thể đăng ký",e.getMessage());}
        });
    }
    private void success() {
        Registration r=store.registration(registrationId);if(r==null){body("Không tìm thấy đăng ký.");return;}
        gap(content,20);UiIcon check=new UiIcon(this,"check",GREEN);LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(dp(64),dp(64));cp.gravity=Gravity.CENTER_HORIZONTAL;content.addView(check,cp);gap(content,22);
        heading("Đã tiếp nhận\nđăng ký của bạn","Đơn vị tổ chức sẽ kiểm tra và cập nhật kết quả sắp xếp suất.");
        LinearLayout card=card(content,WHITE);label(card,store.campaign(r.campaignId).title,18,INK,true,0);label(card,store.campaign(r.campaignId).date()+" · 1 suất ăn",14,MUTED,false,10);gap(card,14);pill(card,"Đã tiếp nhận · Chưa cấp mã nhận",0xFFFFF0DE,ORANGE);
        body("Gửi đăng ký chưa bảo đảm có suất. Bạn có thể xem trạng thái trong Đăng ký của tôi. Nếu hết suất, hồ sơ sẽ vào danh sách chờ.");
        action(content,role==1?"Về danh sách để sắp xếp":"Theo dõi đăng ký",()->{if(role==1){screen="manage";back.clear();render();}else switchTab(1);},true);
    }
    private void openTicket(Registration r) {registrationId=r.id;campaignId=r.campaignId;go("ticket");}
    private void myRegistrations() {
        heading("Đăng ký của tôi","Theo dõi kết quả, thời gian và mã nhận suất.");
        LinearLayout chips=horizontal();content.addView(chips);chip(chips,"Hiện tại",filter!=9,()->{filter=0;render();});chip(chips,"Lịch sử",filter==9,()->{filter=9;render();});gap(content,18);
        int n=0;for(Registration r:store.registrations)if(r.mine) {
            Campaign c=store.campaign(r.campaignId);boolean history=c.archived||r.status.equals(RECEIVED)||r.status.equals(CANCELLED);
            if((filter==9)!=history)continue;n++;
            LinearLayout card=card(content,WHITE);pill(card,statusLabel(r.status),r.status.equals(WAITING)||r.status.equals(PENDING)?0xFFFFF0DE:PALE,r.status.equals(WAITING)||r.status.equals(PENDING)?ORANGE:GREEN);
            label(card,c.title,18,INK,true,12);label(card,c.date()+" · "+c.time,13,MUTED,false,8);label(card,"1 suất · "+(r.assisted?"Được đăng ký hộ":"Tự đăng ký"),12,MUTED,false,8);
            action(card,r.status.equals(ALLOCATED)?"Mở mã nhận suất  →":"Xem trạng thái  →",()->openTicket(r),false);
        }
        if(n==0){empty(content,"Chưa có đăng ký","Chọn một đợt hỗ trợ phù hợp để gửi đăng ký nhận suất.");action(content,"Tìm đợt hỗ trợ",()->{filter=0;switchTab(0);},true);}
    }
    private void ticket() {
        Registration r=store.registration(registrationId);if(r==null){body("Không tìm thấy đăng ký.");return;}Campaign c=store.campaign(r.campaignId);
        heading(r.status.equals(ALLOCATED)?"Suất ăn đã sẵn sàng":statusLabel(r.status),c.title);
        LinearLayout card=card(content,WHITE);card.setGravity(Gravity.CENTER_HORIZONTAL);pill(card,statusLabel(r.status),PALE,GREEN);
        if(r.status.equals(ALLOCATED)) {
            QrView qr=new QrView(this,r.code);LinearLayout.LayoutParams qlp=new LinearLayout.LayoutParams(dp(230),dp(230));qlp.gravity=Gravity.CENTER_HORIZONTAL;qlp.topMargin=dp(10);card.addView(qr,qlp);
            TextView code=text(r.code,30,INK,true);code.setLetterSpacing(0.13f);code.setGravity(Gravity.CENTER);card.addView(code);
            label(card,"Mã nhận 1 suất · dùng một lần cho đợt này",12,MUTED,false,12);
            action(card,"Sao chép mã ngắn",()->{((ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("Mã nhận suất",r.code));toast("Đã sao chép mã nhận.");},false);
        } else if(r.status.equals(PENDING)||r.status.equals(WAITING)) {
            gap(card,24);label(card,r.status.equals(WAITING)?"Bạn đang ở danh sách chờ":"Đăng ký đang được kiểm tra",20,INK,true,0);
            label(card,"Mã nhận chỉ được cấp khi đơn vị sắp xếp suất. Vui lòng kiểm tra lại trạng thái trước khi đến nhận.",14,MUTED,false,16);gap(card,18);
        } else label(card,r.status.equals(RECEIVED)?"Suất ăn đã được ghi nhận giao. Mã không còn hiệu lực.":"Đăng ký đã hủy. Mã không còn hiệu lực.",15,MUTED,false,20);
        gap(content,16);iconLine(content,"calendar",c.date()+" · "+c.time);gap(content,14);iconLine(content,"pin",c.location);
        section("Khi đến nhận");body("Xuất trình QR hoặc đọc mã ngắn. Không dùng được điện thoại? Bạn có thể nhờ điều phối viên hỗ trợ trực tiếp tại điểm phát.");
        if(!r.status.equals(RECEIVED)&&!r.status.equals(CANCELLED)&&role==0)action(content,"Tôi không thể đến nhận",()->new AlertDialog.Builder(this).setTitle("Hủy đăng ký suất ăn?").setMessage("Đơn vị tổ chức có thể sắp xếp suất cho người đang chờ. Mã nhận của bạn sẽ hết hiệu lực.").setNegativeButton("Giữ đăng ký",null).setPositiveButton("Xác nhận hủy",(d,w)->{r.status=CANCELLED;store.save();render();}).show(),false);
    }
    private void dashboard() {
        heading("Cùng nhau,\nlo từng bữa cơm.","Không gian điều phối · Điểm phát An Tâm (mẫu)");
        Campaign c=store.campaign("c1");LinearLayout hero=card(content,PALE);pill(hero,"ĐỢT ĐANG PHÁT",WHITE,GREEN);label(hero,c.title,22,INK,true,14);label(hero,c.date()+" · "+c.time,13,MUTED,false,10);
        metrics(hero,new String[]{"Đã sắp xếp","Đã trao","Đang chờ"},new int[]{store.assigned(c.id),store.received(c),store.count(c.id,WAITING)});
        action(hero,"Tiếp tục điều phối  →",()->{campaignId=c.id;go("manage");},true);
        section("Việc cần theo dõi");
        int pending=0;for(Registration r:store.registrations)if(r.status.equals(PENDING))pending++;
        LinearLayout work=card(content,WHITE);iconLine(work,"ticket",pending+" đăng ký đang chờ kiểm tra");gap(work,16);iconLine(work,"clock",store.count("c1",WAITING)+" hồ sơ trong danh sách chờ");
        body("Kiểm tra hồ sơ đủ điều kiện, rồi sắp xếp theo thứ tự đăng ký hợp lệ. Người đăng ký hộ được theo dõi trong cùng danh sách.");
        action(content,"Tạo đợt hỗ trợ mới",()->go("create"),true);action(content,"Xem tất cả đợt hỗ trợ",()->switchTab(1),false);
    }
    private void campaigns() {
        heading("Đợt hỗ trợ","Theo dõi từ đăng ký đến đối chiếu sau buổi phát.");
        action(content,"+  Tạo đợt hỗ trợ",()->go("create"),true);gap(content,8);
        for(Campaign c:store.campaigns)campaignCard(content,c,true);
    }
    private void manage() {
        Campaign c=store.campaign(campaignId);heading(c.title,c.date()+" · "+c.time);
        LinearLayout numbers=card(content,PALE);metrics(numbers,new String[]{"Đã xác nhận","Đã sắp xếp","Đã trao"},new int[]{c.confirmed,store.assigned(c.id),store.received(c)});
        label(numbers,"Cam kết "+c.pledged+" · Thực chuẩn bị "+c.prepared,12,MUTED,false,10);
        if(c.registrationOpen)action(content,"+  Đăng ký hộ người chăm sóc",()->go("assisted"),true);
        action(content,c.registrationOpen?"Đóng tiếp nhận đăng ký":"Mở tiếp nhận đăng ký (mẫu)",()->new AlertDialog.Builder(this).setTitle("Cập nhật tiếp nhận đăng ký?").setMessage("Điều phối viên quản lý trạng thái mở/đóng trong bản UI. Thời hạn nhập dạng văn bản; chưa tự động đóng theo giờ.").setNegativeButton("Giữ nguyên",null).setPositiveButton("Cập nhật",(d,w)->{c.registrationOpen=!c.registrationOpen;store.save();render();}).show(),false);
        action(content,"Cập nhật số suất & đối chiếu",()->editSupply(c),false);
        section("Danh sách đăng ký");body("Dữ liệu mẫu · Thứ tự đăng ký từ sớm đến muộn.");
        HorizontalScrollView hsv=new HorizontalScrollView(this);hsv.setHorizontalScrollBarEnabled(false);LinearLayout chips=horizontal();hsv.addView(chips);content.addView(hsv);
        String[] options={"Tất cả","Cần sắp xếp","Danh sách chờ","Được nhận","Đã nhận"};
        for(String o:options)chip(chips,o,listFilter.equals(o),()->{listFilter=o;render();});gap(content,16);
        int n=0;for(Registration r:store.inCampaign(c.id)) {
            if(listFilter.equals("Cần sắp xếp")&&!r.status.equals(PENDING)||listFilter.equals("Danh sách chờ")&&!r.status.equals(WAITING)||listFilter.equals("Được nhận")&&!r.status.equals(ALLOCATED)||listFilter.equals("Đã nhận")&&!r.status.equals(RECEIVED))continue;
            n++;LinearLayout card=card(content,WHITE);label(card,r.name,16,INK,true,0);label(card,r.id+" · "+(r.assisted?"Đăng ký hộ":"Tự đăng ký"),11,MUTED,false,7);gap(card,10);pill(card,statusLabel(r.status),r.status.equals(PENDING)||r.status.equals(WAITING)?0xFFFFF0DE:PALE,r.status.equals(PENDING)||r.status.equals(WAITING)?ORANGE:GREEN);
            if(!r.note.isEmpty())label(card,r.note,12,MUTED,false,10);
            if(r.status.equals(PENDING)||r.status.equals(WAITING))action(card,"Kiểm tra và sắp xếp",()->{
                // Check earlier valid records before this one. Exceptions are not silently promoted.
                for(Registration earlier:store.inCampaign(c.id)) {
                    if(earlier==r)break;
                    if(earlier.status.equals(PENDING)||earlier.status.equals(WAITING)) {info("Còn hồ sơ đăng ký trước","Vui lòng xem hồ sơ "+earlier.id+" trước để giữ thứ tự sắp xếp của đợt.");return;}
                }
                new AlertDialog.Builder(this).setTitle("Xác nhận điều kiện tham gia").setMessage("Bạn đã kiểm tra hồ sơ theo tiêu chí công bố của đợt? Số suất được cấp dựa trên số xác nhận cung cấp, không dựa trên cam kết.").setNegativeButton("Quay lại",null).setPositiveButton("Đã kiểm tra",(d,w)->{toast(store.allocate(r));render();}).show();
            },false);
            if(r.status.equals(ALLOCATED))action(card,"Xem mã để cung cấp cho gia đình",()->openTicket(r),false);
        }
        if(n==0)empty(content,"Không có hồ sơ ở trạng thái này","Danh sách sẽ cập nhật khi có đăng ký hoặc thay đổi kết quả.");
    }
    private void editSupply(Campaign c) {
        LinearLayout form=vertical();form.setPadding(dp(24),dp(8),dp(24),0);
        label(form,"Số xác nhận cung cấp",13,INK,true,8);EditText confirmed=rawInput("Số suất",String.valueOf(c.confirmed),InputType.TYPE_CLASS_NUMBER);form.addView(confirmed);
        label(form,"Số thực chuẩn bị",13,INK,true,16);EditText prepared=rawInput("Số suất",String.valueOf(c.prepared),InputType.TYPE_CLASS_NUMBER);form.addView(prepared);
        label(form,"Ghi chú xử lý phần chưa trao",13,INK,true,16);EditText note=rawInput("Cách xử lý thực tế",c.note,InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE);form.addView(note);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Cập nhật đợt hỗ trợ").setView(form).setNegativeButton("Hủy",null).setPositiveButton("Lưu",null).create();
        dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            int cf=number(confirmed),pr=number(prepared);
            int minimum=Math.max(store.assigned(c.id),store.received(c));
            if(cf<minimum||cf>1000){confirmed.setError("Từ "+minimum+" đến 1.000 suất");return;}
            if(pr<store.received(c)||pr>cf){prepared.setError("Từ "+store.received(c)+" đến "+cf+" suất");return;}
            c.confirmed=cf;c.prepared=pr;c.note=note.getText().toString().trim();store.save();dialog.dismiss();render();
        }));dialog.show();
    }
    private void createCampaign() {
        heading("Thêm đợt hỗ trợ","Một nhóm tổ chức · Một điểm phát minh họa");
        EditText title=input(content,"title","Tên đợt hỗ trợ","",InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        EditText offset=input(content,"day","Sau bao nhiêu ngày? (0 = hôm nay)","1",InputType.TYPE_CLASS_NUMBER);
        EditText time=input(content,"time","Khung giờ nhận suất","11:00 – 12:00",InputType.TYPE_CLASS_TEXT);
        EditText capacity=input(content,"capacity","Số suất đã xác nhận cung cấp","30",InputType.TYPE_CLASS_NUMBER);
        EditText deadline=input(content,"deadline","Thời hạn đăng ký","18:00 ngày trước buổi phát",InputType.TYPE_CLASS_TEXT);
        EditText rules=input(content,"rules","Tiêu chí và quy tắc sắp xếp","Mỗi người chăm sóc 1 suất; hồ sơ đủ điều kiện được sắp xếp theo thứ tự đăng ký.",InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        Spinner meal=spinner(content,"meal",new String[]{"Bữa trưa","Bữa chiều"});
        body("Điểm phát: An Tâm · khu sinh hoạt chung (mẫu). Đợt mới được lưu trên thiết bị này để thử giao diện.");
        action(content,"Tạo đợt hỗ trợ",()->{
            if(title.getText().toString().trim().length()<3){title.setError("Nhập tên đợt hỗ trợ");return;}
            int day=number(offset),qty=number(capacity);
            if(day<0||day>365){offset.setError("Nhập số từ 0 đến 365");return;}
            if(qty<1||qty>1000){capacity.setError("Nhập số từ 1 đến 1.000");return;}
            if(time.getText().toString().trim().isEmpty()||deadline.getText().toString().trim().isEmpty()||rules.getText().toString().trim().isEmpty()){info("Thiếu thông tin","Điền thời gian, thời hạn và tiêu chí trước khi tạo đợt.");return;}
            Campaign c=new Campaign("c"+System.currentTimeMillis(),title.getText().toString().trim(),meal.getSelectedItem().toString(),day,qty,time.getText().toString().trim());
            c.prepared=0;c.pledged=qty;c.registrationOpen=true;c.deadline=deadline.getText().toString().trim();c.rules=rules.getText().toString().trim();store.campaigns.add(0,c);store.save();
            drafts.remove(draftKey());fields.clear();spinners.clear();campaignId=c.id;screen="manage";back.clear();hideKeyboard();render();
        },true);
    }
    private void distribution() {
        heading("Trao một bữa cơm,\ngửi một niềm vui.","Kiểm tra đúng mã, xác nhận sau khi giao suất.");
        chooseCampaign(content,false);Campaign c=store.campaign(campaignId);
        LinearLayout stats=card(content,PALE);metrics(stats,new String[]{"Thực chuẩn bị","Đã trao","Chưa trao"},new int[]{c.prepared,store.received(c),Math.max(0,c.prepared-store.received(c))});
        LinearLayout scan=card(content,WHITE);UiIcon icon=new UiIcon(this,"scan",GREEN);LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(dp(64),dp(64));sp.gravity=Gravity.CENTER_HORIZONTAL;scan.addView(icon,sp);
        label(scan,"Nhập mã nhận suất",21,INK,true,18);label(scan,"Nhờ người nhận đọc mã ngắn bên dưới QR.",13,MUTED,false,8);
        EditText code=input(scan,"claim_code","Mã ngắn","",InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);code.setHint("Ví dụ: BC0013");
        action(scan,"Kiểm tra mã",()->checkCode(code.getText().toString().trim()),true);
        action(scan,"Dùng mã mẫu BC0013",()->code.setText("BC0013"),false);
        body("Bản UI hiện hỗ trợ nhập mã ngắn. Quét QR bằng camera sẽ cần tích hợp ở bước phát triển chức năng.");
        Switch network=new Switch(this);network.setText("Mô phỏng có kết nối");network.setTextSize(14);network.setTextColor(INK);network.setChecked(connected);network.setMinHeight(dp(52));content.addView(network);
        network.setOnCheckedChangeListener((button,checked)->{connected=checked;toast(checked?"Đã bật trạng thái kết nối mẫu.":"Mất kết nối mẫu: không thể xác nhận giao suất.");});
        action(content,"Cần điều phối viên hỗ trợ",()->info("Hỗ trợ tại điểm phát","Nếu không có mã, mã sai hoặc mất kết nối, chuyển đến bàn điều phối. Dùng danh sách dự phòng theo quy trình của đơn vị; chỉ cập nhật đã giao sau khi thông tin được kiểm tra."),false);
    }
    private void checkCode(String code) {
        if(!connected){info("Chưa thể kiểm tra mã","Đang mô phỏng mất kết nối. Báo điều phối viên và dùng danh sách dự phòng; chưa ghi nhận giao thành công.");return;}
        Registration r=store.findCode(code);String error=store.claimError(r,campaignId);
        if(error!=null){info("Cần kiểm tra lại",error);return;}
        new AlertDialog.Builder(this).setTitle("Mã hợp lệ · 1 suất ăn").setMessage(r.name+"\n"+store.campaign(campaignId).title+"\n\nChỉ xác nhận sau khi bạn đã giao suất ăn cho người nhận.")
            .setNegativeButton("Chưa giao",null).setPositiveButton("Đã giao suất",(d,w)->{
                if(!connected){info("Chưa lưu giao nhận","Mất kết nối mẫu. Cần đối chiếu với điều phối viên.");return;}
                String recheck=store.claimError(r,campaignId);if(recheck!=null){info("Cần kiểm tra lại",recheck);return;}
                r.status=RECEIVED;store.save();toast("Đã ghi nhận giao 1 suất (dữ liệu mẫu).");render();
            }).show();
    }
    private void handedOver() {
        heading("Những suất đã trao","Ghi nhận trong đợt đang phụ trách · Dữ liệu mẫu");chooseCampaign(content,false);Campaign c=store.campaign(campaignId);
        LinearLayout total=card(content,PALE);label(total,store.received(c)+" suất đã trao",26,GREEN,true,0);label(total,c.title,14,MUTED,false,10);
        int n=0;for(Registration r:store.inCampaign(c.id))if(r.status.equals(RECEIVED)){n++;LinearLayout card=card(content,WHITE);iconLine(card,"check",r.name);label(card,r.code+" · Đã dùng · 1 suất",12,MUTED,false,10);}
        if(n==0)empty(content,"Chưa có lượt giao","Các lượt đã xác nhận sẽ xuất hiện ở đây.");
    }
    private void help() {
        heading("Hỗ trợ tại điểm phát","Hướng dẫn khi gặp tình huống cần kiểm tra.");
        section("Không tìm thấy mã");body("Hướng dẫn gia đình đến bàn điều phối để tra cứu hoặc đăng ký hộ. Không yêu cầu cung cấp ảnh trẻ hoặc bệnh án.");
        section("Mã đã sử dụng hoặc sai đợt");body("Kiểm tra lại đợt đang chọn và chuyển điều phối viên xử lý. Không xác nhận giao thêm từ mã đã dùng.");
        section("Khi mất kết nối");body("Sử dụng danh sách dự phòng theo quy trình của đơn vị. Không mặc định thao tác chưa hoàn tất là đã giao. Đối chiếu rồi cập nhật khi có thể.");
        action(content,"Đầu mối hỗ trợ",()->info("Bàn điều phối","Điểm phát An Tâm · khu sinh hoạt chung (mẫu). Chưa có số điện thoại thực được xác nhận."),true);
    }
    private void donorHome() {
        pill(content,"MỖI SUẤT ĂN, MỘT SỰ ĐỒNG HÀNH",PALE,GREEN);
        heading("Góp một bữa cơm,\ntiếp thêm vững lòng.","Cùng nhóm hỗ trợ chăm lo bữa ăn cho người chăm sóc bệnh nhi.");
        MealArtView art=new MealArtView(this);art.setBackground(round(PALE,24));content.addView(art,new LinearLayout.LayoutParams(-1,dp(180)));gap(content,22);
        section("Chọn đợt bạn muốn đồng hành");
        for(Campaign c:store.campaigns)if(!c.archived){
            LinearLayout card=card(content,WHITE);label(card,c.title,19,INK,true,0);label(card,c.date()+" · "+c.meal,13,MUTED,false,8);
            metrics(card,new String[]{"Cam kết","Đã xác nhận"},new int[]{c.pledged,c.confirmed});
            label(card,"Cam kết chưa đồng nghĩa với suất đã có. Đơn vị sẽ liên hệ xác nhận khả năng cung cấp.",12,MUTED,false,12);
            action(card,"Đăng ký đồng hành  →",()->{campaignId=c.id;go("pledge");},true);
        }
        body("Ứng dụng không giữ tiền và không tự xác nhận chuyển khoản. Nhà hảo tâm và đơn vị tổ chức thống nhất việc cung cấp suất ăn.");
    }
    private void pledge() {
        Campaign c=store.campaign(campaignId);heading("Đồng hành cùng đợt",c.title+" · "+c.date());
        EditText name=input(content,"donor_name","Tên cá nhân hoặc nhóm *","",InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        EditText phone=input(content,"donor_phone","Số điện thoại liên hệ *","",InputType.TYPE_CLASS_PHONE);
        EditText quantity=input(content,"quantity","Số suất mong muốn hỗ trợ *","10",InputType.TYPE_CLASS_NUMBER);
        label(content,"Hình thức đồng hành",13,INK,true,18);Spinner cadence=spinner(content,"cadence",new String[]{"Theo đợt","Mong muốn theo tuần","Mong muốn theo tháng"});
        body("Đơn vị tổ chức sẽ liên hệ để thống nhất. Mong muốn theo tuần hoặc tháng vẫn cần xác nhận số suất thực tế cho từng đợt.");
        consent=new CheckBox(this);consent.setText("Tôi đồng ý để đơn vị liên hệ về đăng ký đồng hành.");consent.setTextSize(13);consent.setTextColor(INK);content.addView(consent);
        Bundle b=drafts.getBundle(draftKey());if(b!=null)consent.setChecked(b.getBoolean("consent"));
        action(content,"Gửi cam kết đồng hành",()->{
            if(!validName(name)||!validPhone(phone))return;int q=number(quantity);
            if(q<1||q>1000){quantity.setError("Nhập từ 1 đến 1.000 suất");return;}
            if(!consent.isChecked()){info("Cần sự đồng ý","Vui lòng đồng ý để đơn vị liên hệ về đăng ký.");return;}
            // Keep the demo bounded to one active commitment, replacing its previous quantity.
            if(store.donorPledged>0){Campaign old=store.campaign(store.donorCampaign);old.pledged=Math.max(0,old.pledged-store.donorPledged);}
            c.pledged+=q;store.donorPledged=q;store.donorCampaign=c.id;store.donorCadence=cadence.getSelectedItem().toString();store.save();
            drafts.remove(draftKey());fields.clear();spinners.clear();consent=null;switchTab(1);info("Đã ghi nhận cam kết","Cam kết đang chờ đơn vị liên hệ. Số suất xác nhận cung cấp chưa thay đổi.");
        },true);
        body("Bản UI lưu một cam kết mẫu đang hoạt động. Gửi lại sẽ thay thế cam kết mẫu trước đó.");
    }
    private void commitments() {
        heading("Cam kết của tôi","Theo dõi số suất bạn mong muốn đồng hành.");
        if(store.donorPledged==0){empty(content,"Bắt đầu từ một bữa ăn","Chọn một đợt phù hợp và gửi mong muốn đồng hành.");action(content,"Xem đợt hỗ trợ",()->switchTab(0),true);return;}
        Campaign c=store.campaign(store.donorCampaign);LinearLayout card=card(content,WHITE);pill(card,"CHỜ ĐƠN VỊ LIÊN HỆ",0xFFFFF0DE,ORANGE);
        label(card,store.donorPledged+" suất ăn",32,GREEN,true,20);label(card,c.title,19,INK,true,12);label(card,c.date()+" · "+store.donorCadence,13,MUTED,false,8);
        label(card,"Đây là số suất cam kết. Số suất xác nhận cung cấp của đợt được đơn vị cập nhật riêng.",14,MUTED,false,18);
        action(card,"Điều chỉnh cam kết",()->{campaignId=c.id;go("pledge");},false);
        section("Bạn có thể theo dõi điều gì?");body("Báo cáo tổng hợp phân biệt cam kết, xác nhận cung cấp, thực chuẩn bị, đã trao và chưa trao. Không công khai thông tin riêng của trẻ hoặc người chăm sóc.");
        action(content,"Xem kết quả các đợt",()->switchTab(2),true);
    }
    private void reports() {
        heading("Từng suất ăn,\nđược ghi nhận rõ.","Báo cáo tổng hợp · Tất cả số liệu là minh họa");
        chooseCampaign(content,true);Campaign c=store.campaign(campaignId);reportCard(c);
        if(role==1)action(content,"Cập nhật thực chuẩn bị & ghi chú",()->editSupply(c),false);
        section("Hiểu đúng các con số");
        body("Cam kết là mong muốn hỗ trợ, chưa mặc định đã có suất. Xác nhận cung cấp là khả năng đơn vị đã thống nhất. Thực chuẩn bị được ghi cho buổi phát. Đã trao là lượt đã được xác nhận giao.");
        body("Báo cáo chỉ sử dụng số liệu tổng hợp, không hiển thị danh tính hoặc thông tin bệnh án của trẻ.");
    }
    private void impact() {
        heading("Những bữa cơm\nđược sẻ chia.","Kết quả tổng hợp từ nhóm tổ chức · Dữ liệu mẫu");
        Campaign c=store.campaign("c0");LinearLayout intro=card(content,PALE);label(intro,"28",48,GREEN,true,0);label(intro,"suất ăn đã trao trong đợt minh họa",16,INK,true,6);
        label(intro,"Mỗi bữa ăn là một sự đồng hành cùng gia đình.",13,MUTED,false,12);section("Đợt hỗ trợ đã kết thúc");reportCard(c);
    }
    private void reportCard(Campaign c) {
        LinearLayout card=card(content,WHITE);pill(card,c.archived?"ĐÃ ĐỐI CHIẾU · MẪU":"ĐANG CẬP NHẬT · MẪU",PALE,GREEN);
        label(card,c.title,21,INK,true,16);label(card,c.date()+" · "+c.meal,13,MUTED,false,8);gap(card,18);
        statLine(card,"Số suất cam kết",c.pledged,false);statLine(card,"Xác nhận cung cấp",c.confirmed,false);statLine(card,"Thực chuẩn bị",c.prepared,false);statLine(card,"Đã trao",store.received(c),true);statLine(card,"Chưa trao",Math.max(0,c.prepared-store.received(c)),false);
        int received=store.received(c);ProgressBar progress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);progress.setMax(Math.max(1,c.prepared));progress.setProgress(received);progress.setProgressTintList(android.content.res.ColorStateList.valueOf(GREEN));card.addView(progress,new LinearLayout.LayoutParams(-1,dp(8)));
        label(card,c.archived?"Ghi chú sau buổi phát":"Ghi chú của đơn vị",12,INK,true,18);label(card,c.note,13,MUTED,false,8);
        if(!c.archived)label(card,"Số chưa trao đang thay đổi trong buổi phát; chưa phải số người không đến nhận.",11,MUTED,false,12);
    }
    private void profile() {
        heading("Không gian của bạn","Bạn đang xem vai trò "+roles[role].toLowerCase(new java.util.Locale("vi"))+".");
        LinearLayout card=card(content,WHITE);iconLine(card,"user",roles[role]);label(card,"Tài khoản trình diễn · Chưa đăng nhập hệ thống thật",13,MUTED,false,12);action(card,"Đổi vai trò để xem giao diện",this::chooseRole,true);
        section("Thông tin của bạn");body("Dữ liệu nhập được lưu trong bộ nhớ riêng của ứng dụng trên thiết bị, dùng để thử giao diện. Bản UI chưa kết nối máy chủ, thanh toán, gửi tin nhắn hoặc camera.");
        section("Quyền riêng tư của gia đình");body("Không yêu cầu tên trẻ, hình ảnh hay bệnh án. Báo cáo dành cho nhà hảo tâm chỉ chứa số liệu tổng hợp.");
        action(content,"Hướng dẫn trình diễn",()->info("Thử một quy trình hoàn chỉnh","1. Gia đình: đăng ký đợt ngày mai.\n2. Đổi sang Điều phối: mở cùng đợt, kiểm tra và sắp xếp hồ sơ.\n3. Gia đình: mở Đăng ký để thấy mã nhận.\n4. Phát suất hôm nay: nhập BC0013, xác nhận sau khi giao; thử lại mã để thấy cảnh báo.\n5. Xem báo cáo đã cập nhật."),false);
        action(content,"Khôi phục dữ liệu mẫu",()->new AlertDialog.Builder(this).setTitle("Khôi phục dữ liệu mẫu?").setMessage("Xóa các đăng ký, cam kết và thay đổi bạn đã nhập trên thiết bị; đưa ứng dụng về dữ liệu mẫu ban đầu.").setNegativeButton("Giữ dữ liệu",null).setPositiveButton("Khôi phục",(d,w)->{store.reset();drafts.clear();fields.clear();consent=null;filter=0;campaignId="c1";registrationId="R0013";listFilter="Tất cả";connected=true;toast("Đã khôi phục dữ liệu mẫu.");render();}).show(),false);
        label(content,"BỮA CƠM BÊN CON  /  MEALS BESIDE YOU",10,GREEN,true,28);label(content,"Đồ án giao diện Android · Phiên bản 1.0",12,MUTED,false,8);
    }
    private void chooseCampaign(LinearLayout parent,boolean includeArchived) {
        List<Campaign> options=new ArrayList<>();for(Campaign c:store.campaigns)if(includeArchived||!c.archived)options.add(c);
        Campaign selected=null;for(Campaign c:options)if(c.id.equals(campaignId))selected=c;
        if(selected==null){selected=options.get(0);campaignId=selected.id;}
        final Campaign current=selected;TextView picker=text(current.title+"  ⌄",15,GREEN,true);picker.setPadding(dp(16),dp(16),dp(16),dp(16));picker.setMinHeight(dp(54));picker.setBackground(outline(WHITE,16));parent.addView(picker,new LinearLayout.LayoutParams(-1,-2));gap(parent,16);
        picker.setOnClickListener(v->{String[] names=new String[options.size()];for(int i=0;i<names.length;i++)names[i]=options.get(i).title+" · "+options.get(i).date();
            new AlertDialog.Builder(this).setTitle("Chọn đợt hỗ trợ").setItems(names,(d,which)->{capture();campaignId=options.get(which).id;render();}).show();});
    }
    private int dp(float n) {return Math.round(n*getResources().getDisplayMetrics().density);}
    private LinearLayout vertical() {LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private LinearLayout horizontal() {LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    private GradientDrawable round(int fill,int radius) {GradientDrawable d=new GradientDrawable();d.setColor(fill);d.setCornerRadius(dp(radius));return d;}
    private GradientDrawable outline(int fill,int radius) {GradientDrawable d=round(fill,radius);d.setStroke(dp(1),LINE);return d;}
    private TextView text(String s,int size,int color,boolean bold) {TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setFontFeatureSettings("kern");t.setTypeface(Typeface.create(bold?"sans-serif-medium":"sans-serif",Typeface.NORMAL));t.setLineSpacing(dp(3),1);t.setIncludeFontPadding(false);return t;}
    private void label(LinearLayout parent,String s,int size,int color,boolean bold,int top) {TextView t=text(s,size,color,bold);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(top);parent.addView(t,lp);}
    private void gap(LinearLayout p,int size) {View v=new View(this);p.addView(v,new LinearLayout.LayoutParams(1,dp(size)));}
    private void heading(String title,String subtitle) {label(content,title,29,INK,true,8);label(content,subtitle,14,MUTED,false,12);gap(content,24);}
    private void section(String s) {label(content,s,19,INK,true,22);gap(content,12);}
    private void body(String s) {label(content,s,14,MUTED,false,8);gap(content,10);}
    private LinearLayout card(LinearLayout parent,int color) {LinearLayout l=vertical();l.setPadding(dp(18),dp(18),dp(18),dp(18));l.setBackground(color==WHITE?outline(color,22):round(color,22));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(14);parent.addView(l,lp);return l;}
    private void pill(LinearLayout parent,String s,int fill,int color) {TextView t=text(s,10,color,true);t.setPadding(dp(10),dp(7),dp(10),dp(7));t.setBackground(round(fill,9));parent.addView(t,new LinearLayout.LayoutParams(-2,-2));}
    private void action(LinearLayout p,String s,Runnable click,boolean primary) {
        Button b=new Button(this,null,android.R.attr.borderlessButtonStyle);b.setText(s);b.setTextSize(14);b.setAllCaps(false);b.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));b.setTextColor(primary?WHITE:GREEN);b.setPadding(dp(12),dp(12),dp(12),dp(12));b.setMinHeight(dp(52));b.setMinimumHeight(dp(52));b.setBackground(primary?getDrawable(R.drawable.button_primary):round(0xFFF1F5EF,14));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(14);p.addView(b,lp);b.setOnClickListener(v->click.run());
    }
    private void chip(LinearLayout p,String s,boolean selected,Runnable click) {TextView t=text(s,12,selected?WHITE:MUTED,selected);t.setGravity(Gravity.CENTER);t.setPadding(dp(14),dp(10),dp(14),dp(10));t.setMinHeight(dp(48));t.setBackground(selected?round(GREEN,13):outline(WHITE,13));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,-2);lp.rightMargin=dp(8);p.addView(t,lp);t.setOnClickListener(v->click.run());}
    private void iconLine(LinearLayout parent,String kind,String value) {LinearLayout row=horizontal();UiIcon icon=new UiIcon(this,kind,GREEN);row.addView(icon,new LinearLayout.LayoutParams(dp(18),dp(18)));TextView t=text(value,13,MUTED,false);LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(0,-2,1);tp.leftMargin=dp(10);row.addView(t,tp);parent.addView(row);}
    private void metrics(LinearLayout parent,String[] names,int[] values) {LinearLayout row=horizontal();row.setGravity(Gravity.TOP);LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,-2);rp.topMargin=dp(18);parent.addView(row,rp);for(int i=0;i<names.length;i++){LinearLayout col=vertical();row.addView(col,new LinearLayout.LayoutParams(0,-2,1));label(col,String.valueOf(values[i]),27,GREEN,true,0);label(col,names[i],10,MUTED,false,8);}}
    private void statLine(LinearLayout parent,String name,int n,boolean bold) {LinearLayout row=horizontal();TextView label=text(name,14,bold?GREEN:MUTED,bold);row.addView(label,new LinearLayout.LayoutParams(0,-2,1));TextView value=text(String.valueOf(n),bold?28:19,bold?GREEN:INK,true);row.addView(value);row.setPadding(0,dp(8),0,dp(8));parent.addView(row);}
    private void empty(LinearLayout parent,String title,String subtitle) {LinearLayout card=card(parent,PALE);label(card,title,21,INK,true,8);label(card,subtitle,14,MUTED,false,12);}
    private EditText rawInput(String hint,String value,int type) {EditText e=new EditText(this);e.setTextSize(15);e.setTextColor(INK);e.setHintTextColor(MUTED);e.setInputType(type);e.setHint(hint);e.setText(value);e.setPadding(dp(14),dp(14),dp(14),dp(14));e.setMinHeight(dp(54));e.setBackground(outline(WHITE,14));e.setMaxLines((type&InputType.TYPE_TEXT_FLAG_MULTI_LINE)!=0?5:1);return e;}
    private EditText input(LinearLayout parent,String key,String title,String value,int type) {label(parent,title,13,INK,true,16);EditText e=rawInput(title,value,type);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(8);parent.addView(e,lp);bindField(key,e,value);return e;}
    private void bindField(String key,EditText e,String fallback) {fields.put(key,e);Bundle b=drafts.getBundle(draftKey());e.setText(b==null?fallback:b.getString(key,fallback));e.setSaveEnabled(false);}
    private Spinner spinner(LinearLayout parent,String key,String[] options) {Spinner s=new Spinner(this);ArrayAdapter<String> a=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,options);s.setAdapter(a);s.setBackground(outline(WHITE,14));s.setPadding(dp(8),dp(4),dp(8),dp(4));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(56));lp.topMargin=dp(16);lp.bottomMargin=dp(12);parent.addView(s,lp);spinners.put(key,s);Bundle b=drafts.getBundle(draftKey());if(b!=null)s.setSelection(b.getInt("spinner:"+key,0));return s;}
    private int number(EditText e) {try{return Integer.parseInt(e.getText().toString().trim());}catch(NumberFormatException ex){return -1;}}
    private boolean validName(EditText e) {if(e.getText().toString().trim().length()<2){e.setError("Nhập tên có ít nhất 2 ký tự");e.requestFocus();return false;}return true;}
    private boolean validPhone(EditText e) {String p=e.getText().toString().replaceAll("[\\s.-]","");if(!p.matches("(0[0-9]{9}|\\+84[0-9]{9})")){e.setError("Nhập số gồm 10 chữ số bắt đầu bằng 0 hoặc +84");e.requestFocus();return false;}return true;}
    private void info(String title,String message) {new AlertDialog.Builder(this).setTitle(title).setMessage(message).setPositiveButton("Đã hiểu",null).show();}
    private void toast(String message) {Toast.makeText(this,message,Toast.LENGTH_LONG).show();}
}
