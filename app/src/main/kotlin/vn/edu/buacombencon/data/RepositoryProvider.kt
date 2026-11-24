package vn.edu.buacombencon.data

import android.content.Context
import vn.edu.buacombencon.DemoStore
import vn.edu.buacombencon.data.demo.DemoAuthRepository
import vn.edu.buacombencon.data.demo.DemoCampaignRepository
import vn.edu.buacombencon.data.demo.DemoDonorRepository
import vn.edu.buacombencon.data.demo.DemoRegistrationRepository
import vn.edu.buacombencon.data.remote.SupabaseAuthRepository
import vn.edu.buacombencon.data.remote.SupabaseCampaignRepository
import vn.edu.buacombencon.data.remote.SupabaseDonorRepository
import vn.edu.buacombencon.data.remote.SupabaseRegistrationRepository
import vn.edu.buacombencon.domain.repository.AuthRepository
import vn.edu.buacombencon.domain.repository.CampaignRepository
import vn.edu.buacombencon.domain.repository.DonorRepository
import vn.edu.buacombencon.domain.repository.RegistrationRepository

enum class DataSourceMode {
    DEMO,
    SUPABASE
}

class RepositoryProvider(context: Context, val demoStore: DemoStore) {
    var mode: DataSourceMode = DataSourceMode.DEMO

    private val demoCampaignRepo = DemoCampaignRepository(demoStore)
    private val demoRegistrationRepo = DemoRegistrationRepository(demoStore)
    private val demoDonorRepo = DemoDonorRepository(demoStore)
    private val demoAuthRepo = DemoAuthRepository(demoStore)

    private val supabaseCampaignRepo by lazy { SupabaseCampaignRepository() }
    private val supabaseRegistrationRepo by lazy { SupabaseRegistrationRepository() }
    private val supabaseDonorRepo by lazy { SupabaseDonorRepository() }
    private val supabaseAuthRepo by lazy { SupabaseAuthRepository() }

    val campaignRepository: CampaignRepository
        get() = if (mode == DataSourceMode.SUPABASE) supabaseCampaignRepo else demoCampaignRepo

    val registrationRepository: RegistrationRepository
        get() = if (mode == DataSourceMode.SUPABASE) supabaseRegistrationRepo else demoRegistrationRepo

    val donorRepository: DonorRepository
        get() = if (mode == DataSourceMode.SUPABASE) supabaseDonorRepo else demoDonorRepo

    val authRepository: AuthRepository
        get() = if (mode == DataSourceMode.SUPABASE) supabaseAuthRepo else demoAuthRepo
}
