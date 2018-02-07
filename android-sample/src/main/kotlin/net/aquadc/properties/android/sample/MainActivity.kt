package net.aquadc.properties.android.sample

import android.app.Activity
import android.os.Bundle
import net.aquadc.properties.android.bindings.bindEnabledTo
import net.aquadc.properties.android.bindings.bindErrorMessageTo
import net.aquadc.properties.android.bindings.bindTextBidirectionally
import net.aquadc.properties.android.bindings.bindTextTo
import net.aquadc.properties.map
import net.aquadc.properties.sample.logic.MainPresenter
import org.jetbrains.anko.*


class MainActivity : Activity() {

    private lateinit var presenter: MainPresenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        presenter = MainPresenter(app.userProp)
        val uiBridge = presenter.ui

        verticalLayout {
            padding = dip(16)

            editText {
                id = 1
                hint = "Email"
                bindTextBidirectionally(uiBridge.emailProp)
                bindErrorMessageTo(uiBridge.emailValidProp.map { if (it) null else "E-mail is invalid" })
            }

            editText {
                id = 2
                hint = "Name"
                bindTextBidirectionally(uiBridge.nameProp)
            }

            editText {
                id = 3
                hint = "Surname"
                bindTextBidirectionally(uiBridge.surnameProp)
            }

            button {
                bindEnabledTo(uiBridge.buttonEnabledProp)
                bindTextTo(uiBridge.buttonTextProp)
                setOnClickListener { presenter.saveButtonClicked() }
            }

            view().lparams(weight = 1f)

            button("Show Monolithic Activity") {
                setOnClickListener { startActivity(intentFor<MonolithicActivity>()) }
            }

        }
    }

}
