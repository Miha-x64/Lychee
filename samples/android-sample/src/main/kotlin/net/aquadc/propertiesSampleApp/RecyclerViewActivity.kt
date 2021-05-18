package net.aquadc.propertiesSampleApp

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import net.aquadc.properties.android.bindings.androidx.widget.recycler.ObservingAdapter
import net.aquadc.properties.android.bindings.androidx.widget.recycler.observeAdapter
import net.aquadc.properties.diff.calculateDiffOn
import net.aquadc.properties.executor.WorkerOnExecutor
import net.aquadc.properties.propertyOf
import net.aquadc.properties.syncIf
import splitties.views.dsl.recyclerview.recyclerView
import java.util.concurrent.Executors


class RecyclerViewActivity : Activity() {

    private val handler = Handler()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val data =
                propertyOf(listOf("empty"))

        val diffData =
                data.calculateDiffOn(worker) { old, new ->
                    DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                        override fun getOldListSize(): Int = old.size
                        override fun getNewListSize(): Int = new.size
                        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
                                old[oldItemPosition] == new[newItemPosition]
                        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
                                old[oldItemPosition] == new[newItemPosition]
                    })
                }

        setContentView(recyclerView {
            layoutManager = LinearLayoutManager(this@RecyclerViewActivity)
            observeAdapter(object : ObservingAdapter<StringHolder>() {

                override fun getItemCount(): Int = diffData.value.size

                override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StringHolder =
                        StringHolder(LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_1, parent, false) as TextView)

                override fun onBindViewHolder(holder: StringHolder, position: Int) =
                        holder.bind(diffData.value[position])

                override fun onObservedStateChanged(observed: Boolean) {
                    diffData.syncIf(observed, onChange, dummy = emptyList())
                }
                private val onChange: (List<String>, List<String>, DiffUtil.DiffResult?) -> Unit = { _, _, diff ->
                    this@RecyclerViewActivity.handler.post {
                        diff?.dispatchUpdatesTo(this) ?: notifyDataSetChanged()
                    }
                }

            })
        })

        handler.postDelayed({ data.value += "second" }, 1000)
        handler.postDelayed({ data.value += "third" }, 2000)
        handler.postDelayed({ data.value += "fourth" }, 3000)
        handler.postDelayed({ data.value = data.value.filterIndexed { index, _ -> index !in 1..2 } }, 4000)
        handler.postDelayed({ data.value += listOf("more", "more", "and more") }, 5000)
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    private companion object {
        val worker = WorkerOnExecutor(Executors.newSingleThreadExecutor())
    }

    class StringHolder(view: TextView) : RecyclerView.ViewHolder(view) {
        fun bind(value: String) {
            (itemView as TextView).text = value
        }
    }

}
