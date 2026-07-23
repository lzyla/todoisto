import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    // Wersje Kotlina są już na classpath (z modułu root), więc bez wersji tutaj.
    kotlin("jvm")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.compose") version "1.7.1"
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
}

compose.desktop {
    application {
        mainClass = "pl.media30.todoisto.desktop.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Pkg)
            packageName = "Todoisto"
            packageVersion = "1.0.0"
            macOS {
                bundleID = "pl.media30.todoisto"
            }
        }
    }
}
