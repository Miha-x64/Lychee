package net.aquadc.properties.android.sample

import net.aquadc.properties.MutableProperty
import net.aquadc.properties.allValues

class MainPresenter(
        private val view: MainPresenter.View
) {

    init {
        view.buttonEnabledProp
                .bind(
                        listOf(view.nameProp, view.surnameProp).allValues { it.isNotEmpty() }
                )

        view.buttonTextProp
                .bind(view.nameProp.mapWith(view.surnameProp) { name, surname ->
                    if (name.isEmpty() || surname.isEmpty()) "Fill in the form to register"
                    else "Register as $name $surname"
                })
    }

    interface View {
        val nameProp: MutableProperty<String>
        val surnameProp: MutableProperty<String>

        val buttonEnabledProp: MutableProperty<Boolean>
        val buttonTextProp: MutableProperty<String>
    }

}