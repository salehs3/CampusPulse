@file:Suppress("GradleDependency", "UnstableApiUsage", "OldTargetApi")

import com.adarshr.gradle.testlogger.theme.ThemeType

plugins {
    id("com.android.application")
    id("org.cs124.gradlegrader") version "2025.10.0"
    checkstyle
    id("com.adarshr.test-logger") version "4.0.0"
}
android {
    namespace = "edu.illinois.cs.cs124.ay2025.mp"
    compileSdk {
        version = release(36)
    }
    defaultConfig {
        applicationId = "edu.illinois.cs.cs124.ay2025.joinable"
        minSdk = 36
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21 // Java source compatibility
        targetCompatibility = JavaVersion.VERSION_21 // Java bytecode target version
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
            all {
                if (it.name == "testReleaseUnitTest") {
                    it.enabled = false
                }
            }
        }
    }
}
dependencies {
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    implementation("androidx.recyclerview:recyclerview:1.4.0")
    implementation("com.squareup.okhttp3:okhttp:5.2.1")
    implementation("com.squareup.okhttp3:mockwebserver:5.2.1")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.20.0")
    implementation("com.fasterxml.jackson.module:jackson-module-parameter-names:2.20.0")
    implementation(platform("org.jetbrains.kotlin:kotlin-bom:2.2.20"))

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.16")
    testImplementation("androidx.test:core:1.7.0")
    testImplementation("androidx.test.ext:junit:1.3.0")
    testImplementation("androidx.test.ext:truth:1.7.0")
    testImplementation("androidx.test.espresso:espresso-core:3.7.0")
    testImplementation("com.google.guava:guava:33.5.0-android")
}
checkstyle {
    toolVersion = "12.0.1"
    configFile = rootProject.file("config/checkstyle/checkstyle.xml")
    isIgnoreFailures = false
}
tasks.register("checkstyle", Checkstyle::class) {
    mustRunAfter(rootProject.tasks.getByName("spotlessApply"))
    source("src/main/java")
    include("**/*.java")
    classpath = files()
}
configurations.checkstyle {
    resolutionStrategy.capabilitiesResolution.withCapability("com.google.collections:google-collections") {
        select("com.google.guava:guava:0")
    }
}
testlogger {
    theme = ThemeType.PLAIN
    slowThreshold = 600000L
}
gradlegrader {
    assignment = "AY2025.MP"
    points {
        total = 100
    }
    checkpoint {
        yamlFile = rootProject.file("grade.yaml")
        configureTests { checkpoint, test ->
            require(checkpoint in setOf("0", "1", "2", "3")) { "Cannot grade unknown checkpoint MP$checkpoint" }
            test.setTestNameIncludePatterns(listOf("MP${checkpoint}Test"))
            test.filter.isFailOnNoMatchingTests = true
        }
    }
    checkstyle {
        points = 10
    }
    earlyDeadline {
        points = { checkpoint ->
            when (checkpoint) {
                in setOf("2", "3") -> 10
                else -> 0
            }
        }
        noteForPoints = { checkpoint, points ->
            "Checkpoint $checkpoint has an early deadline, so the maximum local score is ${100 - points}/100.\n" +
                "$points points will be provided during official grading if you submit code " +
                "that meets the early deadline threshold before the early deadline."
        }
    }
    forceClean = false
    reporting {
        jsonFile = rootProject.file("grade.json")
        post {
            endpoint = "https://cloud.cs124.org/gradlegrader"
        }
        printPretty {
            title = "Grade Summary"
        }
    }
    vcs {
        git = true
        requireCommit = true
    }
    checkClaudeVersion = true
}
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}
project.afterEvaluate {
    tasks.withType(Test::class.java).forEach { test ->
        test.jvmArgs("-Djava.security.manager=allow")
        test.logging.captureStandardError(LogLevel.DEBUG)
    }
}
tasks.withType<Test> {
    environment("TEST_TIMEOUT_MULTIPLIER", System.getenv("TEST_TIMEOUT_MULTIPLIER") ?: "4")
    environment("TEST_TIMEOUT_MIN", System.getenv("TEST_TIMEOUT_MIN") ?: "5000")
    environment("TEST_TIMEOUT_MAX", System.getenv("TEST_TIMEOUT_MAX") ?: "30000")
}
tasks.register("checkTimeProvider") {
    group = "verification"
    description = "Verify that code uses Helpers.getTimeProvider() instead of direct time calls"

    doLast {
        val sourceDir = file("src/main/java")
        val violations = mutableListOf<String>()

        val patterns =
            mapOf(
                Regex("""\bInstant\.now\(\)""") to
                    "Use Helpers.getTimeProvider().now() instead of Instant.now() for testable time",
                Regex("""\bZonedDateTime\.now\(\)""") to
                    "Use ZonedDateTime.now(ZoneId).ofInstant(Helpers.getTimeProvider().now(), ZoneId) instead of ZonedDateTime.now() for testable time",
                Regex("""\bLocalDateTime\.now\(\)""") to
                    "Use LocalDateTime.now(ZoneId).ofInstant(Helpers.getTimeProvider().now(), ZoneId) instead of LocalDateTime.now() for testable time",
            )

        fileTree(sourceDir) {
            include("**/*.java")
            exclude("**/helpers/Helpers.java")
        }.forEach { file ->
            val lines = file.readLines()
            lines.forEachIndexed { index, line ->
                val codeOnly = line.substringBefore("//").trim()
                if (codeOnly.isNotEmpty() && !codeOnly.startsWith("/*") && !codeOnly.startsWith("*")) {
                    patterns.forEach { (pattern, message) ->
                        if (pattern.containsMatchIn(codeOnly)) {
                            val relativePath = file.relativeTo(projectDir)
                            violations.add("$relativePath:${index + 1}: $message")
                        }
                    }
                }
            }
        }

        if (violations.isNotEmpty()) {
            val errorMessage =
                buildString {
                    appendLine("\nTimeProvider violations found:")
                    appendLine()
                    violations.forEach { appendLine("  $it") }
                    appendLine()
                    appendLine("Hint: Use Helpers.getTimeProvider().now() for mockable time in tests.")
                }
            throw GradleException(errorMessage)
        }
    }
}
tasks.withType<Test> {
    dependsOn(tasks.getByName("checkTimeProvider"))
}
tasks.named("checkTimeProvider") {
    mustRunAfter(tasks.getByName("checkstyle"))
}
tasks.named("grade") {
    dependsOn(rootProject.tasks.named("spotlessApply"))
}
