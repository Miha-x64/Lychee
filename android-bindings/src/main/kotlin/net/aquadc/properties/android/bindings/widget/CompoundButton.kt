package net.aquadc.properties.android.bindings.widget

import android.widget.CompoundButton
import net.aquadc.properties.MutableProperty
import net.aquadc.properties.Property
import net.aquadc.properties.android.bindings.bindViewTo

/**
 * Binds checked state to [check] via [CompoundButton.setChecked].
 */
fun CompoundButton.bindCheckedTo(checkedProperty: Property<Boolean>) {
    bindViewTo(checkedProperty, CheckedBinder(null))
}

/**
 * Binds [checkedProperty] to checked sate via [CompoundButton.setOnCheckedChangeListener].
 */
fun CompoundButton.bindToChecked(checkedProperty: MutableProperty<Boolean>) {
    checkedProperty.value = isChecked
    setOnCheckedChangeListener(CheckedBinder(checkedProperty))
}

/**
 * Binds checked state of this [CompoundButton] with [checkedProperty].
 * When this [CompoundButton] gets attached to window, checked state will be set from [checkedProperty].
 */
fun CompoundButton.bindCheckedBidirectionally(checkedProperty: MutableProperty<Boolean>) {
    val listenerAndBinding = CheckedBinder(checkedProperty)
    setOnCheckedChangeListener(listenerAndBinding)
    bindViewTo(checkedProperty, listenerAndBinding)
}

private class CheckedBinder(
        private val checkedProperty: MutableProperty<Boolean>?
) : CompoundButton.OnCheckedChangeListener, (CompoundButton, Boolean) -> Unit {

    private var changing = false

    override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
        if (!changing) {
            changing = true
            checkedProperty!!.value = isChecked
            changing = false
        }
    }

    override fun invoke(p1: CompoundButton, p2: Boolean) {
        if (!changing) {
            changing = true
            p1.isChecked = p2
            changing = false
        }
    }

}
