package vn.edu.buacombencon.domain.repository

import vn.edu.buacombencon.domain.model.Campaign
import vn.edu.buacombencon.domain.model.Registration
import vn.edu.buacombencon.domain.model.UserRole

interface CampaignRepository {
    fun getCampaigns(): List<Campaign>
    fun getCampaign(id: String): Campaign?
    fun createCampaign(campaign: Campaign)
    fun updateSupply(campaignId: String, confirmed: Int, prepared: Int, note: String)
    fun toggleRegistration(campaignId: String, open: Boolean)
}

interface RegistrationRepository {
    fun getRegistrations(): List<Registration>
    fun getRegistration(id: String): Registration?
    fun getMyRegistrations(): List<Registration>
    fun getCampaignRegistrations(campaignId: String): List<Registration>
    fun register(campaignId: String, name: String, phone: String, note: String, mine: Boolean, assisted: Boolean): Registration
    fun cancelRegistration(id: String)
    fun allocate(registrationId: String): String
    fun findByCode(code: String): Registration?
    fun checkClaimError(registration: Registration?, campaignId: String): String?
    fun confirmHandover(registrationId: String): Boolean
    fun countAssigned(campaignId: String): Int
    fun countReceived(campaign: Campaign): Int
    fun countStatus(campaignId: String, status: String): Int
}

interface DonorRepository {
    fun getPledgedAmount(): Int
    fun getDonorCampaignId(): String
    fun getDonorCadence(): String
    fun savePledge(campaignId: String, quantity: Int, cadence: String)
}

interface AuthRepository {
    fun getCurrentRole(): UserRole
    fun setRole(role: UserRole)
    fun isConnected(): Boolean
    fun setConnected(connected: Boolean)
    fun resetDemoData()
}
