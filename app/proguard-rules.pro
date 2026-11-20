# Skip slow multi-pass bytecode optimization while keeping shrinking, obfuscation, and mapping.txt
-dontoptimize

# Keep source file & line numbers for readable stack traces in Play Console
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Required for Kotlin reflection (Moshi KotlinJsonAdapterFactory) & Retrofit suspend functions
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keep class kotlin.Metadata { *; }

# Keep Firestore data models and @Keep annotations
-keep class com.example.data.model.** { *; }
-keepclassmembers class com.example.data.model.** { *; }

# Keep Retrofit & Moshi payment DTOs and service interfaces
-keep class com.example.data.payment.** { *; }
-keepclassmembers class com.example.data.payment.** { *; }
-keep interface com.example.data.payment.PaymentApiService { *; }

# Keep Razorpay Checkout SDK & WebView JS bridge
-keep class com.razorpay.** { *; }
-dontwarn com.razorpay.**
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keepclassmembers class com.example.MainActivity {
    public void onPaymentSuccess(...);
    public void onPaymentError(...);
}
