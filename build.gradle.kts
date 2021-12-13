import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.0"
    kotlin("plugin.serialization") version "1.6.0"
}

group = "fr.ayfri"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("it.skrape:skrapeit:1.1.6")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.1")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}
