# ZXing decodes through direct API calls; no reflection keep rules are required.

# zxing-cpp exposes JNI entry points from its Kotlin wrapper.
-keep class zxingcpp.** { *; }
