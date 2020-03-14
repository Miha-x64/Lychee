
[![Build Status](https://travis-ci.org/Miha-x64/Lychee.svg?branch=master)](https://travis-ci.org/Miha-x64/Lychee)
![Lock-free](https://img.shields.io/badge/%E2%9A%9B-Lock--free-3399aa.svg) 
![Extremely lightweight](https://img.shields.io/badge/🦋-Extremely%20Lightweight-7799cc.svg)
[![Hits-of-Code](https://hitsofcode.com/github/Miha-x64/Lychee)](https://hitsofcode.com/view/github/Miha-x64/Lychee)
[![Channel at Kotlin Slack](https://img.shields.io/static/v1?label=kotlinlang&message=Lychee&color=brightgreen&logo=slack)](https://app.slack.com/client/T09229ZC6/CPVLZ7LBT)
[![Telegram chat](https://img.shields.io/static/v1?label=chat&message=Lychee&color=brightgreen&logo=telegram)](https://t.me/kotlin_lychee)

<!-- abandoned [![Codacy Badge](https://api.codacy.com/project/badge/Grade/89813e3ee28441b3937a76f09e906aef)](https://www.codacy.com/app/Miha-x64/Lychee?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=Miha-x64/Lychee&amp;utm_campaign=Badge_Grade) -->
<!-- abandoned [![codecov](https://codecov.io/gh/Miha-x64/Lychee/branch/master/graph/badge.svg)](https://codecov.io/gh/Miha-x64/Lychee) in module `:properties`, excluding inline functions -->

# Lychee (ex. reactive-properties)

## Adding to a project

[![Download](https://api.bintray.com/packages/miha-x64/maven/net.aquadc.properties%3Aproperties/images/download.svg)](https://bintray.com/miha-x64/maven/net.aquadc.properties%3Aproperties/_latestVersion) Properties

[![Download](https://api.bintray.com/packages/miha-x64/maven/net.aquadc.properties%3Apersistence/images/download.svg)](https://bintray.com/miha-x64/maven/net.aquadc.properties%3Apersistence/_latestVersion) Persistence

[![Download](https://api.bintray.com/packages/miha-x64/maven/net.aquadc.properties%3Aextended-persistence/images/download.svg)](https://bintray.com/miha-x64/maven/net.aquadc.properties%3Aextended-persistence/_latestVersion) Extended Persistence

[![Download](https://api.bintray.com/packages/miha-x64/maven/net.aquadc.properties%3Aandroid-bindings/images/download.svg)](https://bintray.com/miha-x64/maven/net.aquadc.properties%3Aandroid-bindings/_latestVersion) Android Bindings

```
// 'allprojects' section of top-level build.gradle || root of module-level build.gradle
repositories {
    ...
    maven { url 'https://dl.bintray.com/miha-x64/maven' }
}

// module-level build.gradle
dependencies {
    implementation 'net.aquadc.properties:properties:0.0.12' // observables for both JVM and Android
    implementation 'net.aquadc.properties:persistence:0.0.12' // persistence for JVM and Android
    implementation 'net.aquadc.properties:extended-persistence:0.0.12' // partial structs, tuples, either, unsigned, primitive[], token transforms
    implementation 'net.aquadc.properties:android-bindings:0.0.12' // AAR for Android(x): View bindings, Parcel, JsonReader as TokenStream, SharedPreferences as Struct, Handler as Executor
}
```

### ToC
* [Properties](#properties)
* [Other Android data-binding libraries](#other-android-data-binding-libraries)
* [Properties sample](#properties-sample)
* [Sample usage in GUI application](#sample-usage-in-gui-application)
* [Persistence](#persistence) — ui-less data-binding, even for server-side
* [FAQ](#faq)

## Properties

Properties (subjects, observables) inspired by [JavaFX](https://wiki.openjdk.java.net/display/OpenJFX/Main)
and [Vue.js](https://vuejs.org/) MVVM-like approach.
A `Property` provides functionality similar to
`BehaviorSubject` in RxJava, or `Property` in JavaFX,
or `LiveData` in Android Arch.

* Simple and easy-to-use
* Lightweight: persistence + properties + android-bindings define <1500 methods, many of them are `inline fun`s
* zero reflection <small>([the only use of kotlin.reflect](https://github.com/Miha-x64/Lychee/blob/ccea2e165f0da5dbaedac2d3562c4a843614241f/properties/src/main/kotlin/net/aquadc/properties/operatorsInline.kt#L168-L179) is required if you delegate your Kotlin property to a Lychee `Property` and [eliminated by Kotlin 1.3.70+ compiler](https://youtrack.jetbrains.com/issue/KT-14513))</small>
* Extensible: not confined to Android, JavaFX or whatever (want MPP? File an issue with sample use-cases)
* Single-threaded and concurrent (lock-free) properties are supported
* Ready to use MVVM/data-binding for Android and JavaFX
* Sweet with View DSLs like
  [Splitties](https://github.com/LouisCAD/Splitties/) 
  and [TornadoFX](https://github.com/edvin/tornadofx)
* Depends only on Kotlin-stdlib and
  [Kotlin-MPP Collection utils](https://github.com/Miha-x64/Kotlin-MPP_Collection_utils) for overheadless `EnumSet`s
* [Presentation](https://speakerdeck.com/gdg_rnd/mikhail-goriunov-advanced-kotlin-patterns-on-android-properties)
  about properties: initial problem statement and some explanations

## Other Android data-binding libraries

* [agrosner/KBinding](https://github.com/agrosner/KBinding) (MIT): similar to this,
  Observable-based, Android-only, depends on kotlinx.coroutines

* [BennyWang/KBinding](https://github.com/BennyWang/KBinding) (no license):
   Android-only, uses annotation processing, depends on RXJava 1.3

* [LewisRhine/AnkoDataBindingTest](https://github.com/LewisRhine/AnkoDataBindingTest) (no license):
  proof of concept solution from [Data binding in Anko](https://medium.com/lewisrhine/data-binding-in-anko-77cd11408cf9)
  article, Android-only, depends on Anko and AppCompat

* [lightningkite/kotlin-anko-observable](https://github.com/lightningkite/kotlin-anko-observable) (no license):
  Android-only,
  supports easy creation of RecyclerView adapters along with data-binding,
  based on [lightningkite/kotlin-anko](https://github.com/lightningkite/kotlin-anko) (depends on Anko and AppCompat)
  and [lightningkite/kotlin-observable](https://github.com/lightningkite/kotlin-observable)
  (`ObservableProperty<T>` and `ObservableList<T>`);
  [UnknownJoe796/kotlin-components-starter](https://github.com/UnknownJoe796/kotlin-components-starter) (MIT)

* [MarcinMoskala/KotlinAndroidViewBindings](https://github.com/MarcinMoskala/KotlinAndroidViewBindings):
  delegates properties of Views-by-id to to Kotlin properties

## Properties sample

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

Android layout ([Splitties Views DSL](https://github.com/LouisCAD/Splitties/tree/master/modules/views-dsl)):

```kt
setContentView(verticalLayout {
    padding = dip(16)

    addView(editText {
        id = 1 // let view save its state, focus, etc
        hint = "Email"
        bindTextBidirectionally(vm.emailProp)
        bindErrorMessageTo(vm.emailValidProp.map { if (it) null else "E-mail is invalid" })
    })

    addView(editText {
        id = 2
        hint = "Name"
        bindTextBidirectionally(vm.nameProp)
    })

    addView(editText {
        id = 3
        hint = "Surname"
        bindTextBidirectionally(vm.surnameProp)
    })

    addView(button {
        bindEnabledTo(vm.buttonEnabledProp)
        bindTextTo(vm.buttonEnabledProp.map { if (it) "Save changes" else "Nothing changed" })
        setWhenClicked(vm.buttonClickedProp)
        // ^ set flag on action
    })

}.wrapInScrollView())
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
        // 'user' is backed by whatever data source — in-memory, database, SharedPreferences, …
        private val user: TransactionalPropertyStruct<User>
) : PersistableProperties {

    // user input

    // clone 'user' into memory
    private val editableUser = ObservableStruct(user, false)

    // expose properties for View
    val emailProp get() = editableUser prop User.Email
    val nameProp get() = editableUser prop User.Name
    val surnameProp get() = editableUser prop User.Surname

    // handle actions

    val buttonClickedProp = propertyOf(false).clearEachAnd {
        // reset flag and perform action — patch 'user' with values from memory
        user.transaction { t ->
            t.setFrom(editableUser, User.Email + User.Name + User.Surname)
        }
    }

    // preserve/restore state of this ViewModel (for Android)
    override fun saveOrRestore(io: PropertyIo) {
        /*
        When saving state, property values are written to io which is PropertyOutput.
        When restoring, property values are assigned from io which is PropertyInput.
        
        Infix function calls:
        */
        io x emailProp
        io x nameProp
        io x surnameProp
    }

    // some feedback for user actions

    val emailValidProp = emailProp.map { it.contains("@") }

    // compare snapshots
    private val usersDifferProp = user.snapshots().mapWith(editableUser.snapshots(), Objectz.NotEqual)

    val buttonEnabledProp = usersDifferProp and emailValidProp

}
```

## Persistence

The simplest intersection of persistence and databinding is
[`SharedPreferenceProperty`](/src/main/kotlin/net/aquadc/persistence/android/pref/SharedPreferenceProperty.kt)
 (Android): it implements `Property` interface, and stores data inside `SharedPreferences`.

Another thing is [`PersistableProperties`](/src/main/kotlin/net/aquadc/properties/persistence/memento/PersistableProperties.kt):
this interface allows you to save or restore the state of a ViewModel using `ByteArray` via a single method
without declaring symmetrical, bolierplate and error-prone `writeToParcel` and `createFromParcel` methods
and without any Android dependencies.

```kt
override fun saveOrRestore(io: PropertyIo) {    
    io x prop1
    io x prop2
    io x prop3
}
```

But the most interesting and reusable things are all around `Struct`, `Schema`, and `DataType`.

[`Schema`](/src/main/kotlin/net/aquadc/persistence/struct/schema.kt)
is a definition of a data bag with named, ordered, typed fields.
```kt
object Player : Schema<Player>() {
    val Name = "name" let string
    val Surname = "surname" let string
    val Score = "score".mut(i32, default = 0)
}
```
`let` function creates an immutable field definition, `mut` declares a mutable one.
`Name`, `Surname` and `Score` are field definitions,
similarly to [typed key](https://matklad.github.io/2018/05/24/typed-key-pattern.html) pattern.

[`Struct`](/src/main/kotlin/net/aquadc/persistence/struct/struct.kt)
is an instance carrying some data according to a certain `Schema`.
```kt
val player: StructSnapshot<Player> = Player { p ->
    p[Name] = "John"
    p[Surname] = "Galt"
}
```
field values can be accessed with indexing operator:
```kt
assertEquals(0, player[Player.Score]) // Score is equal to the default value which was set in Schema
```
The builder-function is recommended for setting all required fields:
```kt
fun Player(name: String, surname: String) = Player { p ->
    p[Name] = name
    p[Surname] = surname
}
```


Okay, the `player` instance is fully immutable, but what if we want a mutable observable one:
```kt
val observablePlayer = ObservableStruct(player)
val scoreProp: Property<Int> = observablePlayer prop Player.Score
someTextView.bindTextTo(scoreProp.map(CharSequencez.ValueOf))

// both mutate the same text in-memory int value and a text field:
scoreProp.value = 10
observablePlayer[Player.Score] = 20
```

[`SharedPreferencesStruct`](/src/main/kotlin/net/aquadc/persistence/android/pref/SharedPreferencesStruct.kt)
(Android) has very similar interface, but can be mutated only inside a transaction:
```kt
// this will copy data from player into the given SharedPreferences instance
val storedPlayer = SharedPreferencesStruct(player, getSharedPreferences(…))
val scoreProp = storedPlayer prop Player.Score
val score = storedPlayer[Player.Score]
// and this is different:
storedPlayer.transaction { p ->
    p[Score] = 100500
}
```

There is JSON support built on top of `android.util.JsonReader/Writer`:
```kt
// reading
val jsonPlayer = """{"name":"Hank","surname":"Rearden"}"""
        .reader() // StringReader
        .json() // JsonReader
        .tokens() // TokenStream
        .readAs(Player) // StructSnapshot<Player>

val jsonPlayers = """[ {"name":"Hank","surname":"Rearden"}, ... ]"""
        .reader().json().tokens().readListOf(Player)

// writing
type.tokensFrom(value).writeTo(JsonWriter(…))
```

[`TokenStream`](/net/aquadc/persistence/tokens/TokenStream.kt)
abstraction is helpful for changing schema of provided data (instead of using 'mappers'), see
[sample transform usage](https://github.com/Miha-x64/Lychee/blob/master/android-bindings/src/test/kotlin/promo.kt#L61-L69).

Also, [SQL support](/sql/) is currently being developed.

## FAQ

#### What's the purpose of this library?

The main purpose is MVVM/DataBinding, especially in Android
where preserving ViewModel state may be quirky.
ViewModel/ViewState can be declared as a set of mappings,
where the values of some properties depend on some other ones.

#### Why not use an existing solution?

* `javafx.beans.property.Property`

  It was the main source of inspiration. But the class hierarchy is too deep and wide,
  looks like a complicated solution for a simple problem.
  Has no support for multithreading. Looks like unsubscription won't take effect during notification.
  
* `android.util.Property`

  A very trivial thing for animation. Has `ReflectiveProperty` subclass which is close to JavaFX concept
  (every property is a `Property`) not observable and reflective (and thus sad).

* `io.reactivex.BehaviorSubject`
  
  Has no read-only interface. You can either expose an `Observable` (without `get`) or a `BehaviorSubject` (with `get` and `set`).
  Has no single-threaded version. Part of non-modular, poorly designed RxJava.

* `LiveData`
  
  Confined to `Handler`/`Looper` which limits usage to Android only and complicates testing.
  It's also an abstract class, thus customization is limited.

* XML data-binding

  Uses XML layouts (inflexible) and code generation (sucks in many ways, still breaks regularly).
  Ties layouts to hard-coded Java classes thus killing XML reusability.

#### Why version is 0.0.x?

1.x versions mean stable and compatible API/ABI.
Lychee interface is not volatile, but is a subject to change, move, rename.
This means that it can be used in production apps (migrations are easy), but libraries should update as fast as Lychee does.
If your library does (or going to) depend on Lychee, file an issue:
I will take into account which APIs do you use and maybe add a link to your library.

`0.1.0` version is to be released after adding mutational and [linearization](https://github.com/Kotlin/kotlinx-lincheck) tests,
`1.0.0` is planned after dropping workarounds for
  [KT-24981: @JvmSynthetic for classes](https://youtrack.jetbrains.com/issue/KT-24981),
  [KT-24067: type checking and casting of multi-arity function objects](https://youtrack.jetbrains.com/issue/KT-24067),
  and when `inline` classes come more stable (e. g.
    [KT-31431 JVM backend failure on inline functions of inline classes](https://youtrack.jetbrains.com/issue/KT-31431),
    [KT-33224: @JvmName for function with inline class parameter](https://youtrack.jetbrains.com/issue/KT-33224)
  ).

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

All Android bindings are based on [bindViewTo](/android-bindings/src/main/kotlin/net/aquadc/properties/android/bindings/bind.kt#L25)
 which creates a [Binding](/android-bindings/src/main/kotlin/net/aquadc/properties/android/bindings/bind.kt#L64).
It is a [flyweight](https://en.wikipedia.org/wiki/Flyweight_pattern) observing
View attached state, Activity started state, and Property changes.
When view gets attached to window, `Binding` is getting subscribed
to Activity lifecycle via [Lifecycle-Watcher](/android-bindings/src/main/kotlin/net/aquadc/properties/android/Lifecycle-Watcher.kt#L17);
when Activity is started, `Binding` listens for data source.
When Activity gets stopped or View gets detached,
binding unsubscribes and becomes eligible for garbage collection
along with the whole View hierarchy.

#### Is there anything similar to RxJava's Single?

Nope. Java since v. 1.8 contains `CompletableFuture` for async computations.
It also was backported to ~~Java 6.5~~ _Android_ a long time ago.
Note that it is distributed under 'GPL v. 2.0 with classpath exception'
which is not as restrictive as GPL itself.

You can mutate concurrent properties from background threads (e.g. in the end of async computations),
triggering UI state change as needed and without any callbacks.

#### ProGuard rules for Android?

[Here you are.](/android-sample/proguard-rules.pro#L30-L55)
