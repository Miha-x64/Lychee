package net.aquadc.properties.android.sample

import android.app.Activity
import android.os.Bundle
import net.aquadc.properties.android.bindings.*
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

        vm = MainVm(app.userProp)

        verticalLayout {
            padding = dip(16)

            editText {
                id = 1
                hint = "Email"
                bindTextBidirectionally(vm.emailProp)
                bindErrorMessageTo(vm.emailValidProp.map { if (it) null else "E-mail is invalid" })
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
                bindTextTo(vm.buttonTextProp)
                setWhenClicked(vm.buttonClickedProp)
            }

            view().lparams(weight = 1f)

            button("Show Monolithic Activity") {
                setOnClickListener { startActivity(intentFor<MonolithicActivity>()) }
            }

        }
    }

}
