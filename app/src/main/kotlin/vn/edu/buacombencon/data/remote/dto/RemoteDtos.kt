package vn.edu.buacombencon.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import vn.edu.buacombencon.domain.model.Campaign
import vn.edu.buacombencon.domain.model.Commitment
import vn.edu.buacombencon.domain.model.Registration
import vn.edu.buacombencon.domain.model.UserProfile
import vn.edu.buacombencon.domain.model.UserRole

@Serializable
data class CampaignDto(
    val id: String,
    val title: String,
    val meal: String,
    @SerialName("event_date") val eventDate: String,
    @SerialName("pickup_time") val pickupTime: String,
    val location: String,
    @SerialName("confirmed_supply") val confirmedSupply: Int = 0,
    @SerialName("pledged_supply") val pledgedSupply: Int = 0,
    @SerialName("prepared_supply") val preparedSupply: Int = 0,
    val deadline: String = "",
    val rules: String = "",
    val note: String = "",
    @SerialName("is_registration_open") val isRegistrationOpen: Boolean = true,
    @SerialName("is_archived") val isArchived: Boolean = false,
    @SerialName("archived_received") val archivedReceived: Int = 0
) {
    fun toDomain(): Campaign = Campaign(
        id = id,
        title = title,
        meal = meal,
        day = 0, // Computed from eventDate at runtime
        confirmed = confirmedSupply,
        time = pickupTime,
        pledged = pledgedSupply,
        prepared = preparedSupply,
        registrationOpen = isRegistrationOpen,
        location = location,
        deadline = deadline,
        rules = rules,
        note = note,
        archived = isArchived,
        archivedReceived = archivedReceived
    )
}

@Serializable
data class RegistrationDto(
    val id: String,
    @SerialName("campaign_id") val campaignId: String,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("recipient_name") val recipientName: String,
    val phone: String,
    @SerialName("is_assisted") val isAssisted: Boolean = false,
    val status: String,
    @SerialName("queue_number") val queueNumber: Int = 0,
    @SerialName("completed_info_at") val completedInfoAt: String = "",
    @SerialName("reviewed_at") val reviewedAt: String? = null,
    val note: String = ""
) {
    fun toDomain(currentUserId: String? = null): Registration = Registration(
        id = id,
        campaignId = campaignId,
        name = recipientName,
        phone = phone,
        mine = (userId != null && userId == currentUserId),
        assisted = isAssisted,
        status = status,
        note = note
    )
}

@Serializable
data class TicketDto(
    val id: String = "",
    @SerialName("registration_id") val registrationId: String,
    @SerialName("ticket_code") val ticketCode: String,
    @SerialName("qr_payload") val qrPayload: String,
    @SerialName("is_used") val isUsed: Boolean = false,
    @SerialName("is_revoked") val isRevoked: Boolean = false
)

@Serializable
data class HandoverDto(
    val id: String = "",
    @SerialName("registration_id") val registrationId: String,
    @SerialName("campaign_id") val campaignId: String,
    @SerialName("volunteer_id") val volunteerId: String? = null,
    @SerialName("handover_time") val handoverTime: String = "",
    @SerialName("verification_method") val verificationMethod: String = "QR_SCAN"
)

@Serializable
data class CommitmentDto(
    val id: String = "",
    @SerialName("campaign_id") val campaignId: String,
    @SerialName("donor_id") val donorId: String? = null,
    @SerialName("donor_name") val donorName: String,
    @SerialName("donor_phone") val donorPhone: String,
    val quantity: Int,
    val cadence: String = "Theo đợt",
    val consent: Boolean = true,
    val status: String = "PENDING"
) {
    fun toDomain(): Commitment = Commitment(
        id = id,
        campaignId = campaignId,
        donorName = donorName,
        donorPhone = donorPhone,
        quantity = quantity,
        cadence = cadence,
        consent = consent,
        status = status
    )
}

@Serializable
data class ProfileDto(
    val id: String,
    @SerialName("full_name") val fullName: String,
    val phone: String,
    val role: String
) {
    fun toDomain(email: String = ""): UserProfile = UserProfile(
        id = id,
        email = email,
        fullName = fullName,
        phone = phone,
        role = when (role.lowercase()) {
            "coordinator" -> UserRole.COORDINATOR
            "volunteer" -> UserRole.VOLUNTEER
            "donor" -> UserRole.DONOR
            else -> UserRole.CAREGIVER
        }
    )
}

@Serializable
data class RpcSubmitRegistrationResponse(
    val id: String,
    @SerialName("campaign_id") val campaignId: String,
    val status: String,
    @SerialName("queue_number") val queueNumber: Int = 0
)

@Serializable
data class RpcHandoverResponse(
    val success: Boolean,
    @SerialName("registration_id") val registrationId: String,
    @SerialName("recipient_name") val recipientName: String,
    @SerialName("ticket_code") val ticketCode: String
)
