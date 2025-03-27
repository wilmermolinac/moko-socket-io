import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import java.util.Base64

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.multiplatform")
    id("kotlin-parcelize")
    id("dev.icerock.mobile.multiplatform.cocoapods")
    id("dev.icerock.mobile.multiplatform.android-manifest")
    // Se omiten los plugins de Maven Publish y Signing al no publicar en Maven Central
    // id("org.gradle.maven-publish")
    // id("signing")
}

group = "io.github.wilmermolinac" // Tu grupo (formato: io.github.tuUsuario)
version = "0.6.1" // Versión de la librería

kotlin {
    jvmToolchain(11)

    // Registrar el target Android
    androidTarget {
        publishLibraryVariants("release", "debug")
    }
    ios()
    iosSimulatorArm64()
    jvm()

    sourceSets {
        val commonMain by getting

        val commonJvm by creating {
            dependsOn(commonMain)
        }
        val androidMain by getting {
            dependsOn(commonJvm)
        }
        val jvmMain by getting {
            dependsOn(commonJvm)
        }
        val iosMain by getting
        // Para iOS Simulator Arm64, se hace que dependa de iosMain
        val iosSimulatorArm64Main by getting {
            dependsOn(iosMain)
        }
    }

    targets.withType<KotlinNativeTarget>().configureEach {
        compilations.configureEach {
            cinterops.configureEach {
                extraOpts("-compiler-option", "-fmodules")
            }
        }
    }
}

dependencies {
    // Dependencia para la serialización en commonMain
    commonMain.api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
    // Ejemplo de dependencia para Android. Asegúrate de tener definida la coordenada en tu version catalog (libs.appCompat) o reemplázala directamente.
    "androidMainImplementation"(libs.appCompat)
    // Dependencia para JVM (excluyendo org.json)
    "commonJvmImplementation"(libs.socketIo) {
        exclude(group = "org.json", module = "json")
    }
    "jvmMainImplementation"(libs.socketIo)
}

android {
    namespace = "io.github.wilmermolinac.moko.socket"
    // Aquí puedes incluir otras configuraciones de Android según tus necesidades
}

val javadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
}

// Bloque de publicación Maven y signing se omiten al no publicarla en Maven Central.
// Puedes consumir esta librería directamente desde GitHub (por ejemplo, usando JitPack o como composite build).

cocoaPods {
    // Esto generará el podspec para iOS a partir de la configuración multiplataforma
    pod("mokoSocketIo")
}
