
[![Build Status](https://travis-ci.org/Miha-x64/reactive-properties.svg?branch=master)](https://travis-ci.org/Miha-x64/reactive-properties)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/89813e3ee28441b3937a76f09e906aef)](https://www.codacy.com/app/Miha-x64/reactive-properties?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=Miha-x64/reactive-properties&amp;utm_campaign=Badge_Grade)
![Lock-free](https://img.shields.io/badge/%E2%9A%9B-Lock--free-3399aa.svg) 
![Extremely lightweight](https://img.shields.io/badge/🦋-Extremely%20Lightweight-7799cc.svg)

[![codecov](https://codecov.io/gh/Miha-x64/reactive-properties/branch/master/graph/badge.svg)](https://codecov.io/gh/Miha-x64/reactive-properties) in module `:properties`, excluding inline functions

## Adding to a project

[![Download](https://api.bintray.com/packages/miha-x64/maven/net.aquadc.properties%3Aproperties/images/download.svg)](https://bintray.com/miha-x64/maven/net.aquadc.properties%3Aproperties/_latestVersion) Reactive Properties

[![Download](https://api.bintray.com/packages/miha-x64/maven/net.aquadc.properties%3Aandroid-bindings/images/download.svg)](https://bintray.com/miha-x64/maven/net.aquadc.properties%3Aandroid-bindings/_latestVersion) Android Bindings

```
repositories {
    ...
    maven { url 'https://dl.bintray.com/miha-x64/maven' }
}
```

```
dependencies {
    // JVM
    compile 'net.aquadc.properties:properties:0.0.5'

    // Android + Gradle 4
    implementation 'net.aquadc.properties:android-bindings:0.0.5'
    // 'net.aquadc.properties:properties:0.0.5' will be added automatically
}
```

# reactive-properties

Properties (subjects) inspired by JavaFX MVVM-like approach. A `Property` provides functionality similar to `BehaviorSubject` in RxJava, or `Property` in JavaFX, or `LiveData` in Android Arch.

* Simple and easy-to-use
* Lightweight: core + android-bindings are <1000 methods, also highly optimized for runtime
* Extensive: not confined to Android, JavaFX or whatever
* Contains both single-threaded and concurrent (lock-free) implementations
* Provides MVVM / data-binding for Android and JavaFX
* Sweeter with [Anko-layouts](https://github.com/Kotlin/anko#anko-layouts-wiki) 
  and [TornadoFX](https://github.com/edvin/tornadofx)
* Depends only on Kotlin-stdlib
* [Presentation](https://speakerdeck.com/gdg_rnd/mikhail-goriunov-advanced-kotlin-patterns-on-android-properties)
   — problem statement, explanations

## Alternatives

* [agrosner/KBinding](https://github.com/agrosner/KBinding) (MIT) — similar to this,
  Observable-based, Android-only, depends on coroutines
* [BennyWang/KBinding](https://github.com/BennyWang/KBinding) (no license) —
  Android-only, based on annotation processing, depends on RXJava 1.3,
* [LewisRhine/AnkoDataBindingTest](https://github.com/LewisRhine/AnkoDataBindingTest)
   (no license) 
   — theoretical solution from [Data binding in Anko](https://medium.com/lewisrhine/data-binding-in-anko-77cd11408cf9)
   article, Android-only, depends on Anko and AppCompat
* [lightningkite/kotlin-anko-observable](https://github.com/lightningkite/kotlin-anko-observable) (no license),
  based on [lightningkite/kotlin-anko](https://github.com/lightningkite/kotlin-anko) (depends on Anko and AppCompat)
  and [lightningkite/kotlin-observable](https://github.com/lightningkite/kotlin-observable),
  [UnknownJoe796/kotlin-components-starter](https://github.com/UnknownJoe796/kotlin-components-starter)
* [MarcinMoskala/KotlinAndroidViewBindings](https://github.com/MarcinMoskala/KotlinAndroidViewBindings)

## Sample

```kt
val prop: MutableProperty<Int> = propertyOf(1)
val mapped: Property<Int> = prop.map { 10 * it }
assertEquals(10, mapped.value)

prop.value = 5
assertEquals(50, mapped.value)


val tru = propertyOf(true)
val fals = !tru // operator overloading
assertEquals(false, fals.value)
```

## Sample usage in GUI application

Anko layout for Android:

```kt
verticalLayout {
    padding = dip(16)

    editText {
        id = 1 // let view save its state, focus, etc
        hint = "Email"
        bindTextBidirectionally(vm.emailProp)
        bindErrorMessageTo(vm.emailValidProp.map { if (it) null else "E-mail is invalid" })
    }

    editText {
        id = 2
        hint = "Name"
        bindTextBidirectionally(vm.nameProp)
    }

    editText {
        id = 3
        hint = "Surname"
        bindTextBidirectionally(vm.surnameProp)
    }

    button {
        bindEnabledTo(vm.buttonEnabledProp)
        bindTextTo(vm.buttonTextProp)
        setWhenClicked(vm.buttonClickedProp)
        // ^ set flag on action
    }

}
```

JavaFx layout (using JFoenix):

```kt
children.add(JFXTextField().apply {
    promptText = "Email"
    textProperty().bindBidirectionally(vm.emailProp)
})

children.add(Label().apply {
    text = "E-mail is invalid"
    bindVisibilityHardlyTo(!vm.emailValidProp)
})

children.add(JFXTextField().apply {
    promptText = "Name"
    textProperty().bindBidirectionally(vm.nameProp)
})

children.add(JFXTextField().apply {
    promptText = "Surname"
    textProperty().bindBidirectionally(vm.surnameProp)
})

children.add(JFXButton("Press me, hey, you!").apply {
    disableProperty().bindTo(!vm.buttonEnabledProp)
    textProperty().bindTo(vm.buttonTextProp)
    setOnAction { vm.buttonClickedProp.set() }
})
```

Common ViewModel:

```kt
class MainVm(
        private val userProp: MutableProperty<InMemoryUser>
) : PersistableProperties {

    // user input

    val emailProp = propertyOf(userProp.value.email)
    val nameProp = propertyOf(userProp.value.name)
    val surnameProp = propertyOf(userProp.value.surname)
    val buttonClickedProp = propertyOf(false).also {
    // reset flag and perform action — store User being edited into memory
        it.clearEachAnd {
            userProp.value = editedUser.snapshot()
        }
    }

    // preserve/restore state of this ViewModel
    override fun saveOrRestore(d: PropertyIo) {
        d x emailProp
        d x nameProp
        d x surnameProp
    }

    // a feedback for user actions

    val emailValidProp = emailProp.map { it.contains("@") }

    private val editedUser = OnScreenUser(
            emailProp = emailProp,
            nameProp = nameProp,
            surnameProp = surnameProp
    )

    // check equals() every time User on screen or in memory gets changed
    private val usersEqualProp = listOf(userProp, emailProp, nameProp, surnameProp)
            .mapValueList { _ -> userProp.value.equals(editedUser) }

    val buttonEnabledProp = usersEqualProp.mapWith(emailValidProp) { equal, valid -> !equal && valid }
    val buttonTextProp = usersEqualProp.map { if (it) "Nothing changed" else "Save changes" }
    val debouncedEmail = emailProp.debounced(500, TimeUnit.MILLISECONDS).map { "Debounced e-mail: $it" }

}
```

## ProGuard rules for Android
(assume you depend on `:properties` and `:android-bindings`)

```
# using annotations with 'provided' scope
-dontwarn android.support.annotation.**

# bindings to design lib whish has 'provided' scope
-dontwarn android.support.design.widget.**

# bindings to JavaFX
-dontwarn net.aquadc.properties.fx.JavaFxApplicationThreadExecutorFactory

# keep volatile field names for AtomicFieldUpdater
-keepclassmembernames class net.aquadc.properties.internal.** {
  volatile <fields>;
}
-keepclassmembernames class net.aquadc.properties.android.pref.SharedPreferenceProperty {
  volatile <fields>;
}
```

## FAQ

#### What's the purpose of this library?

The main purpose is MVVM/DataBinding, especially in Android,
where preserving ViewModel state may be quirky.
ViewModel/ViewState can be declared as a set of mappings,
where the values of some properties depend on some other ones.

#### Why not use an existing solution?

* `javafx.beans.property.Property`

  It was the main source of inspiration. But the class hierarchy is too deep and wide,
  looks like a complicated solution for a simple problem.
  Has no support for multithreading. Looks like unsubscription won't take effect during notification.
  
* `android.util.Property`

  A very simple, single-threaded, non-observable thing for animation. Has `ReflectiveProperty` subclass,
  which is close to JavaFX concept (every property is a `Property`), but reflective and thus sad.

* `io.reactivex.BehaviorSubject`
  
  Has no read-only interface. You can either expose an `Observable` (without `get`) or a `BehaviorSubject` (with `get` and `set`).
  Has no single-threaded version.

* `LiveData`
  
  Confined to `Handler`/`Looper` which limits usage and complicates testing.
  It's also an abstract class, which leaves no way of creating different implementations.

* XML data-binding

  Uses XML layouts (inflexible) and code generation (sucks in many ways).
  Ties layouts to hard-coded Java classes, which kills XML reusability.

#### May I rely on source or binary compatibility?

Nope, not now.
Visible API will mostly stay the same, but implementation and binary interface may be seriously changed.
Many functions are `inline`, they return instances of `@PublishedApi internal` classes. These classes are [flyweights](https://en.wikipedia.org/wiki/Flyweight_pattern) implementing many interfaces,
but only one interface should be publicly visible.

#### Where and how should I dispose subscriptions?

When the property is not being observed, it not observes its source and thus not being retained by it.
Consider the following code:

```kt
val someGlobalProp = propertyOf(100)
val mappedProp = someGlobalProp.map { it * 10 }
// mappedProp has no listeners and thus not observes someGlobalProp

println(mappedProp.value) // value calculated ondemand

mappedProp.addChangeListener { ... }
// mappedProp now listens for someGlobalProp changes
// and not eligble for GC until someGlobalProp is not

someGlobalProp.value = 1
// mappedProp value calculated due to original value change
// mappedProp's listener was notified
```

All Android bindings are based on [bindViewTo](https://github.com/Miha-x64/reactive-properties/blob/master/android-bindings/src/main/kotlin/net/aquadc/properties/android/bindings/bind.kt#L14)
which creates a [SafeBinding](https://github.com/Miha-x64/reactive-properties/blob/master/android-bindings/src/main/kotlin/net/aquadc/properties/android/bindings/bind.kt#L29).
It is a [flyweight](https://en.wikipedia.org/wiki/Flyweight_pattern) implemening `View.OnAttachStateChangeListener` and `ChangeListener`.
When view gets attached to window, `SafeBinding` is getting subscribed; when view gets detached,
binding unsubscribes and becomes eligible for garbage collection with the whole view hierarchy.
