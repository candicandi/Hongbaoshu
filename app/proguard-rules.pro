# ============================================================================
# Hongbaoshu ProGuard Configuration
# 优化配置以减少被安全软件误报
# ============================================================================

# -------------------- 基础配置 --------------------
# 代码优化选项
-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-verbose

# 优化选项 - 启用更激进的优化
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-allowaccessmodification
-repackageclasses ''

# 保留源文件名和行号(用于崩溃报告)
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# -------------------- Android 基础保留 --------------------
# 保留 Android 组件
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider

# 保留 View 构造函数
-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet);
}
-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# 保留 Parcelable
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# 保留 Serializable
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# -------------------- Kotlin 相关 --------------------
# 保留 Kotlin 元数据
-keep class kotlin.Metadata { *; }

# 保留 Kotlin 协程
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# -------------------- Jetpack Compose --------------------
# 保留 Composable 函数
-keep @androidx.compose.runtime.Composable class * { *; }
-keep class androidx.compose.** { *; }

# 保留 ViewModel
-keep class * extends androidx.lifecycle.ViewModel {
    <init>();
}
-keep class * extends androidx.lifecycle.AndroidViewModel {
    <init>(android.app.Application);
}

# -------------------- 序列化相关 --------------------
# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class com.xuyutech.hongbaoshu.**$$serializer { *; }
-keepclassmembers class com.xuyutech.hongbaoshu.** {
    *** Companion;
}
-keepclasseswithmembers class com.xuyutech.hongbaoshu.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# -------------------- Media3 / ExoPlayer --------------------
-keep class androidx.media3.** { *; }
-keep interface androidx.media3.** { *; }
-dontwarn androidx.media3.**

# -------------------- DataStore --------------------
-keep class androidx.datastore.*.** { *; }

# -------------------- 反射相关 --------------------
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod

# -------------------- 移除日志 --------------------
# 移除 Log 调用以减少代码体积和潜在的信息泄露
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# -------------------- 警告抑制 --------------------
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**