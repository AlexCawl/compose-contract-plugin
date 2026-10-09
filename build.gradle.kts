plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.buildconfig) apply false
}

allprojects {
    group = "com.alexcawl.contract"
    version = "0.1.0-SNAPSHOT"
}
