import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.0"
}

group = "fr.ayfri"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:0.7.3")
    implementation("io.ktor:ktor-client-core:1.6.6")
    implementation("io.ktor:ktor-client-cio:1.6.6")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}
