# This Proguard rule ensures that ComponentInitializers are are neither shrunk nor obfuscated.
# This is because they are discovered and instantiated during application initialization.
-keep class * extends androidx.startup.ComponentInitializer