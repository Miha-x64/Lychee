package net.aquadc.properties.android.sample

import android.app.Activity
import android.os.Bundle
import net.aquadc.properties.android.ParcelPropertiesMemento
import net.aquadc.properties.android.bindings.view.bindEnabledTo
import net.aquadc.properties.android.bindings.view.setWhenClicked
import net.aquadc.properties.android.bindings.widget.bindErrorMessageTo
import net.aquadc.properties.android.bindings.widget.bindTextBidirectionally
import net.aquadc.properties.android.bindings.widget.bindTextTo
import net.aquadc.properties.map
import net.aquadc.properties.sample.logic.MainVm
import org.jetbrains.anko.*

/**
 * Sample MVVm view for Android.
 */
class MainActivity : Activity() {

    private lateinit var vm: MainVm

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // create or restore ViewModel
        vm = (lastNonConfigurationInstance as MainVm?) ?: MainVm(app.userProp)

        // restore ViewModel's state in case of process death
        savedInstanceState?.getParcelable<ParcelPropertiesMemento>("vm")?.restoreTo(vm)

        verticalLayout {
            padding = dip(16)

            editText {
                hint = "Email"
                bindTextBidirectionally(vm.emailProp)
                bindErrorMessageTo(vm.emailValidProp.map { if (it) null else "E-mail is invalid" })
            }

            editText {
                hint = "Name"
                bindTextBidirectionally(vm.nameProp)
            }

            editText {
                hint = "Surname"
                bindTextBidirectionally(vm.surnameProp)
            }

            button {
                bindEnabledTo(vm.buttonEnabledProp)
                bindTextTo(vm.buttonTextProp)
                setWhenClicked(vm.buttonClickedProp)
            }

            textView {
                bindTextTo(vm.debouncedEmail)
            }

            view().lparams(weight = 1f)

            button("Show RecyclerView sample") {
                setOnClickListener { startActivity(intentFor<RecyclerViewActivity>()) }
            }

            button("Show Monolithic Activity") {
                setOnClickListener { startActivity(intentFor<MonolithicActivity>()) }
            }

        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable("vm", ParcelPropertiesMemento(vm))
    }

    override fun onRetainNonConfigurationInstance(): Any? = vm

}
