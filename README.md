
[![Build Status](https://travis-ci.org/Miha-x64/reactive-properties.svg?branch=master)](https://travis-ci.org/Miha-x64/reactive-properties)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/89813e3ee28441b3937a76f09e906aef)](https://www.codacy.com/app/Miha-x64/reactive-properties?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=Miha-x64/reactive-properties&amp;utm_campaign=Badge_Grade)
![Lock-free](https://img.shields.io/badge/%E2%9A%9B-Lock--free-3399aa.svg) 
![Extremely lightweight](https://img.shields.io/badge/🦋-Extremely%20Lightweight-7799cc.svg)

<!-- abandoned [![codecov](https://codecov.io/gh/Miha-x64/reactive-properties/branch/master/graph/badge.svg)](https://codecov.io/gh/Miha-x64/reactive-properties) in module `:properties`, excluding inline functions -->

## Adding to a project

[![Download](https://api.bintray.com/packages/miha-x64/maven/net.aquadc.properties%3Aproperties/images/download.svg)](https://bintray.com/miha-x64/maven/net.aquadc.properties%3Aproperties/_latestVersion) Reactive Properties

[![Download](https://api.bintray.com/packages/miha-x64/maven/net.aquadc.properties%3Apersistence/images/download.svg)](https://bintray.com/miha-x64/maven/net.aquadc.properties%3Apersistence/_latestVersion) Persistence

[![Download](https://api.bintray.com/packages/miha-x64/maven/net.aquadc.properties%3Aandroid-bindings/images/download.svg)](https://bintray.com/miha-x64/maven/net.aquadc.properties%3Aandroid-bindings/_latestVersion) Android Bindings

```
// 'allprojects' section of top-level build.gradle || root of module-level build.gradle
repositories {
    ...
    maven { url 'https://dl.bintray.com/miha-x64/maven' }
}

// module-level build.gradle
dependencies {
    implementation 'net.aquadc.properties:properties:0.0.9' // core, both for JVM and Android
    implementation 'net.aquadc.properties:persistence:0.0.9' // persistence for JVM and Android
    implementation 'net.aquadc.properties:android-bindings:0.0.9' // Android-only AAR package
}
```

# reactive-properties

Properties (subjects) inspired by [JavaFX](https://wiki.openjdk.java.net/display/OpenJFX/Main)
and [Vue.js](https://vuejs.org/) MVVM-like approach.
A `Property` provides functionality similar to
`BehaviorSubject` in RxJava, or `Property` in JavaFX,
or `LiveData` in Android Arch.

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
  Android-only,
  supports easy creation of RecyclerView adapters along with data-binding,
  based on [lightningkite/kotlin-anko](https://github.com/lightningkite/kotlin-anko) (depends on Anko and AppCompat)
  and [lightningkite/kotlin-observable](https://github.com/lightningkite/kotlin-observable)
  (`ObservableProperty<T>` and `ObservableList<T>`);
  [UnknownJoe796/kotlin-components-starter](https://github.com/UnknownJoe796/kotlin-components-starter)

* [MarcinMoskala/KotlinAndroidViewBindings](https://github.com/MarcinMoskala/KotlinAndroidViewBindings)
  — property delegation to view by id

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
        // 'user' is backed by whatever data source — in-memory, database, file, ...
        private val user: TransactionalPropertyStruct<User>
) : PersistableProperties {

    // user input

    // clone 'user' into memory
    private val editableUser = ObservableStruct(user, false)

    // expose properties for View
    val emailProp get() = editableUser prop User.Email
    val nameProp get() = editableUser prop User.Name
    val surnameProp get() = editableUser prop User.Surname

    val buttonClickedProp = propertyOf(false).also {
        // reset flag and perform action — patch 'user' with values from memory
        it.clearEachAnd {
            user.transaction { t ->
                t.setFrom(editableUser, User.Email + User.Name + User.Surname)
            }
        }
    }

    // preserve/restore state of this ViewModel (for Android)
    override fun saveOrRestore(io: PropertyIo) {
        io x emailProp
        io x nameProp
        io x surnameProp
    }

    // a feedback for user actions

    val emailValidProp = emailProp.map { it.contains("@") }

    // compare snapshots
    private val usersDifferProp = user.snapshots().mapWith(editableUser.snapshots(), Objectz.NotEqual)

    val buttonEnabledProp = usersDifferProp and emailValidProp

}
```

## Persistence

The simplest intersection of persistence and databinding is `SharedPreferenceProperty` (Android):
it implements `Property` interface, and stores data inside `SharedPreferences`.

Another thing is `PersistableProperties`: this interface allows you
to save or restore the state of a ViewModel using `ByteArray` via a single method
without declaring symmetrical, bolierplate and error-prone `writeToParcel` and `createFromParcel` methods.

But the most interesting and reusable things are around `Struct`, `Schema`, and `DataType`.

`Schema` is a definition of a data bag with named, ordered, typed fields.
```kt
object Player : Schema<Player>() {
    val Name = "name" let string
    val Surname = "surname" let string
    val Score = "score".mut(int, default = 0)
}
```
`let` function creates an immutable field definition, `mut` declares a mutable one.

`Struct` is an instance carrying some data according to a certain `Schema`.
```kt
val player: StructSnapshot<Player> = Player.build { p ->
    p[Name] = "John"
    p[Surname] = "Galt"
}
```
field values can be accessed with indexing operator:
```kt
assertEquals(0, player[Player.Score]) // Score is equal to the default value which was set in Schema
```
Okay, the `player` is fully immutable, but we want a mutable observable instance:
```kt
val observablePlayer = ObservableStruct(player)
val scoreProp: Property<Int> = observablePlayer prop Player.Score
someTextView.bindTextTo(scoreProp.map(CharSequencez.ValueOf))

// both mutate the same text in-memory int value and a text field:
scoreProp.value = 10
observablePlayer[Player.Score] = 20
```

`SharedPreferencesStruct` (Android) has very similar interface, but can be mutated only inside a transaction:
```kt
// this will copy data from player into the given SharedPreferences instance
val storedPlayer = SharedPreferencesStruct(player, getSharedPreferences(...))
val scoreProp = storedPlayer prop Player.Score
val score = storedPlayer[Player.Score]
// ans this is different:
storedPlayer.transaction { p ->
    p[Score] = 100500
}
```


## ProGuard rules for Android
(for `:persistence`, `:properties` and `:android-bindings`)

```
# libs with compileOnly scope
-dontwarn android.support.annotation.**
-dontwarn android.support.v7.widget.**
-dontwarn android.support.design.widget.**
-dontwarn okio.**

# required by EnumSet
-keepclassmembers enum * {
  public static **[] values();
}

# bindings to JavaFX
-dontwarn net.aquadc.properties.fx.JavaFxApplicationThreadExecutorFactory
-assumenosideeffects class net.aquadc.properties.executor.PlatformExecutors {
    private void findFxFactory(java.util.ArrayList); # bindings to JavaFX
    private void findFjFactory(java.util.ArrayList); # If you're not going to addChangeListener() on ForkJoin threads
}

# debug-only assertions for enforcing type-safety
-assumenosideeffects class net.aquadc.persistence.type.SimpleNoOp {
    private void sanityCheck(java.lang.Object);
}

# https://sourceforge.net/p/proguard/bugs/660/
-keepclassmembernames class net.aquadc.properties.internal.** {
  volatile <fields>;
}
-keepclassmembernames class net.aquadc.properties.android.persistence.pref.SharedPreferenceProperty {
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

println(mappedProp.value) // value calculated on demand

mappedProp.addChangeListener { ... }
// mappedProp now listens for someGlobalProp changes
// and not eligble for GC until someGlobalProp is not

someGlobalProp.value = 1
// mappedProp value calculated due to original value change
// mappedProp's listener was notified
```

All Android bindings are based on [bindViewTo](/android-bindings/src/main/kotlin/net/aquadc/properties/android/bindings/bind.kt#L19)
which creates a [Binding](/android-bindings/src/main/kotlin/net/aquadc/properties/android/bindings/bind.kt#L46).
It is a [flyweight](https://en.wikipedia.org/wiki/Flyweight_pattern) implemening `View.OnAttachStateChangeListener`, `ChangeListener` and `(Boolean) -> Unit`.
When view gets attached to window, `Binding` is getting subscribed
to Activity lifecycle via [Lifecycle-Watcher](/android-bindings/src/main/kotlin/net/aquadc/properties/android/Lifecycle-Watcher.kt#L17);
when Activity is started, `Binding` listens for data source.
When Activity gets stopped or View gets detached,
binding unsubscribes and becomes eligible for garbage collection
along with the whole view hierarchy.

#### Is there anything similar to RxJava's Single?

Nope. Java since v. 1.8 contains `CompletableFuture` for async computations.
It also was backported to ~~Java 6.5~~ _Android_ a long time ago.
Note that it is distributed under 'GPL v. 2.0 with classpath exception'
which is not as restrictive as GPL itself.

You can mutate concurrent properties in the end of async computations (i. e. from CompletableFuture's Consumer),
triggering UI state change as needed.
