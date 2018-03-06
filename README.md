
[![Build Status](https://travis-ci.org/Miha-x64/reactive-properties.svg?branch=master)](https://travis-ci.org/Miha-x64/reactive-properties)

Properties: [![Download](https://api.bintray.com/packages/miha-x64/maven/net.aquadc.properties%3Aproperties/images/download.svg)](https://bintray.com/miha-x64/maven/net.aquadc.properties%3Aproperties/_latestVersion)

Android bindings: [![Download](https://api.bintray.com/packages/miha-x64/maven/net.aquadc.properties%3Aandroid-bindings/images/download.svg)](https://bintray.com/miha-x64/maven/net.aquadc.properties%3Aandroid-bindings/_latestVersion) 

# reactive-properties

Properties (subjects) inspired by JavaFX MVVM-like approach.
* Lightweight
* Contains both unsynchronized and concurrent implementations
* MVVM / data-binding for Android and JavaFX
* Sweeter with [Anko-layouts](https://github.com/Kotlin/anko#anko-layouts-wiki) 
  and [TornadoFX](https://github.com/edvin/tornadofx)
* Depends only on Kotlin-stdlib
* [Presentation](https://speakerdeck.com/gdg_rnd/mikhail-goriunov-advanced-kotlin-patterns-on-android-properties)
   — problem statement, explanations

## Other solutions

* [agrosner/KBinding](https://github.com/agrosner/KBinding) (MIT) — similar to this,
  Observable-based, Android-only, depends on coroutines
* [BennyWang/KBinding](https://github.com/BennyWang/KBinding) (no license) —
  Android-only, based on annotation processing, depends on RXJava 1.3,
* [LewisRhine/AnkoDataBindingTest](https://github.com/LewisRhine/AnkoDataBindingTest)
   (no license) 
   — simple solution from [Data binding in Anko](https://medium.com/lewisrhine/data-binding-in-anko-77cd11408cf9)
   article, Android-only, depends on Anko and AppCompat

## Sample

```kt
val prop = concurrentMutablePropertyOf(1)
val mapped = prop.map { 10 * it }
assertEquals(10, mapped.value)

prop.value = 5
assertEquals(50, mapped.value)


val tru = concurrentMutablePropertyOf(true)
assertEquals(false, (!tru).value)
```

## Sample usage in GUI application

Anko layout for Android:

```kt
verticalLayout {
    padding = dip(16)

    editText {
        hint = "Email"
        bindTextBidirectionally(vm.emailProp)
        bindErrorMessageTo(vm.emailValidProp.map { if (it) null else "E-mail is invalid" })
    }

    editText {
        hint = "Name"
        bindTextBidirectionally(vm.nameProp)
    }

    editText {
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
val emailProp = unsynchronizedMutablePropertyOf(userProp.value.email)
val nameProp = unsynchronizedMutablePropertyOf(userProp.value.name)
val surnameProp = unsynchronizedMutablePropertyOf(userProp.value.surname)
val buttonClickedProp = unsynchronizedMutablePropertyOf(false)

val emailValidProp = unsynchronizedMutablePropertyOf(false)
val buttonEnabledProp = unsynchronizedMutablePropertyOf(false)
val buttonTextProp = unsynchronizedMutablePropertyOf("")

private val editedUser = OnScreenUser(
        emailProp = emailProp,
        nameProp = nameProp,
        surnameProp = surnameProp
)

init {
    val usersEqualProp = listOf(userProp, emailProp, nameProp, surnameProp)
            .mapValueList { _ -> userProp.value.equals(editedUser) }

    emailValidProp.bindTo(emailProp.map { it.contains("@") })
    buttonEnabledProp.bindTo(usersEqualProp.mapWith(emailValidProp) { equal, valid -> !equal && valid })
    buttonTextProp.bindTo(usersEqualProp.map { if (it) "Nothing changed" else "Save changes" })

    buttonClickedProp.takeEachAnd { userProp.value = editedUser.snapshot() }
    // ^ reset flag and perform action
}
```

## ProGuard

```
# using annotations with 'provided' scope
-dontwarn android.support.annotation.**

# design lib with 'provided' scope
-dontwarn android.support.design.widget.**

# safely checking for JavaFX which is not accessible on Android
-dontwarn javafx.application.Platform

# keep volatile field names for AtomicFieldUpdater
-keepclassmembernames class net.aquadc.properties.internal.** {
  volatile <fields>;
}
-keepclassmembernames class net.aquadc.properties.android.pref.SharedPreferenceProperty {
  volatile <fields>;
}
```
