import android.content.ContextWrapper
import android.content.SharedPreferences
import java.awt.image.BufferedImage
import java.lang.reflect.Proxy
import java.nio.file.Path
import javax.imageio.ImageIO
import vn.edu.buacombencon.DemoStore
import vn.edu.buacombencon.DemoStore.Companion.ALLOCATED
import vn.edu.buacombencon.DemoStore.Companion.CANCELLED
import vn.edu.buacombencon.DemoStore.Companion.PENDING
import vn.edu.buacombencon.DemoStore.Companion.RECEIVED
import vn.edu.buacombencon.DemoStore.Companion.WAITING
import vn.edu.buacombencon.QrCode

object DomainChecks {
    private val values = mutableMapOf<String, Any?>()
    private var passed = 0

    private fun verify(condition: Boolean, name: String) {
        check(condition) { name }
        println("PASS $name")
        passed++
    }

    fun preferences(): SharedPreferences = prefProxy(SharedPreferences::class.java)

    private fun <T> prefProxy(type: Class<T>): T {
        val proxy = Proxy.newProxyInstance(type.classLoader, arrayOf(type)) { receiver, method, args ->
            when (method.name) {
                "edit" -> prefProxy(SharedPreferences.Editor::class.java)
                "getString" -> values.getOrDefault(args[0] as String, args[1])
                "apply" -> null
                "commit" -> true
                "clear" -> receiver.also { values.clear() }
                "remove" -> receiver.also { values.remove(args[0] as String) }
                else -> if (method.name.startsWith("put")) {
                    receiver.also { values[args[0] as String] = args[1] }
                } else null
            }
        }
        return type.cast(proxy)
    }

    @JvmStatic
    fun main(args: Array<String>) {
        val context = object : ContextWrapper(null) {
            override fun getSharedPreferences(name: String, mode: Int) = preferences()
        }
        val store = DemoStore(context)
        val closed = runCatching { store.register("c1", "Test family", "0900000000", "", false, true) }
        verify(closed.exceptionOrNull() is IllegalStateException, "reject registration when campaign is closed")
        verify(store.assigned("c1") == 30 && store.count("c1", WAITING) == 5,
            "30 allocated or received and 5 waiting in seeded campaign")
        val own = checkNotNull(store.registration("R0013"))
        verify(own.code == "BC0013", "sample short code maps to the intended registration")
        verify(store.claimError(own, "c2")?.contains("không thuộc") == true,
            "reject code belonging to another campaign")
        verify(store.claimError(own, "c1") == null, "allocated current-day code accepted")
        val current = store.campaign("c1")
        val prepared = current.prepared
        current.prepared = store.received(current)
        verify(store.claimError(own, "c1") != null, "reject handover beyond actual prepared supply")
        current.prepared = prepared
        own.status = RECEIVED
        store.save()
        verify(store.claimError(own, "c1")?.contains("đã được sử dụng") == true, "reject repeated handover")
        verify(store.claimError(null, "c1") != null, "reject unknown code")
        val registration = store.register("c2", "Test family", "0900000000", "", true, false)
        verify(registration.status == PENDING, "submission does not grant a meal automatically")
        val duplicate = runCatching { store.register("c2", "Test family", "0900000000", "", true, false) }
        verify(duplicate.exceptionOrNull() is IllegalStateException,
            "reject a second active self registration for same campaign")
        store.allocate(registration)
        verify(registration.status == ALLOCATED, "coordinator allocates using confirmed supply")
        verify(store.claimError(registration, "c2") != null, "reject handover before campaign day")
        registration.status = CANCELLED
        store.save()
        verify(store.claimError(registration, "c2") != null, "cancelled code cannot be redeemed")
        val waiting = checkNotNull(store.registration("R0031"))
        store.allocate(waiting)
        verify(waiting.status == WAITING, "full confirmed supply keeps applicant waiting")
        checkNotNull(store.registration("R0014")).status = CANCELLED
        store.allocate(waiting)
        verify(waiting.status == ALLOCATED && store.assigned("c1") == 30,
            "released allocation can be reassigned without exceeding capacity")
        val reloaded = DemoStore(context)
        verify(reloaded.registration(registration.id)?.status == CANCELLED, "local changes survive store recreation")
        val archived = store.campaign("c0")
        val received = store.received(archived)
        verify(archived.prepared == 30 && received == 28 && archived.prepared - received == 2,
            "historical reconciliation keeps unhanded meals visible")
        val future = store.campaign("c2")
        val confirmed = future.confirmed
        future.pledged += 20
        store.save()
        verify(future.confirmed == confirmed, "pledged meals remain separate from confirmed supply")
        verify(store.updateSupply(future, 50, 45, "Cập nhật thử") == null,
            "valid supply update persists on c2")
        verify(future.confirmed == 50 && future.prepared == 45, "confirmed and prepared updated")
        verify(store.updateSupply(future, 40, 45, "Sai số lượng") != null,
            "reject prepared exceeding confirmed supply")
        verify(store.updateSupply(future, 0, 0, "Số lượng không hợp lệ") != null,
            "reject confirmed count below minimum threshold")
        store.updateSupply(future, confirmed, 0, "")

        val qr = QrCode("BC0013")
        val image = BufferedImage(290, 290, BufferedImage.TYPE_INT_RGB)
        for (y in 0 until image.height) {
            for (x in 0 until image.width) {
                val col = x / 10 - 4
                val row = y / 10 - 4
                val dark = col in 0 until 21 && row in 0 until 21 && qr.modules[row][col]
                image.setRGB(x, y, if (dark) 0 else 0xffffff)
            }
        }
        ImageIO.write(image, "png", Path.of(args[0], "qr-native-encoder.png").toFile())
        println("TOTAL $passed checks passed")
    }
}
