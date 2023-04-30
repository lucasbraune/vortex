/*
 * This file was generated by the Gradle 'init' task.
 *
 * This generated file contains a sample Kotlin application project to get you started.
 * For more details take a look at the 'Building Java & JVM projects' chapter in the Gradle
 * User Manual available at https://docs.gradle.org/8.0.2/userguide/building_java_projects.html
 */

plugins {
    // Apply the org.jetbrains.kotlin.jvm Plugin to add support for Kotlin.
    id("org.jetbrains.kotlin.jvm") version "1.8.10"
    kotlin("plugin.serialization") version "1.8.21"

    // Apply the application plugin to add support for building a CLI application in Java.
    application
}

val maelstromRuntime: Configuration by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
}

dependencies {
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.9.1")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")
    implementation(project(":protocol"))

    maelstromRuntime(project(mapOf("path" to ":maelstrom", "configuration" to "maelstromBin")))
}

application {
    // Define the main class for the application.
    mainClass.set("vortex.AppKt")
}

tasks.named<Test>("test") {
    // Use JUnit Platform for unit tests.
    useJUnitPlatform()
}

tasks.register<Exec>("runMaelstrom") {
    dependsOn(maelstromRuntime.buildDependencies)
    dependsOn("installDist")
    val maelstromBin = maelstromRuntime.singleFile.path
    val nodeBin = "$buildDir/install/${project.name}/bin/${project.name}"
    val cmd = "$maelstromBin test -w unique-ids --bin $nodeBin --time-limit 30 --rate 1000 --node-count 3 --availability total --nemesis partition"
    commandLine("bash", "-c", cmd)
}