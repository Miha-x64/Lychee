package net.aquadc.properties.android.sample

import android.app.Activity
import android.os.Bundle
import net.aquadc.properties.android.bindEnabledTo
import net.aquadc.properties.android.bindTextBidirectionally
import net.aquadc.properties.android.bindTextTo
import net.aquadc.properties.mutablePropertyOf
import org.jetbrains.anko.*

class MainActivity : Activity(), MainPresenter.View {

    override val emailProp = mutablePropertyOf("")
    override val nameProp = mutablePropertyOf("")
    override val surnameProp = mutablePropertyOf("")

    override val buttonEnabledProp = mutablePropertyOf(false)
    override val buttonTextProp = mutablePropertyOf("")

    private lateinit var presenter: MainPresenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        verticalLayout {
            padding = dip(16)

            editText {
                id = 1
                hint = "Email"
                bindTextBidirectionally(emailProp)
            }

            editText {
                id = 1
                hint = "Name"
                bindTextBidirectionally(nameProp)
            }

            editText {
                id = 2
                hint = "Surname"
                bindTextBidirectionally(surnameProp)
            }

            button {
                bindEnabledTo(buttonEnabledProp)
                bindTextTo(buttonTextProp)
                setOnClickListener { presenter.saveButtonClicked() }
            }

        }

        presenter = MainPresenter(this, (application as App).userProp)
    }

}
