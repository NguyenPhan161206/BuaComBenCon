package vn.edu.buacombencon.ui.caregiver

import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import vn.edu.buacombencon.DemoStore
import vn.edu.buacombencon.QrView
import vn.edu.buacombencon.R
import vn.edu.buacombencon.domain.model.Campaign
import vn.edu.buacombencon.domain.model.Registration
import vn.edu.buacombencon.domain.model.RegistrationStatus
import vn.edu.buacombencon.ui.shell.UiUtils
import java.util.Locale

class CaregiverController(
    private val activity: Activity,
    private val store: DemoStore,
    private val content: LinearLayout,
    private val scroll: ScrollView,
    private val drafts: Bundle,
    private val fields: MutableMap<String, EditText>,
    private var consentHolder: () -> CheckBox?,
    private val setConsent: (CheckBox?) -> Unit,
    private val onNavigate: (String) -> Unit,
    private val onSwitchTab: (Int) -> Unit,
    private val onOpenTicket: (DemoStore.Registration) -> Unit,
    private val onRenderRequest: () -> Unit
) {
    private fun draftKey(campaignId: String) = "0:form:$campaignId"

    fun renderHome(filter: Int, onFilterChange: (Int) -> Unit) {
        val page = activity.layoutInflater.inflate(R.layout.screen_home, content, true)
        val list = page.findViewById<LinearLayout>(R.id.campaign_list)
        if (activity.resources.configuration.screenWidthDp < 360) {
            page.findViewById<View>(R.id.hero_art).translationX = UiUtils.dp(activity, 68)
        }
        UiUtils.click(page, R.id.find_meal) {
            scroll.smoothScrollTo(0, list.top + page.top - UiUtils.dp(activity, 20).toInt())
        }
        store.registrations.firstOrNull { it.mine && it.status == DemoStore.ALLOCATED }?.let { reg ->
            val camp = store.campaign(reg.campaignId)
            val card = activity.layoutInflater.inflate(R.layout.item_pickup, page.findViewById(R.id.home_pickup), true)
            UiUtils.text(card, R.id.pickup_time, "${camp.dayLabel()} · ${camp.time}")
            UiUtils.text(card, R.id.pickup_title, "${camp.title} · 1 suất")
            UiUtils.click(card, R.id.pickup_action) { onOpenTicket(reg) }
        }
        val filters = page.findViewById<LinearLayout>(R.id.filter_row)
        val options = arrayOf("Tất cả", "Hôm nay", "Ngày mai", "Bữa chiều")
        for ((index, title) in options.withIndex()) {
            UiUtils.chip(activity.layoutInflater, filters, title, filter == index) {
                onFilterChange(index)
            }
        }
        val available = store.campaigns.filter { c ->
            !c.archived && when (filter) {
                0 -> true
                1 -> c.day == 0
                2 -> c.day == 1
                3 -> c.meal == "Bữa chiều"
                else -> false
            }
        }
        for (c in available) renderCampaignCard(list, c)
        if (available.isEmpty()) {
            UiUtils.empty(activity.layoutInflater, list, "Chưa có đợt phù hợp", "Bạn có thể chọn bộ lọc khác.")
        }
        activity.layoutInflater.inflate(R.layout.item_notice, list, true)
    }

    private fun renderCampaignCard(parent: LinearLayout, campaign: DemoStore.Campaign) {
        val card = activity.layoutInflater.inflate(R.layout.item_campaign, parent, true)
        val date = campaign.date()
        UiUtils.text(card, R.id.campaign_day, date.substring(0, 2))
        UiUtils.text(card, R.id.campaign_month, "THÁNG ${date.substring(3, 5)}")
        UiUtils.text(card, R.id.campaign_meal, "${campaign.dayLabel()} · ${campaign.meal}")
        UiUtils.text(card, R.id.campaign_title, campaign.title)
        UiUtils.text(card, R.id.campaign_time, campaign.time)
        UiUtils.text(card, R.id.campaign_location, campaign.location)
        val reg = store.mine(campaign.id)
        val status = when {
            reg != null -> DemoStore.statusLabel(reg.status)
            campaign.registrationOpen -> "Tiếp nhận đăng ký · Kết quả do đơn vị xác nhận"
            else -> "Đã đóng đăng ký"
        }
        UiUtils.text(card, R.id.campaign_status, status)
        UiUtils.text(card, R.id.campaign_action, "Xem chi tiết  →")
        UiUtils.click(card, R.id.campaign_action) {
            onNavigate("detail:$campaign.id")
        }
    }

    fun renderDetail(campaignId: String) {
        val campaign = store.campaign(campaignId)
        val page = activity.layoutInflater.inflate(R.layout.screen_detail, content, true)
        val status = if (campaign.archived) "ĐÃ KẾT THÚC"
        else "ĐỢT HỖ TRỢ · ${campaign.meal.uppercase(Locale.forLanguageTag("vi"))}"
        UiUtils.text(page, R.id.detail_status, status)
        UiUtils.text(page, R.id.page_title, campaign.title)
        UiUtils.text(page, R.id.detail_date, "${campaign.dayLabel()} · ${campaign.date()}")
        UiUtils.text(page, R.id.detail_time, campaign.time)
        UiUtils.text(page, R.id.detail_location, campaign.location)
        UiUtils.text(
            page, R.id.detail_registration_info,
            "Hạn đăng ký: ${campaign.deadline}.\nĐơn vị tổ chức: Nhóm hỗ trợ An Tâm (mẫu).\nSố suất xác nhận cung cấp: ${campaign.confirmed}."
        )
        UiUtils.text(page, R.id.detail_rules, campaign.rules)
        UiUtils.click(page, R.id.contact_action) {
            UiUtils.info(
                activity, "Đầu mối hỗ trợ",
                "Bàn điều phối tại điểm phát An Tâm (mẫu). Chưa có số điện thoại thật được xác nhận."
            )
        }
        val reg = store.mine(campaign.id)
        when {
            reg != null -> {
                UiUtils.visible(page, R.id.detail_registered, true)
                UiUtils.text(page, R.id.detail_my_status, DemoStore.statusLabel(reg.status))
                UiUtils.text(
                    page, R.id.detail_ticket_action,
                    if (reg.status == DemoStore.ALLOCATED) "Xem mã nhận suất" else "Xem đăng ký của tôi"
                )
                UiUtils.click(page, R.id.detail_ticket_action) { onOpenTicket(reg) }
            }
            campaign.registrationOpen -> {
                UiUtils.visible(page, R.id.detail_open, true)
                UiUtils.click(page, R.id.register_action) { onNavigate("form") }
            }
            else -> UiUtils.visible(page, R.id.detail_closed, true)
        }
    }

    fun renderForm(campaignId: String, onRegistered: (String) -> Unit) {
        val campaign = store.campaign(campaignId)
        val form = activity.layoutInflater.inflate(R.layout.screen_form, content, true)
        UiUtils.text(form, R.id.form_title, "Đăng ký nhận suất")
        UiUtils.text(form, R.id.form_context, "${campaign.title}\n${campaign.date()} · ${campaign.time}\n1 suất / người chăm sóc")
        val name = bindField(form, R.id.input_name, "name", "", campaignId)
        val phone = bindField(form, R.id.input_phone, "phone", "", campaignId)
        val note = bindField(form, R.id.input_note, "note", "", campaignId)
        val consentBox = bindConsent(form, R.id.consent, campaignId)

        UiUtils.click(form, R.id.submit_form) {
            if (!validName(name) || !validPhone(phone)) return@click
            if (!consentBox.isChecked) {
                UiUtils.info(activity, "Cần sự đồng ý", "Vui lòng đọc và đồng ý phần cung cấp thông tin trước khi gửi.")
                return@click
            }
            val reg = try {
                store.register(campaign.id, name.text.toString().trim(), phone.text.toString().trim(), note.text.toString().trim(), true, false)
            } catch (error: IllegalStateException) {
                UiUtils.info(activity, "Chưa thể đăng ký", error.message.orEmpty())
                return@click
            }
            drafts.remove(draftKey(campaignId))
            fields.clear()
            setConsent(null)
            onRegistered(reg.id)
        }
    }

    fun renderMyRegistrations(filter: Int, onFilterChange: (Int) -> Unit) {
        val page = activity.layoutInflater.inflate(R.layout.screen_my_registrations, content, true)
        val filters = page.findViewById<LinearLayout>(R.id.registration_filters)
        val list = page.findViewById<LinearLayout>(R.id.registration_list)
        UiUtils.chip(activity.layoutInflater, filters, "Hiện tại", filter != 9) { onFilterChange(0) }
        UiUtils.chip(activity.layoutInflater, filters, "Lịch sử", filter == 9) { onFilterChange(9) }

        for (registration in store.registrations) {
            if (!registration.mine) continue
            val campaign = store.campaign(registration.campaignId)
            val history = campaign.archived || registration.status == DemoStore.RECEIVED || registration.status == DemoStore.CANCELLED
            if ((filter == 9) != history) continue
            val card = activity.layoutInflater.inflate(R.layout.item_my_registration, list, true)
            UiUtils.status(card, R.id.registration_status, registration.status)
            UiUtils.text(card, R.id.registration_title, campaign.title)
            UiUtils.text(card, R.id.registration_time, "${campaign.date()} · ${campaign.time}")
            val source = if (registration.assisted) "Được đăng ký hộ" else "Tự đăng ký"
            UiUtils.text(card, R.id.registration_source, "1 suất · $source")
            UiUtils.text(
                card, R.id.registration_action,
                if (registration.status == DemoStore.ALLOCATED) "Mở mã nhận suất  →" else "Xem trạng thái  →"
            )
            UiUtils.click(card, R.id.registration_action) { onOpenTicket(registration) }
        }
        if (list.childCount == 0) {
            UiUtils.empty(activity.layoutInflater, list, "Chưa có đăng ký", "Chọn một đợt hỗ trợ phù hợp để gửi đăng ký nhận suất.")
            UiUtils.visible(page, R.id.find_campaign_action, true)
            UiUtils.click(page, R.id.find_campaign_action) {
                onFilterChange(0)
                onSwitchTab(0)
            }
        }
    }

    fun renderTicket(registrationId: String) {
        val registration = store.registration(registrationId)
        if (registration == null) {
            val msg = activity.layoutInflater.inflate(R.layout.item_message, content, true)
            UiUtils.text(msg, R.id.message_text, "Không tìm thấy đăng ký.")
            return
        }
        val campaign = store.campaign(registration.campaignId)
        val page = activity.layoutInflater.inflate(R.layout.screen_ticket, content, true)
        val label = DemoStore.statusLabel(registration.status)
        UiUtils.text(page, R.id.page_title, if (registration.status == DemoStore.ALLOCATED) "Suất ăn đã sẵn sàng" else label)
        UiUtils.text(page, R.id.page_subtitle, campaign.title)
        UiUtils.text(page, R.id.ticket_status, label)
        val ticketContent = page.findViewById<LinearLayout>(R.id.ticket_content)
        when (registration.status) {
            DemoStore.ALLOCATED -> {
                activity.layoutInflater.inflate(R.layout.ticket_allocated, ticketContent, true)
                page.findViewById<QrView>(R.id.ticket_qr).setPayload(registration.code)
                UiUtils.text(page, R.id.ticket_code, registration.code)
                UiUtils.click(page, R.id.copy_code_action) {
                    activity.getSystemService(ClipboardManager::class.java)
                        ?.setPrimaryClip(ClipData.newPlainText("Mã nhận suất", registration.code))
                    UiUtils.toast(activity, "Đã sao chép mã nhận.")
                }
            }
            DemoStore.PENDING, DemoStore.WAITING -> {
                activity.layoutInflater.inflate(R.layout.ticket_pending, ticketContent, true)
                UiUtils.text(
                    page, R.id.pending_title,
                    if (registration.status == DemoStore.WAITING) "Bạn đang ở danh sách chờ" else "Đăng ký đang được kiểm tra"
                )
            }
            else -> {
                activity.layoutInflater.inflate(R.layout.ticket_inactive, ticketContent, true)
                UiUtils.text(
                    page, R.id.inactive_text,
                    if (registration.status == DemoStore.RECEIVED) "Suất ăn đã được ghi nhận giao. Mã không còn hiệu lực."
                    else "Đăng ký đã hủy. Mã không còn hiệu lực."
                )
            }
        }
        UiUtils.text(page, R.id.ticket_time, "${campaign.date()} · ${campaign.time}")
        UiUtils.text(page, R.id.ticket_location, campaign.location)
        if (registration.status != DemoStore.RECEIVED && registration.status != DemoStore.CANCELLED) {
            UiUtils.visible(page, R.id.cancel_registration_action, true)
            UiUtils.click(page, R.id.cancel_registration_action) {
                AlertDialog.Builder(activity)
                    .setTitle("Hủy đăng ký suất ăn?")
                    .setMessage("Đơn vị tổ chức có thể sắp xếp suất cho người đang chờ. Mã nhận của bạn sẽ hết hiệu lực.")
                    .setNegativeButton("Giữ đăng ký", null)
                    .setPositiveButton("Xác nhận hủy") { _, _ ->
                        registration.status = DemoStore.CANCELLED
                        store.save()
                        onRenderRequest()
                    }
                    .show()
            }
        }
    }

    private fun bindField(root: View, id: Int, key: String, fallback: String, campaignId: String): EditText {
        val input = root.findViewById<EditText>(id)
        fields[key] = input
        input.setText(drafts.getBundle(draftKey(campaignId))?.getString(key, fallback) ?: fallback)
        input.isSaveEnabled = false
        return input
    }

    private fun bindConsent(root: View, id: Int, campaignId: String): CheckBox {
        val box = root.findViewById<CheckBox>(id)
        setConsent(box)
        drafts.getBundle(draftKey(campaignId))?.let { box.isChecked = it.getBoolean("consent") }
        return box
    }

    private fun validName(input: EditText): Boolean {
        if (input.text.toString().trim().length >= 2) return true
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
}
