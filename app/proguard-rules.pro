# R8 for the release build: on the Kompakt's MT6761 a shrunk, non-debuggable build makes Compose
# several times faster (SPEED-LEARNINGS.md). These libraries load classes by reflection, so they
# are kept whole.

# NewPipeExtractor: stream URL extraction, including the Rhino JavaScript engine it runs
# YouTube's player code in.
-keep class org.schabi.newpipe.extractor.** { *; }
-keep class org.mozilla.javascript.** { *; }
-keep class org.mozilla.classfile.** { *; }
-keep class org.jsoup.** { *; }
-dontwarn org.mozilla.javascript.**
-dontwarn org.mozilla.classfile.**
-dontwarn java.beans.**
-dontwarn javax.script.**
-dontwarn jdk.dynalink.**
-dontwarn org.jspecify.annotations.**
-dontwarn com.google.re2j.**

# jaudiotagger: tag reading and writing for local files
-keep class org.jaudiotagger.** { *; }
-dontwarn org.jaudiotagger.**
-dontwarn javax.imageio.**
-dontwarn java.awt.**

# OkHttp's optional TLS providers
-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**

-keepattributes *Annotation*, InnerClasses, Signature, EnclosingMethod
