package vn.edu.buacombencon

import android.content.Context
import android.util.AttributeSet
import android.widget.CheckBox

/** Uses the fractional text size declared in the layout. */
class UiCheckBox(context: Context, attrs: AttributeSet?) : CheckBox(context, attrs) {
    init {
        XmlTextSize.apply(this, context, attrs)
    }
}
