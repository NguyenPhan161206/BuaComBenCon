package vn.edu.buacombencon.ui.volunteer

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Switch
import vn.edu.buacombencon.DemoStore
import vn.edu.buacombencon.R
import vn.edu.buacombencon.ui.shell.UiUtils

class VolunteerController(
    private val activity: Activity,
    private val store: DemoStore,
    private val content: LinearLayout,
    private val drafts: Bundle,
    private val fields: MutableMap<String, EditText>,
    private var isConnected: () -> Boolean,
    private val setConnected: (Boolean) -> Unit,
    private val onChooseCampaign: (View, Boolean) -> Unit,
    private val onRenderRequest: () -> Unit
) {
    fun renderDistribution(campaignId: String) {
        val page = activity.layoutInflater.inflate(R.layout.screen_distribution, content, true)
        onChooseCampaign(page, false)
        val campaign = store.campaign(campaignId)
        val received = store.received(campaign)
        UiUtils.metrics(
            page, arrayOf("Thực chuẩn bị", "Đã trao", "Chưa trao"),
            intArrayOf(campaign.prepared, received, maxOf(0, campaign.prepared - received))
        )
        val codeInput = bindField(page, R.id.claim_code, "claim_code", "")
        UiUtils.click(page, R.id.check_code_action) { checkCode(codeInput.text.toString().trim(), campaignId) }
        UiUtils.click(page, R.id.sample_code_action) { codeInput.setText("BC0013") }
        val network = page.findViewById<Switch>(R.id.network_switch)
        network.isChecked = isConnected()
        network.setOnCheckedChangeListener { _, checked ->
            setConnected(checked)
            UiUtils.toast(
                activity, if (checked) "Đã bật trạng thái kết nối mẫu."
                else "Mất kết nối mẫu: không thể xác nhận giao suất."
            )
        }
        UiUtils.click(page, R.id.distribution_help_action) {
            UiUtils.info(
                activity, "Hỗ trợ tại điểm phát",
                "Nếu không có mã, mã sai hoặc mất kết nối, chuyển đến bàn điều phối. Dùng danh sách dự phòng theo quy trình của đơn vị; chỉ cập nhật đã giao sau khi thông tin được kiểm tra."
            )
        }
    }

    private fun checkCode(code: String, campaignId: String) {
        if (!isConnected()) {
            UiUtils.info(
                activity, "Chưa thể kiểm tra mã",
                "Đang mô phỏng mất kết nối. Báo điều phối viên và dùng danh sách dự phòng; chưa ghi nhận giao thành công."
            )
            return
        }
        val registration = store.findCode(code)
        val error = store.claimError(registration, campaignId)
        if (registration == null || error != null) {
            UiUtils.info(activity, "Cần kiểm tra lại", error.orEmpty())
            return
        }
        AlertDialog.Builder(activity)
            .setTitle("Mã hợp lệ · 1 suất ăn")
            .setMessage("${registration.name}\n${store.campaign(campaignId).title}\n\nChỉ xác nhận sau khi bạn đã giao suất ăn cho người nhận.")
            .setNegativeButton("Chưa giao", null)
            .setPositiveButton("Đã giao suất") { _, _ ->
                if (!isConnected()) {
                    UiUtils.info(activity, "Chưa lưu giao nhận", "Mất kết nối mẫu. Cần đối chiếu với điều phối viên.")
                    return@setPositiveButton
                }
                val recheck = store.claimError(registration, campaignId)
                if (recheck != null) {
                    UiUtils.info(activity, "Cần kiểm tra lại", recheck)
                    return@setPositiveButton
                }
                registration.status = DemoStore.RECEIVED
                store.save()
                UiUtils.toast(activity, "Đã ghi nhận giao 1 suất (dữ liệu mẫu).")
                onRenderRequest()
            }
            .show()
    }

    fun renderHandedOver(campaignId: String) {
        val page = activity.layoutInflater.inflate(R.layout.screen_handed_over, content, true)
        onChooseCampaign(page, false)
        val campaign = store.campaign(campaignId)
        UiUtils.text(page, R.id.handover_total, "${store.received(campaign)} suất đã trao")
        UiUtils.text(page, R.id.handover_campaign, campaign.title)
        val list = page.findViewById<LinearLayout>(R.id.handed_over_list)
        for (registration in store.inCampaign(campaign.id)) {
            if (registration.status != DemoStore.RECEIVED) continue
            val card = activity.layoutInflater.inflate(R.layout.item_handed_over, list, true)
            UiUtils.text(card, R.id.recipient_name, registration.name)
            UiUtils.text(card, R.id.recipient_code, "${registration.code} · Đã dùng · 1 suất")
        }
        if (list.childCount == 0) {
            UiUtils.empty(activity.layoutInflater, list, "Chưa có lượt giao", "Các lượt đã xác nhận sẽ xuất hiện ở đây.")
        }
    }

    fun renderHelp() {
        val page = activity.layoutInflater.inflate(R.layout.screen_help, content, true)
        UiUtils.click(page, R.id.help_contact_action) {
            UiUtils.info(
                activity, "Bàn điều phối",
                "Điểm phát An Tâm · khu sinh hoạt chung (mẫu). Chưa có số điện thoại thực được xác nhận."
            )
        }
    }

    private fun bindField(root: View, id: Int, key: String, fallback: String): EditText {
        val input = root.findViewById<EditText>(id)
        fields[key] = input
        input.setText(drafts.getBundle("2:distribution")?.getString(key, fallback) ?: fallback)
        input.isSaveEnabled = false
        return input
    }
}
