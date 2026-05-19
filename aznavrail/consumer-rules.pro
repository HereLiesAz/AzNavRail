# Prevent R8 from stripping or unboxing models and enums
-keep class com.hereliesaz.aznavrail.model.** { *; }
-keepclassmembers enum com.hereliesaz.aznavrail.model.** { *; }

# Keep annotations to prevent shrinking issues
-keep @interface com.hereliesaz.aznavrail.annotation.** { *; }
