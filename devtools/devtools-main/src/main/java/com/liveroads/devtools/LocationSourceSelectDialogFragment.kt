package com.liveroads.devtools

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.support.annotation.MainThread
import android.support.v4.app.DialogFragment
import android.support.v7.app.AlertDialog
import com.liveroads.common.log.obtainLogger
import com.liveroads.devtools.main.R
import com.liveroads.util.log.*

class LocationSourceSelectDialogFragment : DialogFragment(), DialogInterface.OnClickListener {

    var listener: Listener? = null

    private val logger = obtainLogger()

    private val choices = listOf(
            Choice.DEFAULT,
            Choice.BLACK_BOX,
            Choice.GMS
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        logger.onCreate(savedInstanceState)
        super.onCreate(savedInstanceState)
        isCancelable = true
    }

    override fun onDestroy() {
        logger.onDestroy()
        super.onDestroy()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val items = choices.map { it.getLabel(context) }.toTypedArray()
        val titleResId = arguments?.getInt(MapStyleSelectDialogFragment.ARG_TITLE_RES_ID, 0) ?: 0
        return AlertDialog.Builder(context).run {
            if (titleResId != 0) {
                setTitle(titleResId)
            }
            setItems(items, this@LocationSourceSelectDialogFragment)
            create()
        }
    }

    override fun onClick(dialog: DialogInterface, which: Int) {
        val choice = choices[which]
        listener?.onSelected(this, choice)
        dismiss()
    }

    interface Listener {

        @MainThread
        fun onSelected(fragment: LocationSourceSelectDialogFragment, choice: Choice)

    }

    enum class Choice {
        DEFAULT {
            override fun getLabel(context: Context) = context.getText(R.string.lr_devtools_default_value)
        },
        BLACK_BOX {
            override fun getLabel(context: Context) = context.getText(R.string.lr_devtools_map_source_blackbox)
        },
        GMS {
            override fun getLabel(context: Context) = context.getText(R.string.lr_devtools_map_source_gms)
        };

        abstract fun getLabel(context: Context): CharSequence

    }

    companion object {
        const val ARG_TITLE_RES_ID = "liveroads.MapStyleSelectDialogFragment.ARG_TITLE_RES_ID"
    }

}
