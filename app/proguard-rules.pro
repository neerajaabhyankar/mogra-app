# PyTorch Mobile's Java classes are reached from native code, so R8 cannot see the
# references and will happily delete them. The AAR ships no consumer rules of its own,
# which is why these live here.
-keep class org.pytorch.** { *; }
-keep class com.facebook.jni.** { *; }
-keep class com.facebook.soloader.** { *; }
-dontwarn org.pytorch.**
-dontwarn com.facebook.**

# Anything with a native method is an entry point by definition.
-keepclasseswithmembernames class * { native <methods>; }

# Model assets are addressed by string, so nothing here may be renamed away either.
-keepclassmembers class com.mogralabs.mogra.audio.RaagIdentifier { *; }
