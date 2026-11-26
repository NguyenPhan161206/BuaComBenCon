package vn.edu.buacombencon

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Local UI demonstration data. */
class DemoStore(context: Context) : ScreenData() {
    companion object {
        const val PENDING = "PENDING"
        const val ALLOCATED = "ALLOCATED"
        const val WAITING = "WAITING"
        const val RECEIVED = "RECEIVED"
        const val CANCELLED = "CANCELLED"
        const val REJECTED = "REJECTED"

        fun statusLabel(status: String): String = when (status) {
            ALLOCATED -> "Được nhận"
            WAITING -> "Danh sách chờ"
            RECEIVED -> "Đã nhận"
            CANCELLED -> "Đã hủy"
            REJECTED -> "Không đủ điều kiện"
            else -> "Đã tiếp nhận"
        }
    }

    class Campaign(
        val id: String,
        val title: String,
        val meal: String,
        val day: Int,
        var confirmed: Int,
        val time: String,
    ) {
        var pledged = confirmed + 10
        var prepared = if (day <= 0) confirmed else 0
        var registrationOpen = day > 0
        var location = "Điểm phát An Tâm · khu sinh hoạt chung (mẫu)"
        var deadline = if (day == 0) "10:00 hôm nay" else "18:00 ngày trước buổi phát"
        var rules = "Dành cho người chăm sóc bệnh nhi ung thư được nhóm hỗ trợ xác nhận. Mỗi người chăm sóc đăng ký 1 suất mỗi đợt. Sắp xếp theo thứ tự đăng ký hợp lệ; trường hợp ngoại lệ do điều phối viên xem xét."
        var note = "Chưa có ghi chú xử lý suất còn lại."
        var archived = false
        var archivedReceived = 0
        var fixedDate: LocalDate? = null
        var allocated = false
        val volunteers = mutableListOf("33333333-3333-3333-3333-333333333333")

        fun date(): String {
            fixedDate?.let { return it.format(DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ROOT)) }
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

    class Registration(
        val id: String,
        val campaignId: String,
        val name: String,
        val phone: String,
        val mine: Boolean,
        val assisted: Boolean,
        var status: String,
    ) {
        var code = "BC" + id.substring(1)
        var reviewed = false
        var complete = true
        var note = ""
    }

    private val prefs = context.getSharedPreferences("ui_demo_v1", Context.MODE_PRIVATE)

    init {
        load()
    }

    private fun seed() {
        campaigns.clear()
        registrations.clear()
        donorPledged = 0
        campaigns.add(Campaign("c1", "Bữa trưa ấm lòng", "Bữa trưa", 0, 30, "11:00 – 12:00"))
        campaigns.add(Campaign("c2", "Thêm một bữa cơm lành", "Bữa trưa", 1, 40, "11:00 – 12:00"))
        campaigns.add(Campaign("c3", "Bữa chiều sẻ chia", "Bữa chiều", 2, 25, "16:30 – 17:30"))
        val past = Campaign("c0", "Bữa cơm ngày hôm qua", "Bữa trưa", -1, 30, "11:00 – 12:00")
        past.archived = true
        past.archivedReceived = 28
        past.note = "2 suất chưa trao được nhóm chuyển tới điểm hỗ trợ theo quy tắc của đợt (minh họa)."
        campaigns.add(past)
        for (index in 1..35) {
            val status = when {
                index <= 12 -> RECEIVED
                index <= 30 -> ALLOCATED
                else -> WAITING
            }
            val name = if (index == 13) "Người chăm sóc (mẫu)"
                else "Người chăm sóc " + String.format(Locale.US, "%02d", index)
            registrations.add(
                Registration(String.format(Locale.US, "R%04d", index), "c1", name,
                    "0900000000", index == 13, index % 4 == 0, status)
            )
        }
        registrations.add(Registration("R0036", "c0", "Người chăm sóc (mẫu)",
            "0900000000", true, false, RECEIVED))
    }

    fun reset() {
        seed()
        save()
    }

    private fun load() {
        seed()
        val raw = prefs.getString("state", null) ?: return
        try {
            val root = JSONObject(raw)
            val savedCampaigns = root.getJSONArray("campaigns")
            campaigns.clear()
            for (index in 0 until savedCampaigns.length()) {
                val json = savedCampaigns.getJSONObject(index)
                val campaign = Campaign(json.getString("id"), json.getString("title"),
                    json.getString("meal"), json.getInt("day"), json.getInt("confirmed"), json.getString("time"))
                campaign.pledged = json.getInt("pledged")
                campaign.prepared = json.getInt("prepared")
                campaign.location = json.getString("location")
                campaign.archived = json.getBoolean("archived")
                campaign.archivedReceived = json.getInt("received")
                campaign.note = json.getString("note")
                campaign.deadline = json.getString("deadline")
                campaign.rules = json.getString("rules")
                campaign.registrationOpen = json.optBoolean("open", campaign.day > 0)
                campaigns.add(campaign)
            }

            val savedRegistrations = root.getJSONArray("registrations")
            registrations.clear()
            for (index in 0 until savedRegistrations.length()) {
                val json = savedRegistrations.getJSONObject(index)
                val registration = Registration(json.getString("id"), json.getString("cid"),
                    json.getString("name"), json.getString("phone"), json.getBoolean("mine"),
                    json.getBoolean("assisted"), json.getString("status"))
                registration.note = json.getString("note")
                registrations.add(registration)
            }

            donorPledged = root.optInt("donor", 0)
            donorCampaign = root.optString("donorCampaign", "c2")
            donorCadence = root.optString("cadence", "Theo đợt")
        } catch (_: Exception) {
            seed()
        }
    }

    fun save() {
        try {
            val savedCampaigns = JSONArray()
            for (campaign in campaigns) {
                savedCampaigns.put(JSONObject()
                    .put("id", campaign.id)
                    .put("title", campaign.title)
                    .put("meal", campaign.meal)
                    .put("day", campaign.day)
                    .put("confirmed", campaign.confirmed)
                    .put("time", campaign.time)
                    .put("pledged", campaign.pledged)
                    .put("prepared", campaign.prepared)
                    .put("location", campaign.location)
                    .put("archived", campaign.archived)
                    .put("received", campaign.archivedReceived)
                    .put("note", campaign.note)
                    .put("deadline", campaign.deadline)
                    .put("rules", campaign.rules)
                    .put("open", campaign.registrationOpen))
            }
            val savedRegistrations = JSONArray()
            for (registration in registrations) {
                savedRegistrations.put(JSONObject()
                    .put("id", registration.id)
                    .put("cid", registration.campaignId)
                    .put("name", registration.name)
                    .put("phone", registration.phone)
                    .put("mine", registration.mine)
                    .put("assisted", registration.assisted)
                    .put("status", registration.status)
                    .put("note", registration.note))
            }
            val root = JSONObject()
                .put("campaigns", savedCampaigns)
                .put("registrations", savedRegistrations)
                .put("donor", donorPledged)
                .put("donorCampaign", donorCampaign)
                .put("cadence", donorCadence)
            prefs.edit().putString("state", root.toString()).apply()
        } catch (error: Exception) {
            throw IllegalStateException("Cannot save demo", error)
        }
    }

    fun register(
        campaignId: String, name: String, phone: String, note: String, mine: Boolean, assisted: Boolean,
        complete: Boolean = true,
    ): Registration {
        val campaign = campaign(campaignId)
        check(!campaign.archived && campaign.registrationOpen) { "Đợt này đã đóng đăng ký." }
        val cleanPhone = phone.replace(Regex("[\\s.-]"), "")
        val duplicate = registrations.any {
            it.campaignId == campaignId &&
            it.status != CANCELLED && it.status != REJECTED &&
            (it.phone.replace(Regex("[\\s.-]"), "") == cleanPhone || (mine && it.mine))
        }
        check(!duplicate) { "Người nhận này đã có hồ sơ đăng ký cho đợt này." }
        val id = String.format(Locale.US, "R%04d", registrations.size + 1)
        val registration = Registration(id, campaignId, name, phone, mine, assisted, PENDING)
        registration.note = note
        registration.complete = complete
        registrations.add(registration)
        save()
        return registration
    }

    fun completeRegistration(registrationId: String, note: String): Boolean {
        val reg = registration(registrationId) ?: return false
        reg.complete = true
        if (note.isNotEmpty()) reg.note = note
        save()
        return true
    }

    fun allocate(registration: Registration): String {
        if (registration.status != PENDING && registration.status != WAITING) {
            return "Trạng thái không thể sắp xếp."
        }
        val campaign = campaign(registration.campaignId)
        if (assigned(campaign.id) < campaign.confirmed) {
            registration.status = ALLOCATED
            save()
            return "Đã sắp xếp 1 suất và cấp mã nhận."
        }
        registration.status = WAITING
        save()
        return "Đã đủ suất xác nhận. Hồ sơ được đưa vào danh sách chờ."
    }

    fun review(registration: Registration, approved: Boolean): String {
        check(registration.status == PENDING && !registration.reviewed) { "Hồ sơ không ở trạng thái chờ xét." }
        check(!approved || registration.complete) { "Hồ sơ chưa đủ thông tin." }
        val campaign = campaign(registration.campaignId)
        registration.reviewed = true
        if (!approved) {
            registration.status = REJECTED
            save()
            return "Hồ sơ không đạt điều kiện."
        }
        if (campaign.allocated) {
            registration.status = WAITING
            save()
            return "Hồ sơ đạt điều kiện và được đưa vào danh sách chờ."
        }
        registration.status = PENDING
        save()
        return "Hồ sơ đạt điều kiện, chờ đợt phân bổ."
    }

    fun allocateCampaign(campaignId: String): Pair<Int, Int> {
        val campaign = campaign(campaignId)
        val pending = inCampaign(campaignId).filter { it.status == PENDING && it.reviewed }
        var assignedCount = 0
        var waitingCount = 0
        for (reg in pending) {
            if (assigned(campaign.id) < campaign.confirmed) {
                reg.status = ALLOCATED
                assignedCount++
            } else {
                reg.status = WAITING
                waitingCount++
            }
        }
        campaign.allocated = true
        save()
        return assignedCount to waitingCount
    }

    fun reallocateMeal(campaignId: String): Registration? {
        val campaign = campaign(campaignId)
        check(campaign.allocated) { "Đợt chưa phân bổ lần đầu." }
        if (assigned(campaign.id) >= campaign.confirmed) return null
        val waiting = inCampaign(campaignId).firstOrNull { it.status == WAITING && it.reviewed } ?: return null
        waiting.status = ALLOCATED
        save()
        return waiting
    }

    fun cancelRegistration(registrationId: String): Boolean {
        val reg = registration(registrationId) ?: return false
        reg.status = CANCELLED
        save()
        return true
    }

    fun claimError(registration: Registration?, campaignId: String): String? {
        when {
            registration == null -> return "Không tìm thấy mã. Vui lòng kiểm tra hoặc nhờ điều phối viên hỗ trợ."
            registration.campaignId != campaignId -> return "Mã không thuộc đợt đang chọn."
            registration.status == RECEIVED -> return "Mã đã được sử dụng. Không giao thêm suất; liên hệ điều phối viên nếu cần."
            registration.status != ALLOCATED -> return "Đăng ký chưa được sắp xếp hoặc đã hủy. Liên hệ điều phối viên."
        }
        val campaign = campaign(campaignId)
        if (campaign.day != 0 || campaign.archived) return "Đợt chưa đến ngày phát hoặc đã kết thúc."
        if (received(campaign) >= campaign.prepared) return "Số suất đã trao đã bằng số thực chuẩn bị. Cần điều phối viên kiểm tra."
        return null
    }

    fun findCode(code: String): Registration? {
        val value = code.trim()
        return registrations.firstOrNull { it.code.equals(value, ignoreCase = true) }
    }

    fun updateSupply(campaign: Campaign, confirmed: Int, prepared: Int, note: String): String? {
        val receivedCount = received(campaign)
        val assignedCount = assigned(campaign.id)
        val minimum = maxOf(1, assignedCount, receivedCount)
        if (confirmed !in minimum..1000) {
            return "Số xác nhận phải từ $minimum đến 1.000 suất"
        }
        if (prepared !in receivedCount..confirmed) {
            return "Số thực chuẩn bị phải từ $receivedCount đến $confirmed suất"
        }
        campaign.confirmed = confirmed
        campaign.prepared = prepared
        campaign.note = note
        save()
        return null
    }

    fun assignVolunteer(campaignId: String, volunteerId: String): Boolean {
        val camp = campaign(campaignId)
        if (!camp.volunteers.contains(volunteerId)) {
            camp.volunteers.add(volunteerId)
            save()
            return true
        }
        return false
    }
}

/** Data used to render the XML screens. A connected snapshot lives only in memory. */
open class ScreenData {
    val campaigns = mutableListOf<DemoStore.Campaign>()
    val registrations = mutableListOf<DemoStore.Registration>()
    val receivedCounts = mutableMapOf<String, Int>()
    val handoverTimes = mutableMapOf<String, List<String>>()
    var profileName = ""
    var profilePhone = ""
    var donorPledged = 0
    var donorCampaign = "c2"
    var donorCadence = "Theo đợt"

    fun campaign(id: String): DemoStore.Campaign = campaigns.firstOrNull { it.id == id } ?: campaigns.first()
    fun registration(id: String): DemoStore.Registration? = registrations.firstOrNull { it.id == id }
    fun inCampaign(id: String): List<DemoStore.Registration> = registrations.filter { it.campaignId == id }
    fun mine(id: String): DemoStore.Registration? =
        registrations.firstOrNull { it.mine && it.campaignId == id && it.status != DemoStore.CANCELLED && it.status != DemoStore.REJECTED }

    fun count(id: String, status: String): Int = inCampaign(id).count { it.status == status }
    fun assigned(id: String): Int = count(id, DemoStore.ALLOCATED) + count(id, DemoStore.RECEIVED)
    fun received(campaign: DemoStore.Campaign): Int =
        if (campaign.archived) campaign.archivedReceived else receivedCounts[campaign.id] ?: count(campaign.id, DemoStore.RECEIVED)

}
