package net.aquadc.persistence.sql.async


interface AsyncIterator<out T> {
    suspend operator fun next(): T
    suspend operator fun hasNext(): Boolean
}
interface AsyncIterable<out T> {
    operator fun iterator(): AsyncIterator<T>
}
interface AsyncCollection<out E> : Iterable<E> {
    suspend /*val*/ fun size(): Int
    suspend /*operator*/ fun contains(element: @UnsafeVariance E): Boolean
    suspend fun containsAll(elements: Collection<@UnsafeVariance E>): Boolean
}
interface AsyncList<out E> {
    suspend /*operator*/ fun get(index: Int): E
    suspend fun indexOf(element: @UnsafeVariance E): Int
    suspend fun lastIndexOf(element: @UnsafeVariance E): Int
//    fun listIterator(): ListIterator<E>
//    fun listIterator(index: Int): ListIterator<E>
//    suspend fun subList(fromIndex: Int, toIndex: Int): List<E>
}
