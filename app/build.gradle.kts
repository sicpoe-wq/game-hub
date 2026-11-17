plugins { id("com.android.application"); id("org.jetbrains.kotlin.android"); id("org.jetbrains.kotlin.plugin.compose"); id("app.cash.paparazzi") }
val likenessBuild = project.hasProperty("likeness")
android {
  namespace = "com.tim.gamehub"; compileSdk = 35
  defaultConfig {
    applicationId = "com.tim.gamehub"
    minSdk = 24; targetSdk = 35; versionCode = 12; versionName = "2.1"
    buildConfigField("boolean", "POLITICAL_LIKENESS", if (likenessBuild) "true" else "false")
    if (likenessBuild) {
      applicationIdSuffix = ".private"
      versionNameSuffix = "-private"
    }
  }
  compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
  kotlinOptions { jvmTarget = "17" }
  buildFeatures { compose = true; buildConfig = true }
  testOptions { unitTests.isIncludeAndroidResources = true }
  if (likenessBuild) {
    sourceSets.getByName("main").res.srcDir("src/private/res")
  }
}
dependencies {
  val bom = platform("androidx.compose:compose-bom:2024.10.01")
  implementation(bom)
  implementation("androidx.activity:activity-compose:1.9.3")
  implementation("androidx.compose.ui:ui")
  implementation("androidx.compose.material3:material3")
  implementation("androidx.compose.material:material-icons-core")
  testImplementation("junit:junit:4.13.2")
  testImplementation("org.robolectric:robolectric:4.13")
  testImplementation(bom)
  testImplementation("androidx.compose.ui:ui-test-junit4")
  debugImplementation("androidx.compose.ui:ui-test-manifest")
}
