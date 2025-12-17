import kotlin.toString

plugins {
    id("buildsrc.convention.kotlin-jvm")
    id("org.jetbrains.intellij")
}

dependencies {
    // 基础 Kotlin 依赖 (标准库反射库)
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))

    implementation(libs.gson)         // 用于 JSON 序列化/反序列化
}

val intellijVersions = arrayOf(
    mapOf("jdk" to 21, "version" to "2024.1.4", "since" to "241"),
    mapOf("jdk" to 17, "version" to "2023.1.3", "since" to "231"),
    mapOf("jdk" to 11, "version" to "2021.2.1", "since" to "212")
)
val javaVersion = JavaVersion.current().majorVersion.toInt()

val (targetIdeaVersion, targetSinceBuild) = intellijVersions
    .firstOrNull { javaVersion >= (it["jdk"] as Int) }
    ?.let { it["version"].toString() to it["since"].toString() }
    ?: ("2021.2.1" to "212")

// 配置 IntelliJ 插件环境
intellij {
    version.set(targetIdeaVersion)
    type.set("IC")
    pluginName.set("AsyncTest Caller")
    sandboxDir.set("idea-sandbox")
    plugins.set(listOf("java", "maven", "gradle", "Groovy"))
    instrumentCode.set(false)
    updateSinceUntilBuild.set(false)
}

tasks {
    // 禁用构建插件文件（因为它只是个依赖库）
    patchPluginXml { enabled = false }
    prepareSandbox { enabled = false }
    buildSearchableOptions { enabled = false }
    runIde { enabled = false } // 这个模块不能独立运行 IDE
}