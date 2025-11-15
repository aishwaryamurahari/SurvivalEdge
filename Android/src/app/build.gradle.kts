/*
 * Copyright 2025 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

plugins {
  alias(libs.plugins.android.application)
  // Note: set apply to true to enable google-services (requires google-services.json).
  alias(libs.plugins.google.services) apply false
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.protobuf)
  alias(libs.plugins.hilt.application)
  alias(libs.plugins.ksp)
  //alias(libs.plugins.oss.licenses)
  kotlin("kapt")
}

android {
  namespace = "com.google.ai.edge.gallery"
  compileSdk = 35

  defaultConfig {
    applicationId = "com.google.ai.edge.gallery"
    minSdk = 31
    targetSdk = 35
    versionCode = 13
    versionName = "1.0.7"

    // Gmail OAuth Client ID for email integration
    buildConfigField("String", "GMAIL_CLIENT_ID", "Add your Gmail OAuth Client ID here")

    // Needed for HuggingFace auth workflows.
    // Use the scheme of the "Redirect URLs" in HuggingFace app.
    manifestPlaceholders["appAuthRedirectScheme"] = "com.google.ai.edge.gallery:/oauth2redirect"
    manifestPlaceholders["applicationName"] = "com.google.ai.edge.gallery.GalleryApplication"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    // Add 16KB page size support
    ndk {
      abiFilters += listOf("arm64-v8a", "armeabi-v7a")
    }
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("debug")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  kotlinOptions {
    jvmTarget = "11"
    freeCompilerArgs += "-Xcontext-receivers"
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }

  packagingOptions {
    resources {
      excludes += "/META-INF/{AL2.0,LGPL2.1}"
      excludes += "META-INF/INDEX.LIST"
      excludes += "META-INF/*.SF"
      excludes += "META-INF/*.DSA"
      excludes += "META-INF/*.RSA"
    }
    jniLibs {
      useLegacyPackaging = true
    }
  }

  // Add 16KB alignment support
  ndkVersion = "25.1.8937393"

  // Add this new block for noCompress
  androidResources {
    noCompress += listOf("tflite")
  }
}

dependencies {
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.ui)
  implementation(libs.androidx.ui.graphics)
  implementation(libs.androidx.ui.tooling.preview)
  implementation(libs.androidx.material3)
  implementation(libs.androidx.compose.navigation)
  implementation(libs.kotlinx.serialization.json)
  implementation(libs.material.icon.extended)
  implementation(libs.androidx.work.runtime)
  implementation(libs.androidx.datastore)
  implementation(libs.com.google.code.gson)
  implementation(libs.androidx.lifecycle.process)
  implementation(libs.androidx.security.crypto)
  implementation(libs.litertlm)
  implementation(libs.commonmark)
  implementation(libs.richtext)
implementation("org.tensorflow:tensorflow-lite:2.15.0")
implementation("org.tensorflow:tensorflow-lite-gpu:2.15.0")
implementation("org.tensorflow:tensorflow-lite-support:0.4.4")
  implementation(libs.camerax.core)
  implementation(libs.camerax.camera2)
  implementation(libs.camerax.lifecycle)
  implementation(libs.camerax.view)
  implementation(libs.openid.appauth)
  implementation(libs.androidx.splashscreen)
  implementation(libs.protobuf.javalite)
  implementation(libs.hilt.android)
  implementation(libs.hilt.navigation.compose)
  implementation(libs.play.services.oss.licenses)
  implementation(platform(libs.firebase.bom))
  implementation(libs.firebase.analytics)
  implementation(libs.androidx.exifinterface)

  // API dependencies for cloud models
  implementation("com.squareup.retrofit2:retrofit:2.9.0")
  implementation("com.squareup.retrofit2:converter-gson:2.9.0")
  implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")
  implementation("com.squareup.okhttp3:okhttp:4.11.0")

  // RAG System Dependencies
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
  // implementation("androidx.room:room-runtime:2.6.1")
  // implementation("androidx.room:room-ktx:2.6.1")
  implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.2")
  implementation("ch.qos.logback:logback-classic:1.2.12")
  implementation("io.github.microutils:kotlin-logging:3.0.5")
  kapt(libs.hilt.android.compiler)
  implementation(libs.room.runtime)
  implementation(libs.room.ktx)
  ksp(libs.room.compiler)
  testImplementation(libs.junit)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.ui.test.junit4)
  androidTestImplementation(libs.hilt.android.testing)
  debugImplementation(libs.androidx.ui.tooling)
  debugImplementation(libs.androidx.ui.test.manifest)
}

protobuf {
  protoc { artifact = "com.google.protobuf:protoc:4.26.1" }
  generateProtoTasks { all().forEach { it.plugins { create("java") { option("lite") } } } }
}
