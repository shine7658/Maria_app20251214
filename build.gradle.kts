// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    // 這些是 Android Studio 預設幫你產生的 (保留它們)
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false

    // ★★★ 加入這一行就好 (這是讓 Google 服務能運作的關鍵) ★★★
    // 這一行負責告訴 Gradle 去哪裡下載 Firebase 工具
    id("com.google.gms.google-services") version "4.4.1" apply false
}