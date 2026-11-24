package vn.edu.buacombencon

import android.content.Context
import android.util.AttributeSet
import android.widget.Switch

/** Uses the fractional text size declared in the layout. */
class UiSwitch(context: Context, attrs: AttributeSet?) : Switch(context, attrs) {
    init {
        XmlTextSize.apply(this, context, attrs)
    }
}
