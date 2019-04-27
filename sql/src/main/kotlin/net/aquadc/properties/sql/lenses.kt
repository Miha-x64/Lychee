package net.aquadc.properties.sql

import net.aquadc.persistence.struct.FieldDef
import net.aquadc.persistence.struct.Lens
import net.aquadc.persistence.struct.PartialStruct
import net.aquadc.persistence.struct.Schema


/**
 * A lens to field of a nested struct.
 */
class Telescope<TS : Schema<TS>, US : Schema<US>, T : PartialStruct<US>?, U>(
        name: String,
        private val outer: Lens<TS, T>,
        private val nested: Lens<US, U>
) : Lens<TS, U>(name, nested.type) {

    override val size: Int get() = outer.size + nested.size

    override fun get(index: Int): FieldDef<*, *> {
        val outerSize = outer.size
        return if (index < outerSize) outer[index] else nested[index - outerSize]
    }

    /*override fun invoke(p1: PartialStruct<TS>): U =
            nested(outer(p1))*/

}

/**
 * A namespace where [div] is overloaded for creating new lenses.
 */
interface LensFactory {

    fun concatNames(outer: String, nested: String): String

    operator fun <TS : Schema<TS>, US : Schema<US>, T : PartialStruct<US>, U> Lens<TS, T>.div(
            nested: Lens<US, U>
    ): Lens<TS, U> =
            Telescope(concatNames(this.name, nested.name), this, nested)

}

/**
 * Generates lenses which names are concatenated using snake_case
 */
object SnakeLensFactory : LensFactory {

    override fun concatNames(outer: String, nested: String): String =
            outer + '_' + nested

}

/**
 * Generates lenses which names are concatenated using camelCase
 */
object CamelLensFactory : LensFactory {

    override fun concatNames(outer: String, nested: String): String = buildString {
        append(outer)
        if (nested.isNotEmpty()) {
            append(nested[0].toUpperCase())
            append(nested, 1, nested.length)
        }
    }

}
