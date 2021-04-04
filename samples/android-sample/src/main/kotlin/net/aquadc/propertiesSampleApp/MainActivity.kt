package net.aquadc.propertiesSampleApp

import android.app.Activity
import android.app.Notification
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import net.aquadc.persistence.android.parcel.ParcelPropertiesMemento
import net.aquadc.properties.android.bindings.bindViewTo
import net.aquadc.properties.android.bindings.view.bindEnabledTo
import net.aquadc.properties.android.bindings.view.setWhenClicked
import net.aquadc.properties.android.bindings.widget.bindErrorMessageTo
import net.aquadc.properties.android.bindings.widget.bind
import net.aquadc.properties.android.bindings.widget.textTo
import net.aquadc.properties.android.bindings.widget.bindTextBidirectionally
import net.aquadc.properties.android.bindings.widget.bindTextTo
import net.aquadc.properties.map
import net.aquadc.properties.persistence.memento.restoreTo
import net.aquadc.propertiesSampleLogic.MainVm
import splitties.dimensions.dip
import splitties.systemservices.notificationManager
import splitties.views.dsl.core.button
import splitties.views.dsl.core.editText
import splitties.views.dsl.core.horizontalLayout
import splitties.views.dsl.core.lParams
import splitties.views.dsl.core.textView
import splitties.views.dsl.core.verticalLayout
import splitties.views.dsl.core.wrapInScrollView
import splitties.views.padding


/**
 * Sample MVVm view for Android.
 */
class MainActivity : Activity() {

    private lateinit var vm: MainVm

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // create or restore ViewModel
        vm = (lastNonConfigurationInstance as MainVm?) ?: MainVm(app.user)

        // restore ViewModel's state in case of process death
        savedInstanceState?.getParcelable<ParcelPropertiesMemento>("vm")?.restoreTo(vm)

        setContentView(verticalLayout {
            padding = dip(16)

            addView(editText {
                id = 1
                hint = "Email"
                bindTextBidirectionally(vm.emailProp)
                bindErrorMessageTo(vm.emailValidProp.map {
                    if (it) null else "E-mail is invalid"
                })
            })

            addView(editText {
                id = 2
                hint = "Name"
                bindTextBidirectionally(vm.nameProp)
            })

            addView(editText {
                id = 3
                hint = "Surname"
                bindTextBidirectionally(vm.surnameProp)
            })

            addView(button {
                bindEnabledTo(vm.buttonEnabledProp)
                bindTextTo(vm.buttonEnabledProp.map {
                    if (it) "Save changes" else "Nothing changed"
                })
                setWhenClicked(vm.buttonClickedProp)
            })

            addView(View(this@MainActivity), lParams(weight = 1f))

            addView(textView {
                text = "Other samples"
            })

            addView(horizontalLayout {
                addView(button {
                    text = "RecyclerView"
                    startWhenClicked<RecyclerViewActivity>()
                }, lParams(weight = 1f))

                addView(button {
                    text = "SQLite"
                    startWhenClicked<SqliteActivity>()
                }, lParams(weight = 1f))
            })

            // kinda hacky: I use bindViewTo in order to update notification while activity is visible.
            // Depending on your use-case, lifecycle will be different,
            // so don't forget to unsubscribe and cancel the notification!
            bindViewTo(RemoteViews(packageName, R.layout.notification).bind(
                android.R.id.text1 textTo vm.nameProp,
                android.R.id.text2 textTo vm.emailProp
            )) { v, views ->
                with(v.context) {
                    notificationManager.notify(
                        1,
                        Notification.Builder(this)
                            .setSmallIcon(R.mipmap.ic_launcher)
                            .setContent(views)
                            .setOngoing(true)
                            .build()
                    )
                }
            }

        }.wrapInScrollView())
    }

    private inline fun <reified A : Activity> View.startWhenClicked(): Unit =
            startWhenClicked(A::class.java)

    private fun View.startWhenClicked(klass: Class<out Activity>): Unit =
            setOnClickListener { startActivity(Intent(it.context, klass)) }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable("vm", ParcelPropertiesMemento(vm))
    }

    override fun onRetainNonConfigurationInstance(): Any? = vm

    override fun onDestroy() {
        notificationManager.cancel(1)
        super.onDestroy()
    }

}
