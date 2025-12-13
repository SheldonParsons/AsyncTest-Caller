import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("buildsrc.convention.kotlin-jvm")
    id("org.jetbrains.intellij")
}

group = "com.sheldon"
version = libs.versions.plugin.version.get()

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

println("当前 JDK: $javaVersion, 将构建 IDEA 版本: $targetIdeaVersion (Since: $targetSinceBuild)")

dependencies {
    implementation(project(":common-api"))
    implementation(libs.gson)
    implementation(kotlin("reflect"))


    implementation(libs.guice) {
        exclude(group = "org.checkerframework", module = "checker-compat-qual")
        exclude(group = "com.google.guava", module = "guava")
    }

    implementation(libs.okhttp)
}

// 配置 IntelliJ 插件环境
intellij {
    version.set(targetIdeaVersion)
    type.set("IC")
    pluginName.set("AsyncTest Caller")
    sandboxDir.set("idea-sandbox")
    plugins.set(listOf("java", "maven", "gradle", "Groovy"))
}

tasks {
    patchPluginXml {
        sinceBuild.set(targetSinceBuild)
        untilBuild.set("")
        pluginDescription.set("AsyncTest Caller Plugin")
        changeNotes.set("No updates yet")
    }

    runIde {
        args = listOf("/Users/sheldon/Documents/AsyncTest/other_module/spring-for-plugins-test")
        jvmArgs = listOf("-Didea.is.internal=true", "-Dgradle.version.support.matrix.url=file:///dev/null")
    }
}
val compileKotlin: KotlinCompile by tasks
compileKotlin.compilerOptions {
    freeCompilerArgs.set(listOf("-Xwhen-guards"))
}