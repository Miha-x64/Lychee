package net.aquadc.propertiesSampleApp

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.app.DialogFragment
import android.app.Fragment
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import net.aquadc.persistence.sql.SqliteSession
import net.aquadc.persistence.sql.createTable
import net.aquadc.properties.ChangeListener
import net.aquadc.properties.android.bindings.SetWhenClicked
import net.aquadc.properties.android.bindings.widget.bindTextTo
import net.aquadc.properties.android.bindings.widget.bindToText
import net.aquadc.properties.propertyOf
import net.aquadc.properties.set
import net.aquadc.propertiesSampleLogic.sql.Human
import net.aquadc.propertiesSampleLogic.sql.SampleTables
import net.aquadc.propertiesSampleLogic.sql.SqlViewModel
import splitties.dimensions.dip
import splitties.resources.styledDrawable
import splitties.resources.withStyledAttributes
import splitties.views.dsl.core.editText
import splitties.views.dsl.core.frameLayout
import splitties.views.dsl.core.lParams
import splitties.views.dsl.core.matchParent
import splitties.views.dsl.core.wrapContent
import splitties.views.setPaddingDp
import splitties.views.textAppearance


class SqliteActivity : Activity() {

    internal lateinit var vm: SqlViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        vm = (lastNonConfigurationInstance as SqlViewModel?) ?:
                SqlViewModel(SqliteSession(Helper(applicationContext).writableDatabase))

        if (savedInstanceState == null) {
            fragmentManager
                    .beginTransaction()
                    .add(android.R.id.content, ListFragment(), null)
                    .commit()

            // our ViewModel has no state except persistent and transient
        }

        vm.lastInserted.addChangeListener(lastInsertedChanged)
        vm.selectedProp.addChangeListener(selectedChanged)
    }

    private val lastInsertedChanged: ChangeListener<Human?> = { _, inserted ->
        if (inserted != null) vm.selectedProp.value = inserted
    }

    private val selectedChanged: ChangeListener<Human?> = { _, _ ->
        EditDialogFragment().show(fragmentManager, null)
    }

    override fun onRetainNonConfigurationInstance(): Any =
            vm

    override fun onDestroy() {
        vm.lastInserted.removeChangeListener(lastInsertedChanged)
        vm.selectedProp.removeChangeListener(selectedChanged)
        super.onDestroy()
    }

    private class Helper(
            context: Context
    ) : SQLiteOpenHelper(context, "people", null, 1) {

        override fun onCreate(db: SQLiteDatabase) {
            SampleTables.forEach { db.createTable(it) } // function reference does not work here. WTF?
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            // we're v. 1, yay!
        }

    }

    class SimpleHolder(
            view: TextView,
            onClick: ((Int) -> Unit)?,
            onLongClick: ((Int) -> Unit)?
    ) : RecyclerView.ViewHolder(view) {

        val textProp = propertyOf("")

        init {
            if (onClick != null)
                view.setOnClickListener { onClick(adapterPosition) }

            if (onLongClick != null)
                view.setOnLongClickListener { onLongClick(adapterPosition); true }

            view.bindTextTo(textProp)
        }

    }

    class ListFragment : Fragment() {

        val vm: SqlViewModel
            get() = (activity as SqliteActivity).vm

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setHasOptionsMenu(true)
        }

        override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
            super.onCreateOptionsMenu(menu, inflater)
            menu.add("Create").also {
                it.setOnMenuItemClickListener { _ -> vm.createClicked.set(); true }
                it.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
            }
        }

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle?): View {
            return RecyclerView(container.context).apply {
                layoutManager = LinearLayoutManager(container.context)
                adapter = object : RecyclerView.Adapter<SimpleHolder>(), ChangeListener<List<Human>> {

                    val list = vm.humanListProp

                    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SimpleHolder =
                            SimpleHolder(
                                    TextView(parent.context).apply {
                                        layoutParams = RecyclerView.LayoutParams(matchParent, wrapContent)
                                        setPaddingDp(16, 8, 16, 8)
                                        background = styledDrawable(android.R.attr.selectableItemBackground)
                                        textAppearance = context.withStyledAttributes(android.R.attr.textAppearanceListItemSmall) { getResourceId(it, 0) }
                                    },
                                    { vm.selectedProp.value = list.value[it] },
                                    null
                            )

                    override fun onBindViewHolder(holder: SimpleHolder, position: Int) {
                        val human = list.value[position]
                        holder.textProp.bindTo(vm.nameSurnameProp(human))
                    }

                    override fun getItemCount(): Int =
                            list.value.size

                    private var recyclers = 0
                    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
                        if (recyclers++ == 0) list.addChangeListener(this)
                    }
                    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
                        if (--recyclers == 0) list.removeChangeListener(this)
                    }

                    override fun invoke(old: List<Human>, new: List<Human>) {
                        notifyDataSetChanged()
                    }

                }
            }
        }

    }

    class EditDialogFragment : DialogFragment() {

        val vm: SqlViewModel
            get() = (activity as SqliteActivity).vm

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = with(activity) {
            AlertDialog.Builder(activity)
                    .setTitle("Edit")
                    .setView(
                            frameLayout {
                                addView(editText {
                                    bindTextTo(vm.nameProp)
                                    bindToText(vm.editableNameProp)
                                }, lParams(matchParent, wrapContent) {
                                    leftMargin = dip(8)
                                    rightMargin = dip(8)
                                })
                            })
                    .setPositiveButton("Ok", null)
                    .setNegativeButton("Delete", SetWhenClicked(vm.deleteClicked))
                    .create()
        }

    }

}
