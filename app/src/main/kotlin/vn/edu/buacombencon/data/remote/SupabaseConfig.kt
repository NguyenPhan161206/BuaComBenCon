package vn.edu.buacombencon.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime

object SupabaseConfig {
    // Local Supabase default endpoints
    const val DEFAULT_URL = "http://10.0.2.2:54321" // 10.0.2.2 maps to host localhost in Android Emulator
    const val DEFAULT_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZS1kZW1vIiwicm9sZSI6ImFub24iLCJleHAiOjE5ODM0NDEyMDAsImlhdCI6MTY2Nzg0MTIwMH0.eyJuYW1lIjoiQnVhdENvbUJlbkNvbiJ9"

    var currentUrl: String = DEFAULT_URL
    var currentAnonKey: String = DEFAULT_ANON_KEY

    val client: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = currentUrl,
            supabaseKey = currentAnonKey
        ) {
            install(Auth)
            install(Postgrest)
            install(Realtime)
        }
    }
}
