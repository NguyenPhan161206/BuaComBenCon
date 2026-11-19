import com.android.ide.common.rendering.api.*;
import com.android.ide.common.resources.*;
import com.android.ide.common.resources.configuration.*;
import com.android.resources.*;
import com.android.resources.aar.*;
import com.android.layoutlib.bridge.Bridge;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.*;
import java.nio.file.*;
import java.io.*;
import java.util.*;
import javax.imageio.ImageIO;

public class RenderPreview {
 static final Path STUDIO=Path.of(System.getProperty("studio.dir","/home/binhnguyen/android-studio"));
 static final Path SDK=Path.of(System.getProperty("sdk.dir","/home/binhnguyen/Android/Sdk"));
 static final Path DATA=STUDIO.resolve("plugins/design-tools/resources/layoutlib/data");
 static int width=786,height=1704;
 static final ResourceNamespace NS=ResourceNamespace.RES_AUTO;
 static class Parser extends KXmlParser implements ILayoutPullParser {
  Parser(String file)throws Exception{setFeature(FEATURE_PROCESS_NAMESPACES,true);setInput(new FileReader(file));}
  public Object getViewCookie(){return null;}
  public ResourceNamespace getLayoutNamespace(){return NS;}
 }
 static class Callback extends LayoutlibCallback {
  Map<Integer,ResourceReference> ids=new HashMap<>();Map<ResourceReference,Integer> reverse=new HashMap<>();int next=0x7f100000;
  Callback()throws Exception{for(Class<?> c:vn.edu.buacombencon.R.class.getDeclaredClasses())for(java.lang.reflect.Field f:c.getFields()){ResourceType t=ResourceType.fromClassName(c.getSimpleName());if(t!=null&&f.getType()==int.class){ResourceReference r=new ResourceReference(NS,t,f.getName());int id=f.getInt(null);ids.put(id,r);reverse.put(r,id);}}}
  public Object loadView(String name,Class[] types,Object[] args)throws Exception{return Class.forName(name).getConstructor(types).newInstance(args);}
  public ResourceReference resolveResourceId(int id){return ids.get(id);}
  public int getOrGenerateResourceId(ResourceReference r){Integer v=reverse.get(r);if(v!=null)return v;int id=next++;reverse.put(r,id);ids.put(id,r);return id;}
  public ILayoutPullParser getParser(ResourceValue v){try{return new Parser(v.getValue());}catch(Exception e){return null;}}
  public AdapterBinding getAdapterBinding(Object o,Map<String,String> a){return null;}
  public ActionBarCallback getActionBarCallback(){return null;}
  public XmlPullParser createXmlParserForPsiFile(String f){return createXmlParserForFile(f);}
  public XmlPullParser createXmlParserForFile(String f){try{return new Parser(f);}catch(Exception e){throw new RuntimeException(e);}}
  public XmlPullParser createXmlParser(){KXmlParser p=new KXmlParser();try{p.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES,true);}catch(Exception e){}return p;}
  public void error(String msg,Throwable t){System.err.println(msg);if(t!=null)t.printStackTrace();}
  public String getApplicationId(){return "vn.edu.buacombencon";}
 }

 static class PreviewActivity extends vn.edu.buacombencon.MainActivity {
  android.view.View root;
  android.content.Context bridgeContext;
  PreviewActivity(android.content.Context base,android.view.View root){attachBaseContext(base);bridgeContext=base;this.root=root;}
  public android.view.View getCurrentFocus(){return null;}
  public android.window.OnBackInvokedDispatcher getOnBackInvokedDispatcher(){return new android.window.OnBackInvokedDispatcher(){public void registerOnBackInvokedCallback(int priority,android.window.OnBackInvokedCallback callback){}public void unregisterOnBackInvokedCallback(android.window.OnBackInvokedCallback callback){}};}
  public android.view.LayoutInflater getLayoutInflater(){return android.view.LayoutInflater.from(bridgeContext);}
  public <T extends android.view.View> T findViewById(int id){return root.findViewById(id);}
  public android.content.res.Resources.Theme getTheme(){return bridgeContext.getTheme();}
  public android.content.SharedPreferences getSharedPreferences(String n,int mode){return (android.content.SharedPreferences)DomainChecks.prefProxy(android.content.SharedPreferences.class);}
 }
 static void set(Object a,String key,Object value)throws Exception{java.lang.reflect.Field f=vn.edu.buacombencon.MainActivity.class.getDeclaredField(key);f.setAccessible(true);f.set(a,value);}
 static void populate(RenderSession session,String[] args){
  session.execute(()->{try{
   android.view.View root=(android.view.View)session.getRootViews().get(0).getViewObject();
   PreviewActivity activity=new PreviewActivity(root.getContext(),root);
   set(activity,"store",new vn.edu.buacombencon.DemoStore(activity));
   set(activity,"content",root.findViewById(vn.edu.buacombencon.R.id.content));
   set(activity,"nav",root.findViewById(vn.edu.buacombencon.R.id.bottom_nav));
   set(activity,"scroll",root.findViewById(vn.edu.buacombencon.R.id.scroll));
   if(args.length>2)set(activity,"role",Integer.parseInt(args[2]));
   if(args.length>3)set(activity,"tab",Integer.parseInt(args[3]));
   if(args.length>4)set(activity,"screen",args[4]);
   if(args.length>5)set(activity,"campaignId",args[5]);
   java.lang.reflect.Method m=vn.edu.buacombencon.MainActivity.class.getDeclaredMethod("render");m.setAccessible(true);m.invoke(activity);System.out.println("Content children="+((android.widget.LinearLayout)root.findViewById(vn.edu.buacombencon.R.id.content)).getChildCount());root.requestLayout();root.measure(android.view.View.MeasureSpec.makeMeasureSpec(width,android.view.View.MeasureSpec.EXACTLY),android.view.View.MeasureSpec.makeMeasureSpec(height,android.view.View.MeasureSpec.EXACTLY));root.layout(0,0,width,height);android.graphics.Bitmap bitmap=android.graphics.Bitmap.createBitmap(width,height,android.graphics.Bitmap.Config.ARGB_8888);root.draw(new android.graphics.Canvas(bitmap));audit(root);try(java.io.FileOutputStream file=new java.io.FileOutputStream(args[1])){bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,file);}
   android.widget.ScrollView scroll=root.findViewById(vn.edu.buacombencon.R.id.scroll);scroll.scrollTo(0,scroll.getChildAt(0).getHeight());android.graphics.Bitmap bottom=android.graphics.Bitmap.createBitmap(width,height,android.graphics.Bitmap.Config.ARGB_8888);root.draw(new android.graphics.Canvas(bottom));try(java.io.FileOutputStream file=new java.io.FileOutputStream(args[1].replace(".png","-bottom.png"))){bottom.compress(android.graphics.Bitmap.CompressFormat.PNG,100,file);}
  }catch(Exception e){throw new RuntimeException(e);}});
 }

 static void audit(android.view.View v){
  if(v instanceof android.widget.TextView){android.widget.TextView t=(android.widget.TextView)v;android.text.Layout l=t.getLayout();if(l!=null&&l.getHeight()>t.getHeight()-t.getCompoundPaddingTop()-t.getCompoundPaddingBottom()+2)System.err.println("CLIPPED TEXT: "+t.getText()+" need="+l.getHeight()+" got="+t.getHeight());}
  if(v instanceof android.view.ViewGroup){android.view.ViewGroup g=(android.view.ViewGroup)v;for(int i=0;i<g.getChildCount();i++)audit(g.getChildAt(i));}
 }
 static ILayoutLog LOG=new ILayoutLog(){public void error(String tag,String msg,Throwable t,Object data,Object cookie){System.err.println("ERROR "+tag+": "+msg);if(t!=null)t.printStackTrace();}public void error(String tag,String msg,Object data,Object cookie){System.err.println("ERROR "+tag+": "+msg);}public void warning(String tag,String msg,Object data,Object cookie){System.err.println("WARN "+tag+": "+msg);}};
 public static void main(String[] a)throws Exception{
  Path project=Path.of(a[0]);int w=a.length>6?Integer.parseInt(a[6]):393;int h=a.length>7?Integer.parseInt(a[7]):852;float font=a.length>8?Float.parseFloat(a[8]):1f;width=w*2;height=h*2;
  FrameworkResourceRepository framework=FrameworkResourceRepository.create(SDK.resolve("platforms/android-36/data/res"),Set.of("en"),null,false);
  AarSourceResourceRepository app=AarSourceResourceRepository.createForTest(project.resolve("app/src/main/res"),NS,null);
  FolderConfiguration cfg=FolderConfiguration.getConfigForQualifierString("en-rUS-ldltr-sw"+Math.min(w,h)+"dp-w"+w+"dp-h"+h+"dp-normal-long-notround-"+(w>h?"land":"port")+"-notnight-xhdpi-finger-keyssoft-nokeys-navhidden-nonav-v36");
  Map<ResourceNamespace,Map<ResourceType,ResourceValueMap>> resources=new HashMap<>();
  resources.put(ResourceNamespace.ANDROID,new HashMap<>(ResourceRepositoryUtil.getConfiguredResources(framework,cfg).row(ResourceNamespace.ANDROID)));
  resources.put(NS,new HashMap<>(ResourceRepositoryUtil.getConfiguredResources(app,cfg).row(NS)));
  ResourceResolver resolver=ResourceResolver.create(resources,new ResourceReference(NS,ResourceType.STYLE,"AppTheme"));
  Map<String,Map<String,Integer>> enums=new HashMap<>();for(ResourceItem ri:framework.getAllResources()){ResourceValue v=ri.getResourceValue();if(v instanceof AttrResourceValue)enums.put(v.getName(),((AttrResourceValue)v).getAttributeValues());}
  Properties props=new Properties();try(InputStream in=Files.newInputStream(SDK.resolve("platforms/android-36/build.prop"))){props.load(in);}Map<String,String> properties=new HashMap<>();props.forEach((k,v)->properties.put(k.toString(),v.toString()));
  properties.put("ro.product.cpu.abilist","x86_64,x86");properties.put("ro.product.cpu.abilist64","x86_64");properties.put("ro.product.cpu.abilist32","x86");properties.put("ro.build.version.sdk","36");properties.put("ro.build.version.codename","REL");
  Bridge b=new Bridge();boolean ok=b.init(properties,DATA.resolve("fonts").toFile(),DATA.resolve("linux/lib64").toString(),DATA.resolve("icu/icudt78l.dat").toString(),DATA.resolve("hyphen-data").toString(),new String[0],enums,LOG);if(!ok)throw new RuntimeException("Bridge init failed");
  HardwareConfig hw=new HardwareConfig(width,height,Density.XHIGH,320,320,ScreenSize.NORMAL,w>h?ScreenOrientation.LANDSCAPE:ScreenOrientation.PORTRAIT,ScreenRound.NOTROUND,false);
  Callback callback=new Callback();SessionParams params=new SessionParams(new Parser(project.resolve("app/src/main/res/layout/activity_main.xml").toString()),SessionParams.RenderingMode.NORMAL,new Object(),hw,resolver,callback,26,36,LOG);
  params.setAssetRepository(new AssetRepository(){public boolean isSupported(){return true;}public InputStream openNonAsset(int cookie,String path,int access)throws IOException{return new FileInputStream(path);}public InputStream openAsset(String path,int access)throws IOException{return new FileInputStream(path);}});params.setFontScale(font);params.setForceNoDecor();params.setLocale("vi");
  RenderSession session=b.createSession(params);System.out.println(session.getResult());if(session.getResult().getException()!=null)session.getResult().getException().printStackTrace();
  populate(session,a);System.out.println("Populate: "+session.getResult());if(session.getResult().getException()!=null)session.getResult().getException().printStackTrace();System.out.println("Render: "+session.render(5000));
  System.out.println("WROTE "+a[1]);
  session.dispose();b.dispose();System.exit(0);
 }
}
