package vn.edu.buacombencon

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.widget.TextView

/** Retain fractional pixels from sp attributes at larger font scales. */
internal object XmlTextSize {
    fun apply(view: TextView, context: Context, attrs: AttributeSet?) {
        val values = context.obtainStyledAttributes(attrs, intArrayOf(android.R.attr.textSize))
        val pixels = try {
            values.getDimension(0, view.textSize)
        } finally {
            values.recycle()
        }
        view.setTextSize(TypedValue.COMPLEX_UNIT_PX, pixels)
    }
}
