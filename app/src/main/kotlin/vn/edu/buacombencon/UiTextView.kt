package vn.edu.buacombencon

import android.content.Context
import android.util.AttributeSet
import android.widget.TextView

/** Uses the fractional text size declared in the layout. */
class UiTextView(context: Context, attrs: AttributeSet?) : TextView(context, attrs) {
    init {
        XmlTextSize.apply(this, context, attrs)
    }
}
