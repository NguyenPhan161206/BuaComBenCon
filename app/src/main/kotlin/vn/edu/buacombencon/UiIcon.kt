package vn.edu.buacombencon

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View

/** Small line icons drawn independently of the device font. */
class UiIcon(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    private var kind: String
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
        strokeWidth = 1.7f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    init {
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        val values = context.obtainStyledAttributes(attrs, R.styleable.UiIcon)
        try {
            kind = values.getString(R.styleable.UiIcon_iconKind) ?: "home"
            paint.color = values.getColor(R.styleable.UiIcon_iconColor, context.getColor(R.color.green))
        } finally {
            values.recycle()
        }
    }

    fun setIcon(value: String, color: Int) {
        kind = value
        paint.color = color
        invalidate()
    }

    private fun line(canvas: Canvas, vararg points: Int) {
        val path = Path()
        path.moveTo(points[0].toFloat(), points[1].toFloat())
        for (index in 2 until points.size step 2) {
            path.lineTo(points[index].toFloat(), points[index + 1].toFloat())
        }
        canvas.drawPath(path, paint)
    }

    override fun onDraw(canvas: Canvas) {
        canvas.save()
        canvas.scale(width / 24f, height / 24f)
        when (kind) {
            "home" -> {
                line(canvas, 3, 10, 12, 3, 21, 10)
                line(canvas, 5, 9, 5, 21, 10, 21, 10, 14, 14, 14, 14, 21, 19, 21, 19, 9)
            }
            "ticket" -> {
                canvas.drawRoundRect(3f, 5f, 21f, 19f, 3f, 3f, paint)
                line(canvas, 8, 5, 8, 8)
                line(canvas, 8, 11, 8, 13)
                line(canvas, 8, 16, 8, 19)
                line(canvas, 12, 10, 17, 10)
                line(canvas, 12, 14, 16, 14)
            }
            "heart" -> {
                val heart = Path()
                heart.moveTo(12f, 21f)
                heart.cubicTo(-4f, 10f, 4f, -1f, 12f, 7f)
                heart.cubicTo(20f, -1f, 28f, 10f, 12f, 21f)
                canvas.drawPath(heart, paint)
            }
            "chart" -> {
                line(canvas, 4, 3, 4, 21, 22, 21)
                line(canvas, 9, 17, 9, 11)
                line(canvas, 14, 17, 14, 6)
                line(canvas, 19, 17, 19, 9)
            }
            "user" -> {
                canvas.drawCircle(12f, 7f, 4f, paint)
                canvas.drawArc(4f, 14f, 20f, 26f, 180f, 180f, false, paint)
            }
            "pin" -> {
                canvas.drawCircle(12f, 9f, 2.4f, paint)
                val pin = Path()
                pin.moveTo(12f, 22f)
                pin.cubicTo(-2f, 8f, 6f, 2f, 12f, 2f)
                pin.cubicTo(18f, 2f, 26f, 8f, 12f, 22f)
                canvas.drawPath(pin, paint)
            }
            "clock" -> {
                canvas.drawCircle(12f, 12f, 9f, paint)
                line(canvas, 12, 6, 12, 12, 16, 14)
            }
            "check" -> {
                canvas.drawCircle(12f, 12f, 9f, paint)
                line(canvas, 7, 12, 10, 15, 17, 8)
            }
            "scan" -> {
                line(canvas, 3, 8, 3, 3, 8, 3)
                line(canvas, 16, 3, 21, 3, 21, 8)
                line(canvas, 3, 16, 3, 21, 8, 21)
                line(canvas, 16, 21, 21, 21, 21, 16)
                line(canvas, 5, 12, 19, 12)
            }
            else -> {
                canvas.drawRoundRect(4f, 5f, 20f, 21f, 3f, 3f, paint)
                line(canvas, 4, 10, 20, 10)
                line(canvas, 8, 2, 8, 7)
                line(canvas, 16, 2, 16, 7)
            }
        }
        canvas.restore()
    }
}
