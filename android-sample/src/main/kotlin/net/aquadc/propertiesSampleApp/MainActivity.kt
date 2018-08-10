package net.aquadc.propertiesSampleApp

import android.app.Activity
import android.os.Bundle
import net.aquadc.properties.android.ParcelPropertiesMemento
import net.aquadc.properties.android.bindings.view.bindEnabledTo
import net.aquadc.properties.android.bindings.view.setWhenClicked
import net.aquadc.properties.android.bindings.widget.bindErrorMessageTo
import net.aquadc.properties.android.bindings.widget.bindTextBidirectionally
import net.aquadc.properties.android.bindings.widget.bindTextTo
import net.aquadc.properties.map
import net.aquadc.propertiesSampleLogic.MainVm
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
                id = 1
                hint = "Email"
                bindTextBidirectionally(vm.emailProp)
                bindErrorMessageTo(vm.emailValidProp.map {
                    if (it) null else "E-mail is invalid"
                })
            }

            editText {
                id = 2
                hint = "Name"
                bindTextBidirectionally(vm.nameProp)
            }

            editText {
                id = 3
                hint = "Surname"
                bindTextBidirectionally(vm.surnameProp)
            }

            button {
                bindEnabledTo(vm.buttonEnabledProp)
                bindTextTo(vm.buttonEnabledProp.map {
                    if (it) "Save changes" else "Nothing changed"
                })
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
