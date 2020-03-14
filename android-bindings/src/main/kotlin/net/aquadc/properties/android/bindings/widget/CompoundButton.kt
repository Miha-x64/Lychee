@file:JvmName("CompoundButtonBindings")
package net.aquadc.properties.android.bindings.widget

import android.widget.CompoundButton
import net.aquadc.properties.MutableProperty
import net.aquadc.properties.Property
import net.aquadc.properties.android.bindings.Binding
import net.aquadc.properties.android.bindings.bindViewTo
import net.aquadc.properties.android.bindings.bindViewToBinding

/**
 * Binds checked state to [check] via [CompoundButton.setChecked].
 */
fun CompoundButton.bindCheckedTo(checkedProperty: Property<Boolean>) =
        bindViewTo(checkedProperty) { v, checked -> v.isChecked = checked }

/**
 * Binds [checkedProperty] to checked sate via [CompoundButton.setOnCheckedChangeListener].
 */
fun CompoundButton.bindToChecked(checkedProperty: MutableProperty<Boolean>) {
    checkedProperty.value = isChecked
    setOnCheckedChangeListener(CheckedBinder(this, checkedProperty))
}

/**
 * Binds checked state of this [CompoundButton] with [checkedProperty].
 * When this [CompoundButton] gets attached to window, checked state will be set from [checkedProperty].
 */
fun CompoundButton.bindCheckedBidirectionally(checkedProperty: MutableProperty<Boolean>) {
    val listenerAndBinding = CheckedBinder(this, checkedProperty)
    setOnCheckedChangeListener(listenerAndBinding)
    bindViewToBinding(checkedProperty, listenerAndBinding)
}

private class CheckedBinder(
        view: CompoundButton,
        property: MutableProperty<Boolean>
) : Binding<CompoundButton, Boolean>(view, property), CompoundButton.OnCheckedChangeListener {

    private var changing = false

    override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
        if (!changing) {
            changing = true
            (property as MutableProperty<Boolean>).value = isChecked
            changing = false
        }
    }

    override fun bind(view: CompoundButton, value: Boolean) {
        if (!changing) {
            changing = true
            view.isChecked = value
            changing = false
        }
    }
}
