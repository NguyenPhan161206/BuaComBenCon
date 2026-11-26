package vn.edu.buacombencon.ui.coordinator

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import vn.edu.buacombencon.DemoStore
import vn.edu.buacombencon.R
import vn.edu.buacombencon.ui.shell.MealActions
import vn.edu.buacombencon.ui.shell.UiUtils
import vn.edu.buacombencon.ScreenData

internal class CoordinatorController(
    private val activity: Activity,
    private val store: ScreenData,
    private val actions: MealActions,
    private val content: LinearLayout,
    private val drafts: Bundle,
    private val fields: MutableMap<String, EditText>,
    private val spinners: MutableMap<String, Spinner>,
    private var consentHolder: () -> CheckBox?,
    private val setConsent: (CheckBox?) -> Unit,
    private val onNavigate: (String) -> Unit,
    private val onSwitchTab: (Int) -> Unit,
    private val draftKey: () -> String,
    private val onOpenTicket: (DemoStore.Registration) -> Unit,
    private val onRenderRequest: () -> Unit
) {
    fun renderDashboard() {
        val page = activity.layoutInflater.inflate(R.layout.screen_dashboard, content, true)
        val campaign = store.campaign("c1")
        UiUtils.text(page, R.id.dashboard_campaign, campaign.title)
        UiUtils.text(page, R.id.dashboard_time, "${campaign.date()} · ${campaign.time}")
        UiUtils.metrics(
            page, arrayOf("Đã sắp xếp", "Đã trao", "Đang chờ"),
            intArrayOf(store.assigned(campaign.id), store.received(campaign), store.count(campaign.id, DemoStore.WAITING))
        )
        UiUtils.click(page, R.id.manage_action) {
            onNavigate("manage:${campaign.id}")
        }
        val pending = store.registrations.count { it.status == DemoStore.PENDING }
        UiUtils.text(page, R.id.pending_count, "$pending đăng ký đang chờ kiểm tra")
        UiUtils.text(page, R.id.waiting_count, "${store.count(campaign.id, DemoStore.WAITING)} hồ sơ trong danh sách chờ")
        UiUtils.click(page, R.id.create_action) { onNavigate("create") }
        UiUtils.click(page, R.id.campaigns_action) { onSwitchTab(1) }
    }

    fun renderCampaigns() {
        val page = activity.layoutInflater.inflate(R.layout.screen_campaigns, content, true)
        UiUtils.click(page, R.id.create_action) { onNavigate("create") }
        val list = page.findViewById<LinearLayout>(R.id.campaign_list)
        for (campaign in store.campaigns) {
            val card = activity.layoutInflater.inflate(R.layout.item_campaign, list, true)
            val date = campaign.date()
            UiUtils.text(card, R.id.campaign_day, date.substring(0, 2))
            UiUtils.text(card, R.id.campaign_month, "THÁNG ${date.substring(3, 5)}")
            UiUtils.text(card, R.id.campaign_meal, "${campaign.dayLabel()} · ${campaign.meal}")
            UiUtils.text(card, R.id.campaign_title, campaign.title)
            UiUtils.text(card, R.id.campaign_time, campaign.time)
            UiUtils.text(card, R.id.campaign_location, campaign.location)
            val status = if (campaign.archived) {
                "Đã kết thúc · ${store.received(campaign)} suất đã trao"
            } else {
                "${store.assigned(campaign.id)} / ${campaign.confirmed} suất đã sắp xếp · ${store.count(campaign.id, DemoStore.WAITING)} chờ"
            }
            UiUtils.text(card, R.id.campaign_status, status)
            val action = if (campaign.archived) "Xem báo cáo  →" else "Quản lý đợt  →"
            UiUtils.text(card, R.id.campaign_action, action)
            UiUtils.click(card, R.id.campaign_action) {
                onNavigate(if (campaign.archived) "detail:${campaign.id}" else "manage:${campaign.id}")
            }
        }
    }

    fun renderManage(campaignId: String, listFilter: String, onFilterChange: (String) -> Unit) {
        val campaign = store.campaign(campaignId)
        val page = activity.layoutInflater.inflate(R.layout.screen_manage, content, true)
        UiUtils.text(page, R.id.page_title, campaign.title)
        UiUtils.text(page, R.id.page_subtitle, "${campaign.date()} · ${campaign.time}")
        UiUtils.metrics(
            page, arrayOf("Đã xác nhận", "Đã sắp xếp", "Đã trao"),
            intArrayOf(campaign.confirmed, store.assigned(campaign.id), store.received(campaign))
        )
        UiUtils.text(page, R.id.supply_summary, "Cam kết ${campaign.pledged} · Thực chuẩn bị ${campaign.prepared}")
        UiUtils.visible(page, R.id.assisted_action, campaign.registrationOpen)
        UiUtils.click(page, R.id.assisted_action) { onNavigate("assisted") }
        UiUtils.text(
            page, R.id.toggle_registration_action,
            if (campaign.registrationOpen) "Đóng tiếp nhận đăng ký" else "Mở tiếp nhận đăng ký"
        )
        UiUtils.click(page, R.id.toggle_registration_action) {
            AlertDialog.Builder(activity)
                .setTitle("Cập nhật tiếp nhận đăng ký?")
                .setMessage(if (actions.connected) "Server vẫn kiểm tra hạn tiếp nhận đã công bố." else "Cập nhật trạng thái của đợt mẫu.")
                .setNegativeButton("Giữ nguyên", null)
                .setPositiveButton("Cập nhật") { _, _ ->
                    if (actions.connected) {
                        actions.action("set_registration_open", "p_campaign_id" to campaign.id,
                            "p_open" to !campaign.registrationOpen) { onRenderRequest() }
                    } else {
                        campaign.registrationOpen = !campaign.registrationOpen
                        (store as DemoStore).save()
                        onRenderRequest()
                    }
                }
                .show()
        }
        UiUtils.click(page, R.id.edit_supply_action) { editSupply(campaign) }
        if (actions.connected) {
            val id = if (campaign.allocated) R.id.reallocate_campaign_action else R.id.allocate_campaign_action
            UiUtils.visible(page, id, true)
            UiUtils.click(page, id) {
                val rpc = if (campaign.allocated) "reallocate_meal" else "allocate_campaign_meals"
                AlertDialog.Builder(activity)
                    .setTitle(if (campaign.allocated) "Cấp lại cho hồ sơ đầu hàng chờ?" else "Phân bổ lần đầu?")
                    .setNegativeButton("Quay lại", null)
                    .setPositiveButton("Xác nhận") { _, _ ->
                        actions.action(rpc, "p_campaign_id" to campaign.id) {
                            UiUtils.toast(activity, if (campaign.allocated) "Đã cấp lại 1 suất."
                                else "Đã cấp ${it.getInt("allocated")} suất, ${it.getInt("waiting")} hồ sơ chờ.")
                            onRenderRequest()
                        }
                    }.show()
            }
        }
        val filters = page.findViewById<LinearLayout>(R.id.manage_filters)
        val list = page.findViewById<LinearLayout>(R.id.managed_registration_list)
        val options = linkedMapOf(
            "Tất cả" to null,
            "Cần sắp xếp" to DemoStore.PENDING,
            "Danh sách chờ" to DemoStore.WAITING,
            "Được nhận" to DemoStore.ALLOCATED,
            "Đã nhận" to DemoStore.RECEIVED
        )
        for (title in options.keys) {
            UiUtils.chip(activity.layoutInflater, filters, title, listFilter == title) {
                onFilterChange(title)
            }
        }
        val statusFilter = options[listFilter]
        for (registration in store.inCampaign(campaign.id)) {
            if (statusFilter != null && registration.status != statusFilter) continue
            val card = activity.layoutInflater.inflate(R.layout.item_managed_registration, list, true)
            UiUtils.text(card, R.id.registration_name, registration.name)
            val source = if (registration.assisted) "Đăng ký hộ" else "Tự đăng ký"
            UiUtils.text(card, R.id.registration_meta, "${registration.id} · $source")
            UiUtils.status(card, R.id.registration_status, registration.status)
            if (registration.note.isNotEmpty()) {
                UiUtils.visible(card, R.id.registration_note, true)
                UiUtils.text(card, R.id.registration_note, registration.note)
            }
            if (registration.status == DemoStore.PENDING && !registration.reviewed) {
                if (!registration.complete) {
                    UiUtils.visible(card, R.id.complete_registration_action, true)
                    UiUtils.click(card, R.id.complete_registration_action) {
                        if (actions.connected) {
                            actions.action("complete_registration", "p_registration_id" to registration.id,
                                "p_note" to registration.note) { onRenderRequest() }
                        } else {
                            (store as DemoStore).completeRegistration(registration.id, registration.note)
                            onRenderRequest()
                        }
                    }
                }
                UiUtils.visible(card, R.id.allocate_action, true)
                UiUtils.text(card, R.id.allocate_action, "Xét điều kiện")
                UiUtils.click(card, R.id.allocate_action) {
                    if (actions.connected) reviewRegistration(registration)
                    else confirmAllocation(campaign, registration)
                }
            } else if (!actions.connected && registration.status == DemoStore.WAITING) {
                UiUtils.visible(card, R.id.allocate_action, true)
                UiUtils.click(card, R.id.allocate_action) { confirmAllocation(campaign, registration) }
            }
            if (registration.status == DemoStore.ALLOCATED) {
                UiUtils.visible(card, R.id.ticket_action, true)
                UiUtils.click(card, R.id.ticket_action) { onOpenTicket(registration) }
            }
        }
        if (list.childCount == 0) {
            UiUtils.empty(
                activity.layoutInflater, list, "Không có hồ sơ ở trạng thái này",
                "Danh sách sẽ cập nhật khi có đăng ký hoặc thay đổi kết quả."
            )
        }
    }

    private fun reviewRegistration(registration: DemoStore.Registration) {
        fun review(approved: Boolean) {
            actions.action("review_registration", "p_registration_id" to registration.id, "p_is_approved" to approved) {
                onRenderRequest()
            }
        }
        val dialog = AlertDialog.Builder(activity)
            .setTitle(registration.name).setMessage("Xét hồ sơ theo điều kiện đã công bố của đợt.")
            .setNeutralButton("Quay lại", null)
            .setNegativeButton("Không đạt") { _, _ -> review(false) }
            .setPositiveButton("Đạt") { _, _ -> review(true) }
            .show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = registration.complete
    }

    private fun confirmAllocation(campaign: DemoStore.Campaign, registration: DemoStore.Registration) {
        val earlier = store.inCampaign(campaign.id)
            .takeWhile { it !== registration }
            .firstOrNull { it.status == DemoStore.PENDING || it.status == DemoStore.WAITING }
        if (earlier != null) {
            UiUtils.info(
                activity, "Còn hồ sơ đăng ký trước",
                "Vui lòng xem hồ sơ ${earlier.id} trước để giữ thứ tự sắp xếp của đợt."
            )
            return
        }
        AlertDialog.Builder(activity)
            .setTitle("Xác nhận điều kiện tham gia")
            .setMessage("Bạn đã kiểm tra hồ sơ theo tiêu chí công bố của đợt? Số suất được cấp dựa trên số xác nhận cung cấp, không dựa trên cam kết.")
            .setNegativeButton("Quay lại", null)
            .setPositiveButton("Đã kiểm tra") { _, _ ->
                UiUtils.toast(activity, (store as DemoStore).allocate(registration))
                onRenderRequest()
            }
            .show()
    }

    private fun editSupply(campaign: DemoStore.Campaign) {
        val form = activity.layoutInflater.inflate(R.layout.dialog_supply, null, false)
        val confirmed = form.findViewById<EditText>(R.id.supply_confirmed)
        val prepared = form.findViewById<EditText>(R.id.supply_prepared)
        val note = form.findViewById<EditText>(R.id.supply_note)
        confirmed.setText(campaign.confirmed.toString())
        prepared.setText(campaign.prepared.toString())
        note.setText(campaign.note)
        val dialog = AlertDialog.Builder(activity)
            .setTitle("Cập nhật đợt hỗ trợ")
            .setView(form)
            .setNegativeButton("Hủy", null)
            .setPositiveButton("Lưu", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val confirmedCount = confirmed.text.toString().trim().toIntOrNull() ?: -1
                val preparedCount = prepared.text.toString().trim().toIntOrNull() ?: -1
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
                actions.updateSupply(campaign, confirmedCount, preparedCount, note.text.toString().trim()) {
                    dialog.dismiss()
                    onRenderRequest()
                }
            }
        }
        dialog.show()
    }

    fun renderCreateCampaign(onCreated: (String) -> Unit) {
        val page = activity.layoutInflater.inflate(R.layout.screen_create, content, true)
        val title = bindField(page, R.id.create_title, "title", "")
        val offset = bindField(page, R.id.create_day, "day", "1")
        val time = bindField(page, R.id.create_time, "time", "11:00 – 12:00")
        val capacity = bindField(page, R.id.create_capacity, "capacity", "30")
        val deadline = bindField(page, R.id.create_deadline, "deadline", "18:00 ngày trước buổi phát")
        val rules = bindField(
            page, R.id.create_rules, "rules",
            "Mỗi người chăm sóc 1 suất; hồ sơ đủ điều kiện được sắp xếp theo thứ tự đăng ký."
        )
        val meal = bindSpinner(page, R.id.create_meal, "meal", arrayOf("Bữa trưa", "Bữa chiều"))
        if (actions.connected) {
            deadline.isEnabled = false
            deadline.setText("Đóng nhận hồ sơ mới trước giờ phát 2 tiếng")
        }
        UiUtils.click(page, R.id.submit_create) {
            if (title.text.toString().trim().length < 3) {
                title.error = "Nhập tên đợt hỗ trợ"
                return@click
            }
            val day = offset.text.toString().trim().toIntOrNull() ?: -1
            val quantity = capacity.text.toString().trim().toIntOrNull() ?: -1
            if (day !in 0..365) {
                offset.error = "Nhập số từ 0 đến 365"
                return@click
            }
            if (quantity !in 1..1000) {
                capacity.error = "Nhập số từ 1 đến 1.000"
                return@click
            }
            if (time.text.toString().trim().isEmpty() || deadline.text.toString().trim().isEmpty() || rules.text.toString().trim().isEmpty()) {
                UiUtils.info(activity, "Thiếu thông tin", "Điền thời gian, thời hạn và tiêu chí trước khi tạo đợt.")
                return@click
            }
            val campaign = DemoStore.Campaign(
                "c${System.currentTimeMillis()}", title.text.toString().trim(),
                meal.selectedItem.toString(), day, quantity, time.text.toString().trim()
            )
            campaign.prepared = 0
            campaign.pledged = quantity
            campaign.registrationOpen = true
            campaign.deadline = deadline.text.toString().trim()
            campaign.rules = rules.text.toString().trim()
            fun done(id: String) {
                drafts.remove(draftKey())
                fields.clear()
                spinners.clear()
                onCreated(id)
            }
            if (actions.connected) {
                val times = campaign.time.split(Regex("""\s*[-–]\s*"""), limit = 2)
                val start = times.firstOrNull()?.let { runCatching { LocalTime.parse(it) }.getOrNull() }
                val end = times.getOrNull(1)?.let { runCatching { LocalTime.parse(it) }.getOrNull() }
                if (start == null || end == null || end <= start) {
                    time.error = "Nhập khung giờ HH:mm – HH:mm trong cùng ngày."
                    return@click
                }
                val date = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusDays(day.toLong())
                actions.action("create_campaign", "p_title" to campaign.title, "p_meal" to campaign.meal,
                    "p_date" to date.toString(), "p_pickup_start" to start.toString(), "p_pickup_end" to end.toString(),
                    "p_location" to campaign.location, "p_rules" to campaign.rules, "p_confirmed" to quantity,
                    "p_volunteer_id" to "33333333-3333-3333-3333-333333333333") {
                    done(it.getString("id"))
                }
            } else {
                store.campaigns.add(0, campaign)
                (store as DemoStore).save()
                done(campaign.id)
            }
        }
    }

    fun renderAssistedForm(campaignId: String, onRegistered: (String) -> Unit) {
        val campaign = store.campaign(campaignId)
        val form = activity.layoutInflater.inflate(R.layout.screen_form, content, true)
        UiUtils.text(form, R.id.form_title, "Đăng ký hộ gia đình")
        UiUtils.text(form, R.id.form_context, "${campaign.title}\n${campaign.date()} · ${campaign.time}\n1 suất / người chăm sóc")
        val name = bindField(form, R.id.input_name, "name", "")
        val phone = bindField(form, R.id.input_phone, "phone", "")
        val note = bindField(form, R.id.input_note, "note", "")
        val consentBox = form.findViewById<CheckBox>(R.id.consent)
        setConsent(consentBox)
        consentBox.text = "Tôi đã trao đổi với người chăm sóc và được đồng ý ghi nhận thông tin liên hệ."
        drafts.getBundle(draftKey())?.let { consentBox.isChecked = it.getBoolean("consent") }
        val incomplete = form.findViewById<CheckBox>(R.id.incomplete_registration)
        UiUtils.visible(form, R.id.incomplete_registration, actions.connected)
        incomplete.isChecked = drafts.getBundle(draftKey())?.getBoolean("incomplete") ?: false
        incomplete.setOnCheckedChangeListener { _, checked ->
            val draft = drafts.getBundle(draftKey()) ?: Bundle()
            draft.putBoolean("incomplete", checked)
            drafts.putBundle(draftKey(), draft)
        }

        UiUtils.click(form, R.id.submit_form) {
            val nameVal = name.text.toString().trim()
            val phoneVal = phone.text.toString().trim()
            if (nameVal.length < 2) {
                name.error = "Nhập tên có ít nhất 2 ký tự"
                return@click
            }
            val cleanPhone = phoneVal.replace(Regex("[\\s.-]"), "")
            if (!cleanPhone.matches(Regex("(0[0-9]{9}|\\+84[0-9]{9})"))) {
                phone.error = "Nhập số gồm 10 chữ số bắt đầu bằng 0 hoặc +84"
                return@click
            }
            if (!consentBox.isChecked) {
                UiUtils.info(activity, "Cần sự đồng ý", "Vui lòng đọc và đồng ý phần cung cấp thông tin trước khi gửi.")
                return@click
            }
            actions.register(campaign.id, nameVal, cleanPhone, note.text.toString().trim(),
                assisted = true, complete = !incomplete.isChecked) { id ->
                drafts.remove(draftKey())
                fields.clear()
                setConsent(null)
                onRegistered(id)
            }
        }
    }

    private fun bindField(root: View, id: Int, key: String, fallback: String): EditText {
        val input = root.findViewById<EditText>(id)
        fields[key] = input
        input.setText(drafts.getBundle(draftKey())?.getString(key, fallback) ?: fallback)
        input.isSaveEnabled = false
        return input
    }

    private fun bindSpinner(root: View, id: Int, key: String, options: Array<String>): Spinner {
        val menu = root.findViewById<Spinner>(id)
        menu.adapter = ArrayAdapter(activity, android.R.layout.simple_spinner_dropdown_item, options)
        spinners[key] = menu
        drafts.getBundle(draftKey())?.let { menu.setSelection(it.getInt("spinner:$key", 0)) }
        return menu
    }
}
