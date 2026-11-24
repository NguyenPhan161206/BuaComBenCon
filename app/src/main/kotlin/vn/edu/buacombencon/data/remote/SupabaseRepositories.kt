package vn.edu.buacombencon.data.remote

import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import vn.edu.buacombencon.DemoStore
import vn.edu.buacombencon.ScreenData
import vn.edu.buacombencon.domain.model.UserRole
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

internal class SupabaseRepository(baseUrl: String, private val publishableKey: String) {
    private val base = URL(baseUrl.trim().trimEnd('/'))
    private val zone = ZoneId.of("Asia/Ho_Chi_Minh")
    private var accessToken: String? = null
    private var refreshToken: String? = null
    private var expiresAt = 0L
    private var serverTime = Instant.EPOCH
    private lateinit var userId: String
    private lateinit var profileName: String
    private lateinit var profilePhone: String
    lateinit var role: UserRole
        private set

    init {
        require(base.userInfo == null && base.query == null && base.ref == null &&
            (base.protocol == "https" || (base.protocol == "http" && base.host in setOf("10.0.2.2", "127.0.0.1", "localhost")))) {
            "Dùng HTTPS hoặc địa chỉ Supabase local."
        }
        require(publishableKey.isNotBlank()) { "Nhập publishable key hoặc anon key." }
    }

    @Synchronized
    fun signIn(email: String, password: String) {
        val session = JSONObject(request("/auth/v1/token?grant_type=password",
            payload("email" to email, "password" to password), authenticated = false))
        applySession(session)
        userId = session.getJSONObject("user").getString("id")
        val profile = JSONArray(request("/rest/v1/profiles?select=role,full_name,phone&id=eq.${Uri.encode(userId)}", authenticated = true))
            .optJSONObject(0) ?: throw ApiError("Tài khoản chưa được cấp vai trò.")
        profileName = profile.getString("full_name")
        profilePhone = profile.getString("phone")
        role = when (profile.getString("role")) {
            "caregiver" -> UserRole.CAREGIVER
            "coordinator" -> UserRole.COORDINATOR
            "volunteer" -> UserRole.VOLUNTEER
            "donor" -> UserRole.DONOR
            else -> throw ApiError("Vai trò tài khoản không hợp lệ.")
        }
    }

    fun requestPasswordReset(email: String) {
        request("/auth/v1/recover", payload("email" to email), authenticated = false)
    }

    fun signUp(email: String, password: String) {
        request("/auth/v1/signup", payload("email" to email, "password" to password), authenticated = false)
    }

    fun confirmSignup(email: String, code: String) {
        request("/auth/v1/verify", payload("type" to "signup", "email" to email, "token" to code),
            authenticated = false)
    }

    fun resetPassword(email: String, code: String, newPassword: String) {
        val verified = request("/auth/v1/verify",
            payload("type" to "recovery", "email" to email, "token" to code), authenticated = false)
        applySession(JSONObject(verified))
        request("/auth/v1/user", payload("password" to newPassword), authenticated = true, method = "PUT")
        accessToken = null
        refreshToken = null
    }

    @Synchronized
    fun snapshot(): ScreenData {
        val campaigns = rows("/rest/v1/campaigns?select=*&order=event_date.desc,id").map { row ->
            val date = LocalDate.parse(row.getString("event_date"))
            val day = ChronoUnit.DAYS.between(LocalDate.now(zone), date).toInt()
            DemoStore.Campaign(row.getString("id"), row.getString("title"), row.getString("meal"), day,
                row.getInt("confirmed_supply"), row.getString("pickup_time")).apply {
                fixedDate = date
                pledged = row.getInt("pledged_supply")
                prepared = row.getInt("prepared_supply")
                location = row.getString("location")
                deadline = row.getString("deadline")
                rules = row.getString("rules")
                note = row.getString("note")
                archived = row.getBoolean("is_archived")
                archivedReceived = row.getInt("archived_received")
                registrationOpen = !archived && row.getBoolean("is_registration_open") &&
                    serverTime.isBefore(Instant.parse(row.getString("registration_closes_at")))
                allocated = !row.isNull("allocated_at")
            }
        }
        val summaries = if (role == UserRole.VOLUNTEER) campaigns.filterNot { it.archived }.associate { campaign ->
            campaign.id to rpc("campaign_summary", "p_campaign_id" to campaign.id)
        } else emptyMap()
        val registrations = if (role == UserRole.VOLUNTEER || role == UserRole.DONOR) emptyList() else {
            val tickets = rows("/rest/v1/tickets?select=registration_id,ticket_code,is_used,is_revoked&order=registration_id")
                .associateBy { it.getString("registration_id") }
            rows("/rest/v1/registrations?select=*&order=completed_info_at.asc.nullslast,queue_number.asc")
                .map { row ->
                    DemoStore.Registration(row.getString("id"), row.getString("campaign_id"),
                        row.getString("recipient_name"), row.getString("phone"), row.optString("user_id") == userId,
                        row.getBoolean("is_assisted"), row.getString("status")).apply {
                        note = row.getString("note")
                        reviewed = !row.isNull("is_approved")
                        complete = !row.isNull("completed_info_at")
                        val ticket = tickets[id]
                        code = ticket?.takeUnless { it.getBoolean("is_used") || it.getBoolean("is_revoked") }
                            ?.getString("ticket_code").orEmpty()
                    }
                }
        }
        val commitments = if (role == UserRole.DONOR)
            JSONArray(request("/rest/v1/commitments?select=campaign_id,quantity,cadence&order=created_at.desc,id&limit=1",
                authenticated = true)).optJSONObject(0) else null
        return ScreenData().apply {
            this.campaigns.addAll(campaigns)
            this.registrations.addAll(registrations)
            this.profileName = this@SupabaseRepository.profileName
            this.profilePhone = this@SupabaseRepository.profilePhone
            for ((campaignId, summary) in summaries) {
                receivedCounts[campaignId] = summary.getInt("received")
                val times = summary.getJSONArray("recent_handovers")
                val format = DateTimeFormatter.ofPattern("HH:mm · dd/MM/yyyy").withZone(zone)
                handoverTimes[campaignId] = List(times.length()) { index ->
                    format.format(Instant.parse(times.getString(index)))
                }
            }
            commitments?.let { row ->
                donorPledged = row.getInt("quantity")
                donorCampaign = row.getString("campaign_id")
                donorCadence = row.getString("cadence")
            }
        }
    }

    @Synchronized
    fun rpc(name: String, vararg pairs: Pair<String, Any?>): JSONObject {
        val result = request("/rest/v1/rpc/$name", payload(*pairs), authenticated = true)
        return if (result.isBlank() || result == "null") JSONObject() else JSONObject(result)
    }

    private fun rows(path: String): List<JSONObject> {
        val pageSize = 500
        val result = mutableListOf<JSONObject>()
        val join = if ('?' in path) '&' else '?'
        do {
            val page = JSONArray(request("$path${join}limit=$pageSize&offset=${result.size}", authenticated = true))
            for (index in 0 until page.length()) result.add(page.getJSONObject(index))
        } while (page.length() == pageSize)
        return result
    }

    private fun applySession(session: JSONObject) {
        accessToken = session.getString("access_token")
        refreshToken = session.getString("refresh_token")
        expiresAt = System.currentTimeMillis() + session.getLong("expires_in") * 1000
    }

    private fun bearer(): String {
        if (expiresAt - System.currentTimeMillis() < 30_000) {
            val token = refreshToken ?: throw ApiError("Phiên đăng nhập đã hết hạn.")
            applySession(JSONObject(request("/auth/v1/token?grant_type=refresh_token",
                payload("refresh_token" to token), authenticated = false)))
        }
        return accessToken ?: throw ApiError("Phiên đăng nhập đã hết hạn.")
    }

    private fun request(
        path: String, body: JSONObject? = null, authenticated: Boolean,
        method: String = if (body == null) "GET" else "POST"
    ): String {
        val token = if (authenticated) bearer() else null
        val connection = URL(base.toExternalForm() + path).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.connectTimeout = 6_000
            connection.readTimeout = 10_000
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("apikey", publishableKey)
            if (token != null) connection.setRequestProperty("Authorization", "Bearer $token")
            connection.setRequestProperty("Accept", "application/json")
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            }
            val status = connection.responseCode
            val date = connection.getHeaderFieldDate("Date", 0L)
            if (date > 0L) serverTime = Instant.ofEpochMilli(date)
            if (status in 200..299) return connection.inputStream?.bufferedReader()?.use { it.readText() }.orEmpty()
            val message = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
            val error = runCatching { JSONObject(message) }.getOrNull()
            throw ApiError(error?.optString("message")?.takeIf { it.isNotBlank() }
                ?: error?.optString("msg")?.takeIf { it.isNotBlank() }
                ?: "Supabase trả về HTTP $status.")
        } finally {
            connection.disconnect()
        }
    }

    private fun payload(vararg pairs: Pair<String, Any?>) =
        JSONObject().apply { for ((name, value) in pairs) put(name, value ?: JSONObject.NULL) }
}

internal class ApiError(message: String) : IllegalStateException(message)
