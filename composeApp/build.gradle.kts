import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.jetbrainsComposeCompiler)
    alias(libs.plugins.cocoapods)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ktlint)
}


val flavor: String by project
val appId = when (flavor) {
    "dw" -> "org.dw.probe"
    else -> "org.ooni.probe"
}
println("The current build flavor is set to '$flavor' with suffix set to '$appId'.")

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    cocoapods {
        ios.deploymentTarget = "9.0"

        version = "1.0"
        summary = "Compose App"
        homepage = "https://github.com/ooni/probe-multiplatform"

        framework {
            baseName = "composeApp"
            isStatic = true
            binaryOption("bundleId", "composeApp")
        }

        podfile = project.file("../iosApp/Podfile")
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.ui.tooling.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.android.oonimkall)
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.kotlin.serialization)

            // add source directories and resources based on flavor
            when (flavor) {
                "ooni" -> {
                    getByName("commonMain") {
                        kotlin.srcDir("src/ooniMain/kotlin")
                    }
                }
                "dw" -> {
                    getByName("commonMain") {
                        kotlin.srcDir("src/dwMain/kotlin")
                    }
                }
            }
        }
        all {
            languageSettings.optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
        }
    }

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    compilerOptions {
        // Common compiler options applied to all Kotlin source sets
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
    composeCompiler {
        enableStrongSkippingMode = true
    }
}

compose.resources {
    when (flavor) {
        "ooni" -> {
            customDirectory(
                sourceSetName = "commonMain",
                directoryProvider = provider { layout.projectDirectory.dir("src/ooniMain/resources") }
            )
        }
        "dw" -> {
            customDirectory(
                sourceSetName = "commonMain",
                directoryProvider = provider { layout.projectDirectory.dir("src/dwMain/resources") }
            )
        }
    }
}
android {
    namespace = "org.ooni.probe"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = appId
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("debug") {
            //applicationIdSuffix = ".debug"
        }
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    dependencies {
        debugImplementation(libs.compose.ui.tooling)
    }
    android {
        lint {
            warningsAsErrors = true
            disable += "AndroidGradlePluginVersion"
        }
    }
}

ktlint {
    filter {
        exclude("**/generated/**")
        include("**/kotlin/**")
    }
    additionalEditorconfig.put("ktlint_function_naming_ignore_when_annotated_with", "Composable")
}

//tasks.register("runDebug", Exec::class) {
//    dependsOn("clean", "uninstallDebug", "installDebug")
//    commandLine(
//        "adb", "shell", "am", "start", "-n",
//        "$appId/org.ooni.probe.MainActivity"
//    )
//}
