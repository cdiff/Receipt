# =============================================================
# 영수증 쏙 (Receipt) — ProGuard Rules
# =============================================================

# ---------------------------------------------------------------
# 1. 스택 트레이스 보존 (크래시 로그 해독용)
#    → 난독화 후에도 줄 번호가 남아 디버깅 가능
# ---------------------------------------------------------------
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ---------------------------------------------------------------
# 2. Room Database
#    Entity 필드 이름이 바뀌면 DB 컬럼을 못 찾아 RuntimeException 발생
# ---------------------------------------------------------------
-keep class com.pasic.receipt.data.local.entity.ReceiptEntity { *; }
-keep class com.pasic.receipt.data.local.entity.CategoryEntity { *; }
-keep class com.pasic.receipt.data.local.ReceiptDatabase { *; }
-keep interface com.pasic.receipt.data.local.dao.** { *; }

# Room 내부 생성 코드 보호 (KSP 생성 _Impl 클래스)
-keep class * extends androidx.room.RoomDatabase { *; }
-dontwarn androidx.room.**

# ---------------------------------------------------------------
# 3. Hilt (Dagger) 의존성 주입
#    Hilt가 런타임에 생성한 컴포넌트를 리플렉션으로 찾기 때문에
#    이름이 바뀌면 주입 실패 → 앱 크래시
# ---------------------------------------------------------------
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep @dagger.hilt.android.HiltAndroidApp class * { *; }
-keep @dagger.hilt.android.AndroidEntryPoint class * { *; }
-keep @javax.inject.Singleton class * { *; }
-dontwarn dagger.hilt.**
-dontwarn dagger.**

# ---------------------------------------------------------------
# 4. WorkManager — NotificationPushWorker
#    Worker 클래스 이름이 바뀌면 WorkManager가 클래스를 못 찾아
#    JobCancelledException 발생
# ---------------------------------------------------------------
-keep class com.pasic.receipt.data.notification.NotificationPushWorker { *; }
-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.CoroutineWorker { *; }
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-dontwarn androidx.work.**

# ---------------------------------------------------------------
# 5. Firebase & Google AI (Gemini)
#    Firebase는 내부적으로 리플렉션을 많이 사용함
# ---------------------------------------------------------------
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# ---------------------------------------------------------------
# 6. Kotlin Coroutines
# ---------------------------------------------------------------
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# ---------------------------------------------------------------
# 7. Kotlin (리플렉션, 메타데이터 등)
# ---------------------------------------------------------------
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**

# ---------------------------------------------------------------
# 8. Android 공통 — Parcelable, Serializable
# ---------------------------------------------------------------
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator CREATOR;
}
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ---------------------------------------------------------------
# 9. Enum — name() / ordinal() 사용 보호
#    NotificationCategory, NotificationTab, DateSection 등 enum
# ---------------------------------------------------------------
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ---------------------------------------------------------------
# 10. Lottie 애니메이션
# ---------------------------------------------------------------
-dontwarn com.airbnb.lottie.**
-keep class com.airbnb.lottie.** { *; }

# ---------------------------------------------------------------
# 11. ML Kit (한국어 OCR)
# ---------------------------------------------------------------
-dontwarn com.google.mlkit.**
-keep class com.google.mlkit.** { *; }

# ---------------------------------------------------------------
# 12. CameraX
# ---------------------------------------------------------------
-dontwarn androidx.camera.**
-keep class androidx.camera.** { *; }