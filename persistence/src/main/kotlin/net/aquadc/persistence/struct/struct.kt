package net.aquadc.persistence.struct


/**
 * Represents an instance of a struct —
 * a map with keys of type [FieldDef] (can be treated as [String] or [Byte])
 * and heterogeneous statically typed values.
 * @see StructDef
 * @see FieldDef
 * @see TransactionalStruct
 * @see StructSnapshot — fully immutable implementation
 * @see net.aquadc.properties.persistence.ObservableStruct from `:properties` — observable implementation
 */
interface Struct<DEF : StructDef<DEF>> {

    /**
     * Represents the type of this struct.
     */
    val type: DEF

    /**
     * Returns the value of the requested field.
     */
    operator fun <T> get(field: FieldDef<DEF, T>): T

}

/**
 * A struct which can be mutated inside a transaction.
 */
interface TransactionalStruct<DEF : StructDef<DEF>> : Struct<DEF> {
    fun beginTransaction(): StructTransaction<DEF>
}

interface StructTransaction<DEF : StructDef<DEF>> : AutoCloseable {
    operator fun <T> set(field: FieldDef.Mutable<DEF, T>, update: T)
    fun setSuccessful()
}

inline fun <DEF : StructDef<DEF>, R> TransactionalStruct<DEF>.transaction(block: (StructTransaction<DEF>) -> R): R {
    val transaction = beginTransaction()
    try {
        val r = block(transaction)
        transaction.setSuccessful()
        return r
    } finally {
        transaction.close()
    }
}
