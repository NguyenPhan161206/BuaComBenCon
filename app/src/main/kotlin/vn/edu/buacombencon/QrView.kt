package vn.edu.buacombencon

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class QrView(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    private var code: QrCode? = null
    private val paint = Paint().apply { color = Color.BLACK }

    fun setPayload(payload: String) {
        code = QrCode(payload)
        contentDescription = "Mã QR nhận suất. Có thể dùng mã ngắn $payload"
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(Color.WHITE)
        val modules = code?.modules ?: return
        val unit = minOf(width, height) / 29f
        val left = (width - 29 * unit) / 2
        val top = (height - 29 * unit) / 2
        for (y in modules.indices) {
            for (x in modules[y].indices) {
                if (modules[y][x]) {
                    canvas.drawRect(left + (x + 4) * unit, top + (y + 4) * unit,
                        left + (x + 5) * unit, top + (y + 5) * unit, paint)
                }
            }
        }
    }
}
