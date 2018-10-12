package net.aquadc.persistence.source

import net.aquadc.persistence.struct.FieldDef
import net.aquadc.persistence.struct.StructDef

/**
 * Represents a (potentially weakly typed) data set which can be read.
 * Consumability, type coercion, etc are implementation-dependent.
 */
interface DataReader {

    /**
     * Reads key-value pairs according to the given [StructDef].
     * Will *not* save, mutate, or spoil returned array somehow
     * @return an array with order according to [FieldDef.ordinal] and types according to [FieldDef.type]
     * @throws RuntimeException when values have incorrect types, or required fields are missing and have no [FieldDef.default]
     */
    fun readKeyValuePairs(def: StructDef<*>): Array<Any?>

}
