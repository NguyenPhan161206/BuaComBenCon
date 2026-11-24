package vn.edu.buacombencon.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import vn.edu.buacombencon.data.remote.dto.CampaignDto
import vn.edu.buacombencon.data.remote.dto.CommitmentDto
import vn.edu.buacombencon.data.remote.dto.RegistrationDto
import vn.edu.buacombencon.data.remote.dto.RpcHandoverResponse
import vn.edu.buacombencon.data.remote.dto.RpcSubmitRegistrationResponse
import vn.edu.buacombencon.data.remote.dto.TicketDto
import vn.edu.buacombencon.domain.model.Campaign
import vn.edu.buacombencon.domain.model.Registration
import vn.edu.buacombencon.domain.model.UserRole
import vn.edu.buacombencon.domain.repository.AuthRepository
import vn.edu.buacombencon.domain.repository.CampaignRepository
import vn.edu.buacombencon.domain.repository.DonorRepository
import vn.edu.buacombencon.domain.repository.RegistrationRepository

class SupabaseCampaignRepository(private val client: SupabaseClient = SupabaseConfig.client) : CampaignRepository {
    override fun getCampaigns(): List<Campaign> = runBlocking {
        client.from("campaigns")
            .select()
            .decodeList<CampaignDto>()
            .map { it.toDomain() }
    }

    override fun getCampaign(id: String): Campaign? = runBlocking {
        client.from("campaigns")
            .select { filter { eq("id", id) } }
            .decodeSingleOrNull<CampaignDto>()
            ?.toDomain()
    }

    override fun createCampaign(campaign: Campaign) {
        runBlocking {
            val dto = CampaignDto(
                id = campaign.id,
                title = campaign.title,
                meal = campaign.meal,
                eventDate = campaign.date(),
                pickupTime = campaign.time,
                location = campaign.location,
                confirmedSupply = campaign.confirmed,
                pledgedSupply = campaign.pledged,
                preparedSupply = campaign.prepared,
                deadline = campaign.deadline,
                rules = campaign.rules,
                note = campaign.note,
                isRegistrationOpen = campaign.registrationOpen,
                isArchived = campaign.archived,
                archivedReceived = campaign.archivedReceived
            )
            client.from("campaigns").insert(dto)
        }
    }

    override fun updateSupply(campaignId: String, confirmed: Int, prepared: Int, note: String) {
        runBlocking {
            client.from("campaigns").update(
                {
                    set("confirmed_supply", confirmed)
                    set("prepared_supply", prepared)
                    set("note", note)
                }
            ) {
                filter { eq("id", campaignId) }
            }
        }
    }

    override fun toggleRegistration(campaignId: String, open: Boolean) {
        runBlocking {
            client.from("campaigns").update(
                { set("is_registration_open", open) }
            ) {
                filter { eq("id", campaignId) }
            }
        }
    }
}

class SupabaseRegistrationRepository(private val client: SupabaseClient = SupabaseConfig.client) : RegistrationRepository {
    private val currentUserId: String? get() = client.auth.currentSessionOrNull()?.user?.id

    override fun getRegistrations(): List<Registration> = runBlocking {
        client.from("registrations")
            .select()
            .decodeList<RegistrationDto>()
            .map { it.toDomain(currentUserId) }
    }

    override fun getRegistration(id: String): Registration? = runBlocking {
        client.from("registrations")
            .select { filter { eq("id", id) } }
            .decodeSingleOrNull<RegistrationDto>()
            ?.toDomain(currentUserId)
    }

    override fun getMyRegistrations(): List<Registration> = runBlocking {
        val uid = currentUserId ?: return@runBlocking emptyList()
        client.from("registrations")
            .select { filter { eq("user_id", uid) } }
            .decodeList<RegistrationDto>()
            .map { it.toDomain(uid) }
    }

    override fun getCampaignRegistrations(campaignId: String): List<Registration> = runBlocking {
        client.from("registrations")
            .select { filter { eq("campaign_id", campaignId) } }
            .decodeList<RegistrationDto>()
            .map { it.toDomain(currentUserId) }
    }

    override fun register(
        campaignId: String,
        name: String,
        phone: String,
        note: String,
        mine: Boolean,
        assisted: Boolean
    ): Registration = runBlocking {
        val params = buildJsonObject {
            put("p_campaign_id", campaignId)
            put("p_recipient_name", name)
            put("p_phone", phone)
            put("p_note", note)
            put("p_is_assisted", assisted)
        }
        val res = client.postgrest.rpc("submit_registration", params).decodeAs<RpcSubmitRegistrationResponse>()
        Registration(
            id = res.id,
            campaignId = res.campaignId,
            name = name,
            phone = phone,
            mine = mine,
            assisted = assisted,
            status = res.status,
            note = note
        )
    }

    override fun cancelRegistration(id: String) {
        runBlocking {
            val params = buildJsonObject { put("p_registration_id", id) }
            client.postgrest.rpc("cancel_registration", params)
        }
    }

    override fun allocate(registrationId: String): String = runBlocking {
        val reg = getRegistration(registrationId) ?: return@runBlocking "Không tìm thấy hồ sơ."
        val params = buildJsonObject { put("p_campaign_id", reg.campaignId) }
        client.postgrest.rpc("allocate_campaign_meals", params)
        "Đã xử lý sắp xếp suất qua hệ thống Supabase."
    }

    override fun findByCode(code: String): Registration? = runBlocking {
        val ticket = client.from("tickets")
            .select { filter { eq("ticket_code", code.trim()) } }
            .decodeSingleOrNull<TicketDto>() ?: return@runBlocking null
        getRegistration(ticket.registrationId)
    }

    override fun checkClaimError(registration: Registration?, campaignId: String): String? = runBlocking {
        if (registration == null) return@runBlocking "Không tìm thấy mã. Vui lòng kiểm tra hoặc nhờ điều phối viên hỗ trợ."
        if (registration.campaignId != campaignId) return@runBlocking "Mã không thuộc đợt đang chọn."
        val params = buildJsonObject {
            put("p_ticket_code", registration.code)
            put("p_campaign_id", campaignId)
        }
        try {
            val res = client.postgrest.rpc("verify_ticket", params)
            null
        } catch (e: Exception) {
            e.message ?: "Mã không hợp lệ."
        }
    }

    override fun confirmHandover(registrationId: String): Boolean = runBlocking {
        val reg = getRegistration(registrationId) ?: return@runBlocking false
        val params = buildJsonObject {
            put("p_ticket_code", reg.code)
            put("p_campaign_id", reg.campaignId)
            put("p_verification_method", "QR_SCAN")
        }
        try {
            val res = client.postgrest.rpc("confirm_handover", params).decodeAs<RpcHandoverResponse>()
            res.success
        } catch (_: Exception) {
            false
        }
    }

    override fun countAssigned(campaignId: String): Int = runBlocking {
        val list = client.from("registrations")
            .select(Columns.list("id", "status")) { filter { eq("campaign_id", campaignId) } }
            .decodeList<RegistrationDto>()
        list.count { it.status == "ALLOCATED" || it.status == "RECEIVED" }
    }

    override fun countReceived(campaign: Campaign): Int = runBlocking {
        if (campaign.archived) return@runBlocking campaign.archivedReceived
        val list = client.from("registrations")
            .select(Columns.list("id", "status")) { filter { eq("campaign_id", campaign.id) } }
            .decodeList<RegistrationDto>()
        list.count { it.status == "RECEIVED" }
    }

    override fun countStatus(campaignId: String, status: String): Int = runBlocking {
        val list = client.from("registrations")
            .select(Columns.list("id", "status")) { filter { eq("campaign_id", campaignId) } }
            .decodeList<RegistrationDto>()
        list.count { it.status == status }
    }
}

class SupabaseDonorRepository(private val client: SupabaseClient = SupabaseConfig.client) : DonorRepository {
    private var cachedPledge = 0
    private var cachedCampaign = "c2"
    private var cachedCadence = "Theo đợt"

    override fun getPledgedAmount(): Int = cachedPledge
    override fun getDonorCampaignId(): String = cachedCampaign
    override fun getDonorCadence(): String = cachedCadence

    override fun savePledge(campaignId: String, quantity: Int, cadence: String) {
        cachedPledge = quantity
        cachedCampaign = campaignId
        cachedCadence = cadence
        runBlocking {
            val dto = CommitmentDto(
                campaignId = campaignId,
                donorId = client.auth.currentSessionOrNull()?.user?.id,
                donorName = "Nhà hảo tâm (mẫu)",
                donorPhone = "0900000003",
                quantity = quantity,
                cadence = cadence,
                consent = true,
                status = "PENDING"
            )
            client.from("commitments").insert(dto)
        }
    }
}

class SupabaseAuthRepository(
    private val client: SupabaseClient = SupabaseConfig.client,
    private var currentRole: UserRole = UserRole.CAREGIVER
) : AuthRepository {
    override fun getCurrentRole(): UserRole = currentRole

    override fun setRole(role: UserRole) {
        currentRole = role
    }

    override fun isConnected(): Boolean = true

    override fun setConnected(connected: Boolean) {
        // Controlled by network manager
    }

    override fun resetDemoData() {
        // Supabase database reset requires db admin
    }
}
