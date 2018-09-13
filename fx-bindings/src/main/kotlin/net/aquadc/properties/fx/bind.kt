package net.aquadc.properties.fx

import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.ObservableValue
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import net.aquadc.properties.MutableProperty
import net.aquadc.properties.Property
import javafx.beans.property.Property as FxProperty

/**
 * Binds [this] and [that] property values.
 * [this] takes [that] value first.
 */
@Deprecated("use prop.fx()", replaceWith = ReplaceWith("this.bindBidirectional(that.fx())"))
fun <T> FxProperty<T>.bindBidirectionally(that: MutableProperty<T>) {
    this.value = that.value
    var mutatingThis = false
    var mutatingThat = false

    this.addListener { _, _, new ->
        if (!mutatingThis) {
            mutatingThat = true
            that.value = new
            mutatingThat = false
        }
    }

    that.addChangeListener { _, new ->
        if (!mutatingThat) {
            mutatingThis = true
            this.value = new
            mutatingThis = false
        }
    }
}

/**
 * Binds [this] property value to [that].
 */
@Deprecated("use prop.fx()", replaceWith = ReplaceWith("this.bind(that.fx())"))
fun <T> FxProperty<in T>.bindTo(that: Property<T>) {
    this.value = that.value
    that.addChangeListener { _, new ->
        this.value = new
    }
}

/**
 * Returns [FxProperty] view on this [MutableProperty].
 */
fun <T> MutableProperty<T>.fx(): FxProperty<T> {
    val prop = SimpleObjectProperty<T>()
    prop.bindBidirectionally(this)
    return prop
}

/**
 * Returns [ObservableValue] view on this [Property].
 */
fun <T> Property<T>.fx(): ObservableValue<T> {
    val prop = SimpleObjectProperty<T>()
    prop.bindTo(this)
    return prop
}

/**
 * Returns [ObservableList] view on this [Property] of [List].
 * @implNote this naïve implementation updates [ObservableList] fully
 */
fun <T> Property<List<T>>.fxList(): ObservableList<T> {
    val list = FXCollections.observableArrayList(value)
    addChangeListener { _, new ->
        list.clear()
        list.addAll(new)
    }
    return list
}

/**
 * Binds [this] value to [that] value.
 */
fun <T> MutableProperty<T>.bindTo(that: ObservableValue<T>) {
    value = that.value
    that.addListener { _, _, new ->
        this.value = new
    }
}
