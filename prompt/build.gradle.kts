plugins {
    id("org.jetbrains.kotlin.jvm")
    id("io.gitlab.arturbosch.detekt")
    id("org.jlleitschuh.gradle.ktlint")
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(project(":model"))
    implementation(project(":baseline"))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

detekt {
    config.setFrom(rootProject.files("config/detekt.yml"))
    buildUponDefaultConfig = true
}
