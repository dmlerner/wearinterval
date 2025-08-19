plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
    id("com.diffplug.spotless")
}

android {
    namespace = "com.wearinterval"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.wearinterval"
        minSdk = 30
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
        execution = "ANDROIDX_TEST_ORCHESTRATOR"

        // Increase instrumented test parallelism by 50%
        managedDevices {
            localDevices {
                create("pixel_watch_2") {
                    device = "Pixel Watch 2 (API 30)"
                    apiLevel = 30
                    systemImageSource = "aosp-atd"
                }
            }
        }
    }

    buildTypes {
        debug {
            enableUnitTestCoverage = true
            enableAndroidTestCoverage = true
        }
    }
}

// JaCoCo coverage configuration
apply(plugin = "jacoco")

tasks.withType<Test> {
    // Test parallelism configuration - increased by 50%
    maxParallelForks = (Runtime.getRuntime().availableProcessors() * 9 / 12).coerceAtLeast(1)
    forkEvery = 30

    // JaCoCo configuration
    configure<JacocoTaskExtension> {
        isIncludeNoLocationClasses = true
        excludes = listOf("jdk.internal.*")
    }
}

// Combined coverage report for both unit and instrumented tests
tasks.register<JacocoReport>("jacocoTestReport") {
    group = "verification"
    description = "Generate combined JaCoCo coverage report for unit and instrumented tests"

    dependsOn("testDebugUnitTest")

    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }

    val fileFilter =
        listOf(
            "**/R.class", "**/R$*.class", "**/BuildConfig.*", "**/Manifest*.*",
            "**/*Test*.*", "android/**/*.*", "**/data/database/**/*.*",
            "**/*_Hilt*.class", "**/hilt_aggregated_deps/**", "**/dagger/**",
        )

    val debugTree = fileTree("${layout.buildDirectory.get()}/tmp/kotlin-classes/debug")
    debugTree.exclude(fileFilter)
    classDirectories.setFrom(debugTree)

    val sourceDirs =
        listOf(
            "src/main/java",
            "src/main/kotlin",
        )
    sourceDirectories.setFrom(sourceDirs.map { file(it) })

    // Include both unit test and instrumented test execution data
    executionData.setFrom(
        fileTree(layout.buildDirectory.get()).include(
            "**/*.exec",
            "**/*.ec",
            "**/coverage.ec",
        ),
    )
}

// Combined coverage report that includes instrumented tests when available
tasks.register<JacocoReport>("combinedCoverageReport") {
    group = "verification"
    description = "Generate combined coverage report including instrumented tests when available"

    dependsOn("testDebugUnitTest")
    // Note: Don't depend on connectedDebugAndroidTest to allow offline usage

    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }

    val fileFilter =
        listOf(
            "**/R.class", "**/R$*.class", "**/BuildConfig.*", "**/Manifest*.*",
            "**/*Test*.*", "android/**/*.*", "**/data/database/**/*.*",
            "**/*_Hilt*.class", "**/hilt_aggregated_deps/**", "**/dagger/**",
        )

    val debugTree = fileTree("${layout.buildDirectory.get()}/tmp/kotlin-classes/debug")
    debugTree.exclude(fileFilter)
    classDirectories.setFrom(debugTree)

    val sourceDirs =
        listOf(
            "src/main/java",
            "src/main/kotlin",
        )
    sourceDirectories.setFrom(sourceDirs.map { file(it) })

    // Combine execution data from both test types
    executionData.setFrom(
        fileTree(layout.buildDirectory.get()).include(
            "jacoco/testDebugUnitTest.exec",
            "outputs/unit_test_code_coverage/**/*.exec",
            "outputs/code_coverage/debugAndroidTest/connected/**/*.ec",
        ),
    )

    finalizedBy("printCoverageResults")
}

// Task to print coverage summary
tasks.register("printCoverageResults") {
    group = "verification"
    description = "Print coverage results summary"

    doLast {
        val coverageFile = file("${layout.buildDirectory.get()}/reports/jacoco/combinedCoverageReport/jacocoTestReport.xml")
        if (coverageFile.exists()) {
            println("📊 Combined Coverage Report Generated!")
            println("📁 HTML Report: ${layout.buildDirectory.get()}/reports/jacoco/combinedCoverageReport/html/index.html")
            println("📄 XML Report: ${coverageFile.absolutePath}")
        }
    }
}

dependencies {
    // Wear OS & Compose
    implementation("androidx.wear.compose:compose-material:1.4.0")
    implementation("androidx.wear.compose:compose-foundation:1.4.0")
    implementation("androidx.wear.compose:compose-navigation:1.4.0")
    implementation("androidx.wear:wear:1.3.0")
    implementation("androidx.core:core-splashscreen:1.0.1")

    // Horologist - Google's high-quality Wear OS components
    implementation("com.google.android.horologist:horologist-composables:0.6.19")
    implementation("com.google.android.horologist:horologist-compose-layout:0.6.19")

    // Standard Compose
    implementation("androidx.compose.ui:ui:1.6.8")
    implementation("androidx.compose.ui:ui-tooling-preview:1.6.8")
    implementation("androidx.activity:activity-compose:1.9.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.4")

    // Architecture & DI
    implementation("com.google.dagger:hilt-android:2.51.1")
    ksp("com.google.dagger:hilt-compiler:2.51.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // Data & Persistence
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Coroutines & Flow
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-guava:1.8.1")

    // Wear OS Platform APIs - Updated to latest ProtoLayout for Tiles API
    implementation("androidx.wear.protolayout:protolayout:1.3.0")
    implementation("androidx.wear.protolayout:protolayout-material:1.3.0")
    implementation("androidx.wear.protolayout:protolayout-expression:1.3.0")
    implementation("androidx.wear.tiles:tiles:1.5.0") // TileService and deprecated API interop
    implementation("androidx.wear.watchface:watchface-complications-data-source:1.2.1")

    // Testing - Unit Tests
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    testImplementation("io.mockk:mockk:1.13.12")
    testImplementation("app.cash.turbine:turbine:1.1.0")
    testImplementation("com.google.truth:truth:1.4.4")
    testImplementation("androidx.arch.core:core-testing:2.2.0")

    // Robolectric for Android framework testing without emulator
    testImplementation("org.robolectric:robolectric:4.15.1")
    testImplementation("androidx.test:core:1.5.0")

    // Testing - Android/Integration Tests
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.6.8")
    androidTestImplementation("androidx.room:room-testing:2.6.1")
    androidTestImplementation("com.google.dagger:hilt-android-testing:2.51.1")
    androidTestImplementation("com.google.truth:truth:1.4.4")
    androidTestImplementation("app.cash.turbine:turbine:1.1.0")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    androidTestUtil("androidx.test:orchestrator:1.5.0")
    kspAndroidTest("com.google.dagger:hilt-compiler:2.51.1")

    // Debug Tools
    debugImplementation("androidx.compose.ui:ui-tooling:1.6.8")
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.6.8")
}

// Allow references to generated code
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

// Spotless configuration for code formatting
spotless {
    kotlin {
        target("src/**/*.kt", "*.kt")
        targetExclude("**/build/**/*.kt")
        ktfmt("0.44").googleStyle()
    }
    kotlinGradle {
        target("*.gradle.kts")
        ktlint("1.0.1")
    }
}
