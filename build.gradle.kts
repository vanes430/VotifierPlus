import java.text.SimpleDateFormat
import java.util.Date

plugins {
    id("java")
    alias(libs.plugins.spotless)
    alias(libs.plugins.shadow)
}

group = "com.bencodez"
version = "1.4.4-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

spotless {
    java {
        licenseHeaderFile(rootProject.file("gradle/spotless-header.java"))
        trimTrailingWhitespace()
        endWithNewline()
        targetExclude("**/VoteConnectionHandler.java")
    }
}

dependencies {
    compileOnly(libs.paper.api)
    compileOnly(libs.velocity.api)
    compileOnly(libs.configurate.core)
    compileOnly(libs.configurate.yaml)
}

tasks.processResources {
    filteringCharset = "UTF-8"
    filesMatching(listOf("plugin.yml", "velocity-plugin.json")) {
        expand(
            "name" to rootProject.name,
            "version" to project.version,
            "buildNumber" to (System.getenv("BUILD_NUMBER") ?: "NOTSET"),
            "buildProfile" to "prod",
            "timestamp" to SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date())
        )
    }
}

tasks.jar {
    archiveBaseName.set("VotifierPlus")
    archiveClassifier.set("")
}

java {
    withSourcesJar()
}
