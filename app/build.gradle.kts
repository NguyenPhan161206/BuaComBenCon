plugins { id("com.android.application") }
android {
    namespace = "vn.edu.buacombencon"
    compileSdk = 36
    defaultConfig {
        applicationId = "vn.edu.buacombencon"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0-ui"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
