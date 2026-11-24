package vn.edu.buacombencon

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.inputmethod.InputMethodManager
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
import vn.edu.buacombencon.ui.account.AccountController
import vn.edu.buacombencon.ui.caregiver.CaregiverController
import vn.edu.buacombencon.ui.coordinator.CoordinatorController
import vn.edu.buacombencon.ui.donor.DonorController
import vn.edu.buacombencon.data.RepositoryProvider
import vn.edu.buacombencon.data.remote.SupabaseConfig
import vn.edu.buacombencon.data.remote.SupabaseRepository
import vn.edu.buacombencon.ui.shell.MealActions
import vn.edu.buacombencon.ui.shell.UiUtils
import vn.edu.buacombencon.ui.volunteer.VolunteerController

/** Loads XML layouts and orchestrates the MVC controllers and navigation. */
open class MainActivity : Activity() {
    private val roles = arrayOf("Người chăm sóc", "Điều phối viên", "Tình nguyện viên", "Nhà hảo tâm")
    private val shortRoles = arrayOf("Gia đình", "Điều phối", "Phát suất", "Đồng hành")
    private val tabs = arrayOf(
        arrayOf("Trang chủ", "Đăng ký", "Kết quả", "Tài khoản"),
        arrayOf("Tổng quan", "Đợt hỗ trợ", "Báo cáo", "Tài khoản"),
        arrayOf("Phát suất", "Đã trao", "Hỗ trợ", "Tài khoản"),
        arrayOf("Đồng hành", "Cam kết", "Kết quả", "Tài khoản"),
    )

    // Kept accessible for QA layoutlib and reflection compatibility
    protected lateinit var store: DemoStore
    protected lateinit var content: LinearLayout
    protected lateinit var nav: LinearLayout
    protected lateinit var scroll: ScrollView
    protected var role = 0
    protected var tab = 0
    protected var filter = 0
    protected var screen = "tab"
    protected var campaignId = "c1"
    protected var registrationId = "R0013"
    protected var listFilter = "Tất cả"
    protected var back = arrayListOf<String>()
    protected var drafts = Bundle()
    protected val fields = linkedMapOf<String, EditText>()
    protected val spinners = linkedMapOf<String, Spinner>()
    protected var consent: CheckBox? = null
    protected var connected = true
    protected var modernBack: OnBackInvokedCallback? = null

    private lateinit var data: ScreenData
    private lateinit var actions: MealActions
    private var remoteLoaded = false

    // Controllers
    private lateinit var caregiverController: CaregiverController
    private lateinit var coordinatorController: CoordinatorController
    private lateinit var volunteerController: VolunteerController
    private lateinit var donorController: DonorController
    private lateinit var accountController: AccountController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = DemoStore(this)
        if (savedInstanceState != null &&
            savedInstanceState.getBoolean("supabaseSession") == (RepositoryProvider.remote != null)) {
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
                view.setPadding(
                    insets.systemWindowInsetLeft, insets.systemWindowInsetTop,
                    insets.systemWindowInsetRight, insets.systemWindowInsetBottom
                )
            }
            insets
        }
        root.requestApplyInsets()
        initializeData()
        if (actions.connected) refreshRemote() else render()
    }

    private fun initializeData() {
        if (!::actions.isInitialized) actions = MealActions(this, store, ::setSnapshot)
        if (!::data.isInitialized) data = if (actions.connected) ScreenData() else store
    }

    private fun setSnapshot(snapshot: ScreenData) {
        data = snapshot
        remoteLoaded = true
        initControllers()
    }

    private fun initControllers() {
        caregiverController = CaregiverController(
            activity = this,
            store = data,
            actions = actions,
            content = content,
            scroll = scroll,
            drafts = drafts,
            fields = fields,
            consentHolder = { consent },
            setConsent = { consent = it },
            onNavigate = { target ->
                if (target.startsWith("detail:")) {
                    campaignId = target.substringAfter("detail:")
                    go("detail")
                } else {
                    go(target)
                }
            },
            onSwitchTab = { switchTab(it) },
            onOpenTicket = { openTicket(it) },
            onRenderRequest = { render() }
        )

        coordinatorController = CoordinatorController(
            activity = this,
            store = data,
            actions = actions,
            content = content,
            drafts = drafts,
            fields = fields,
            spinners = spinners,
            consentHolder = { consent },
            setConsent = { consent = it },
            onNavigate = { target ->
                when {
                    target.startsWith("manage:") -> {
                        campaignId = target.substringAfter("manage:")
                        go("manage")
                    }
                    target.startsWith("detail:") -> {
                        campaignId = target.substringAfter("detail:")
                        go("detail")
                    }
                    else -> go(target)
                }
            },
            onSwitchTab = { switchTab(it) },
            draftKey = ::draftKey,
            onOpenTicket = { openTicket(it) },
            onRenderRequest = { render() }
        )

        volunteerController = VolunteerController(
            activity = this,
            store = data,
            actions = actions,
            content = content,
            drafts = drafts,
            fields = fields,
            isConnected = { connected },
            setConnected = { connected = it },
            onChooseCampaign = { page, includeArchived -> chooseCampaign(page, includeArchived) },
            draftKey = ::draftKey,
            onRenderRequest = { render() }
        )

        donorController = DonorController(
            activity = this,
            store = data,
            actions = actions,
            content = content,
            drafts = drafts,
            fields = fields,
            spinners = spinners,
            consentHolder = { consent },
            setConsent = { consent = it },
            onNavigate = { target ->
                if (target.startsWith("pledge:")) {
                    campaignId = target.substringAfter("pledge:")
                    go("pledge")
                } else {
                    go(target)
                }
            },
            onSwitchTab = { switchTab(it) },
            onChooseCampaign = { page, includeArchived -> chooseCampaign(page, includeArchived) },
            draftKey = ::draftKey
        )

        accountController = AccountController(
            activity = this,
            store = data,
            actions = actions,
            content = content,
            roles = roles,
            onChooseRole = { chooseRole() },
            onRefresh = ::refreshRemote,
            onSignOut = ::signOut,
            onResetDemo = {
                store.reset()
                drafts.clear()
                fields.clear()
                spinners.clear()
                consent = null
                filter = 0
                campaignId = "c1"
                registrationId = "R0013"
                listFilter = "Tất cả"
                connected = true
                render()
            }
        )
    }

    private fun draftKey() = "$role:$screen:$campaignId"

    private fun capture() {
        if (fields.isEmpty() && spinners.isEmpty() && consent == null) return
        val draft = drafts.getBundle(draftKey()) ?: Bundle()
        for ((key, field) in fields) draft.putString(key, field.text.toString())
        for ((key, spinner) in spinners) draft.putInt("spinner:$key", spinner.selectedItemPosition)
        consent?.let { draft.putBoolean("consent", it.isChecked) }
        drafts.putBundle(draftKey(), draft)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        capture()
        super.onSaveInstanceState(outState)
        outState.putBoolean("supabaseSession", RepositoryProvider.remote != null)
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
        if (actions.connected) {
            AlertDialog.Builder(this)
                .setTitle(roles[role])
                .setItems(arrayOf("Làm mới dữ liệu", "Đăng xuất")) { _, which ->
                    if (which == 0) refreshRemote() else signOut()
                }.show()
            return
        }
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
            .setNeutralButton("Kết nối Supabase") { _, _ -> connectionDialog() }
            .setNegativeButton("Đóng", null)
            .show()
    }

    private fun connectionDialog() {
        val form = layoutInflater.inflate(R.layout.dialog_connection, null, false)
        val url = form.findViewById<EditText>(R.id.connection_url)
        val key = form.findViewById<EditText>(R.id.connection_key)
        val email = form.findViewById<EditText>(R.id.connection_email)
        val password = form.findViewById<EditText>(R.id.connection_password)
        val signUp = form.findViewById<View>(R.id.connection_sign_up)
        url.setText(SupabaseConfig.DEFAULT_URL)
        val dialog = AlertDialog.Builder(this).setTitle("Đăng nhập Supabase").setView(form)
            .setPositiveButton("Đăng nhập", null)
            .setNeutralButton("Quên mật khẩu", null)
            .setNegativeButton("Hủy", null).create()
        dialog.setOnShowListener {
            fun connection(): SupabaseRepository? = try {
                SupabaseRepository(url.text.toString(), key.text.toString().trim())
            } catch (error: Exception) {
                url.error = error.message
                null
            }
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val address = email.text.toString().trim()
                val secret = password.text.toString()
                val repository = connection() ?: return@setOnClickListener
                if (address.isBlank() || secret.isBlank()) {
                    email.error = "Nhập email và mật khẩu."
                    return@setOnClickListener
                }
                actions.run({
                    repository.signIn(address, secret)
                    repository.snapshot()
                }) { snapshot ->
                    dialog.dismiss()
                    RepositoryProvider.remote = repository
                    clearNavigation()
                    role = repository.role.index
                    setSnapshot(snapshot)
                    render()
                }
            }
            signUp.setOnClickListener {
                val address = email.text.toString().trim()
                val secret = password.text.toString()
                val repository = connection() ?: return@setOnClickListener
                if (address.isBlank() || secret.length < 8) {
                    password.error = "Nhập email và mật khẩu ít nhất 8 ký tự."
                    return@setOnClickListener
                }
                actions.run({ repository.signUp(address, secret) }) {
                    verifyEmailDialog(repository, address, null)
                }
            }
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                val address = email.text.toString().trim()
                val repository = connection() ?: return@setOnClickListener
                if (address.isBlank()) {
                    email.error = "Nhập email cần khôi phục."
                    return@setOnClickListener
                }
                actions.run({ repository.requestPasswordReset(address) }) {
                    verifyEmailDialog(repository, address, password)
                }
            }
        }
        dialog.show()
    }

    private fun verifyEmailDialog(repository: SupabaseRepository, email: String, loginPassword: EditText?) {
        val reset = loginPassword != null
        val form = layoutInflater.inflate(R.layout.dialog_password_reset, null, false)
        val code = form.findViewById<EditText>(R.id.recovery_code)
        val password = form.findViewById<EditText>(R.id.new_password)
        password.visibility = if (reset) View.VISIBLE else View.GONE
        val dialog = AlertDialog.Builder(this).setTitle(if (reset) "Đặt lại mật khẩu" else "Xác nhận email")
            .setMessage("Mở email đã nhận và nhập mã xác nhận.").setView(form)
            .setPositiveButton(if (reset) "Lưu mật khẩu" else "Xác nhận", null)
            .setNegativeButton("Quay lại", null).create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val token = code.text.toString().trim()
                val secret = password.text.toString()
                if (!token.matches(Regex("[0-9]{6,10}"))) {
                    code.error = "Nhập mã trong email."
                    return@setOnClickListener
                }
                if (reset && secret.length < 8) {
                    password.error = "Nhập mật khẩu mới từ 8 ký tự."
                    return@setOnClickListener
                }
                actions.run({
                    if (reset) repository.resetPassword(email, token, secret)
                    else repository.confirmSignup(email, token)
                }) {
                    dialog.dismiss()
                    loginPassword?.setText("")
                    UiUtils.toast(this, if (reset) "Đã đổi mật khẩu. Hãy đăng nhập lại."
                        else "Đã xác nhận email. Bạn có thể đăng nhập.")
                }
            }
        }
        dialog.show()
    }

    private fun clearNavigation() {
        fields.clear()
        spinners.clear()
        drafts.clear()
        consent = null
        screen = "tab"
        tab = 0
        filter = 0
        listFilter = "Tất cả"
        back.clear()
        campaignId = "c1"
        registrationId = "R0013"
    }

    private fun signOut() {
        RepositoryProvider.remote = null
        clearNavigation()
        role = 0
        data = store
        remoteLoaded = false
        initControllers()
        render()
    }

    private fun refreshRemote() {
        val remote = RepositoryProvider.remote ?: return
        capture()
        actions.run({ remote.snapshot() }, onError = { error ->
            render()
            actions.showError(error)
        }) { snapshot ->
            role = remote.role.index
            setSnapshot(snapshot)
            render()
        }
    }

    override fun onDestroy() {
        if (::actions.isInitialized) actions.close()
        super.onDestroy()
    }

    /** Called by RenderPreview reflection and runtime. */
    open fun render() {
        initializeData()
        if (!::caregiverController.isInitialized) initControllers()
        fields.clear()
        spinners.clear()
        consent = null
        content.removeAllViews()
        UiUtils.text(findViewById(R.id.app_root), R.id.role_switch, "${shortRoles[role]} ⌄")
        drawNav()
        if (actions.connected && !remoteLoaded) {
            UiUtils.empty(layoutInflater, content, "Chưa tải được dữ liệu", "Chọn Làm mới ở menu tài khoản.")
            return
        }
        if (actions.connected && data.campaigns.isEmpty() && screen != "create" && tab != 3) {
            UiUtils.empty(layoutInflater, content, "Chưa có đợt hỗ trợ", "Các đợt trong phạm vi của bạn sẽ xuất hiện ở đây.")
            if (role == 1) coordinatorController.renderCampaigns()
            return
        }
        if (actions.connected && data.campaigns.none { it.id == campaignId } && data.campaigns.isNotEmpty()) {
            if (screen in setOf("detail", "form", "assisted", "manage", "pledge")) {
                screen = "tab"
                back.clear()
                UiUtils.toast(this, "Đợt này không còn trong phạm vi truy cập.")
            }
            campaignId = data.campaigns.first().id
        }
        if (screen != "tab") UiUtils.click(UiUtils.inflate(layoutInflater, R.layout.item_back, content), R.id.back_button, ::navigateBack)
        when (screen) {
            "detail" -> detail()
            "form" -> caregiverController.renderForm(campaignId) { regId ->
                registrationId = regId
                screen = "success"
                back.clear()
                hideKeyboard()
                render()
            }
            "assisted" -> coordinatorController.renderAssistedForm(campaignId) { regId ->
                registrationId = regId
                screen = "success"
                back.clear()
                back.add("manage")
                hideKeyboard()
                render()
            }
            "success" -> success()
            "ticket" -> caregiverController.renderTicket(registrationId)
            "manage" -> coordinatorController.renderManage(campaignId, listFilter) {
                listFilter = it
                render()
            }
            "create" -> coordinatorController.renderCreateCampaign { newId ->
                campaignId = newId
                screen = "manage"
                back.clear()
                hideKeyboard()
                render()
            }
            "pledge" -> donorController.renderPledge(campaignId)
            else -> renderTab()
        }
        updateBackHandler()
        scroll.post { scroll.scrollTo(0, 0) }
    }

    private fun renderTab() {
        if (tab == 3) {
            accountController.renderProfile(role)
            return
        }
        when (role) {
            0 -> when (tab) {
                0 -> caregiverController.renderHome(filter) {
                    filter = it
                    render()
                }
                1 -> caregiverController.renderMyRegistrations(filter) {
                    filter = it
                    render()
                }
                else -> donorController.renderImpact()
            }
            1 -> when (tab) {
                0 -> coordinatorController.renderDashboard()
                1 -> coordinatorController.renderCampaigns()
                else -> donorController.renderReports { editSupply(it) }
            }
            2 -> when (tab) {
                0 -> volunteerController.renderDistribution()
                1 -> volunteerController.renderHandedOver()
                else -> volunteerController.renderHelp()
            }
            else -> when (tab) {
                0 -> donorController.renderDonorHome()
                1 -> donorController.renderCommitments()
                else -> donorController.renderReports(null)
            }
        }
    }

    private fun drawNav() {
        nav.removeAllViews()
        val icons = if (role == 2) arrayOf("scan", "check", "heart", "user")
        else arrayOf("home", "ticket", "chart", "user")
        for ((index, title) in tabs[role].withIndex()) {
            val item = UiUtils.inflate(layoutInflater, R.layout.item_nav, nav)
            val selected = index == tab
            val color = getColor(if (selected) R.color.green else R.color.muted)
            item.findViewById<UiIcon>(R.id.nav_icon).setIcon(icons[index], color)
            val label = item.findViewById<TextView>(R.id.nav_label)
            label.text = title
            label.setTextColor(color)
            label.typeface = UiUtils.typeface(selected)
            item.contentDescription = title
            item.setBackgroundResource(if (selected) R.drawable.ui_nav_selected else R.drawable.ui_nav)
            item.setOnClickListener { switchTab(index) }
        }
    }

    private fun detail() {
        val campaign = data.campaign(campaignId)
        caregiverController.renderDetail(campaignId)
        if (campaign.archived) {
            findViewById<View>(R.id.detail_archived)?.let { UiUtils.visible(it, R.id.detail_archived, true) }
            val container = findViewById<LinearLayout>(R.id.detail_report)
            if (container != null) donorController.renderReportCard(container, campaign)
        } else if (role == 3) {
            val page = content
            UiUtils.visible(page, R.id.detail_pledge_action, true)
            UiUtils.click(page, R.id.detail_pledge_action) { go("pledge") }
        }
    }

    private fun success() {
        val registration = data.registration(registrationId)
        if (registration == null) {
            val msg = layoutInflater.inflate(R.layout.item_message, content, true)
            UiUtils.text(msg, R.id.message_text, "Không tìm thấy đăng ký.")
            return
        }
        val campaign = data.campaign(registration.campaignId)
        val page = layoutInflater.inflate(R.layout.screen_success, content, true)
        UiUtils.text(page, R.id.success_campaign, campaign.title)
        UiUtils.text(page, R.id.success_date, "${campaign.date()} · 1 suất ăn")
        UiUtils.text(
            page, R.id.success_action,
            if (role == 1) "Về danh sách để sắp xếp" else "Theo dõi đăng ký"
        )
        UiUtils.click(page, R.id.success_action) {
            if (role == 1) {
                screen = "manage"
                back.clear()
                render()
            } else {
                switchTab(1)
            }
        }
    }

    private fun openTicket(registration: DemoStore.Registration) {
        registrationId = registration.id
        campaignId = registration.campaignId
        go("ticket")
    }

    private fun editSupply(campaign: DemoStore.Campaign) {
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
                val confirmedCount = confirmed.text.toString().trim().toIntOrNull() ?: -1
                val preparedCount = prepared.text.toString().trim().toIntOrNull() ?: -1
                val receivedCount = data.received(campaign)
                val minimum = maxOf(data.assigned(campaign.id), receivedCount)
                if (confirmedCount !in minimum..1000) {
                    confirmed.error = "Từ $minimum đến 1.000 suất"
                    return@setOnClickListener
                }
                if (preparedCount !in receivedCount..confirmedCount) {
                    prepared.error = "Từ $receivedCount đến $confirmedCount suất"
                    return@setOnClickListener
                }
                actions.updateSupply(campaign, confirmedCount, preparedCount, note.text.toString().trim()) {
                    dialog.dismiss()
                    render()
                }
            }
        }
        dialog.show()
    }

    private fun chooseCampaign(page: View, includeArchived: Boolean): String? {
        val options = data.campaigns.filter {
            if (actions.connected && role == 3 && includeArchived) it.archived
            else includeArchived || !it.archived
        }
        if (options.isEmpty()) {
            UiUtils.text(page, R.id.campaign_picker, "Chưa có đợt phù hợp")
            return null
        }
        val selected = options.firstOrNull { it.id == campaignId } ?: options.first()
        campaignId = selected.id
        UiUtils.text(page, R.id.campaign_picker, "${selected.title}  ⌄")
        UiUtils.click(page, R.id.campaign_picker) {
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
        return selected.id
    }
}
