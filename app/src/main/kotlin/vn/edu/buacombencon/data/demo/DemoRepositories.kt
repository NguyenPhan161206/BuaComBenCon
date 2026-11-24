package vn.edu.buacombencon.data.demo

import vn.edu.buacombencon.DemoStore
import vn.edu.buacombencon.domain.model.Campaign
import vn.edu.buacombencon.domain.model.Registration
import vn.edu.buacombencon.domain.model.RegistrationStatus
import vn.edu.buacombencon.domain.model.UserRole
import vn.edu.buacombencon.domain.repository.AuthRepository
import vn.edu.buacombencon.domain.repository.CampaignRepository
import vn.edu.buacombencon.domain.repository.DonorRepository
import vn.edu.buacombencon.domain.repository.RegistrationRepository

/**
 * Maps between DemoStore legacy objects and domain models.
 */
object DemoMapper {
    fun toDomain(storeCampaign: DemoStore.Campaign): Campaign {
        val c = Campaign(
            id = storeCampaign.id,
            title = storeCampaign.title,
            meal = storeCampaign.meal,
            day = storeCampaign.day,
            confirmed = storeCampaign.confirmed,
            time = storeCampaign.time,
            pledged = storeCampaign.pledged,
            prepared = storeCampaign.prepared,
            registrationOpen = storeCampaign.registrationOpen,
            location = storeCampaign.location,
            deadline = storeCampaign.deadline,
            rules = storeCampaign.rules,
            note = storeCampaign.note,
            archived = storeCampaign.archived,
            archivedReceived = storeCampaign.archivedReceived
        )
        return c
    }

    fun toStore(domain: Campaign, storeCampaign: DemoStore.Campaign) {
        storeCampaign.confirmed = domain.confirmed
        storeCampaign.pledged = domain.pledged
        storeCampaign.prepared = domain.prepared
        storeCampaign.registrationOpen = domain.registrationOpen
        storeCampaign.location = domain.location
        storeCampaign.deadline = domain.deadline
        storeCampaign.rules = domain.rules
        storeCampaign.note = domain.note
        storeCampaign.archived = domain.archived
        storeCampaign.archivedReceived = domain.archivedReceived
    }

    fun toDomain(storeReg: DemoStore.Registration): Registration {
        return Registration(
            id = storeReg.id,
            campaignId = storeReg.campaignId,
            name = storeReg.name,
            phone = storeReg.phone,
            mine = storeReg.mine,
            assisted = storeReg.assisted,
            status = storeReg.status,
            note = storeReg.note
        )
    }
}

class DemoCampaignRepository(private val store: DemoStore) : CampaignRepository {
    override fun getCampaigns(): List<Campaign> {
        return store.campaigns.map { DemoMapper.toDomain(it) }
    }

    override fun getCampaign(id: String): Campaign? {
        val found = store.campaigns.firstOrNull { it.id == id } ?: return null
        return DemoMapper.toDomain(found)
    }

    override fun createCampaign(campaign: Campaign) {
        val storeCampaign = DemoStore.Campaign(
            campaign.id,
            campaign.title,
            campaign.meal,
            campaign.day,
            campaign.confirmed,
            campaign.time
        ).apply {
            prepared = campaign.prepared
            pledged = campaign.pledged
            registrationOpen = campaign.registrationOpen
            deadline = campaign.deadline
            rules = campaign.rules
            location = campaign.location
            note = campaign.note
            archived = campaign.archived
            archivedReceived = campaign.archivedReceived
        }
        store.campaigns.add(0, storeCampaign)
        store.save()
    }

    override fun updateSupply(campaignId: String, confirmed: Int, prepared: Int, note: String) {
        val found = store.campaign(campaignId)
        found.confirmed = confirmed
        found.prepared = prepared
        found.note = note
        store.save()
    }

    override fun toggleRegistration(campaignId: String, open: Boolean) {
        val found = store.campaign(campaignId)
        found.registrationOpen = open
        store.save()
    }
}

class DemoRegistrationRepository(private val store: DemoStore) : RegistrationRepository {
    override fun getRegistrations(): List<Registration> {
        return store.registrations.map { DemoMapper.toDomain(it) }
    }

    override fun getRegistration(id: String): Registration? {
        val found = store.registration(id) ?: return null
        return DemoMapper.toDomain(found)
    }

    override fun getMyRegistrations(): List<Registration> {
        return store.registrations.filter { it.mine }.map { DemoMapper.toDomain(it) }
    }

    override fun getCampaignRegistrations(campaignId: String): List<Registration> {
        return store.inCampaign(campaignId).map { DemoMapper.toDomain(it) }
    }

    override fun register(
        campaignId: String,
        name: String,
        phone: String,
        note: String,
        mine: Boolean,
        assisted: Boolean
    ): Registration {
        val reg = store.register(campaignId, name, phone, note, mine, assisted)
        return DemoMapper.toDomain(reg)
    }

    override fun cancelRegistration(id: String) {
        val found = store.registration(id) ?: return
        found.status = RegistrationStatus.CANCELLED.code
        store.save()
    }

    override fun allocate(registrationId: String): String {
        val found = store.registration(registrationId) ?: return "Không tìm thấy hồ sơ."
        return store.allocate(found)
    }

    override fun findByCode(code: String): Registration? {
        val found = store.findCode(code) ?: return null
        return DemoMapper.toDomain(found)
    }

    override fun checkClaimError(registration: Registration?, campaignId: String): String? {
        val storeReg = registration?.let { store.registration(it.id) }
        return store.claimError(storeReg, campaignId)
    }

    override fun confirmHandover(registrationId: String): Boolean {
        val found = store.registration(registrationId) ?: return false
        found.status = RegistrationStatus.RECEIVED.code
        store.save()
        return true
    }

    override fun countAssigned(campaignId: String): Int = store.assigned(campaignId)

    override fun countReceived(campaign: Campaign): Int {
        val storeCamp = store.campaign(campaign.id)
        return store.received(storeCamp)
    }

    override fun countStatus(campaignId: String, status: String): Int = store.count(campaignId, status)
}

class DemoDonorRepository(private val store: DemoStore) : DonorRepository {
    override fun getPledgedAmount(): Int = store.donorPledged

    override fun getDonorCampaignId(): String = store.donorCampaign

    override fun getDonorCadence(): String = store.donorCadence

    override fun savePledge(campaignId: String, quantity: Int, cadence: String) {
        if (store.donorPledged > 0) {
            val old = store.campaign(store.donorCampaign)
            old.pledged = maxOf(0, old.pledged - store.donorPledged)
        }
        val camp = store.campaign(campaignId)
        camp.pledged += quantity
        store.donorPledged = quantity
        store.donorCampaign = campaignId
        store.donorCadence = cadence
        store.save()
    }
}

class DemoAuthRepository(
    private val store: DemoStore,
    private var currentRole: UserRole = UserRole.CAREGIVER,
    private var connected: Boolean = true
) : AuthRepository {
    override fun getCurrentRole(): UserRole = currentRole

    override fun setRole(role: UserRole) {
        currentRole = role
    }

    override fun isConnected(): Boolean = connected

    override fun setConnected(connected: Boolean) {
        this.connected = connected
    }

    override fun resetDemoData() {
        store.reset()
    }
}
