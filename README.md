[![Build Status](https://travis-ci.org/Miha-x64/Lychee.svg?branch=master)](https://travis-ci.org/Miha-x64/Lychee)
![Extremely lightweight](https://img.shields.io/badge/🦋-Extremely%20Lightweight-7799cc.svg)
[![Hits-of-Code](https://hitsofcode.com/github/Miha-x64/Lychee)](https://hitsofcode.com/view/github/Miha-x64/Lychee)
[![Kotlin 1.5](https://img.shields.io/badge/kotlin-1.5-blue.svg)](http://kotlinlang.org)
[![Awesome Kotlin](https://kotlin.link/awesome-kotlin.svg)](https://kotlin.link)
[![Channel at Kotlin Slack](https://img.shields.io/static/v1?label=kotlinlang&message=Lychee&color=brightgreen&logo=slack)](https://app.slack.com/client/T09229ZC6/CPVLZ7LBT)
[![Telegram chat](https://img.shields.io/static/v1?label=chat&message=Lychee&color=brightgreen&logo=telegram)](https://t.me/kotlin_lychee)

<!-- abandoned [![Codacy Badge](https://api.codacy.com/project/badge/Grade/89813e3ee28441b3937a76f09e906aef)](https://www.codacy.com/app/Miha-x64/Lychee?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=Miha-x64/Lychee&amp;utm_campaign=Badge_Grade) -->
<!-- abandoned [![codecov](https://codecov.io/gh/Miha-x64/Lychee/branch/master/graph/badge.svg)](https://codecov.io/gh/Miha-x64/Lychee) in module `:properties`, excluding inline functions -->

# Lychee (ex. reactive-properties)

Lychee is a library to rule all the data.

### ToC
* [Approach to declaring data](#approach-to-declaring-data)
* [Properties](#properties)
* [Other data-binding libraries](#other-data-binding-libraries)
* [Properties sample](#properties-sample)
* [Sample usage in GUI application](#sample-usage-in-gui-application)
* [Persistence and Android](#persistence-and-android)
* [SQL](#sql-experimental) <!-- TODO other SQL libraries -->
* [HTTP](#http)
* [FAQ](#faq)
* [Adding to a project](#adding-to-a-project)

### Approach to declaring data

Typically, we declare data using classes:
```kt
/*data*/ class Player(
    val name: String,
    val surname: String,
    var score: Int,
)
```
But there are some mistakes in the example above:
* there aren't any convenient ways to manipulate the properties of arbitrary classes:
  * reflection does not play well with ProGuard/R8 and Graal,
  * kapt is slow and does not play well with separate compilation,
  * both break encapsulation by exposing field names (or values of annotations) as serialization interface,
  * none of them knows precisely how to serialize values, they just try to guess according to field types,
  * there are no standard annotations:
    every JSON library has its own annotations (Gson: `@SerializedName` and `@TypeAdapter`),
    every ORM, ActiveRecord, or another database-related thing has its own, too,
  * `TypeAdapter` concept is cursed:
    every library tries to support all standard types
    (how often do you need to store `AtomicIntegerArray`? Gson has built-in support for it, this is crazy!),
    and tree shakers (a.k.a. dead code eliminators) cannot figure out which ones are actually used
    (this requires deep understanding of reflection API and `Map<Type, TypeAdapter>` machinery,
    10+ years of ProGuard were not enough to get this deep);
* along with interface (property names and types),
  this also declares implementation details (backing fields).
  Thus, you're getting only in-memory representation.
  (To workaround the issue, Realm, for example,
  extends your classes so getters&setters are overridden while fields are unused,
  and rewrites your bare field accesses, if any, to use getters&setters.)
  Theoretically, this can be fixed by extracting `interface`:
    ```kt
    interface Player {
        val name: String
        val surname: String
        var score: Int
     // fun copy()? can we also ask to implement equals()? no.
    }
    data class MemoryPlayer(override val …) : Player
    class JsonPlayer(private val json: JsonObject) : Player {
        override val name: String get() = json.getString("name")
        …
    }
    class SqlPlayer(private val connection: Connection) : Player {
        override val name: String get() = connection.createStatement()…
    }
    ```

   but implementations are 146% boilerplate;
* no mutability control. `var score` is mutable but not observable;
* `hashCode`, `equals`, and `toString` contain generated bytecode fully consisting of boilerplate;
* data class `copy` not only consists of boilerplate
  but also becomes binary incompatible after every primary constructor change;
* data class `componentN`s are pure evil in 99% cases:
  destructuring is good with positional things like `Pair` or `Triple` but not with named properties.

`:persistence` module provides the solution. Interface is declared by inheriting `Schema`:
```kt
object Player : Schema<Player>() {
    val Name = "name" let string
    val Surname = "surname" let string
    val Score = "score".mut(i32, default = 0)
}
```
Here, `Player`, `string`, and `i32` (not `int` because it's Java keyword) are all subtypes of `DataType`.
Thus, they declare how to store data both in-memory and on wire.
`Name`, `Surname`, and `Score` are field definitions, two immutable and one mutable,
based on [typed key](https://matklad.github.io/2018/05/24/typed-key-pattern.html) pattern.

Implementations are subtypes of `Struct<SCHEMA>`,
so they implement storage machinery while staying decoupled
from data schema:
```kt
val player: StructSnapshot<Player> = Player { p ->
    p[Name] = "John"
    p[Surname] = "Galt"
    // Score gets its default value.
}
```
`StructSnapshot` is immutable (and very cheap: it is an Array, not a HashMap) implementation. It can only be read from:
```kt
assertEquals(0, player[Player.Score])
```
Here, `Player {}` is `SCHEMA.invoke(build: SCHEMA.() -> Unit)` function which tries to mimic struct literal;
`p` is `StructBuilder<SCHEMA>`–a fully mutable temporary object.
`Struct`s implement `hashCode`, `equals`, `toString`, and `copy` of this kind: `player.copy { it[Score] = 9000 }`.
It creates new `StructBuilder` and passes it to the function you provide.
(Similar thing is called `newBuilder` in OkHttp, and `buildUpon` in `android.net.Uri`.)

There's also a good practice to implement a constructor function
which gives less chance of forgetting to specify required field values:
```kt
fun Player(name: String, surname: String) = Player { p ->
    p[Name] = name
    p[Surname] = surname
}
```

## Properties

Properties (subjects, observables) inspired by [JavaFX](https://wiki.openjdk.java.net/display/OpenJFX/Main)
and [Vue.js](https://vuejs.org/) MVVM-like approach are available in `:properties` module.
A `Property` provides functionality similar to
`BehaviorSubject` in RxJava, or `Property` in JavaFX,
or `LiveData` in Android Arch.

* Simple and easy-to-use
* Lightweight: persistence + properties + android-bindings define around 1500 methods
  including easy-to-shrink `inline fun`s and `value class`es
* zero reflection <small>([the only use of kotlin.reflect](https://github.com/Miha-x64/Lychee/blob/ccea2e165f0da5dbaedac2d3562c4a843614241f/properties/src/main/kotlin/net/aquadc/properties/operatorsInline.kt#L168-L179) is required if you delegate your Kotlin property to a Lychee `Property` and [eliminated by Kotlin 1.3.70+ compiler](https://youtrack.jetbrains.com/issue/KT-14513))</small>
* Extensible: not confined to Android, JavaFX or whatever (want MPP? File an issue with sample use-cases)
* Single-threaded and concurrent (lock-free) implementations
* Ready to use Android bindings like `tv.bindTextTo(prop)`, not `ld.observe(viewLifecycleOwner) { tv.text = it }`
* Some bindings for JavaFX
* Sweet with View DSLs like
  [Splitties](https://github.com/LouisCAD/Splitties/) 
  and [TornadoFX](https://github.com/edvin/tornadofx)
* Depends only on Kotlin-stdlib and
  [Kotlin-MPP Collection utils](https://github.com/Miha-x64/Kotlin-MPP_Collection_utils) for overheadless `EnumSet`s
* [Presentation](https://speakerdeck.com/gdg_rnd/mikhail-goriunov-advanced-kotlin-patterns-on-android-properties)
  about properties: initial problem statement and some explanations

With `:persistence` + `:properties`, it's also possible to observe mutable fields:
```kt
val observablePlayer = ObservableStruct(player)
val scoreProp: Property<Int> = observablePlayer prop Player.Score
someTextView.bindTextTo(scoreProp.map(CharSequencez.ValueOf)) // bind to UI, for example

// both mutate the same text in-memory int value and a text field:
scoreProp.value = 10
observablePlayer[Player.Score] = 20
```


## Other data-binding libraries
to explain why I've rolled my own:

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

* [MarcinMoskala/KotlinAndroidViewBindings](https://github.com/MarcinMoskala/KotlinAndroidViewBindings) (Apache 2.0):
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

JavaFX layout (using JFoenix):

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
    // user is backed by arbitrary data source: in-memory, database, SharedPreferences, …
    private val user: TransactionalPropertyStruct<User>
) : PersistableProperties {

    // user input

    // clone user into memory
    private val editableUser = ObservableStruct(user, false)

    // expose properties for View
    val emailProp get() = editableUser prop User.Email
    val nameProp get() = editableUser prop User.Name
    val surnameProp get() = editableUser prop User.Surname

    // handle actions

    val buttonClickedProp = propertyOf(false).clearEachAnd {
        // reset flag and perform action—patch user with values from memory
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

## Persistence and Android

Things available in `:android-bindings`:

* [`SharedPreferenceProperty`](/android-bindings/src/main/kotlin/net/aquadc/persistence/android/pref/SharedPreferenceProperty.kt)
 implements `Property` interface, and stores data inside `SharedPreferences`;

* [`SharedPreferencesStruct`](android-bindings/src/main/kotlin/net/aquadc/persistence/android/pref/SharedPreferencesStruct.kt)
  is observable struct stored in `SharedPreferences`;

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

* implementing [`PersistableProperties`](/properties/src/main/kotlin/net/aquadc/properties/persistence/memento/PersistableProperties.kt)
helps you to save or restore the state of a ViewModel to `ByteArray`/`Parcel` by implementing a single method,
without declaring symmetrical, bolierplate, and error-prone `writeToParcel` and `createFromParcel` methods
and without having Android dependencies:

```kt
class SomeViewModel : PersistableProperties {
    …
    override fun saveOrRestore(io: PropertyIo) {    
        io x prop1
        io x prop2
        io x prop3
    }
}
```
see full [save & restore example](/samples/android-sample/src/main/kotlin/net/aquadc/propertiesSampleApp/MainActivity.kt).

* JSON support built on top of `android.util.JsonReader/Writer`:
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

[`TokenStream`](/persistence/src/main/kotlin/net/aquadc/persistence/tokens/TokenStream.kt)
abstraction is an iterator over tokens and it's helpful for
changing schema of provided data (instead of using “mappers”),
see [sample transform usage](/android-bindings/src/test/kotlin/promo.kt#L61-L69).

## Android `RemoteViews`

`RemoteViews` API differs from normal `View`s API.
Thus, `:android-bindings` module provides separate API for this. For example,
```kt
RemoteViews(packageName, R.layout.notification).bind(
    android.R.id.text1 textTo vm.nameProp,
    android.R.id.text2 textTo vm.emailProp,
)
```
returns you a `Property<RemoteViews>`, so you can just observe it
and update notification on every change.

## SQL (experimental)

`:sql` module provides `Table`, a wrapper over `Schema`:
```kt
// trivial table. Primary key column is not mentioned within Schema
val Players = tableOf(Player, "players", "_id", i64)
```
With `Session` (implementations: Android-specific `SqliteSession`, ~~general-purpose `JdbcSession`~~ DON'T USE THIS SHIT TILL THE NEXT RELEASE), you're getting
* SQL templates:
```kt
val selectNameEmailBySmth = Query(
    "SELECT a.name, b.email FROM anywhere a JOIN anything b WHERE smth = ?",
    /* argument */ string,
    // return a list of positionally bound string-to-string tuples:
    structs(projection(string, string), BindBy.Position)
) // Session.(String) -> List<Struct<Tuple<String, …, String, …>>>

val updateNameByEmail = Mutation(
    "UPDATE users SET name = ? WHERE email = ?",
    string, string,
    execute()
) // Transaction.(String, String) -> Unit
```

(To be clear, the receivers are not exactly Session and Transaction. You can call a mutation just on a session, or query in a transaction.)

* Triggers:
```kt
val listener = session.observe(
    UserTable to TriggerEvent.INSERT,
    UserTable to TriggerEvent.DELETE,
) { report ->
    val userChanges = report.of(UserTable)
    println("+" + userChanges.inserted.size)
    println("-" + userChanges.removed.size)
}
…
listener.close() // unsubscribe
```

## HTTP

HTTP is ~~unfortunately~~ the most popular application layer protocol.
Its abilities are not restricted to passing binary or JSON bodies:
there are headers, query parameters, form fields, multipart, and more.

With `:http`, you can declare endpoints using some of `DataType`s from `:persistence`:
```kt
val user = GET("/user/{role}/",
    Header("X-Token"), Path("role"), Query("id", uuid),
    Response<String>())
```
This gives you several features:
* HTTP client templates: `val getUser = okHttpClient.template(baseUrl, user, deferred(::parseUser))` =>
  `(token: String, role: String, id: UUID) -> Deferred<String>`.
  Here you define `parseUser` yourself. Thus, you are free to handle responses as you want:
  throw exception for non-2xx responses,
  or return `Either<HttpException, ResponseEntity>`,
  or ignore response code at all and just parse response body;  
* server-side type-safe routing:
  `undertowRoutingHandler.add(user, ::respond, ::respondBadRequest) { token, role, id -> "response" }`;
* link generation: if endpoint declaration uses GET method
  and does not contain headers, it is possible to build URL:
```kt
GET("/user/{role}/", Path("role"), Query("id", uuid))
    .url(baseUrl, "admin", UUID.randomUUID())
    // => //user/admin/?id=0b46b157-84b9-474c-83bb-76c2ddf58e75
```

**Hey, have you just reinvented Retrofit?**

Well, yes, but actually no. Retrofit
* works only on client-side,
* requires method return types (Call, Observable, Deferred) to be tied to async framework,
* promotes Service-style interfaces.

Lychee-HTTP, on the other side,
* allows `Endpoint`s to be both invoked from client-side and implemented at server-side,
* decouples async wrapper from return value,
* httpClient.template(endpoint) returns a function, server-side endpoint handler is a funcion,
  thus, no Services/Controllers.

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
  [KT-24067: type checking and casting of multi-arity function objects](https://youtrack.jetbrains.com/issue/KT-24067)
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

#### How much black magic do you use under the hood?

Some operator overloading, some value classes, several compilation error suppressions, tons of unchecked casts.
No reflection, zero annotation processing.
If you encounter any problems, they most likely will be related to type inference or Java interop.

#### Is there anything similar to RxJava's Single?

Nope. Java since v. 1.8 contains `CompletableFuture` for async computations.
It also was backported to ~~Java 6.5~~ _Android_ a long time ago.
Note that it is distributed under “GPL v. 2.0 with classpath exception”
which is not as restrictive as GPL itself.

You can mutate concurrent properties from background threads (e.g. in the end of async computations),
triggering UI state change as needed and without any callbacks.

#### ProGuard rules for Android?

[Here you are.](/samples/android-sample/proguard-rules.pro#L30-L55)

## Adding to a project

[![Download](https://maven-badges.herokuapp.com/maven-central/su.lychee/properties/badge.svg?style=flat)](https://search.maven.org/artifact/su.lychee/properties/0.0.17/jar) Properties

[![Download](https://maven-badges.herokuapp.com/maven-central/su.lychee/persistence/badge.svg?style=flat)](https://search.maven.org/artifact/su.lychee/persistence/0.0.17/jar) Persistence

[![Download](https://maven-badges.herokuapp.com/maven-central/su.lychee/extended-persistence/badge.svg?style=flat)](https://search.maven.org/artifact/su.lychee/extended-persistence/0.0.17/jar) Extended Persistence

[![Download](https://maven-badges.herokuapp.com/maven-central/su.lychee/android-bindings/badge.svg?style=flat)](https://search.maven.org/artifact/su.lychee/android-bindings/0.0.17/aar) Android Bindings

[![Download](https://maven-badges.herokuapp.com/maven-central/su.lychee/http/badge.svg?style=flat)](https://search.maven.org/artifact/su.lychee/http/0.0.17/jar) HTTP

[![Download](https://maven-badges.herokuapp.com/maven-central/su.lychee/sql/badge.svg?style=flat)](https://search.maven.org/artifact/su.lychee/sql/0.0.17/jar) SQL (experimental)

[![Download](https://maven-badges.herokuapp.com/maven-central/su.lychee/android-json/badge.svg?style=flat)](https://search.maven.org/artifact/su.lychee/android-json/0.0.17/jar) Android JSON

[![Download](https://maven-badges.herokuapp.com/maven-central/su.lychee/android-json-on-jvm/badge.svg?style=flat)](https://search.maven.org/artifact/su.lychee/android-json-on-jvm/0.0.17/jar) Android JSON on JVM

```groovy
// `allprojects` section of top-level build.gradle || root of module-level build.gradle
repositories {
    ...
    mavenCentral()
    maven { url "https://jitpack.io" } // our dependency, Collection-utils, still lives there
}

// module-level build.gradle
dependencies {
    // I am a bit dumb, so with my first publication on Maven Central I ended up having empty <dependencies> for some of artifacts
    implementation "com.github.Miha-x64.Kotlin-MPP_Collection_utils:Collection-utils-jvm:1.0-alpha05"
  
    def lychee = '0.0.17'
//  val lychee = "0.0.17"
    implementation("su.lychee:properties:$lychee") // observables for both JVM and Android
    implementation("su.lychee:persistence:$lychee") // persistence for JVM and Android
    implementation("su.lychee:extended-persistence:$lychee") // partial structs, tuples, either, unsigned, primitive[], token transforms
    implementation("su.lychee:android-bindings:$lychee") // AAR for Android(x): View bindings, Parcel, SharedPreferences as Struct, Handler as Executor
    implementation("su.lychee:android-json:$lychee") // android.util.JsonReader as TokenStream
    implementation("su.lychee:android-json-on-jvm:$lychee") // implements android.util.JsonReader for server and desktop, use with android-json outside of Android 
    implementation("su.lychee:sql:$lychee") // observable SQL and SQL templates
    implementation("su.lychee:http:$lychee") // RPC over HTTP: client-side HTTP templates, server-side routing, type-safe link generator
}
```
