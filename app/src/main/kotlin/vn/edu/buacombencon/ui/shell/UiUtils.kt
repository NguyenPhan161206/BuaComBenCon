package vn.edu.buacombencon.ui.shell

import android.app.AlertDialog
import android.content.Context
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import vn.edu.buacombencon.R
import vn.edu.buacombencon.domain.model.RegistrationStatus
import kotlin.math.roundToInt

object UiUtils {
    fun dp(context: Context, value: Int): Float =
        (value * context.resources.displayMetrics.density).roundToInt().toFloat()

    fun inflate(inflater: LayoutInflater, layout: Int, parent: ViewGroup): View =
        inflater.inflate(layout, parent, false).also(parent::addView)

    fun text(root: View, id: Int, value: String) {
        root.findViewById<TextView>(id)?.text = value
    }

    fun click(root: View, id: Int, action: () -> Unit) {
        root.findViewById<View>(id)?.setOnClickListener { action() }
    }

    fun visible(root: View, id: Int, show: Boolean) {
        root.findViewById<View>(id)?.visibility = if (show) View.VISIBLE else View.GONE
    }

    fun typeface(selected: Boolean): Typeface =
        Typeface.create(if (selected) "sans-serif-medium" else "sans-serif", Typeface.NORMAL)

    fun status(root: View, id: Int, value: String) {
        val pill = root.findViewById<TextView>(id) ?: return
        val warning = value == RegistrationStatus.PENDING.code || value == RegistrationStatus.WAITING.code
        pill.text = RegistrationStatus.labelOf(value)
        pill.setTextColor(root.context.getColor(if (warning) R.color.ui_orange else R.color.green))
        pill.setBackgroundResource(if (warning) R.drawable.ui_pill_warning else R.drawable.ui_pill)
    }

    fun chip(inflater: LayoutInflater, parent: LinearLayout, value: String, selected: Boolean, action: () -> Unit) {
        val chip = inflate(inflater, R.layout.item_chip, parent) as TextView
        chip.text = value
        chip.setTextColor(parent.context.getColor(if (selected) R.color.white else R.color.muted))
        chip.typeface = typeface(selected)
        chip.setBackgroundResource(if (selected) R.drawable.ui_chip_selected else R.drawable.ui_chip)
        chip.setOnClickListener { action() }
    }

    fun metrics(root: View, names: Array<String>, values: IntArray) {
        val numberIds = intArrayOf(R.id.metric_value_0, R.id.metric_value_1, R.id.metric_value_2)
        val nameIds = intArrayOf(R.id.metric_name_0, R.id.metric_name_1, R.id.metric_name_2)
        for (index in names.indices) {
            text(root, numberIds[index], values[index].toString())
            text(root, nameIds[index], names[index])
        }
    }

    fun empty(inflater: LayoutInflater, parent: LinearLayout, title: String, subtitle: String) {
        val card = inflate(inflater, R.layout.item_empty, parent)
        text(card, R.id.empty_title, title)
        text(card, R.id.empty_subtitle, subtitle)
    }

    fun toast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    fun info(context: Context, title: String, message: String) {
        AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Đã hiểu", null)
            .show()
    }
}
