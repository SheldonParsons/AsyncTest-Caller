import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
plugins {
    id("buildsrc.convention.kotlin-jvm")
    id("org.jetbrains.intellij")
}
group = "com.sheldon"
version = libs.versions.plugin.version.get()
val targetIdeaVersion = if (project.hasProperty("targetIde")) "2024.1.4" else "2022.3.3"
val targetSinceBuild = "223"
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
println("构建目标 IDEA 版本: $targetIdeaVersion (Since: $targetSinceBuild), JVM Target: 17")
dependencies {
    implementation(project(":common-api"))
    implementation(libs.gson)
    implementation(kotlin("reflect"))
    implementation("io.swagger.core.v3:swagger-models:2.2.19")
    implementation("io.swagger.core.v3:swagger-core:2.2.19")
    implementation(libs.commonsLang3)
    implementation(libs.guice) {
        exclude(group = "org.checkerframework", module = "checker-compat-qual")
        exclude(group = "com.google.guava", module = "guava")
    }
    implementation(libs.okhttp)
}
intellij {
    version.set(targetIdeaVersion)
    type.set("IC")
    pluginName.set("AsyncTest Caller")
    sandboxDir.set("idea-sandbox")
    plugins.set(listOf("java", "maven", "gradle", "properties", "yaml"))
}
tasks {
    patchPluginXml {
        sinceBuild.set(targetSinceBuild)
        untilBuild.set("")
    }
    runIde {
        args = listOf("/Users/sheldon/Documents/GitLabProject/order/gree-business-order-admin")
        jvmArgs = listOf("-Didea.is.internal=true", "-Dgradle.version.support.matrix.url=file:///dev/null")
    }
    instrumentCode {
        enabled = false
    }
    buildSearchableOptions { enabled = false }
    runPluginVerifier {
        ideVersions.set(
            listOf(
                "2022.3.3",
                "2023.1.5",
                "2024.1.4"
            )
        )
        failureLevel.set(
            listOf(
                org.jetbrains.intellij.tasks.RunPluginVerifierTask.FailureLevel.COMPATIBILITY_PROBLEMS,
                org.jetbrains.intellij.tasks.RunPluginVerifierTask.FailureLevel.MISSING_DEPENDENCIES,
                org.jetbrains.intellij.tasks.RunPluginVerifierTask.FailureLevel.INVALID_PLUGIN
            )
        )
    }
}
val compileKotlin: KotlinCompile by tasks
compileKotlin.compilerOptions {
    jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    freeCompilerArgs.set(listOf("-Xwhen-guards"))
}