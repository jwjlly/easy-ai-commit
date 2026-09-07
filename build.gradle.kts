import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    kotlin("jvm") version "2.3.20"
    id("org.jetbrains.intellij.platform") version "2.18.1"
}

group = "com.layxy.easyaicommit"
version = "0.2.0"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        val localIdePath = providers.gradleProperty("localIdePath").orNull
        if (localIdePath != null) {
            local(localIdePath)
        } else {
            intellijIdeaCommunity("2024.2.6")
        }
        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)
    }

    // Kotlin 1.9-compatible client for the 2024.2 platform baseline.
    implementation("com.squareup.okhttp3:okhttp:4.12.0") {
        // Use the platform's Kotlin runtime instead of packaging a second copy.
        exclude(group = "org.jetbrains.kotlin")
    }
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testImplementation("com.squareup.okhttp3:okhttp-tls:4.12.0")
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        jvmTarget = JvmTarget.JVM_21
        languageVersion = KotlinVersion.KOTLIN_2_1
        apiVersion = KotlinVersion.KOTLIN_1_9
    }
}

intellijPlatform {
    pluginConfiguration {
        name = "Easy AI Commit"
        version = project.version.toString()

        ideaVersion {
            sinceBuild = "242"
        }

        changeNotes = """
            <ul>
                <li>Added per-model proxy settings: IDE proxy, direct connection, or custom HTTP/SOCKS5 proxy,
                    with optional HTTP Basic proxy authentication.</li>
                <li>Added a configurable total request timeout of 5–300 seconds (default: 90 seconds).
                    Timed-out requests are canceled without replacing the existing commit message.</li>
                <li>Breaking behavior change: the previous 90-second read timeout is now a total network
                    request deadline, including connection and the complete response. Slow requests may
                    time out sooner; adjust the model profile timeout if needed.</li>
                <li>Added credential save read-back verification and clearer proxy errors.
                    Connection tests use current form values and discard stale results.</li>
                <li>Updated English, Simplified Chinese, Japanese, and Korean UI text and documentation.</li>
                <li>New profiles and profiles without a stored proxy mode default to no proxy (direct connection).
                    Explicitly stored proxy choices are preserved. The default total timeout is 90 seconds.
                    The IntelliJ IDEA 2024.2 minimum supported version is unchanged.</li>
            </ul>
        """.trimIndent()
    }

    providers.gradleProperty("localIdePath").orNull?.let { localIdePath ->
        pluginVerification {
            ides {
                local(localIdePath)
            }
        }
    }
}

tasks {
    processResources {
        val expansionProperties = mapOf("pluginVersion" to project.version.toString())
        inputs.properties(expansionProperties)
        filesMatching("easy-ai-commit-version.properties") {
            expand(expansionProperties)
        }
    }

    wrapper {
        gradleVersion = "9.3.1"
        distributionType = Wrapper.DistributionType.BIN
    }
}
