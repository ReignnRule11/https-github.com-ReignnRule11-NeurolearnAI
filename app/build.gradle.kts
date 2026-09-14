import com.google.gms.googleservices.GoogleServicesPlugin.MissingGoogleServicesStrategy
import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
  alias(libs.plugins.google.services)
}

  android {
  namespace = "com.example"
  compileSdk = 36

  defaultConfig {
    applicationId = "com.aistudio.neurolearn.kxmpzq"
    minSdk = 24
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
    create("release") {
      val keystorePropsFile = rootProject.file("keystore.properties")
      val keystoreProps = Properties()
      if (keystorePropsFile.exists()) {
        keystorePropsFile.inputStream().use { keystoreProps.load(it) }
      }
      val envKeystore = System.getenv("KEYSTORE_PATH") ?: keystoreProps.getProperty("storeFile")
      val envStorePassword = System.getenv("STORE_PASSWORD") ?: keystoreProps.getProperty("storePassword")
      val envKeyPassword = System.getenv("KEY_PASSWORD") ?: keystoreProps.getProperty("keyPassword")
      val envKeyAlias = System.getenv("KEY_ALIAS") ?: keystoreProps.getProperty("keyAlias") ?: "upload"
      val allowDebugReleaseSigning = System.getenv("ALLOW_DEBUG_RELEASE_SIGNING") == "true"
      val uploadKeystore = envKeystore?.let { path ->
        val asFile = file(path)
        if (asFile.isAbsolute) asFile else rootProject.file(path)
      } ?: file("${rootDir}/my-upload-key.jks")
      val looksLikeDebugKeystore = uploadKeystore.name.equals("debug.keystore", ignoreCase = true)
      if (looksLikeDebugKeystore && !allowDebugReleaseSigning &&
        gradle.startParameter.taskNames.any { it.contains("Release", ignoreCase = true) }
      ) {
        throw GradleException(
          "Release signing rejected debug.keystore. Provide a production upload keystore via KEYSTORE_PATH or keystore.properties."
        )
      }
      if (uploadKeystore.exists() && !envStorePassword.isNullOrBlank() && !looksLikeDebugKeystore) {
        storeFile = uploadKeystore
        storePassword = envStorePassword
        keyAlias = envKeyAlias
        keyPassword = envKeyPassword ?: envStorePassword
      } else if (allowDebugReleaseSigning) {
        storeFile = file("${rootDir}/debug.keystore")
        storePassword = "android"
        keyAlias = "androiddebugkey"
        keyPassword = "android"
      } else if (gradle.startParameter.taskNames.any { it.contains("Release", ignoreCase = true) }) {
        throw GradleException(
          "Release signing requires KEYSTORE_PATH and STORE_PASSWORD (or keystore.properties). " +
            "Set ALLOW_DEBUG_RELEASE_SIGNING=true only for non-Play verification builds."
        )
      } else {
        storeFile = file("${rootDir}/debug.keystore")
        storePassword = "android"
        keyAlias = "androiddebugkey"
        keyPassword = "android"
      }
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
      buildConfigField("boolean", "PLAY_BILLING_ENABLED", "true")
    }
    debug {
      signingConfig = signingConfigs.getByName("debugConfig")
      buildConfigField("boolean", "PLAY_BILLING_ENABLED", "true")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
}

ksp {
  arg("room.schemaLocation", "$projectDir/schemas")
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
}

val assemblingRelease = gradle.startParameter.taskNames.any { it.contains("Release", ignoreCase = true) }
val allowMissingGoogleServices = System.getenv("ALLOW_MISSING_GOOGLE_SERVICES") == "true"
googleServices {
  missingGoogleServicesStrategy =
    if (assemblingRelease && !allowMissingGoogleServices) {
      MissingGoogleServicesStrategy.ERROR
    } else {
      MissingGoogleServicesStrategy.WARN
    }
}


// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  // implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  // implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  // implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  implementation(libs.firebase.ai)
  implementation(libs.firebase.appcheck.recaptcha)
  implementation(libs.firebase.firestore)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  implementation(libs.billing.ktx)
  // implementation(libs.play.services.location)
  implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
}
