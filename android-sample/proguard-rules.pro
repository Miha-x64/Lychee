
-dontskipnonpubliclibraryclasses
-dontpreverify
-optimizationpasses 5
-overloadaggressively
-allowaccessmodification

-repackageclasses "_"
-renamesourcefileattribute "_"

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

-keepclassmembers enum * {
  public static **[] values();
#  public static ** valueOf(java.lang.String);
}

# using annotations with 'provided' scope
-dontwarn android.support.annotation.**

# design lib with 'provided' scope
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
