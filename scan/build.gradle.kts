import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.intellij.platform.module")
}

dependencies {
    implementation(project(":model"))

    testImplementation(libs.junit.jupiter)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    intellijPlatform {
        bundledPlugin("com.intellij.java")
        bundledPlugin("org.jetbrains.idea.maven")
        testFramework(TestFrameworkType.Platform)
    }
}

tasks.test {
    useJUnitPlatform()
}
