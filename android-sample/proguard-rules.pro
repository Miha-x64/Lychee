
-dontskipnonpubliclibraryclasses
-dontpreverify
-optimizationpasses 5
-overloadaggressively
-allowaccessmodification

-repackageclasses ""
-renamesourcefileattribute ""

-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
  public static void checkExpressionValueIsNotNull(java.lang.Object, java.lang.String);
  public static void checkFieldIsNotNull(java.lang.Object, java.lang.String);
  public static void checkFieldIsNotNull(java.lang.Object, java.lang.String, java.lang.String);
  public static void checkNotNull(java.lang.Object);
  public static void checkNotNull(java.lang.Object, java.lang.String);
  public static void checkNotNullExpressionValue(java.lang.Object, java.lang.String);
  public static void checkNotNullParameter(java.lang.Object, java.lang.String);
  public static void checkParameterIsNotNull(java.lang.Object, java.lang.String);
  public static void checkReturnedValueIsNotNull(java.lang.Object, java.lang.String);
  public static void checkReturnedValueIsNotNull(java.lang.Object, java.lang.String, java.lang.String);
}

-keepclassmembers class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator CREATOR;
}

# required by EnumSet
-keepclassmembers enum * {
  public static **[] values();
}

# libs with compileOnly scope
-dontwarn android.support.annotation.**
-dontwarn android.support.v7.widget.**
-dontwarn android.support.design.widget.**
-dontwarn okio.**

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
-assumenosideeffects class net.aquadc.persistence.extended.CollectionNoOp {
    private void sanityCheck(java.lang.Object);
}

# https://sourceforge.net/p/proguard/bugs/660/
-keepclassmembernames class net.aquadc.properties.internal.** {
  volatile <fields>;
}
