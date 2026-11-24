package vn.edu.buacombencon.ui.donor

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Spinner
import vn.edu.buacombencon.DemoStore
import vn.edu.buacombencon.R
import vn.edu.buacombencon.ui.shell.UiUtils

class DonorController(
    private val activity: Activity,
    private val store: DemoStore,
    private val content: LinearLayout,
    private val drafts: Bundle,
    private val fields: MutableMap<String, EditText>,
    private val spinners: MutableMap<String, Spinner>,
    private var consentHolder: () -> CheckBox?,
    private val setConsent: (CheckBox?) -> Unit,
    private val onNavigate: (String) -> Unit,
    private val onSwitchTab: (Int) -> Unit,
    private val onChooseCampaign: (View, Boolean) -> Unit
) {
    fun renderDonorHome() {
        val page = activity.layoutInflater.inflate(R.layout.screen_donor_home, content, true)
        val list = page.findViewById<LinearLayout>(R.id.donor_campaign_list)
        for (campaign in store.campaigns) {
            if (campaign.archived) continue
            val card = activity.layoutInflater.inflate(R.layout.item_donor_campaign, list, true)
            UiUtils.text(card, R.id.campaign_title, campaign.title)
            UiUtils.text(card, R.id.campaign_time, "${campaign.date()} · ${campaign.meal}")
            UiUtils.metrics(card, arrayOf("Cam kết", "Đã xác nhận"), intArrayOf(campaign.pledged, campaign.confirmed))
            UiUtils.click(card, R.id.pledge_action) {
                onNavigate("pledge:${campaign.id}")
            }
        }
    }

    fun renderPledge(campaignId: String) {
        val campaign = store.campaign(campaignId)
        val page = activity.layoutInflater.inflate(R.layout.screen_pledge, content, true)
        UiUtils.text(page, R.id.page_subtitle, "${campaign.title} · ${campaign.date()}")
        val name = bindField(page, R.id.donor_name, "donor_name", "")
        val phone = bindField(page, R.id.donor_phone, "donor_phone", "")
        val quantity = bindField(page, R.id.pledge_quantity, "quantity", "10")
        val cadence = bindSpinner(
            page, R.id.pledge_cadence, "cadence",
            arrayOf("Theo đợt", "Mong muốn theo tuần", "Mong muốn theo tháng")
        )
        val consentBox = bindConsent(page, R.id.pledge_consent)
        UiUtils.click(page, R.id.submit_pledge) {
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
            val count = quantity.text.toString().trim().toIntOrNull() ?: -1
            if (count !in 1..1000) {
                quantity.error = "Nhập từ 1 đến 1.000 suất"
                return@click
            }
            if (!consentBox.isChecked) {
                UiUtils.info(activity, "Cần sự đồng ý", "Vui lòng đồng ý để đơn vị liên hệ về đăng ký.")
                return@click
            }
            if (store.donorPledged > 0) {
                val old = store.campaign(store.donorCampaign)
                old.pledged = maxOf(0, old.pledged - store.donorPledged)
            }
            campaign.pledged += count
            store.donorPledged = count
            store.donorCampaign = campaign.id
            store.donorCadence = cadence.selectedItem.toString()
            store.save()
            fields.clear()
            spinners.clear()
            setConsent(null)
            onSwitchTab(1)
            UiUtils.info(
                activity, "Đã ghi nhận cam kết",
                "Cam kết đang chờ đơn vị liên hệ. Số suất xác nhận cung cấp chưa thay đổi."
            )
        }
    }

    fun renderCommitments() {
        val page = activity.layoutInflater.inflate(R.layout.screen_commitments, content, true)
        if (store.donorPledged == 0) {
            UiUtils.empty(
                activity.layoutInflater, page.findViewById(R.id.commitment_empty), "Bắt đầu từ một bữa ăn",
                "Chọn một đợt phù hợp và gửi mong muốn đồng hành."
            )
            UiUtils.visible(page, R.id.commitment_browse_action, true)
            UiUtils.click(page, R.id.commitment_browse_action) { onSwitchTab(0) }
            return
        }
        UiUtils.visible(page, R.id.commitment_details, true)
        val campaign = store.campaign(store.donorCampaign)
        UiUtils.text(page, R.id.commitment_quantity, "${store.donorPledged} suất ăn")
        UiUtils.text(page, R.id.commitment_title, campaign.title)
        UiUtils.text(page, R.id.commitment_time, "${campaign.date()} · ${store.donorCadence}")
        UiUtils.click(page, R.id.commitment_edit_action) {
            onNavigate("pledge:${campaign.id}")
        }
        UiUtils.click(page, R.id.commitment_report_action) { onSwitchTab(2) }
    }

    fun renderReports(campaignId: String, onEditSupply: ((DemoStore.Campaign) -> Unit)?) {
        val page = activity.layoutInflater.inflate(R.layout.screen_reports, content, true)
        onChooseCampaign(page, true)
        val campaign = store.campaign(campaignId)
        renderReportCard(page.findViewById(R.id.report_container), campaign)
        if (onEditSupply != null) {
            UiUtils.visible(page, R.id.report_edit_action, true)
            UiUtils.click(page, R.id.report_edit_action) { onEditSupply(campaign) }
        }
    }

    fun renderImpact() {
        val page = activity.layoutInflater.inflate(R.layout.screen_impact, content, true)
        renderReportCard(page.findViewById(R.id.impact_report), store.campaign("c0"))
    }

    fun renderReportCard(parent: LinearLayout, campaign: DemoStore.Campaign) {
        val card = activity.layoutInflater.inflate(R.layout.item_report, parent, true)
        UiUtils.text(card, R.id.report_status, if (campaign.archived) "ĐÃ ĐỐI CHIẾU · MẪU" else "ĐANG CẬP NHẬT · MẪU")
        UiUtils.text(card, R.id.report_title, campaign.title)
        UiUtils.text(card, R.id.report_time, "${campaign.date()} · ${campaign.meal}")
        val received = store.received(campaign)
        val ids = intArrayOf(R.id.stat_0, R.id.stat_1, R.id.stat_2, R.id.stat_3, R.id.stat_4)
        val values = intArrayOf(
            campaign.pledged, campaign.confirmed, campaign.prepared, received,
            maxOf(0, campaign.prepared - received)
        )
        for (index in ids.indices) UiUtils.text(card, ids[index], values[index].toString())
        val progress = card.findViewById<ProgressBar>(R.id.report_progress)
        progress.max = maxOf(1, campaign.prepared)
        progress.progress = received
        UiUtils.text(card, R.id.report_note_title, if (campaign.archived) "Ghi chú sau buổi phát" else "Ghi chú của đơn vị")
        UiUtils.text(card, R.id.report_note, campaign.note)
        UiUtils.visible(card, R.id.report_live_note, !campaign.archived)
    }

    private fun bindField(root: View, id: Int, key: String, fallback: String): EditText {
        val input = root.findViewById<EditText>(id)
        fields[key] = input
        input.setText(drafts.getBundle("3:pledge")?.getString(key, fallback) ?: fallback)
        input.isSaveEnabled = false
        return input
    }

    private fun bindSpinner(root: View, id: Int, key: String, options: Array<String>): Spinner {
        val menu = root.findViewById<Spinner>(id)
        menu.adapter = ArrayAdapter(activity, android.R.layout.simple_spinner_dropdown_item, options)
        spinners[key] = menu
        drafts.getBundle("3:pledge")?.let { menu.setSelection(it.getInt("spinner:$key", 0)) }
        return menu
    }

    private fun bindConsent(root: View, id: Int): CheckBox {
        val box = root.findViewById<CheckBox>(id)
        setConsent(box)
        drafts.getBundle("3:pledge")?.let { box.isChecked = it.getBoolean("consent") }
        return box
    }
}
