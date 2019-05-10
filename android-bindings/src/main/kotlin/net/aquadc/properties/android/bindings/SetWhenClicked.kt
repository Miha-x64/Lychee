package net.aquadc.properties.android.bindings

import android.content.DialogInterface
import android.view.View
import net.aquadc.properties.MutableProperty
import net.aquadc.properties.set

/**
 * A bridge between callback-based Android API and Property-based observable APIs.
 */
class SetWhenClicked(
        private val target: MutableProperty<Boolean>
) : View.OnClickListener, View.OnLongClickListener, DialogInterface.OnClickListener {

    override fun onClick(v: View?) {
        target.set()
    }

    override fun onLongClick(v: View?): Boolean {
        target.set()
        return true
    }

    override fun onClick(dialog: DialogInterface?, which: Int) {
        target.set()
    }

}
