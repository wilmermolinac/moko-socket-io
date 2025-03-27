plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.multiplatform")
    id("kotlin-parcelize")
    id("dev.icerock.mobile.multiplatform.cocoapods")
    id("dev.icerock.mobile.multiplatform.android-manifest")
    id("maven-publish")
    id("signing")
}

group = "io.github.wilmermolinac" // Grupo con el formato "io.github.wilmermolinac"
version = "0.6.1" // Versión que deseas publicar

kotlin {
    jvmToolchain(11)

    androidTarget {
        publishLibraryVariants("release", "debug")
    }
    ios()
    iosSimulatorArm64()
    jvm()

    sourceSets {
        // Usamos "by getting" para obtener los source sets existentes
        val commonMain by getting
        val androidMain by getting
        val jvmMain by getting
        val iosMain by getting
        val iosSimulatorArm64Main by getting {
            dependsOn(iosMain)
        }
    }

    targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget>().configureEach {
        compilations.all {
            cinterops.all {
                extraOpts("-compiler-option", "-fmodules")
            }
        }
    }
}

dependencies {
    // En Kotlin DSL usamos paréntesis para declarar dependencias
    commonMain.api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
    // Otras dependencias que requieras...
}

android {
    namespace = "dev.icerock.moko.socket"
    // Configuración de Android...
}

val javadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
}

// Publicación Maven
publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["kotlin"])
            artifactId = "moko-socket-io"
            artifact(javadocJar.get())

            pom {
                name.set("MOKO Socket IO")
                description.set("Socket.IO implementation for Kotlin Multiplatform")
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

// Configuración de Signing (opcional, si publicas en repositorios que requieren firma)
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

// Configuración de CocoaPods para generar el podspec a partir de tu configuración KMM
cocoaPods {
    pod("mokoSocketIo")
}
