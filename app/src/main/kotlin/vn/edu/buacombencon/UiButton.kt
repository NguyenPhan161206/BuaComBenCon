package vn.edu.buacombencon

import android.content.Context
import android.util.AttributeSet
import android.widget.Button

/** Uses the fractional text size declared in the layout. */
class UiButton(context: Context, attrs: AttributeSet?) : Button(context, attrs) {
    init {
        XmlTextSize.apply(this, context, attrs)
    }
}
