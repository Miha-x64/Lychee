package net.aquadc.properties.android.bindings

import android.content.DialogInterface
import android.view.View
import net.aquadc.properties.MutableProperty

/**
 * A bridge between callback-based Android API and Property-based observable APIs.
 */
class SetWhenClicked<T>(
    private val target: MutableProperty<in T>,
    private val value: T
) : View.OnClickListener, View.OnLongClickListener, DialogInterface.OnClickListener {

    override fun onClick(v: View?) {
        target.value = value
    }

    override fun onLongClick(v: View?): Boolean {
        target.value = value
        return true
    }

    override fun onClick(dialog: DialogInterface?, which: Int) {
        target.value = value
    }

}

@JvmName("toTrue") // Java: SetWhenClicked.toTrue(prop)
inline fun SetWhenClicked(target: MutableProperty<Boolean>): SetWhenClicked<Boolean> =
    SetWhenClicked(target, true)
