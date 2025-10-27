@file:Suppress("GradleDependency", "AndroidGradlePluginVersion")

import org.apache.tools.ant.taskdefs.condition.Os
import java.util.Properties

plugins {
    id("com.android.application") version "8.13.0" apply false
    id("com.diffplug.spotless") version "8.0.0"
    java
}
subprojects {
    tasks.withType(JavaCompile::class.java) {
        options.compilerArgs.addAll(listOf("-parameters"))
    }
}
spotless {
    java {
        googleJavaFormat("1.29.0")
        target("app/src/*/java/**/*.java")
    }
    kotlinGradle {
        ktlint("1.7.1")
        target("**/*.gradle.kts")
    }
}
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.register("checkPaths") {
    group = "verification"
    description = "Verify project and Android SDK paths contain only ASCII characters"

    doLast {
        val projectPath = projectDir.absolutePath
        var androidSdkPath: String? = null
        val localProperties = File(rootProject.projectDir, "local.properties")
        if (localProperties.exists()) {
            val properties = Properties()
            localProperties.inputStream().use { properties.load(it) }
            androidSdkPath = properties.getProperty("sdk.dir")
        }
        if (androidSdkPath == null) {
            androidSdkPath = System.getenv("ANDROID_HOME") ?: System.getenv("ANDROID_SDK_ROOT")
        }
        val problems = mutableListOf<String>()

        fun String.isAsciiOnly() = all { it.code < 128 }
        if (!projectPath.isAsciiOnly()) {
            problems.add("Project path contains non-ASCII characters: $projectPath")
        }
        val gradleUserHome = System.getenv("GRADLE_USER_HOME")
        val userHome = System.getProperty("user.home")
        if (gradleUserHome != null) {
            if (!gradleUserHome.isAsciiOnly()) {
                problems.add("GRADLE_USER_HOME contains non-ASCII characters: $gradleUserHome")
            }
        } else {
            if (!userHome.isAsciiOnly()) {
                problems.add("User home directory contains non-ASCII characters: $userHome")
            }
        }
        if (androidSdkPath != null && !androidSdkPath.isAsciiOnly()) {
            problems.add("Android SDK path contains non-ASCII characters: $androidSdkPath")
        }
        if (problems.isNotEmpty()) {
            val errorMessage =
                buildString {
                    appendLine("\n" + "=".repeat(80))
                    appendLine("Path Validation Failed")
                    appendLine("=".repeat(80))
                    appendLine()
                    problems.forEach { appendLine("  - $it") }
                    appendLine()
                    appendLine("Non-ASCII characters in paths can cause build failures with Android tools.")
                    appendLine(
                        "Please move your project, Android SDK, or Gradle home directory to a path with only ASCII characters.",
                    )
                    appendLine("=".repeat(80))
                }
            throw GradleException(errorMessage)
        } else {
            println("\n" + "=".repeat(80))
            println("Path Validation Passed")
            println("=".repeat(80))
            println()
            println("Project path: $projectPath")
            val displayGradleHome = gradleUserHome ?: "$userHome/.gradle"
            println("Gradle user home: $displayGradleHome")
            if (androidSdkPath != null) {
                println("Android SDK path: $androidSdkPath")
            } else {
                println("Android SDK path: Not configured (ANDROID_HOME/ANDROID_SDK_ROOT not set)")
            }
            println()
            println("All paths contain only ASCII characters.")
            println("=".repeat(80))
        }
    }
}

tasks.register("showAndroidStudioTerminalInstructions") {
    group = "help"
    description = "Shows instructions for configuring Java in the Android Studio terminal"

    doLast {
        val javaHome = System.getProperty("java.home")
        val fileSeparator = System.getProperty("file.separator")
        val javaBin = "$javaHome${fileSeparator}bin"

        println("\n" + "=".repeat(80))
        println("Android Studio Terminal Java Configuration")
        println("=".repeat(80))
        println()
        println("Detected Java installation:")
        println("  JAVA_HOME: $javaHome")
        println("  Java bin directory: $javaBin")
        println()
        println("Instructions:")
        println("  1. Open Settings: Android Studio → Settings (⌘, on Mac, Ctrl+Alt+S elsewhere)")
        println("  2. Navigate to: Tools → Terminal")
        println("  3. Add to 'Environment variables' field:")
        println()
        println("     JAVA_HOME=$javaHome;PATH=$javaBin")
        println()
        println("  4. Click 'Apply' and 'OK'")
        println("  5. Restart terminal windows in Android Studio")
        println("  6. Verify with: java -version")
        println()
        println("=".repeat(80))
    }
}
