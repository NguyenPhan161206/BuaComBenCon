package vn.edu.buacombencon.domain.model

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

enum class RegistrationStatus(val code: String, val label: String) {
    PENDING("PENDING", "Đã tiếp nhận"),
    ALLOCATED("ALLOCATED", "Được nhận"),
    WAITING("WAITING", "Danh sách chờ"),
    RECEIVED("RECEIVED", "Đã nhận"),
    CANCELLED("CANCELLED", "Đã hủy");

    companion object {
        fun fromCode(code: String): RegistrationStatus =
            values().firstOrNull { it.code == code } ?: PENDING

        fun labelOf(code: String): String = fromCode(code).label
    }
}

enum class UserRole(val index: Int, val title: String, val shortName: String) {
    CAREGIVER(0, "Người chăm sóc", "Gia đình"),
    COORDINATOR(1, "Điều phối viên", "Điều phối"),
    VOLUNTEER(2, "Tình nguyện viên", "Phát suất"),
    DONOR(3, "Nhà hảo tâm", "Đồng hành");

    companion object {
        fun fromIndex(index: Int): UserRole =
            values().firstOrNull { it.index == index } ?: CAREGIVER
    }
}

data class Campaign(
    val id: String,
    val title: String,
    val meal: String,
    val day: Int,
    var confirmed: Int,
    val time: String,
    var pledged: Int = confirmed + 10,
    var prepared: Int = if (day <= 0) confirmed else 0,
    var registrationOpen: Boolean = day > 0,
    var location: String = "Điểm phát An Tâm · khu sinh hoạt chung (mẫu)",
    var deadline: String = if (day == 0) "10:00 hôm nay" else "18:00 ngày trước buổi phát",
    var rules: String = "Dành cho người chăm sóc bệnh nhi ung thư được nhóm hỗ trợ xác nhận. Mỗi người chăm sóc đăng ký 1 suất mỗi đợt. Sắp xếp theo thứ tự đăng ký hợp lệ; trường hợp ngoại lệ do điều phối viên xem xét.",
    var note: String = "Chưa có ghi chú xử lý suất còn lại.",
    var archived: Boolean = false,
    var archivedReceived: Int = 0
) {
    fun date(): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, day)
        return SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("vi-VN")).format(calendar.time)
    }

    fun dayLabel(): String = when (day) {
        0 -> "Hôm nay"
        1 -> "Ngày mai"
        else -> date().substring(0, 5)
    }
}

data class Registration(
    val id: String,
    val campaignId: String,
    val name: String,
    val phone: String,
    val mine: Boolean,
    val assisted: Boolean,
    var status: String,
    var note: String = ""
) {
    val code: String get() = "BC" + id.substring(1)
}

data class Commitment(
    val id: String = "",
    val campaignId: String,
    val donorName: String,
    val donorPhone: String,
    val quantity: Int,
    val cadence: String,
    val consent: Boolean = true,
    val status: String = "PENDING"
)

data class UserProfile(
    val id: String,
    val email: String,
    val fullName: String,
    val phone: String,
    val role: UserRole
)
