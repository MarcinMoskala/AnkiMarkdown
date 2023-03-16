import java.util.*

plugins {
    kotlin("multiplatform") version "1.8.10"
    kotlin("plugin.serialization") version "1.6.21"
    id("dev.petuska.npm.publish") version "3.0.1"
    java
}

group = "com.marcinmoskala"
val libVersion =  "0.0.28"
version = libVersion

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/kotlinx-html/maven")
}

kotlin {
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "1.8"
        }
        withJava()
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }
    js(IR) {
        browser()
        binaries.library()
    }
    sourceSets {
        val ktorVersion = "2.0.3"

        val commonMain by getting {
            dependencies {
                implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.3")
                implementation("net.mamoe.yamlkt:yamlkt:0.10.2")

                implementation("io.ktor:ktor-client-core:$ktorVersion")
                implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
                implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.6.4")
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation("io.ktor:ktor-client-okhttp:$ktorVersion")
            }
        }
        val jvmTest by getting
        val jsMain by getting
        val jsTest by getting
    }
}

val properties = Properties().apply {
    rootProject.file("local.properties")
        .takeIf { it.exists() }
        ?.reader()
        ?.let { load(it) }
}

npmPublish {
    packages {
        named("js") {
            packageName.set("anki-markdown")
            version.set(libVersion)
        }
    }
    registries {
        register("npmjs") {
            uri.set(uri("https://registry.npmjs.org")) //
            authToken.set((properties["npmSecret"] as? String).orEmpty())
        }
    }
}