# Nostrange Proguard rules
-keepattributes *Annotation*
-keepclassmembers class * {
    @kotlinx.serialization.Serializable *;
}
-keep class com.nostrange.app.domain.model.** { *; }
-keep class com.nostrange.app.data.local.entity.** { *; }
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**
