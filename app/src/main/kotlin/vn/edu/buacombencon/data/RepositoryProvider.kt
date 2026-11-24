package vn.edu.buacombencon.data

import vn.edu.buacombencon.data.remote.SupabaseRepository

/** Holds the signed-in connection across Activity recreation. Tokens stay in process memory. */
internal object RepositoryProvider {
    var remote: SupabaseRepository? = null
}
