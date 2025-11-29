import com.vanniktech.maven.publish.SonatypeHost

plugins {
    kotlin("multiplatform")
    id("com.android.library")
    id("org.jetbrains.compose")
    id("org.jetbrains.dokka")
    id("com.vanniktech.maven.publish")
}

kotlin {
    androidTarget {
        publishLibraryVariants("release")
    }

    jvm("desktop")

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "lib"
            isStatic = true
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val androidMain by getting {
            dependencies {
            }
        }
        val iosX64Main by getting
        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting
        val iosMain by creating {
            dependsOn(commonMain)
            iosX64Main.dependsOn(this)
            iosArm64Main.dependsOn(this)
            iosSimulatorArm64Main.dependsOn(this)
        }
        val desktopMain by getting {
            dependencies {
            }
        }
    }
}

android {
    compileSdk = (findProperty("android.compileSdk") as String).toInt()
    namespace = "footballpitch"

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    sourceSets["main"].res.srcDirs("src/androidMain/res")
    sourceSets["main"].resources.srcDirs("src/commonMain/resources")

    defaultConfig {
        minSdk = (findProperty("android.minSdk") as String).toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        jvmToolchain(17)
    }
}

mavenPublishing {
    // Configure publishing to Maven Central only when explicitly requested to avoid CI/local failures.
    val shouldPublish = (findProperty("publishToMavenCentral") as String?)?.toBoolean() == true
    if (shouldPublish) {
        publishToMavenCentral(SonatypeHost.S01, automaticRelease = true)
        signAllPublications()
    }
    // Coordinates used when this library is published to a Maven repository.
    coordinates("io.github.ibrahimalzaidi", "compose-football-pitch", "0.1.0")

    pom {
        name.set(project.name)
        description.set("A Compose Multiplatform library for rendering customizable football pitches and team lineups.")
        inceptionYear.set("2025")
        url.set("https://github.com/IbrahimAlzaidi/ComposeFootballPitch")
        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        developers {
            developer {
                id.set("ibrahimalzaidi")
                name.set("Ibrahim Alzaidi")
                url.set("https://github.com/IbrahimAlzaidi/")
            }
        }
        scm {
            url.set("https://github.com/IbrahimAlzaidi/ComposeFootballPitch")
            connection.set("scm:git:git://github.com/IbrahimAlzaidi/ComposeFootballPitch.git")
            developerConnection.set("scm:git:ssh://git@github.com/IbrahimAlzaidi/ComposeFootballPitch.git")
        }
    }
}
