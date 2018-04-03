package net.aquadc.properties.android.sample

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.support.v7.util.DiffUtil
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import net.aquadc.properties.diff.calculateDiffOn
import net.aquadc.properties.executor.WorkerOnExecutor
import net.aquadc.properties.unsynchronizedMutablePropertyOf
import org.jetbrains.anko.recyclerview.v7.recyclerView
import java.util.concurrent.Executors


class RecyclerViewActivity : Activity() {

    private val handler = Handler()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val data =
                unsynchronizedMutablePropertyOf(listOf("empty"))
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

        recyclerView {
            layoutManager = LinearLayoutManager(this@RecyclerViewActivity)
            adapter = object : RecyclerView.Adapter<StringHolder>() {

                override fun getItemCount(): Int = diffData.value.size

                override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StringHolder =
                        StringHolder(LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_1, parent, false) as TextView)

                override fun onBindViewHolder(holder: StringHolder, position: Int) =
                        holder.bind(diffData.value[position])

                private var recyclers = 0
                override fun onAttachedToRecyclerView(recyclerView: RecyclerView?) {
                    if (recyclers++ == 0) diffData.addChangeListener(onChange)
                }
                override fun onDetachedFromRecyclerView(recyclerView: RecyclerView?) {
                    if (--recyclers == 0) diffData.removeChangeListener(onChange)
                }
                private val onChange: (List<String>, List<String>, DiffUtil.DiffResult) -> Unit = { _, _, diff ->
                    handler.post {
                        diff.dispatchUpdatesTo(this)
                    }
                }

            }
        }

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
