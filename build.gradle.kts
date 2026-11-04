import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    kotlin("jvm") version "2.3.20"
    id("org.jetbrains.intellij.platform") version "2.18.1"
}

group = "com.layxy.easyaicommit"
version = "0.3.0"

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
                <li>Added Responses API support under OpenAI Compatible. Set the Base URL to a full endpoint
                    ending in /responses, such as https://api.openai.com/v1/responses, for generation and
                    connection tests.</li>
                <li>Existing base URLs and /chat/completions endpoints continue to use Chat Completions.
                    Anthropic Compatible connections are unchanged.</li>
                <li>Responses API requests disable response storage and support text extraction alongside
                    reasoning output. Incomplete, failed, refused, or empty responses preserve the existing
                    commit message.</li>
                <li>Added configuration guidance and error messages in English, Simplified Chinese,
                    Japanese, and Korean, with synchronized documentation.</li>
                <li>No breaking changes. The IntelliJ IDEA 2024.2 minimum supported version is unchanged.</li>
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
