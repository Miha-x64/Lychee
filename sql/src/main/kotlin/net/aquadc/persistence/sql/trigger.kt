@file:[
    JvmName("Triggers")
    Suppress("NOTHING_TO_INLINE")
]
package net.aquadc.persistence.sql

import androidx.annotation.GuardedBy
import net.aquadc.collections.InlineEnumSet
import net.aquadc.collections.contains
import net.aquadc.collections.forEach
import net.aquadc.collections.isEmpty
import net.aquadc.collections.noneOf
import net.aquadc.collections.plus
import net.aquadc.persistence.NullSchema
import net.aquadc.persistence.realToString
import net.aquadc.persistence.struct.FieldDef
import net.aquadc.persistence.struct.FldSet
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.struct.StoredLens
import net.aquadc.persistence.struct.emptyFieldSet
import net.aquadc.persistence.struct.forEachBit
import net.aquadc.persistence.struct.plus
import net.aquadc.properties.diff.DiffChangeListener
import net.aquadc.properties.diff.addUnconfinedChangeListener
import net.aquadc.properties.diff.concurrentDiffPropertyOf
import java.io.Closeable
import java.lang.Integer.signum

enum class TriggerEvent(@JvmField val balance: Int) {
    INSERT(+1), UPDATE(0), DELETE(-1),
//  I typically avoid SCREAMING case but gonna keep this tradition for SQL
}

typealias TriggerSubject = Pair<Table<*, *>, TriggerEvent>

/**
 * Holds changes of some records in some tables.
 * Basically a typesafe Map<Table, Changes>
 */
class TriggerReport internal constructor(
    private val tableToChanges: Map<Table<*, *>, ListChanges<*, *>>
) {

    @Suppress("UNCHECKED_CAST")
    fun <SCH : Schema<SCH>, ID : IdBound> of(table: Table<SCH, ID>): ListChanges<SCH, ID> =
        (tableToChanges[table] ?: noChanges) as ListChanges<SCH, ID>

    /*@JvmName("ofTableWithLongPk")
    fun <SCH : Schema<SCH>> of(table: Table<SCH, Long>): ListChangesLongPk<SCH> = TODO()*/

    internal fun take(filter: Map<Table<*, *>, InlineEnumSet<TriggerEvent>>): TriggerReport? {
        if (tableToChanges.isEmpty())
            return null // it's important not to trigger listeners when nothing has changed

        var myCopy: HashMap<Table<*, *>, ListChanges<*, *>>? = null

        tableToChanges.forEach { (table, changes) ->
            if (!filter.containsKey(table)) {
                (myCopy ?: HashMap(tableToChanges).also { myCopy = it }).remove(table)
            } else {
                val myChanges = changes.take(filter[table]!!)
                if (myChanges !== changes) {
                    (myCopy ?: HashMap(tableToChanges).also { myCopy = it })[table] = myChanges
                }
            }
        }

        return when {
            myCopy == null -> this
            myCopy!!.isEmpty() -> null
            else -> TriggerReport(myCopy!!)
        }
    }

    override fun toString(): String = tableToChanges.values.realToString("TriggerReport(", ")")
}

private val noChanges = ListChanges<NullSchema, IdBound>(emptySet(), emptyMap(), emptySet(), null, longArrayOf())
class ListChanges<SCH : Schema<SCH>, ID : IdBound> internal constructor(
    val inserted: Set<ID>,
    updated: Map<ID, /*index:*/Int>,
    val removed: Set<ID>,
    private val table: Table<SCH, ID>?,
    private val changes: LongArray
) {
    private val _updated: Map<ID, /*index:*/Int> = updated
    val updated: Set<ID> get() = _updated.keys
    private val numWords =
        if (table == null) -1 else wordCountForCols(table.managedColumns.size)//.also { check(changes.size == updated.size * it) }

    inline operator fun component1(): Set<ID> = inserted
    inline operator fun component2(): Set<ID> = updated
    inline operator fun component3(): Set<ID> = removed

    fun updatedColumns(id: ID): Array<StoredLens<SCH, *, *>> {
        val updIndex = _updated[id] ?: return emptyArray()
        val updOffset = updIndex * numWords
        val outArray = arrayOfNulls<StoredLens<SCH, *, *>>(changes.popCount(updOffset, count = numWords))
        var cur = 0
        val columns = table!!.managedColumns // if table == null, _updated must be empty so we won't reach here
        for (wordNo in 0 until numWords) {
            changes[updOffset + wordNo].forEachBit { _, bitIdx ->
                outArray[cur++] = columns[64*wordNo + bitIdx]
            }
        }
        check(outArray.size == cur)
        return outArray as Array<StoredLens<SCH, *, *>/*!!*/>
    }
    fun updatedFields(id: ID): FldSet<SCH> {
        val updIndex = _updated[id] ?: return emptyFieldSet()
        val updOffset = updIndex * numWords
        var outSet: FldSet<SCH> = emptyFieldSet<SCH>()
        val columns = table!!.managedColumns // if table == null, _updated must be empty so we won't reach here
        for (wordNo in 0 until numWords) {
            changes[updOffset + wordNo].forEachBit { _, bitIdx ->
                outSet += columns[64*wordNo + bitIdx][0] as FieldDef<SCH, *, *>
            }
        }
        return outSet
    }

    internal fun take(which: InlineEnumSet<TriggerEvent>): ListChanges<SCH, ID> {
        val es = emptySet<Nothing>()
        val em = emptyMap<ID, Nothing>()
        val ins = if (TriggerEvent.INSERT in which) inserted else es
        val upd = if (TriggerEvent.UPDATE in which) _updated else em
        val rem = if (TriggerEvent.DELETE in which) removed else es
        return when {
            ins === es && upd === em && rem === es -> @Suppress("UNCHECKED_CAST") // \n required
                noChanges as ListChanges<SCH, ID>
            ins === inserted && upd === updated && rem === removed ->
                this
            else ->
                ListChanges(ins, upd, rem, table, if (upd === _updated) changes else noChanges.changes)
        }
    }

    private fun LongArray.popCount(from: Int, count: Int): Int {
        var cnt = 0
        for (i in from.until(from+count)) {
            cnt += java.lang.Long.bitCount(this[i])
        }
        return cnt
    }

    override fun toString(): String =
        if (table == null) "ListChanges(empty)"
        else "ListChanges(${table.name}: ${inserted.size} inserted, ${updated.size} updated, ${removed.size} removed)"
}
/*class ListChangesLongPk<SCH : Schema<SCH>>(
    val inserted: LongSet,
    val updated: LongSet,
    val removed: LongSet,
    private val table: Table<SCH, Long>,
    private val changes: LongArray
) {
    fun updatedColumns(id: Long): Array<StoredLens<SCH, *, *>> = TODO()
    fun updatedFields(id: Long): FldSet<SCH> = TODO()
}*/

// object/class Changes : Schema<Changes> { …
// It's not much efficient or handy to create a Schema and a Table for changes because
// it copies primary key of original table and it could be a 'native' type.

internal fun wordCountForCols(colCount: Int) = colCount / 64 + signum(colCount % 64)

@OptIn(ExperimentalStdlibApi::class)
internal class Triggerz { // @file:JvmName("Triggers")

    @GuardedBy("activeSubjects") private val activeSubjects = ArrayList<TriggerSubject>()
    fun activeSubjects(): Collection<Table<*, *>> =
        synchronized(activeSubjects) { activeSubjects.mapTo(HashSet(), TriggerSubject::first) }

    // reuse lock-free notification + instant unsubscription machinery
    private val notifier = concurrentDiffPropertyOf<Unit, TriggerReport>(Unit)

    fun addListener(transact: () -> InternalTransaction<*>, subjects: Array<out TriggerSubject>, listener: (TriggerReport) -> Unit): Closeable {
        synchronized(activeSubjects) {
            // unfortunately, having AtomicReference<Array<TriggerSubject>> won't help:
            // adding two identical triggers concurrently could lead to such an execution that
            // one thread starts setting triggers while other one returns before triggers are actually set.
            // Let us be conservative and safe first; relaxed and fast options could be added in subsequent versions.

            val diff = HashMap<Table<*, *>, InlineEnumSet<TriggerEvent>>()
            subjects.forEach {
                if (it !in activeSubjects) diff[it.first] = (diff[it.first] ?: noneOf<TriggerEvent>()) + it.second
                activeSubjects += it
            }
            if (diff.isNotEmpty()) {
                val t = transact()
                try {
                    t.addTriggers(diff)
                    t.setSuccessful()
                } finally {
                    t.close(false)
                }
            } // make some IO while holding a lock, yay!
        }

        val filter = subjects.toSubjectMap()
        val wrap: DiffChangeListener<Unit, TriggerReport> = { _, _, report ->
            // TODO: add an option not to filter
            report.take(filter)?.let(listener)
        }
        notifier.addUnconfinedChangeListener(wrap)
        return Closeable {
            notifier.removeChangeListener(wrap) // remove listener object instantaneously to avoid memory leaks!
            // Triggers could be removed later, by the way.

            synchronized(activeSubjects) {
                val diff = HashMap<Table<*, *>, InlineEnumSet<TriggerEvent>>()
                subjects.forEach {
                    activeSubjects.remove(it)
                    if (it !in activeSubjects) diff[it.first] = (diff[it.first] ?: noneOf<TriggerEvent>()) + it.second
                }
                if (diff.isNotEmpty()) {
                    val t = transact()
                    try {
                        t.removeTriggers(diff)
                        t.setSuccessful()
                    } finally {
                        t.close(false)
                    }
                }
            }
        }
    }

    // TODO: make enqueue+notify non-blocking, find a way to push updates directly to the queue of DiffProperty
    @GuardedBy("queue") private val queue = ArrayDeque<TriggerReport>()
    fun enqueue(report: TriggerReport) {
        synchronized(queue) { queue.add(report) }
    }
    fun notifyPending() {
        synchronized(queue) {
            while (queue.isNotEmpty()) {
                check(notifier.casValue(Unit, Unit, queue.removeFirst()))
            }
        }
    }

}

internal fun Array<out TriggerSubject>.toSubjectMap(): Map<Table<*, *>, InlineEnumSet<TriggerEvent>> {
    val map = HashMap<Table<*, *>, InlineEnumSet<TriggerEvent>>()
    forEach { (table, event) ->
        val set = map[table] ?: noneOf<TriggerEvent>()
        map[table] = set + event
    }
    return map
}

internal inline fun StringBuilder.appendJoining(
    from: InlineEnumSet<TriggerEvent>, separator: CharSequence, appendE: StringBuilder.(TriggerEvent) -> StringBuilder
): StringBuilder {
    if (from.isEmpty) return this

    from.forEach { appendE(it).append(separator) }
    setLength(length - separator.length)
    return this
}
