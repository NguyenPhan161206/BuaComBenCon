package vn.edu.buacombencon

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
import java.util.Locale
import kotlin.math.roundToInt
import vn.edu.buacombencon.DemoStore.Campaign
import vn.edu.buacombencon.DemoStore.Registration
import vn.edu.buacombencon.DemoStore.Companion.ALLOCATED
import vn.edu.buacombencon.DemoStore.Companion.CANCELLED
import vn.edu.buacombencon.DemoStore.Companion.PENDING
import vn.edu.buacombencon.DemoStore.Companion.RECEIVED
import vn.edu.buacombencon.DemoStore.Companion.WAITING
import vn.edu.buacombencon.DemoStore.Companion.statusLabel

/** Loads XML layouts and binds the local demo data and interactions. */
open class MainActivity : Activity() {
    private val roles = arrayOf("Người chăm sóc", "Điều phối viên", "Tình nguyện viên", "Nhà hảo tâm")
    private val shortRoles = arrayOf("Gia đình", "Điều phối", "Phát suất", "Đồng hành")
    private val tabs = arrayOf(
        arrayOf("Trang chủ", "Đăng ký", "Kết quả", "Tài khoản"),
        arrayOf("Tổng quan", "Đợt hỗ trợ", "Báo cáo", "Tài khoản"),
        arrayOf("Phát suất", "Đã trao", "Hỗ trợ", "Tài khoản"),
        arrayOf("Đồng hành", "Cam kết", "Kết quả", "Tài khoản"),
    )
    private lateinit var store: DemoStore
    private lateinit var content: LinearLayout
    private lateinit var nav: LinearLayout
    private lateinit var scroll: ScrollView
    private var role = 0
    private var tab = 0
    private var filter = 0
    private var screen = "tab"
    private var campaignId = "c1"
    private var registrationId = "R0013"
    private var listFilter = "Tất cả"
    private var back = arrayListOf<String>()
    private var drafts = Bundle()
    private val fields = linkedMapOf<String, EditText>()
    private val spinners = linkedMapOf<String, Spinner>()
    private var consent: CheckBox? = null
    private var connected = true
    private var modernBack: OnBackInvokedCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = DemoStore(this)
        if (savedInstanceState != null) {
            role = savedInstanceState.getInt("role")
            tab = savedInstanceState.getInt("tab")
            screen = savedInstanceState.getString("screen", "tab")
            campaignId = savedInstanceState.getString("campaign", "c1")
            registrationId = savedInstanceState.getString("registration", "R0013")
            drafts = savedInstanceState.getBundle("drafts") ?: Bundle()
            back = savedInstanceState.getStringArrayList("back") ?: arrayListOf()
            filter = savedInstanceState.getInt("filter")
            connected = savedInstanceState.getBoolean("connected", true)
            listFilter = savedInstanceState.getString("listFilter", "Tất cả")
        }
        setContentView(R.layout.activity_main)
        content = findViewById(R.id.content)
        nav = findViewById(R.id.bottom_nav)
        scroll = findViewById(R.id.scroll)
        findViewById<View>(R.id.role_switch).setOnClickListener { chooseRole() }
        val root = findViewById<View>(R.id.app_root)
        root.setOnApplyWindowInsetsListener { view, insets ->
            if (Build.VERSION.SDK_INT >= 30) {
                val bars = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
                val ime = insets.getInsets(WindowInsets.Type.ime())
                view.setPadding(bars.left, bars.top, bars.right, maxOf(bars.bottom, ime.bottom))
                nav.visibility = if (insets.isVisible(WindowInsets.Type.ime())) View.GONE else View.VISIBLE
            } else {
                @Suppress("DEPRECATION")
                view.setPadding(insets.systemWindowInsetLeft, insets.systemWindowInsetTop,
                    insets.systemWindowInsetRight, insets.systemWindowInsetBottom)
            }
            insets
        }
        root.requestApplyInsets()
        render()
    }

    private fun draftKey() = "$role:$screen:$campaignId"

    private fun capture() {
        if (fields.isEmpty() && spinners.isEmpty() && consent == null) return
        val draft = Bundle()
        for ((key, field) in fields) draft.putString(key, field.text.toString())
        for ((key, spinner) in spinners) draft.putInt("spinner:$key", spinner.selectedItemPosition)
        consent?.let { draft.putBoolean("consent", it.isChecked) }
        drafts.putBundle(draftKey(), draft)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        capture()
        super.onSaveInstanceState(outState)
        outState.putInt("role", role)
        outState.putInt("tab", tab)
        outState.putString("screen", screen)
        outState.putString("campaign", campaignId)
        outState.putString("registration", registrationId)
        outState.putBundle("drafts", drafts)
        outState.putStringArrayList("back", back)
        outState.putInt("filter", filter)
        outState.putBoolean("connected", connected)
        outState.putString("listFilter", listFilter)
    }

    private fun go(next: String) {
        capture()
        back.add(screen)
        screen = next
        hideKeyboard()
        render()
    }

    private fun switchTab(next: Int) {
        capture()
        tab = next
        if (role == 0 && next == 0 && filter == 9) filter = 0
        screen = "tab"
        back.clear()
        hideKeyboard()
        render()
    }

    @SuppressLint("GestureBackNavigation")
    @Suppress("OVERRIDE_DEPRECATION")
    override fun onBackPressed() = navigateBack()

    private fun navigateBack() {
        capture()
        when {
            back.isNotEmpty() -> {
                screen = back.removeAt(back.lastIndex)
                hideKeyboard()
                render()
            }
            screen != "tab" -> {
                screen = "tab"
                render()
            }
            tab != 0 -> switchTab(0)
            else -> finish()
        }
    }

    private fun updateBackHandler() {
        if (Build.VERSION.SDK_INT < 33) return
        val intercept = back.isNotEmpty() || screen != "tab" || tab != 0
        val callback = modernBack
        if (intercept && callback == null) {
            val handler = OnBackInvokedCallback { navigateBack() }
            onBackInvokedDispatcher.registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT, handler)
            modernBack = handler
        } else if (!intercept && callback != null) {
            onBackInvokedDispatcher.unregisterOnBackInvokedCallback(callback)
            modernBack = null
        }
    }

    private fun hideKeyboard() {
        val focus = currentFocus ?: return
        getSystemService(InputMethodManager::class.java)?.hideSoftInputFromWindow(focus.windowToken, 0)
    }

    private fun chooseRole() {
        AlertDialog.Builder(this)
            .setTitle("Chọn vai trò trình diễn")
            .setSingleChoiceItems(roles, role) { dialog, which ->
                capture()
                role = which
                tab = 0
                screen = "tab"
                back.clear()
                dialog.dismiss()
                render()
            }
            .setNegativeButton("Đóng", null)
            .show()
    }

    private fun render() {
        fields.clear()
        spinners.clear()
        consent = null
        content.removeAllViews()
        text(findViewById(R.id.app_root), R.id.role_switch, "${shortRoles[role]} ⌄")
        drawNav()
        if (screen != "tab") click(show(R.layout.item_back), R.id.back_button, ::navigateBack)
        when (screen) {
            "detail" -> detail()
            "form" -> registrationForm(false)
            "assisted" -> registrationForm(true)
            "success" -> success()
            "ticket" -> ticket()
            "manage" -> manage()
            "create" -> createCampaign()
            "pledge" -> pledge()
            else -> renderTab()
        }
        updateBackHandler()
        scroll.post { scroll.scrollTo(0, 0) }
    }

    private fun renderTab() {
        if (tab == 3) {
            profile()
            return
        }
        when (role) {
            0 -> when (tab) {
                0 -> home()
                1 -> myRegistrations()
                else -> impact()
            }
            1 -> when (tab) {
                0 -> dashboard()
                1 -> campaigns()
                else -> reports()
            }
            2 -> when (tab) {
                0 -> distribution()
                1 -> handedOver()
                else -> help()
            }
            else -> when (tab) {
                0 -> donorHome()
                1 -> commitments()
                else -> reports()
            }
        }
    }

    private fun drawNav() {
        nav.removeAllViews()
        val icons = if (role == 2) arrayOf("scan", "check", "heart", "user")
            else arrayOf("home", "ticket", "chart", "user")
        for ((index, title) in tabs[role].withIndex()) {
            val item = inflate(R.layout.item_nav, nav)
            val selected = index == tab
            val color = getColor(if (selected) R.color.green else R.color.muted)
            item.findViewById<UiIcon>(R.id.nav_icon).setIcon(icons[index], color)
            val label = item.findViewById<TextView>(R.id.nav_label)
            label.text = title
            label.setTextColor(color)
            label.typeface = typeface(selected)
            item.contentDescription = title
            item.setBackgroundResource(if (selected) R.drawable.ui_nav_selected else R.drawable.ui_nav)
            item.setOnClickListener { switchTab(index) }
        }
    }

    private fun home() {
        val page = show(R.layout.screen_home)
        val list = page.findViewById<LinearLayout>(R.id.campaign_list)
        if (resources.configuration.screenWidthDp < 360) {
            page.findViewById<View>(R.id.hero_art).translationX = dp(68)
        }
        click(page, R.id.find_meal) {
            scroll.smoothScrollTo(0, list.top + page.top - dp(20).toInt())
        }
        store.registrations.firstOrNull { it.mine && it.status == ALLOCATED }?.let { registration ->
            val campaign = store.campaign(registration.campaignId)
            val card = inflate(R.layout.item_pickup, page.findViewById(R.id.home_pickup))
            text(card, R.id.pickup_time, "${campaign.dayLabel()} · ${campaign.time}")
            text(card, R.id.pickup_title, "${campaign.title} · 1 suất")
            click(card, R.id.pickup_action) { openTicket(registration) }
        }
        val filters = page.findViewById<LinearLayout>(R.id.filter_row)
        val options = arrayOf("Tất cả", "Hôm nay", "Ngày mai", "Bữa chiều")
        for ((index, title) in options.withIndex()) {
            chip(filters, title, filter == index) {
                filter = index
                render()
            }
        }
        val available = store.campaigns.filter { campaign ->
            !campaign.archived && when (filter) {
                0 -> true
                1 -> campaign.day == 0
                2 -> campaign.day == 1
                3 -> campaign.meal == "Bữa chiều"
                else -> false
            }
        }
        for (campaign in available) campaignCard(list, campaign, false)
        if (available.isEmpty()) empty(list, "Chưa có đợt phù hợp", "Bạn có thể chọn bộ lọc khác.")
        inflate(R.layout.item_notice, list)
    }

    private fun campaignCard(parent: LinearLayout, campaign: Campaign, management: Boolean) {
        val card = inflate(R.layout.item_campaign, parent)
        val date = campaign.date()
        text(card, R.id.campaign_day, date.substring(0, 2))
        text(card, R.id.campaign_month, "THÁNG ${date.substring(3, 5)}")
        text(card, R.id.campaign_meal, "${campaign.dayLabel()} · ${campaign.meal}")
        text(card, R.id.campaign_title, campaign.title)
        text(card, R.id.campaign_time, campaign.time)
        text(card, R.id.campaign_location, campaign.location)
        val status = if (management) {
            if (campaign.archived) "Đã kết thúc · ${store.received(campaign)} suất đã trao"
            else "${store.assigned(campaign.id)} / ${campaign.confirmed} suất đã sắp xếp · ${store.count(campaign.id, WAITING)} chờ"
        } else {
            val registration = store.mine(campaign.id)
            when {
                registration != null -> statusLabel(registration.status)
                campaign.registrationOpen -> "Tiếp nhận đăng ký · Kết quả do đơn vị xác nhận"
                else -> "Đã đóng đăng ký"
            }
        }
        text(card, R.id.campaign_status, status)
        val action = when {
            !management -> "Xem chi tiết  →"
            campaign.archived -> "Xem báo cáo  →"
            else -> "Quản lý đợt  →"
        }
        text(card, R.id.campaign_action, action)
        click(card, R.id.campaign_action) {
            campaignId = campaign.id
            go(if (management && !campaign.archived) "manage" else "detail")
        }
    }

    private fun detail() {
        val campaign = store.campaign(campaignId)
        val page = show(R.layout.screen_detail)
        val status = if (campaign.archived) "ĐÃ KẾT THÚC"
            else "ĐỢT HỖ TRỢ · ${campaign.meal.uppercase(Locale.forLanguageTag("vi"))}"
        text(page, R.id.detail_status, status)
        text(page, R.id.page_title, campaign.title)
        text(page, R.id.detail_date, "${campaign.dayLabel()} · ${campaign.date()}")
        text(page, R.id.detail_time, campaign.time)
        text(page, R.id.detail_location, campaign.location)
        text(page, R.id.detail_registration_info,
            "Hạn đăng ký: ${campaign.deadline}.\nĐơn vị tổ chức: Nhóm hỗ trợ An Tâm (mẫu).\nSố suất xác nhận cung cấp: ${campaign.confirmed}.")
        text(page, R.id.detail_rules, campaign.rules)
        click(page, R.id.contact_action) {
            info("Đầu mối hỗ trợ",
                "Bàn điều phối tại điểm phát An Tâm (mẫu). Chưa có số điện thoại thật được xác nhận.")
        }
        if (campaign.archived) {
            visible(page, R.id.detail_archived, true)
            reportCard(page.findViewById(R.id.detail_report), campaign)
        } else if (role == 0) {
            val registration = store.mine(campaign.id)
            when {
                registration != null -> {
                    visible(page, R.id.detail_registered, true)
                    text(page, R.id.detail_my_status, statusLabel(registration.status))
                    text(page, R.id.detail_ticket_action,
                        if (registration.status == ALLOCATED) "Xem mã nhận suất" else "Xem đăng ký của tôi")
                    click(page, R.id.detail_ticket_action) { openTicket(registration) }
                }
                campaign.registrationOpen -> {
                    visible(page, R.id.detail_open, true)
                    click(page, R.id.register_action) { go("form") }
                }
                else -> visible(page, R.id.detail_closed, true)
            }
        } else if (role == 3) {
            visible(page, R.id.detail_pledge_action, true)
            click(page, R.id.detail_pledge_action) { go("pledge") }
        }
    }

    private fun registrationForm(assisted: Boolean) {
        val campaign = store.campaign(campaignId)
        val form = show(R.layout.screen_form)
        text(form, R.id.form_title, if (assisted) "Đăng ký hộ gia đình" else "Đăng ký nhận suất")
        text(form, R.id.form_context, "${campaign.title}\n${campaign.date()} · ${campaign.time}\n1 suất / người chăm sóc")
        val name = field(form, R.id.input_name, "name", "")
        val phone = field(form, R.id.input_phone, "phone", "")
        val note = field(form, R.id.input_note, "note", "")
        val consentBox = bindConsent(form, R.id.consent)
        if (assisted) consentBox.text = "Tôi đã trao đổi với người chăm sóc và được đồng ý ghi nhận thông tin liên hệ."
        click(form, R.id.submit_form) {
            if (!validName(name) || !validPhone(phone)) return@click
            if (!consentBox.isChecked) {
                info("Cần sự đồng ý", "Vui lòng đọc và đồng ý phần cung cấp thông tin trước khi gửi.")
                return@click
            }
            val registration = try {
                store.register(campaign.id, name.value(), phone.value(), note.value(), !assisted, assisted)
            } catch (error: IllegalStateException) {
                info("Chưa thể đăng ký", error.message.orEmpty())
                return@click
            }
            registrationId = registration.id
            drafts.remove(draftKey())
            fields.clear()
            consent = null
            screen = "success"
            back.clear()
            if (assisted) back.add("manage")
            hideKeyboard()
            render()
        }
    }

    private fun success() {
        val registration = store.registration(registrationId)
        if (registration == null) {
            message("Không tìm thấy đăng ký.")
            return
        }
        val campaign = store.campaign(registration.campaignId)
        val page = show(R.layout.screen_success)
        text(page, R.id.success_campaign, campaign.title)
        text(page, R.id.success_date, "${campaign.date()} · 1 suất ăn")
        text(page, R.id.success_action,
            if (role == 1) "Về danh sách để sắp xếp" else "Theo dõi đăng ký")
        click(page, R.id.success_action) {
            if (role == 1) {
                screen = "manage"
                back.clear()
                render()
            } else {
                switchTab(1)
            }
        }
    }

    private fun openTicket(registration: Registration) {
        registrationId = registration.id
        campaignId = registration.campaignId
        go("ticket")
    }

    private fun myRegistrations() {
        val page = show(R.layout.screen_my_registrations)
        val filters = page.findViewById<LinearLayout>(R.id.registration_filters)
        val list = page.findViewById<LinearLayout>(R.id.registration_list)
        chip(filters, "Hiện tại", filter != 9) { filter = 0; render() }
        chip(filters, "Lịch sử", filter == 9) { filter = 9; render() }

        for (registration in store.registrations) {
            if (!registration.mine) continue
            val campaign = store.campaign(registration.campaignId)
            val history = campaign.archived || registration.status == RECEIVED || registration.status == CANCELLED
            if ((filter == 9) != history) continue
            val card = inflate(R.layout.item_my_registration, list)
            status(card, R.id.registration_status, registration.status)
            text(card, R.id.registration_title, campaign.title)
            text(card, R.id.registration_time, "${campaign.date()} · ${campaign.time}")
            val source = if (registration.assisted) "Được đăng ký hộ" else "Tự đăng ký"
            text(card, R.id.registration_source, "1 suất · $source")
            text(card, R.id.registration_action,
                if (registration.status == ALLOCATED) "Mở mã nhận suất  →" else "Xem trạng thái  →")
            click(card, R.id.registration_action) { openTicket(registration) }
        }
        if (list.childCount == 0) {
            empty(list, "Chưa có đăng ký", "Chọn một đợt hỗ trợ phù hợp để gửi đăng ký nhận suất.")
            visible(page, R.id.find_campaign_action, true)
            click(page, R.id.find_campaign_action) {
                filter = 0
                switchTab(0)
            }
        }
    }

    private fun ticket() {
        val registration = store.registration(registrationId)
        if (registration == null) {
            message("Không tìm thấy đăng ký.")
            return
        }
        val campaign = store.campaign(registration.campaignId)
        val page = show(R.layout.screen_ticket)
        val label = statusLabel(registration.status)
        text(page, R.id.page_title, if (registration.status == ALLOCATED) "Suất ăn đã sẵn sàng" else label)
        text(page, R.id.page_subtitle, campaign.title)
        text(page, R.id.ticket_status, label)
        val ticketContent = page.findViewById<LinearLayout>(R.id.ticket_content)
        when (registration.status) {
            ALLOCATED -> {
                layoutInflater.inflate(R.layout.ticket_allocated, ticketContent, true)
                page.findViewById<QrView>(R.id.ticket_qr).setPayload(registration.code)
                text(page, R.id.ticket_code, registration.code)
                click(page, R.id.copy_code_action) {
                    getSystemService(ClipboardManager::class.java)
                        ?.setPrimaryClip(ClipData.newPlainText("Mã nhận suất", registration.code))
                    toast("Đã sao chép mã nhận.")
                }
            }
            PENDING, WAITING -> {
                layoutInflater.inflate(R.layout.ticket_pending, ticketContent, true)
                text(page, R.id.pending_title,
                    if (registration.status == WAITING) "Bạn đang ở danh sách chờ" else "Đăng ký đang được kiểm tra")
            }
            else -> {
                layoutInflater.inflate(R.layout.ticket_inactive, ticketContent, true)
                text(page, R.id.inactive_text,
                    if (registration.status == RECEIVED) "Suất ăn đã được ghi nhận giao. Mã không còn hiệu lực."
                    else "Đăng ký đã hủy. Mã không còn hiệu lực.")
            }
        }
        text(page, R.id.ticket_time, "${campaign.date()} · ${campaign.time}")
        text(page, R.id.ticket_location, campaign.location)
        if (role == 0 && registration.status != RECEIVED && registration.status != CANCELLED) {
            visible(page, R.id.cancel_registration_action, true)
            click(page, R.id.cancel_registration_action) {
                AlertDialog.Builder(this)
                    .setTitle("Hủy đăng ký suất ăn?")
                    .setMessage("Đơn vị tổ chức có thể sắp xếp suất cho người đang chờ. Mã nhận của bạn sẽ hết hiệu lực.")
                    .setNegativeButton("Giữ đăng ký", null)
                    .setPositiveButton("Xác nhận hủy") { _, _ ->
                        registration.status = CANCELLED
                        store.save()
                        render()
                    }
                    .show()
            }
        }
    }

    private fun dashboard() {
        val page = show(R.layout.screen_dashboard)
        val campaign = store.campaign("c1")
        text(page, R.id.dashboard_campaign, campaign.title)
        text(page, R.id.dashboard_time, "${campaign.date()} · ${campaign.time}")
        metrics(page, arrayOf("Đã sắp xếp", "Đã trao", "Đang chờ"),
            intArrayOf(store.assigned(campaign.id), store.received(campaign), store.count(campaign.id, WAITING)))
        click(page, R.id.manage_action) {
            campaignId = campaign.id
            go("manage")
        }
        val pending = store.registrations.count { it.status == PENDING }
        text(page, R.id.pending_count, "$pending đăng ký đang chờ kiểm tra")
        text(page, R.id.waiting_count, "${store.count("c1", WAITING)} hồ sơ trong danh sách chờ")
        click(page, R.id.create_action) { go("create") }
        click(page, R.id.campaigns_action) { switchTab(1) }
    }

    private fun campaigns() {
        val page = show(R.layout.screen_campaigns)
        click(page, R.id.create_action) { go("create") }
        val list = page.findViewById<LinearLayout>(R.id.campaign_list)
        for (campaign in store.campaigns) campaignCard(list, campaign, true)
    }

    private fun manage() {
        val campaign = store.campaign(campaignId)
        val page = show(R.layout.screen_manage)
        text(page, R.id.page_title, campaign.title)
        text(page, R.id.page_subtitle, "${campaign.date()} · ${campaign.time}")
        metrics(page, arrayOf("Đã xác nhận", "Đã sắp xếp", "Đã trao"),
            intArrayOf(campaign.confirmed, store.assigned(campaign.id), store.received(campaign)))
        text(page, R.id.supply_summary, "Cam kết ${campaign.pledged} · Thực chuẩn bị ${campaign.prepared}")
        visible(page, R.id.assisted_action, campaign.registrationOpen)
        click(page, R.id.assisted_action) { go("assisted") }
        text(page, R.id.toggle_registration_action,
            if (campaign.registrationOpen) "Đóng tiếp nhận đăng ký" else "Mở tiếp nhận đăng ký (mẫu)")
        click(page, R.id.toggle_registration_action) {
            AlertDialog.Builder(this)
                .setTitle("Cập nhật tiếp nhận đăng ký?")
                .setMessage("Điều phối viên quản lý trạng thái mở/đóng trong bản UI. Thời hạn nhập dạng văn bản; chưa tự động đóng theo giờ.")
                .setNegativeButton("Giữ nguyên", null)
                .setPositiveButton("Cập nhật") { _, _ ->
                    campaign.registrationOpen = !campaign.registrationOpen
                    store.save()
                    render()
                }
                .show()
        }
        click(page, R.id.edit_supply_action) { editSupply(campaign) }
        val filters = page.findViewById<LinearLayout>(R.id.manage_filters)
        val list = page.findViewById<LinearLayout>(R.id.managed_registration_list)
        val options = linkedMapOf("Tất cả" to null, "Cần sắp xếp" to PENDING,
            "Danh sách chờ" to WAITING, "Được nhận" to ALLOCATED, "Đã nhận" to RECEIVED)
        for (title in options.keys) {
            chip(filters, title, listFilter == title) {
                listFilter = title
                render()
            }
        }
        val statusFilter = options[listFilter]
        for (registration in store.inCampaign(campaign.id)) {
            if (statusFilter != null && registration.status != statusFilter) continue
            val card = inflate(R.layout.item_managed_registration, list)
            text(card, R.id.registration_name, registration.name)
            val source = if (registration.assisted) "Đăng ký hộ" else "Tự đăng ký"
            text(card, R.id.registration_meta, "${registration.id} · $source")
            status(card, R.id.registration_status, registration.status)
            if (registration.note.isNotEmpty()) {
                visible(card, R.id.registration_note, true)
                text(card, R.id.registration_note, registration.note)
            }
            if (registration.status == PENDING || registration.status == WAITING) {
                visible(card, R.id.allocate_action, true)
                click(card, R.id.allocate_action) { confirmAllocation(campaign, registration) }
            }
            if (registration.status == ALLOCATED) {
                visible(card, R.id.ticket_action, true)
                click(card, R.id.ticket_action) { openTicket(registration) }
            }
        }
        if (list.childCount == 0) {
            empty(list, "Không có hồ sơ ở trạng thái này",
                "Danh sách sẽ cập nhật khi có đăng ký hoặc thay đổi kết quả.")
        }
    }

    private fun confirmAllocation(campaign: Campaign, registration: Registration) {
        val earlier = store.inCampaign(campaign.id)
            .takeWhile { it !== registration }
            .firstOrNull { it.status == PENDING || it.status == WAITING }
        if (earlier != null) {
            info("Còn hồ sơ đăng ký trước",
                "Vui lòng xem hồ sơ ${earlier.id} trước để giữ thứ tự sắp xếp của đợt.")
            return
        }
        AlertDialog.Builder(this)
            .setTitle("Xác nhận điều kiện tham gia")
            .setMessage("Bạn đã kiểm tra hồ sơ theo tiêu chí công bố của đợt? Số suất được cấp dựa trên số xác nhận cung cấp, không dựa trên cam kết.")
            .setNegativeButton("Quay lại", null)
            .setPositiveButton("Đã kiểm tra") { _, _ ->
                toast(store.allocate(registration))
                render()
            }
            .show()
    }

    private fun editSupply(campaign: Campaign) {
        val form = layoutInflater.inflate(R.layout.dialog_supply, null, false)
        val confirmed = form.findViewById<EditText>(R.id.supply_confirmed)
        val prepared = form.findViewById<EditText>(R.id.supply_prepared)
        val note = form.findViewById<EditText>(R.id.supply_note)
        confirmed.setText(campaign.confirmed.toString())
        prepared.setText(campaign.prepared.toString())
        note.setText(campaign.note)
        val dialog = AlertDialog.Builder(this)
            .setTitle("Cập nhật đợt hỗ trợ")
            .setView(form)
            .setNegativeButton("Hủy", null)
            .setPositiveButton("Lưu", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val confirmedCount = number(confirmed)
                val preparedCount = number(prepared)
                val receivedCount = store.received(campaign)
                val minimum = maxOf(store.assigned(campaign.id), receivedCount)
                if (confirmedCount !in minimum..1000) {
                    confirmed.error = "Từ $minimum đến 1.000 suất"
                    return@setOnClickListener
                }
                if (preparedCount !in receivedCount..confirmedCount) {
                    prepared.error = "Từ $receivedCount đến $confirmedCount suất"
                    return@setOnClickListener
                }
                campaign.confirmed = confirmedCount
                campaign.prepared = preparedCount
                campaign.note = note.value()
                store.save()
                dialog.dismiss()
                render()
            }
        }
        dialog.show()
    }

    private fun createCampaign() {
        val page = show(R.layout.screen_create)
        val title = field(page, R.id.create_title, "title", "")
        val offset = field(page, R.id.create_day, "day", "1")
        val time = field(page, R.id.create_time, "time", "11:00 – 12:00")
        val capacity = field(page, R.id.create_capacity, "capacity", "30")
        val deadline = field(page, R.id.create_deadline, "deadline", "18:00 ngày trước buổi phát")
        val rules = field(page, R.id.create_rules, "rules",
            "Mỗi người chăm sóc 1 suất; hồ sơ đủ điều kiện được sắp xếp theo thứ tự đăng ký.")
        val meal = spinner(page, R.id.create_meal, "meal", arrayOf("Bữa trưa", "Bữa chiều"))
        click(page, R.id.submit_create) {
            if (title.value().length < 3) {
                title.error = "Nhập tên đợt hỗ trợ"
                return@click
            }
            val day = number(offset)
            val quantity = number(capacity)
            if (day !in 0..365) {
                offset.error = "Nhập số từ 0 đến 365"
                return@click
            }
            if (quantity !in 1..1000) {
                capacity.error = "Nhập số từ 1 đến 1.000"
                return@click
            }
            if (time.value().isEmpty() || deadline.value().isEmpty() || rules.value().isEmpty()) {
                info("Thiếu thông tin", "Điền thời gian, thời hạn và tiêu chí trước khi tạo đợt.")
                return@click
            }
            val campaign = Campaign("c${System.currentTimeMillis()}", title.value(),
                meal.selectedItem.toString(), day, quantity, time.value())
            campaign.prepared = 0
            campaign.pledged = quantity
            campaign.registrationOpen = true
            campaign.deadline = deadline.value()
            campaign.rules = rules.value()
            store.campaigns.add(0, campaign)
            store.save()
            drafts.remove(draftKey())
            fields.clear()
            spinners.clear()
            campaignId = campaign.id
            screen = "manage"
            back.clear()
            hideKeyboard()
            render()
        }
    }

    private fun distribution() {
        val page = show(R.layout.screen_distribution)
        chooseCampaign(page, false)
        val campaign = store.campaign(campaignId)
        val received = store.received(campaign)
        metrics(page, arrayOf("Thực chuẩn bị", "Đã trao", "Chưa trao"),
            intArrayOf(campaign.prepared, received, maxOf(0, campaign.prepared - received)))
        val code = field(page, R.id.claim_code, "claim_code", "")
        click(page, R.id.check_code_action) { checkCode(code.value()) }
        click(page, R.id.sample_code_action) { code.setText("BC0013") }
        val network = page.findViewById<Switch>(R.id.network_switch)
        network.isChecked = connected
        network.setOnCheckedChangeListener { _, checked ->
            connected = checked
            toast(if (checked) "Đã bật trạng thái kết nối mẫu."
                else "Mất kết nối mẫu: không thể xác nhận giao suất.")
        }
        click(page, R.id.distribution_help_action) {
            info("Hỗ trợ tại điểm phát",
                "Nếu không có mã, mã sai hoặc mất kết nối, chuyển đến bàn điều phối. Dùng danh sách dự phòng theo quy trình của đơn vị; chỉ cập nhật đã giao sau khi thông tin được kiểm tra.")
        }
    }

    private fun checkCode(code: String) {
        if (!connected) {
            info("Chưa thể kiểm tra mã",
                "Đang mô phỏng mất kết nối. Báo điều phối viên và dùng danh sách dự phòng; chưa ghi nhận giao thành công.")
            return
        }
        val registration = store.findCode(code)
        val error = store.claimError(registration, campaignId)
        if (registration == null || error != null) {
            info("Cần kiểm tra lại", error.orEmpty())
            return
        }
        AlertDialog.Builder(this)
            .setTitle("Mã hợp lệ · 1 suất ăn")
            .setMessage("${registration.name}\n${store.campaign(campaignId).title}\n\nChỉ xác nhận sau khi bạn đã giao suất ăn cho người nhận.")
            .setNegativeButton("Chưa giao", null)
            .setPositiveButton("Đã giao suất") { _, _ ->
                if (!connected) {
                    info("Chưa lưu giao nhận", "Mất kết nối mẫu. Cần đối chiếu với điều phối viên.")
                    return@setPositiveButton
                }
                val recheck = store.claimError(registration, campaignId)
                if (recheck != null) {
                    info("Cần kiểm tra lại", recheck)
                    return@setPositiveButton
                }
                registration.status = RECEIVED
                store.save()
                toast("Đã ghi nhận giao 1 suất (dữ liệu mẫu).")
                render()
            }
            .show()
    }

    private fun handedOver() {
        val page = show(R.layout.screen_handed_over)
        chooseCampaign(page, false)
        val campaign = store.campaign(campaignId)
        text(page, R.id.handover_total, "${store.received(campaign)} suất đã trao")
        text(page, R.id.handover_campaign, campaign.title)
        val list = page.findViewById<LinearLayout>(R.id.handed_over_list)
        for (registration in store.inCampaign(campaign.id)) {
            if (registration.status != RECEIVED) continue
            val card = inflate(R.layout.item_handed_over, list)
            text(card, R.id.recipient_name, registration.name)
            text(card, R.id.recipient_code, "${registration.code} · Đã dùng · 1 suất")
        }
        if (list.childCount == 0) {
            empty(list, "Chưa có lượt giao", "Các lượt đã xác nhận sẽ xuất hiện ở đây.")
        }
    }

    private fun help() {
        val page = show(R.layout.screen_help)
        click(page, R.id.help_contact_action) {
            info("Bàn điều phối",
                "Điểm phát An Tâm · khu sinh hoạt chung (mẫu). Chưa có số điện thoại thực được xác nhận.")
        }
    }

    private fun donorHome() {
        val page = show(R.layout.screen_donor_home)
        val list = page.findViewById<LinearLayout>(R.id.donor_campaign_list)
        for (campaign in store.campaigns) {
            if (campaign.archived) continue
            val card = inflate(R.layout.item_donor_campaign, list)
            text(card, R.id.campaign_title, campaign.title)
            text(card, R.id.campaign_time, "${campaign.date()} · ${campaign.meal}")
            metrics(card, arrayOf("Cam kết", "Đã xác nhận"), intArrayOf(campaign.pledged, campaign.confirmed))
            click(card, R.id.pledge_action) {
                campaignId = campaign.id
                go("pledge")
            }
        }
    }

    private fun pledge() {
        val campaign = store.campaign(campaignId)
        val page = show(R.layout.screen_pledge)
        text(page, R.id.page_subtitle, "${campaign.title} · ${campaign.date()}")
        val name = field(page, R.id.donor_name, "donor_name", "")
        val phone = field(page, R.id.donor_phone, "donor_phone", "")
        val quantity = field(page, R.id.pledge_quantity, "quantity", "10")
        val cadence = spinner(page, R.id.pledge_cadence, "cadence",
            arrayOf("Theo đợt", "Mong muốn theo tuần", "Mong muốn theo tháng"))
        val consentBox = bindConsent(page, R.id.pledge_consent)
        click(page, R.id.submit_pledge) {
            if (!validName(name) || !validPhone(phone)) return@click
            val count = number(quantity)
            if (count !in 1..1000) {
                quantity.error = "Nhập từ 1 đến 1.000 suất"
                return@click
            }
            if (!consentBox.isChecked) {
                info("Cần sự đồng ý", "Vui lòng đồng ý để đơn vị liên hệ về đăng ký.")
                return@click
            }
            // The demo keeps one active commitment and replaces its prior quantity.
            if (store.donorPledged > 0) {
                val old = store.campaign(store.donorCampaign)
                old.pledged = maxOf(0, old.pledged - store.donorPledged)
            }
            campaign.pledged += count
            store.donorPledged = count
            store.donorCampaign = campaign.id
            store.donorCadence = cadence.selectedItem.toString()
            store.save()
            drafts.remove(draftKey())
            fields.clear()
            spinners.clear()
            consent = null
            switchTab(1)
            info("Đã ghi nhận cam kết",
                "Cam kết đang chờ đơn vị liên hệ. Số suất xác nhận cung cấp chưa thay đổi.")
        }
    }

    private fun commitments() {
        val page = show(R.layout.screen_commitments)
        if (store.donorPledged == 0) {
            empty(page.findViewById(R.id.commitment_empty), "Bắt đầu từ một bữa ăn",
                "Chọn một đợt phù hợp và gửi mong muốn đồng hành.")
            visible(page, R.id.commitment_browse_action, true)
            click(page, R.id.commitment_browse_action) { switchTab(0) }
            return
        }
        visible(page, R.id.commitment_details, true)
        val campaign = store.campaign(store.donorCampaign)
        text(page, R.id.commitment_quantity, "${store.donorPledged} suất ăn")
        text(page, R.id.commitment_title, campaign.title)
        text(page, R.id.commitment_time, "${campaign.date()} · ${store.donorCadence}")
        click(page, R.id.commitment_edit_action) {
            campaignId = campaign.id
            go("pledge")
        }
        click(page, R.id.commitment_report_action) { switchTab(2) }
    }

    private fun reports() {
        val page = show(R.layout.screen_reports)
        chooseCampaign(page, true)
        val campaign = store.campaign(campaignId)
        reportCard(page.findViewById(R.id.report_container), campaign)
        visible(page, R.id.report_edit_action, role == 1)
        click(page, R.id.report_edit_action) { editSupply(campaign) }
    }

    private fun impact() {
        val page = show(R.layout.screen_impact)
        reportCard(page.findViewById(R.id.impact_report), store.campaign("c0"))
    }

    private fun reportCard(parent: LinearLayout, campaign: Campaign) {
        val card = inflate(R.layout.item_report, parent)
        text(card, R.id.report_status, if (campaign.archived) "ĐÃ ĐỐI CHIẾU · MẪU" else "ĐANG CẬP NHẬT · MẪU")
        text(card, R.id.report_title, campaign.title)
        text(card, R.id.report_time, "${campaign.date()} · ${campaign.meal}")
        val received = store.received(campaign)
        val ids = intArrayOf(R.id.stat_0, R.id.stat_1, R.id.stat_2, R.id.stat_3, R.id.stat_4)
        val values = intArrayOf(campaign.pledged, campaign.confirmed, campaign.prepared, received,
            maxOf(0, campaign.prepared - received))
        for (index in ids.indices) text(card, ids[index], values[index].toString())
        val progress = card.findViewById<ProgressBar>(R.id.report_progress)
        progress.max = maxOf(1, campaign.prepared)
        progress.progress = received
        text(card, R.id.report_note_title, if (campaign.archived) "Ghi chú sau buổi phát" else "Ghi chú của đơn vị")
        text(card, R.id.report_note, campaign.note)
        visible(card, R.id.report_live_note, !campaign.archived)
    }

    private fun profile() {
        val page = show(R.layout.screen_profile)
        text(page, R.id.page_subtitle, "Bạn đang xem vai trò ${roles[role].lowercase(Locale.forLanguageTag("vi"))}.")
        text(page, R.id.profile_role, roles[role])
        click(page, R.id.profile_role_action, ::chooseRole)
        click(page, R.id.profile_guide_action) {
            info("Thử một quy trình hoàn chỉnh",
                "1. Gia đình: đăng ký đợt ngày mai.\n2. Đổi sang Điều phối: mở cùng đợt, kiểm tra và sắp xếp hồ sơ.\n3. Gia đình: mở Đăng ký để thấy mã nhận.\n4. Phát suất hôm nay: nhập BC0013, xác nhận sau khi giao; thử lại mã để thấy cảnh báo.\n5. Xem báo cáo đã cập nhật.")
        }
        click(page, R.id.profile_reset_action) {
            AlertDialog.Builder(this)
                .setTitle("Khôi phục dữ liệu mẫu?")
                .setMessage("Xóa các đăng ký, cam kết và thay đổi bạn đã nhập trên thiết bị; đưa ứng dụng về dữ liệu mẫu ban đầu.")
                .setNegativeButton("Giữ dữ liệu", null)
                .setPositiveButton("Khôi phục") { _, _ ->
                    store.reset()
                    drafts.clear()
                    fields.clear()
                    consent = null
                    filter = 0
                    campaignId = "c1"
                    registrationId = "R0013"
                    listFilter = "Tất cả"
                    connected = true
                    toast("Đã khôi phục dữ liệu mẫu.")
                    render()
                }
                .show()
        }
    }

    private fun chooseCampaign(page: View, includeArchived: Boolean) {
        val options = store.campaigns.filter { includeArchived || !it.archived }
        val selected = options.firstOrNull { it.id == campaignId } ?: options.first()
        campaignId = selected.id
        text(page, R.id.campaign_picker, "${selected.title}  ⌄")
        click(page, R.id.campaign_picker) {
            val names = options.map { "${it.title} · ${it.date()}" }.toTypedArray()
            AlertDialog.Builder(this)
                .setTitle("Chọn đợt hỗ trợ")
                .setItems(names) { _, which ->
                    capture()
                    campaignId = options[which].id
                    render()
                }
                .show()
        }
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).roundToInt().toFloat()

    private fun inflate(layout: Int, parent: ViewGroup): View =
        layoutInflater.inflate(layout, parent, false).also(parent::addView)

    private fun show(layout: Int): View = inflate(layout, content)

    private fun text(root: View, id: Int, value: String) {
        root.findViewById<TextView>(id).text = value
    }

    private fun click(root: View, id: Int, action: () -> Unit) {
        root.findViewById<View>(id).setOnClickListener { action() }
    }

    private fun visible(root: View, id: Int, show: Boolean) {
        root.findViewById<View>(id).visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun typeface(selected: Boolean): Typeface =
        Typeface.create(if (selected) "sans-serif-medium" else "sans-serif", Typeface.NORMAL)

    private fun status(root: View, id: Int, value: String) {
        val pill = root.findViewById<TextView>(id)
        val warning = value == PENDING || value == WAITING
        pill.text = statusLabel(value)
        pill.setTextColor(getColor(if (warning) R.color.ui_orange else R.color.green))
        pill.setBackgroundResource(if (warning) R.drawable.ui_pill_warning else R.drawable.ui_pill)
    }

    private fun chip(parent: LinearLayout, value: String, selected: Boolean, action: () -> Unit) {
        val chip = inflate(R.layout.item_chip, parent) as TextView
        chip.text = value
        chip.setTextColor(getColor(if (selected) R.color.white else R.color.muted))
        chip.typeface = typeface(selected)
        chip.setBackgroundResource(if (selected) R.drawable.ui_chip_selected else R.drawable.ui_chip)
        chip.setOnClickListener { action() }
    }

    private fun metrics(root: View, names: Array<String>, values: IntArray) {
        val numberIds = intArrayOf(R.id.metric_value_0, R.id.metric_value_1, R.id.metric_value_2)
        val nameIds = intArrayOf(R.id.metric_name_0, R.id.metric_name_1, R.id.metric_name_2)
        for (index in names.indices) {
            text(root, numberIds[index], values[index].toString())
            text(root, nameIds[index], names[index])
        }
    }

    private fun empty(parent: LinearLayout, title: String, subtitle: String) {
        val card = inflate(R.layout.item_empty, parent)
        text(card, R.id.empty_title, title)
        text(card, R.id.empty_subtitle, subtitle)
    }

    private fun message(value: String) {
        text(show(R.layout.item_message), R.id.message_text, value)
    }

    private fun field(root: View, id: Int, key: String, fallback: String): EditText {
        val input = root.findViewById<EditText>(id)
        fields[key] = input
        input.setText(drafts.getBundle(draftKey())?.getString(key, fallback) ?: fallback)
        input.isSaveEnabled = false
        return input
    }

    private fun spinner(root: View, id: Int, key: String, options: Array<String>): Spinner {
        val menu = root.findViewById<Spinner>(id)
        menu.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, options)
        spinners[key] = menu
        drafts.getBundle(draftKey())?.let { menu.setSelection(it.getInt("spinner:$key", 0)) }
        return menu
    }

    private fun bindConsent(root: View, id: Int): CheckBox {
        val box = root.findViewById<CheckBox>(id)
        consent = box
        drafts.getBundle(draftKey())?.let { box.isChecked = it.getBoolean("consent") }
        return box
    }

    private fun EditText.value(): String = text.toString().trim()
    private fun number(input: EditText): Int = input.value().toIntOrNull() ?: -1

    private fun validName(input: EditText): Boolean {
        if (input.value().length >= 2) return true
        input.error = "Nhập tên có ít nhất 2 ký tự"
        input.requestFocus()
        return false
    }

    private fun validPhone(input: EditText): Boolean {
        val value = input.text.toString().replace(Regex("[\\s.-]"), "")
        if (value.matches(Regex("(0[0-9]{9}|\\+84[0-9]{9})"))) return true
        input.error = "Nhập số gồm 10 chữ số bắt đầu bằng 0 hoặc +84"
        input.requestFocus()
        return false
    }

    private fun info(title: String, message: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Đã hiểu", null)
            .show()
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}
