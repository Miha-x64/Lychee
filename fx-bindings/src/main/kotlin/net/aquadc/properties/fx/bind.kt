package net.aquadc.properties.fx

import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.ObservableValue
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import net.aquadc.properties.MutableProperty
import net.aquadc.properties.Property
import javafx.beans.property.Property as FxProperty

@Deprecated("use prop.fx()", ReplaceWith("this.bindBidirectional(that.fx())"), DeprecationLevel.ERROR)
fun <T> FxProperty<T>.bindBidirectionally(that: MutableProperty<T>)  {
}

@Deprecated("use prop.fx()", ReplaceWith("this.bind(that.fx())"), DeprecationLevel.ERROR)
fun <T> FxProperty<in T>.bindTo(that: Property<T>) {
}

/**
 * Returns [FxProperty] view on this [MutableProperty].
 */
fun <T> MutableProperty<T>.fx(): FxProperty<T> {
    val prop = SimpleObjectProperty<T>()
    prop.value = this.value
    var mutatingThis = false
    var mutatingThat = false
    prop.addListener { _, _, new ->
        if (!mutatingThis) {
            mutatingThat = true
            this.value = new
            mutatingThat = false
        }
    }
    this.addChangeListener { _, new ->
        if (!mutatingThat) {
            mutatingThis = true
            prop.value = new
            mutatingThis = false
        }
    }
    return prop
}

/**
 * Returns [ObservableValue] view on this [Property].
 */
fun <T> Property<T>.fx(): ObservableValue<T> {
    val prop = SimpleObjectProperty<T>()
    prop.value = this.value
    this.addChangeListener { _, new ->
        prop.value = new
    }
    return prop
}

/**
 * Returns [ObservableList] view on this [Property] of [List].
 * @implNote this naïve implementation updates [ObservableList] fully
 */
fun <T> Property<List<T>>.fxList(): ObservableList<T> {
    val list = FXCollections.observableArrayList(value)
    addChangeListener { _, new ->
        list.setAll(new)
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
