package net.aquadc.propertiesSampleApp

import android.app.*
import android.content.Context
import android.content.DialogInterface
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.graphics.Color
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.TypedValue
import android.view.*
import android.widget.TextView
import net.aquadc.properties.ChangeListener
import net.aquadc.properties.android.bindings.widget.bindTextTo
import net.aquadc.properties.android.bindings.widget.bindToText
import net.aquadc.properties.propertyOf
import net.aquadc.properties.set
import net.aquadc.properties.sql.SqliteSession
import net.aquadc.properties.sql.createTable
import net.aquadc.propertiesSampleLogic.sql.Human
import net.aquadc.propertiesSampleLogic.sql.SqlViewModel
import net.aquadc.propertiesSampleLogic.sql.Tables
import org.jetbrains.anko.*


class SqliteActivity : Activity() {

    internal lateinit var vm: SqlViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        vm = (lastNonConfigurationInstance as? SqlViewModel) ?:
                SqlViewModel(SqliteSession(Helper(applicationContext).writableDatabase))

        if (savedInstanceState == null) {
            fragmentManager
                    .beginTransaction()
                    .add(android.R.id.content, ListFragment(), null)
                    .commit()
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

    override fun onDestroy() {
        vm.lastInserted.removeChangeListener(lastInsertedChanged)
        vm.selectedProp.removeChangeListener(selectedChanged)
        super.onDestroy()
    }

    private class Helper(
            context: Context
    ) : SQLiteOpenHelper(context, "people", null, 1) {

        override fun onCreate(db: SQLiteDatabase) {
            Tables.forEach(db::createTable)
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            // we're v. 1, yay!
        }

    }

    class SimpleHolder(
            view: TextView,
            clickListener: ((Int) -> Unit)?,
            longClickListener: ((Int) -> Unit)?
    ) : RecyclerView.ViewHolder(view) {

        val textProp = propertyOf("")

        init {
            if (clickListener != null)
                view.setOnClickListener { clickListener(adapterPosition) }

            if (longClickListener != null)
                view.setOnLongClickListener { longClickListener(adapterPosition); true }

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
                                        setPadding(dip(16), dip(8), dip(16), dip(8))
                                        backgroundResource = attr(android.R.attr.selectableItemBackground).resourceId
                                        textAppearance = attr(android.R.attr.textAppearanceListItemSmall).resourceId
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

    class EditDialogFragment : DialogFragment(), DialogInterface.OnClickListener {

        val vm: SqlViewModel
            get() = (activity as SqliteActivity).vm

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
                AlertDialog.Builder(activity)
                        .setTitle("Edit")
                        .setView(UI {
                            frameLayout {
                                editText {
                                    bindTextTo(vm.nameProp)
                                    bindToText(vm.editableNameProp)
                                }.lparams(matchParent, wrapContent) {
                                    leftMargin = dip(8)
                                    rightMargin = dip(8)
                                }
                            }
                        }.view)
                        .setPositiveButton("Ok", null)
                        .setNegativeButton("Delete", this)
                        .create()

        override fun onClick(dialog: DialogInterface, which: Int) {
            vm.deleteClicked.set()
        }

    }

}
