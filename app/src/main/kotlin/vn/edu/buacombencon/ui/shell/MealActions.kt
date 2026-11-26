package vn.edu.buacombencon.ui.shell

import android.app.Activity
import android.app.AlertDialog
import org.json.JSONObject
import vn.edu.buacombencon.DemoStore
import vn.edu.buacombencon.ScreenData
import vn.edu.buacombencon.data.RepositoryProvider
import vn.edu.buacombencon.data.remote.ApiError
import vn.edu.buacombencon.data.remote.SupabaseRepository
import java.io.IOException
import java.util.concurrent.Executors

internal class MealActions(
    private val activity: Activity,
    private val demo: DemoStore,
    private val update: (ScreenData) -> Unit
) {
    private val worker = Executors.newSingleThreadExecutor()
    private var progress: AlertDialog? = null
    val connected: Boolean get() = RepositoryProvider.remote != null

    fun close() {
        progress?.dismiss()
        worker.shutdown()
    }

    fun <T> run(
        task: () -> T,
        onError: (Exception) -> Unit = ::showError,
        onSuccess: (T) -> Unit
    ) {
        if (progress != null) return
        progress = AlertDialog.Builder(activity).setMessage("Đang kết nối với Supabase…")
            .setCancelable(false).show()
        worker.execute {
            val result = runCatching(task)
            activity.runOnUiThread {
                if (activity.isDestroyed || activity.isFinishing) return@runOnUiThread
                progress?.dismiss()
                progress = null
                result.fold(onSuccess) { failure ->
                    onError(failure as? Exception ?: throw failure)
                }
            }
        }
    }

    fun action(
        name: String,
        vararg arguments: Pair<String, Any?>,
        onError: (Exception) -> Unit = ::showError,
        onSuccess: (JSONObject) -> Unit
    ) {
        val remote = checkNotNull(RepositoryProvider.remote)
        run({
            val result = remote.rpc(name, *arguments)
            result to runCatching { remote.snapshot() }
        }, onError) { (result, snapshot) ->
            snapshot.onSuccess(update).onFailure {
                UiUtils.info(activity, "Đã lưu trên Supabase", "Chưa tải được dữ liệu mới. Hãy dùng nút Làm mới.")
            }
            onSuccess(result)
        }
    }

    fun register(
        campaignId: String, name: String, phone: String, note: String, assisted: Boolean,
        complete: Boolean = true,
        onRegistered: (String) -> Unit
    ) {
        if (connected) {
            action("submit_registration", "p_campaign_id" to campaignId, "p_recipient_name" to name,
                "p_phone" to phone, "p_note" to note, "p_is_assisted" to assisted,
                "p_consent" to true, "p_is_complete" to complete) {
                onRegistered(it.getString("id"))
            }
        } else {
            try {
                onRegistered(demo.register(campaignId, name, phone, note, !assisted, assisted).id)
            } catch (error: IllegalStateException) {
                showError(error)
            }
        }
    }

    fun updateSupply(campaign: DemoStore.Campaign, confirmed: Int, prepared: Int, note: String, onDone: () -> Unit) {
        if (connected) {
            action("update_campaign_supply", "p_campaign_id" to campaign.id,
                "p_confirmed" to confirmed, "p_prepared" to prepared, "p_note" to note) { onDone() }
        } else {
            val error = demo.updateSupply(campaign, confirmed, prepared, note)
            if (error != null) {
                showError(IllegalStateException(error))
                return
            }
            onDone()
        }
    }

    fun showError(error: Exception) {
        val message = when (error) {
            is ApiError, is IllegalArgumentException, is IllegalStateException -> error.message.orEmpty()
            is IOException -> "Không nhận được phản hồi từ Supabase. Làm mới dữ liệu trước khi gửi lại."
            else -> "Không tải được dữ liệu Supabase. Kiểm tra cấu hình và thử lại."
        }
        UiUtils.info(activity, "Chưa thể hoàn tất", message)
    }
}
