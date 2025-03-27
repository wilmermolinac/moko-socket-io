import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import java.util.Base64

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.multiplatform")
    id("kotlin-parcelize")
    id("dev.icerock.mobile.multiplatform.cocoapods")
    id("dev.icerock.mobile.multiplatform.android-manifest")
    id("org.gradle.maven-publish")
    id("signing")
}

group = "io.github.wilmermolinac" // Tu grupo en GitHub
version = libs.versions.mokoSocketIoVersion.get() // O reemplázalo por "0.6.1" si lo prefieres

kotlin {
    jvmToolchain(11)
    androidTarget {
        publishLibraryVariants("release", "debug")
    }
    ios()
    iosSimulatorArm64()
    jvm()

    sourceSets {
        val commonMain by getting

        val commonJvm = create("commonJvm") {
            dependsOn(commonMain)
        }

        val androidMain by getting {
            dependsOn(commonJvm)
        }

        val jvmMain by getting {
            dependsOn(commonJvm)
        }

        val iosMain by getting
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
    // Dependencia para la serialización
    commonMain.api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
    // Ejemplo de dependencia para Android; asegúrate de tener definida la coordenada en tu version catalog o reemplázala directamente
    "androidMainImplementation"(libs.appCompat)
    // Dependencia para JVM (excluyendo org.json)
    "commonJvmImplementation"(libs.socketIo) {
        exclude(group = "org.json", module = "json")
    }
    "jvmMainImplementation"(libs.socketIo)
}

android {
    namespace = "io.github.wilmermolinac.moko.socket" // Tu namespace para Android
    // Otras configuraciones de Android...
}

val javadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
}

publishing {
    repositories.maven("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/") {
        name = "OSSRH"
        credentials {
            username = System.getenv("OSSRH_USER")
            password = System.getenv("OSSRH_KEY")
        }
    }

    publications.withType<org.gradle.api.publish.maven.MavenPublication> {
        artifact(javadocJar.get())

        pom {
            name.set("Moko Socket IO")
            description.set("Socket.IO implementation for Kotlin Multiplatform library")
            url.set("https://github.com/wilmermolinac/moko-socket-io")
            licenses {
                license {
                    name.set("Apache-2.0")
                    url.set("https://github.com/wilmermolinac/moko-socket-io/blob/master/LICENSE.md")
                    distribution.set("repo")
                }
            }
            developers {
                developer {
                    id.set("wilmermolinac")
                    name.set("Wilmer Molina")
                    email.set("wamcstudios@gmail.com")
                }
            }
            scm {
                connection.set("scm:git:https://github.com/wilmermolinac/moko-socket-io.git")
                developerConnection.set("scm:git:https://github.com/wilmermolinac/moko-socket-io.git")
                url.set("https://github.com/wilmermolinac/moko-socket-io")
            }
        }
    }

    repositories.maven {
        name = "GitHubPackages"
        url = uri("https://maven.pkg.github.com/wilmermolinac/moko-socket-io")
        credentials {
            username = (project.findProperty("gpr.user") as? String) ?: System.getenv("GITHUB_USERNAME")
            password = (project.findProperty("gpr.key") as? String) ?: System.getenv("GITHUB_TOKEN")
        }
    }
}

signing {
    val signingKeyId: String? = System.getenv("SIGNING_KEY_ID")
    val signingPassword: String? = System.getenv("SIGNING_PASSWORD")
    val signingKey: String? = System.getenv("SIGNING_KEY")?.let { key ->
        String(Base64.getDecoder().decode(key))
    }
    if (signingKeyId != null && signingKey != null && signingPassword != null) {
        useInMemoryPgpKeys(signingKeyId, signingKey, signingPassword)
        sign(publishing.publications)
    }
}

cocoaPods {
    // Esto generará el podspec a partir de la configuración multiplataforma
    pod("mokoSocketIo")
}
