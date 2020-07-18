import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

repositories {
    mavenCentral()
    jcenter()
}

plugins {
    application
    kotlin("jvm") version "1.3.72"
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(group = "org.seleniumhq.selenium", name = "selenium-java", version = "3.141.59")
    implementation(group = "io.github.bonigarcia", name = "webdrivermanager", version = "3.6.1")
    testImplementation(group = "org.junit.jupiter", name = "junit-jupiter", version = "5.6.2")
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "1.8"
}

val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = "1.8"
}

tasks.test {
    useJUnitPlatform()
}
