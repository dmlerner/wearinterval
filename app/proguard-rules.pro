# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Keep Hilt generated classes
-keep class dagger.hilt.** { *; }
-keep class **_HiltModules** { *; }
-keep class **_Factory { *; }
-keep class **_MembersInjector { *; }
-keep class **_GeneratedInjector { *; }
-dontwarn com.wearinterval.WearIntervalApplication_GeneratedInjector

# Keep Room entity classes
-keep class com.wearinterval.data.database.entity.** { *; }

# Keep data classes used in StateFlow
-keep class com.wearinterval.domain.model.** { *; }

# Keep Health Services classes for heart rate functionality
-keep class androidx.health.services.client.** { *; }
-keep interface androidx.health.services.client.** { *; }
-keep class com.google.android.gms.wearable.** { *; }
-keep class com.wearinterval.data.health.** { *; }

# Keep heart rate related enums and data classes
-keep class androidx.health.services.client.data.DataType { *; }
-keep class androidx.health.services.client.data.DataPointContainer { *; }
-keep class androidx.health.services.client.data.DataPoint { *; }
-keep class androidx.health.services.client.data.Value { *; }

# Keep measure callback interfaces
-keepclassmembers class * implements androidx.health.services.client.MeasureCallback {
   *;
}