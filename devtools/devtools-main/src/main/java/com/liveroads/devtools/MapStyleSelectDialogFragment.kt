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

class MapStyleSelectDialogFragment : DialogFragment(), DialogInterface.OnClickListener {

    var listener: Listener? = null

    private val logger = obtainLogger()

    private val choices = listOf(
            Choice.DEFAULT,
            Choice.DAY_THEME,
            Choice.NIGHT_THEME,
            Choice.DAY_THEME_HD,
            Choice.NIGHT_THEME_HD
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
        val titleResId = arguments?.getInt(ARG_TITLE_RES_ID, 0) ?: 0
        return AlertDialog.Builder(context).run {
            if (titleResId != 0) {
                setTitle(titleResId)
            }
            setItems(items, this@MapStyleSelectDialogFragment)
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
        fun onSelected(fragment: MapStyleSelectDialogFragment, choice: Choice)

    }

    enum class Choice {
        DEFAULT {
            override fun getLabel(context: Context) = context.getText(R.string.lr_devtools_default_value)
        },
        DAY_THEME {
            override fun getLabel(context: Context) = context.getText(R.string.lr_devtools_day_theme)
        },
        NIGHT_THEME {
            override fun getLabel(context: Context) = context.getText(R.string.lr_devtools_night_theme)
        },
        DAY_THEME_HD {
            override fun getLabel(context: Context) = context.getText(R.string.lr_devtools_day_theme_hd)
        },
        NIGHT_THEME_HD {
            override fun getLabel(context: Context) = context.getText(R.string.lr_devtools_night_theme_hd)
        };

        abstract fun getLabel(context: Context): CharSequence

    }

    companion object {
        const val ARG_TITLE_RES_ID = "liveroads.MapStyleSelectDialogFragment.ARG_TITLE_RES_ID"
    }

}
