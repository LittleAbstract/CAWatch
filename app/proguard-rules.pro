# CA Detector - ProGuard Rules
# Android generic proguard rules

-keepattributes *Annotation*
-keepclassmembers class * {
    @androidx.annotation.Keep <methods>;
}

-keep class * extends android.app.Activity
-keep class * extends android.app.Application
-keep class * extends android.app.Service
-keep class * extends android.view.View
-keep class * extends android.content.BroadcastReceiver

# Room Database
-keep class androidx.room.** { *; }
-keep class * extends androidx.room.Database { *; }
-keep class * implements androidx.room.Repository { *; }

# Keep all DAO interfaces and their implementations
-keep interface androidx.room.Dao { *; }
-keep class * implements androidx.room.Dao { *; }

# Keep all Entity classes
-keep class * extends androidx.room.Entity { *; }

# Kotlin
-keep class kotlin.Metadata { *; }
-keep class kotlin.internal.** { *; }
-keep class kotlinx.coroutines.internal.** { *; }
-keep class kotlinx.coroutines.** { *; }

-keep class * implements java.io.Serializable { *; }

# AndroidX
-keep class androidx.** { *; }
-keep interface androidx.** { *; }

# Keep all classes in the com.security.careactivator package
-keep class com.security.careactivator.** { *; }

# For enum classes
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep R classes
-keep class **.R$* { *; }

# Keep all Activities, Services, and Receivers
-keep class * extends android.app.Activity
-keep class * extends android.app.Service
-keep class * extends android.content.BroadcastReceiver

# Keep all Parcelable classes
-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}
