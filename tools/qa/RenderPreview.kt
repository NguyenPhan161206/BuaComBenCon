import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import android.widget.TextView
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
import com.android.ide.common.rendering.api.*
import com.android.ide.common.resources.*
import com.android.ide.common.resources.configuration.FolderConfiguration
import com.android.layoutlib.bridge.Bridge
import com.android.resources.*
import com.android.resources.aar.AarSourceResourceRepository
import com.android.resources.aar.FrameworkResourceRepository
import org.kxml2.io.KXmlParser
import org.xmlpull.v1.XmlPullParser
import java.io.FileReader
import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties
import kotlin.system.exitProcess
import vn.edu.buacombencon.DemoStore
import vn.edu.buacombencon.MainActivity
import vn.edu.buacombencon.R

object RenderPreview {
    private val studio = Path.of(System.getProperty("studio.dir", "/home/binhnguyen/android-studio"))
    private val sdk = Path.of(System.getProperty("sdk.dir", "/home/binhnguyen/Android/Sdk"))
    private val data = studio.resolve("plugins/design-tools/resources/layoutlib/data")
    private val namespace = ResourceNamespace.RES_AUTO

    private class Parser(path: String) : KXmlParser(), ILayoutPullParser {
        init {
            setFeature(FEATURE_PROCESS_NAMESPACES, true)
            setInput(FileReader(path))
        }
        override fun getViewCookie(): Any? = null
        override fun getLayoutNamespace(): ResourceNamespace = RenderPreview.namespace
    }

    private class Callback : LayoutlibCallback() {
        private val ids = mutableMapOf<Int, ResourceReference>()
        private val reverse = mutableMapOf<ResourceReference, Int>()
        private var next = 0x7f100000

        init {
            for (resourceClass in R::class.java.declaredClasses) {
                val type = ResourceType.fromClassName(resourceClass.simpleName) ?: continue
                for (field in resourceClass.fields) {
                    if (field.type != Int::class.javaPrimitiveType) continue
                    val reference = ResourceReference(namespace, type, field.name)
                    val id = field.getInt(null)
                    ids[id] = reference
                    reverse[reference] = id
                }
            }
        }

        override fun loadView(name: String, types: Array<Class<*>>, args: Array<Any>): Any =
            Class.forName(name).getConstructor(*types).newInstance(*args)
        override fun resolveResourceId(id: Int) = ids[id]
        override fun getOrGenerateResourceId(resource: ResourceReference): Int =
            reverse.getOrPut(resource) { next++.also { ids[it] = resource } }
        override fun getParser(value: ResourceValue) = runCatching { Parser(value.value) }.getOrNull()
        override fun getAdapterBinding(view: Any?, attributes: Map<String, String>?) = null
        override fun getActionBarCallback() = null
        override fun createXmlParserForPsiFile(path: String) = createXmlParserForFile(path)
        override fun createXmlParserForFile(path: String): XmlPullParser = Parser(path)
        override fun createXmlParser(): XmlPullParser = KXmlParser().apply {
            setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
        }
        override fun error(message: String, throwable: Throwable?) {
            System.err.println(message)
            throwable?.printStackTrace()
        }
        override fun getApplicationId() = "vn.edu.buacombencon"
    }

    private class PreviewActivity(private val base: Context, private val root: View) : MainActivity() {
        init { attachBaseContext(base) }
        override fun getCurrentFocus(): View? = null
        override fun getOnBackInvokedDispatcher() = object : OnBackInvokedDispatcher {
            override fun registerOnBackInvokedCallback(priority: Int, callback: OnBackInvokedCallback) = Unit
            override fun unregisterOnBackInvokedCallback(callback: OnBackInvokedCallback) = Unit
        }
        override fun getLayoutInflater(): LayoutInflater = LayoutInflater.from(base)
        override fun <T : View?> findViewById(id: Int): T? = root.findViewById(id)
        override fun getTheme(): Resources.Theme = base.theme
        override fun getSharedPreferences(name: String, mode: Int) = DomainChecks.preferences()
    }

    private fun populate(session: RenderSession, args: Array<String>, width: Int, height: Int) {
        session.execute {
            val root = session.rootViews.first().viewObject as View
            val activity = PreviewActivity(root.context, root)
            val store = DemoStore(activity)
            System.getProperty("preview.status")?.let {
                checkNotNull(store.registration("R0013")).status = it
            }
            System.getProperty("preview.pledge")?.let {
                store.donorPledged = it.toInt()
                store.donorCampaign = "c1"
            }
            val type = MainActivity::class.java
            fun set(key: String, value: Any) {
                type.getDeclaredField(key).apply { isAccessible = true }.set(activity, value)
            }
            set("store", store)
            set("content", root.findViewById<View>(R.id.content))
            set("nav", root.findViewById<View>(R.id.bottom_nav))
            set("scroll", root.findViewById<View>(R.id.scroll))
            args.getOrNull(2)?.let { set("role", it.toInt()) }
            args.getOrNull(3)?.let { set("tab", it.toInt()) }
            args.getOrNull(4)?.let { set("screen", it) }
            args.getOrNull(5)?.let { set("campaignId", it) }
            System.getProperty("preview.filter")?.let { set("filter", it.toInt()) }
            type.getDeclaredMethod("render").apply { isAccessible = true }.invoke(activity)

            root.requestLayout()
            root.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY))
            root.layout(0, 0, width, height)
            fun save(path: String) {
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                root.draw(Canvas(bitmap))
                Files.newOutputStream(Path.of(path)).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            }
            save(args[1])
            audit(root)
            val scroll = root.findViewById<ScrollView>(R.id.scroll)
            scroll.scrollTo(0, scroll.getChildAt(0).height)
            save(args[1].replace(".png", "-bottom.png"))
        }
    }

    private fun audit(view: View) {
        if (view is TextView) {
            val layout = view.layout
            val available = view.height - view.compoundPaddingTop - view.compoundPaddingBottom
            if (layout != null && layout.height > available + 2) {
                System.err.println("CLIPPED TEXT: ${view.text} need=${layout.height} got=${view.height}")
            }
        }
        if (view is ViewGroup) {
            for (index in 0 until view.childCount) audit(view.getChildAt(index))
        }
    }

    private val log = object : ILayoutLog {
        override fun error(tag: String?, message: String?, error: Throwable?, data: Any?, cookie: Any?) {
            System.err.println("ERROR $tag: $message")
            error?.printStackTrace()
        }
        override fun error(tag: String?, message: String?, data: Any?, cookie: Any?) {
            System.err.println("ERROR $tag: $message")
        }
        override fun warning(tag: String?, message: String?, data: Any?, cookie: Any?) {
            System.err.println("WARN $tag: $message")
        }
    }

    @JvmStatic
    fun main(args: Array<String>) {
        val project = Path.of(args[0])
        val w = args.getOrNull(6)?.toInt() ?: 393
        val h = args.getOrNull(7)?.toInt() ?: 852
        val width = w * 2
        val height = h * 2
        val landscape = w > h
        val orientation = if (landscape) "land" else "port"
        val fontScale = args.getOrNull(8)?.toFloat() ?: 1f
        val platform = sdk.resolve("platforms/android-36")
        val framework = FrameworkResourceRepository.create(platform.resolve("data/res"), setOf("en"), null, false)
        val app = AarSourceResourceRepository.createForTest(project.resolve("app/src/main/res"), namespace, null)
        val config = FolderConfiguration.getConfigForQualifierString(
            "en-rUS-ldltr-sw${minOf(w, h)}dp-w${w}dp-h${h}dp-normal-long-notround-$orientation-notnight-xhdpi-finger-keyssoft-nokeys-navhidden-nonav-v36"
        )
        val resources = mapOf(
            ResourceNamespace.ANDROID to framework.getConfiguredResources(config).row(ResourceNamespace.ANDROID).toMap(),
            namespace to app.getConfiguredResources(config).row(namespace).toMap(),
        )
        val resolver = ResourceResolver.create(resources, ResourceReference(namespace, ResourceType.STYLE, "AppTheme"))
        val enums = mutableMapOf<String, Map<String, Int>>()
        for (item in framework.allResources) {
            val value = item.resourceValue
            if (value is AttrResourceValue) enums[value.name] = value.attributeValues
        }
        val properties = Properties()
        Files.newInputStream(platform.resolve("build.prop")).use(properties::load)
        val build = properties.entries.associate { it.key.toString() to it.value.toString() }.toMutableMap()
        build["ro.product.cpu.abilist"] = "x86_64,x86"
        build["ro.product.cpu.abilist64"] = "x86_64"
        build["ro.product.cpu.abilist32"] = "x86"
        build["ro.build.version.sdk"] = "36"
        build["ro.build.version.codename"] = "REL"

        val bridge = Bridge()
        check(bridge.init(build, data.resolve("fonts").toFile(), data.resolve("linux/lib64").toString(),
            data.resolve("icu/icudt78l.dat").toString(), data.resolve("hyphen-data").toString(),
            emptyArray(), enums, log)) { "Bridge init failed" }
        val hardware = HardwareConfig(width, height, Density.XHIGH, 320f, 320f, ScreenSize.NORMAL,
            if (landscape) ScreenOrientation.LANDSCAPE else ScreenOrientation.PORTRAIT, ScreenRound.NOTROUND, false)
        val parser = Parser(project.resolve("app/src/main/res/layout/activity_main.xml").toString())
        val params = SessionParams(parser, SessionParams.RenderingMode.NORMAL, Any(), hardware,
            resolver, Callback(), 26, 36, log)
        params.setAssetRepository(object : AssetRepository() {
            override fun isSupported() = true
            override fun openNonAsset(cookie: Int, path: String, access: Int) = Files.newInputStream(Path.of(path))
            override fun openAsset(path: String, access: Int) = Files.newInputStream(Path.of(path))
        })
        params.setFontScale(fontScale)
        params.setForceNoDecor()
        params.setLocale("vi")
        val session = bridge.createSession(params)
        println(session.result)
        session.result.exception?.let { throw it }
        populate(session, args, width, height)
        println("Populate: ${session.result}")
        session.result.exception?.let { throw it }
        println("Render: ${session.render(5000)}")
        println("WROTE ${args[1]}")
        session.dispose()
        bridge.dispose()
        exitProcess(0)
    }
}
