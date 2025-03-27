plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.multiplatform")
    id("kotlin-parcelize")
    id("dev.icerock.mobile.multiplatform.cocoapods")
    id("dev.icerock.mobile.multiplatform.android-manifest")
    id("maven-publish")
    id("signing")
}

group = "io.github.wilmermolinac"      // Aquí defines el grupo con el formato "io.github.tuUsuario"
version = "0.6.1"                     // Establece la versión que deseas publicar

kotlin {
    jvmToolchain(11)

    androidTarget {
        publishLibraryVariants("release", "debug")
    }
    ios()
    iosSimulatorArm64()
    jvm()

    sourceSets {
        def commonMain = getByName("commonMain")
        def androidMain = getByName("androidMain")
        def jvmMain = getByName("jvmMain")
        def iosMain = getByName("iosMain")
        def iosSimulatorArm64Main = getByName("iosSimulatorArm64Main") {
            dependsOn(iosMain)
        }
    }

    targets.withType(org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget).configureEach {
        compilations.all {
            cinterops.all {
                extraOpts("-compiler-option", "-fmodules")
            }
        }
    }
}

dependencies {
    // Dependencias comunes y específicas según tu proyecto
    commonMainApi "org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1" // Ejemplo
    // Otras dependencias...
}

android {
    namespace = "dev.icerock.moko.socket"
    // Configuración de Android...
}

// Publicación Maven
publishing {
    publications {
        create(MavenPublication, "maven") {
            from(components["kotlin"])
            artifactId = "moko-socket-io"
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
                        email.set("wamcstudios@gmail.com") // Reemplaza con tu correo
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
    repositories {
        // Ejemplo: Publicar en GitHub Packages. Asegúrate de tener configuradas las credenciales.
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/wilmermolinac/moko-socket-io")
            credentials {
                username = project.findProperty("gpr.user") ?: System.getenv("GITHUB_USERNAME")
                password = project.findProperty("gpr.key") ?: System.getenv("GITHUB_TOKEN")
            }
        }
        // Si deseas publicar en OSSRH (Maven Central), configúralo aquí.
    }
}

// Configuración de Signing (opcional si publicas en repositorios que requieren firma)
signing {
    // Si tus variables de entorno están definidas, se usarán para firmar la publicación
    def signingKeyId = System.getenv("SIGNING_KEY_ID")
    def signingPassword = System.getenv("SIGNING_PASSWORD")
    def signingKey = System.getenv("SIGNING_KEY")?.with { key -> new String(Base64.getDecoder().decode(key)) }
    if (signingKeyId != null && signingKey != null && signingPassword != null) {
        useInMemoryPgpKeys(signingKeyId, signingKey, signingPassword)
        sign(publishing.publications)
    }
}

// Configuración de CocoaPods (para iOS)
cocoaPods {
    // Esto generará el podspec a partir de tu configuración KMM
    pod("mokoSocketIo")
}
