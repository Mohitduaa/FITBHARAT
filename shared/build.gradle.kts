plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.android.kotlin.multiplatform.library)
  alias(libs.plugins.compose.multiplatform)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.room)
}

kotlin {
  androidLibrary {
    namespace = "com.example.shared"
    compileSdk { version = release(37) }
    minSdk = 24
    withHostTestBuilder {}
    // Needed so compose resources (fonts, exercise photos) are packaged into the Android app.
    androidResources { enable = true }
  }

  listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
    target.binaries.framework {
      baseName = "Shared"
      isStatic = true
    }
  }

  sourceSets {
    commonMain.dependencies {
      implementation(compose.runtime)
      implementation(compose.foundation)
      implementation(compose.material3)
      implementation(compose.materialIconsExtended)
      implementation(compose.components.resources)
      implementation(libs.androidx.lifecycle.viewmodel)
      implementation(libs.androidx.lifecycle.viewmodel.compose.kmp)
      implementation(libs.androidx.lifecycle.runtime.compose.kmp)
      implementation(libs.androidx.room.runtime)
      implementation(libs.androidx.sqlite.bundled)
      implementation(libs.kotlinx.coroutines.core)
      implementation(libs.kotlinx.datetime)
      implementation(libs.kotlinx.serialization.json)
      implementation(libs.ktor.client.core)
    }
    androidMain.dependencies {
      implementation(libs.ktor.client.okhttp)
      implementation(libs.androidx.activity.compose)
      implementation(libs.androidx.core.ktx)
      implementation(libs.play.services.code.scanner)
    }
    iosMain.dependencies {
      implementation(libs.ktor.client.darwin)
    }
    commonTest.dependencies {
      implementation(libs.kotlin.test)
    }
  }
}

room {
  schemaDirectory("$projectDir/schemas")
}

dependencies {
  add("kspAndroid", libs.androidx.room.compiler)
  add("kspIosArm64", libs.androidx.room.compiler)
  add("kspIosSimulatorArm64", libs.androidx.room.compiler)
}

compose.resources {
  packageOfResClass = "fitbharat.shared.generated.resources"
  generateResClass = always
}
