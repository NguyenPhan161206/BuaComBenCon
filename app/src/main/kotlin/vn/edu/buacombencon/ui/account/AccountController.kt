package vn.edu.buacombencon.ui.account

import android.app.Activity
import android.app.AlertDialog
import android.widget.LinearLayout
import vn.edu.buacombencon.R
import vn.edu.buacombencon.ui.shell.MealActions
import vn.edu.buacombencon.ui.shell.UiUtils
import vn.edu.buacombencon.ScreenData
import java.util.Locale

internal class AccountController(
    private val activity: Activity,
    private val store: ScreenData,
    private val actions: MealActions,
    private val content: LinearLayout,
    private val roles: Array<String>,
    private val onChooseRole: () -> Unit,
    private val onResetDemo: () -> Unit,
    private val onRefresh: () -> Unit,
    private val onSignOut: () -> Unit
) {
    fun renderProfile(role: Int) {
        val page = activity.layoutInflater.inflate(R.layout.screen_profile, content, true)
        UiUtils.text(page, R.id.page_subtitle, "Bạn đang xem vai trò ${roles[role].lowercase(Locale.forLanguageTag("vi"))}.")
        UiUtils.text(page, R.id.profile_role, roles[role])
        if (actions.connected) {
            UiUtils.text(page, R.id.page_subtitle, store.profileName)
            UiUtils.text(page, R.id.profile_role_action, "Làm mới dữ liệu")
            UiUtils.click(page, R.id.profile_role_action, onRefresh)
            UiUtils.visible(page, R.id.profile_guide_action, false)
            UiUtils.text(page, R.id.profile_reset_action, "Đăng xuất")
            UiUtils.click(page, R.id.profile_reset_action, onSignOut)
            return
        }
        UiUtils.click(page, R.id.profile_role_action) { onChooseRole() }
        UiUtils.click(page, R.id.profile_guide_action) {
            UiUtils.info(
                activity, "Thử một quy trình hoàn chỉnh",
                "1. Gia đình: đăng ký đợt ngày mai.\n2. Đổi sang Điều phối: mở cùng đợt, kiểm tra và sắp xếp hồ sơ.\n3. Gia đình: mở Đăng ký để thấy mã nhận.\n4. Phát suất hôm nay: nhập BC0013, xác nhận sau khi giao; thử lại mã để thấy cảnh báo.\n5. Xem báo cáo đã cập nhật."
            )
        }
        UiUtils.click(page, R.id.profile_reset_action) {
            AlertDialog.Builder(activity)
                .setTitle("Khôi phục dữ liệu mẫu?")
                .setMessage("Xóa các đăng ký, cam kết và thay đổi bạn đã nhập trên thiết bị; đưa ứng dụng về dữ liệu mẫu ban đầu.")
                .setNegativeButton("Giữ dữ liệu", null)
                .setPositiveButton("Khôi phục") { _, _ ->
                    onResetDemo()
                    UiUtils.toast(activity, "Đã khôi phục dữ liệu mẫu.")
                }
                .show()
        }
    }
}
