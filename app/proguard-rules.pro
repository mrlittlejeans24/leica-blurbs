# Jsoup ships an optional dependency on the W3C/XML SAX bits that R8 warns about.
-dontwarn org.jsoup.**
-keep class org.jsoup.** { *; }

# OkHttp / Okio
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**

# Room generates implementations at compile time; keep its annotations intact.
-keep class androidx.room.** { *; }
