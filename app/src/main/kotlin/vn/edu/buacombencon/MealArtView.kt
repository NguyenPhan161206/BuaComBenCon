package vn.edu.buacombencon

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View

/** Meal illustration used by the home and campaign layouts. */
class MealArtView(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private fun oval(canvas: Canvas, color: String, x: Float, y: Float, width: Float, height: Float) {
        paint.color = Color.parseColor(color)
        paint.style = Paint.Style.FILL
        canvas.drawOval(x, y, x + width, y + height, paint)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val unit = minOf(width, height) / 200f
        canvas.save()
        canvas.translate((width - 200 * unit) / 2, (height - 200 * unit) / 2)
        canvas.scale(unit, unit)
        oval(canvas, "#DAE6CE", 18f, 16f, 180f, 180f)
        oval(canvas, "#CAD7BD", 26f, 43f, 164f, 144f)
        oval(canvas, "#FFFEF5", 20f, 34f, 164f, 144f)
        oval(canvas, "#E9E8D7", 32f, 46f, 140f, 120f)
        oval(canvas, "#F8F5E6", 36f, 50f, 132f, 112f)
        // A mound of rice with individual grains.
        oval(canvas, "#FFFFFF", 83f, 63f, 68f, 80f)
        paint.color = Color.parseColor("#E3E0CD")
        paint.strokeWidth = 2.1f
        paint.strokeCap = Paint.Cap.ROUND
        for (index in 0 until 19) {
            val x = 97f + index * 17 % 43
            val y = 76f + index * 13 % 55
            canvas.drawLine(x, y, x + 3, y + 2, paint)
        }
        oval(canvas, "#C47741", 52f, 93f, 37f, 45f)
        oval(canvas, "#D99353", 49f, 89f, 37f, 38f)
        oval(canvas, "#E4AD65", 56f, 94f, 22f, 24f)
        oval(canvas, "#397450", 46f, 57f, 22f, 27f)
        oval(canvas, "#4D8654", 63f, 53f, 24f, 26f)
        oval(canvas, "#588F58", 75f, 66f, 21f, 24f)
        oval(canvas, "#2F6749", 57f, 74f, 25f, 24f)
        oval(canvas, "#EB9846", 83f, 131f, 24f, 12f)
        oval(canvas, "#EFAC55", 101f, 137f, 24f, 12f)
        oval(canvas, "#DF8041", 71f, 139f, 22f, 11f)
        // Chopsticks.
        paint.color = Color.parseColor("#A97C45")
        paint.strokeWidth = 4f
        canvas.drawLine(157f, 20f, 182f, 166f, paint)
        paint.color = Color.parseColor("#BF9458")
        canvas.drawLine(166f, 19f, 190f, 165f, paint)
        paint.color = Color.parseColor("#85A16A")
        paint.strokeWidth = 2.5f
        val steam = Path()
        steam.moveTo(108f, 42f)
        steam.cubicTo(95f, 26f, 123f, 27f, 110f, 10f)
        paint.style = Paint.Style.STROKE
        canvas.drawPath(steam, paint)
        oval(canvas, "#F7CF85", 26f, 158f, 9f, 9f)
        oval(canvas, "#83A571", 10f, 62f, 7f, 7f)
        canvas.restore()
    }
}
