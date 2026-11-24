package vn.edu.buacombencon

import android.content.Context
import android.util.AttributeSet
import android.widget.EditText

/** Uses the fractional text size declared in the layout. */
class UiEditText(context: Context, attrs: AttributeSet?) : EditText(context, attrs) {
    init {
        XmlTextSize.apply(this, context, attrs)
    }
}
