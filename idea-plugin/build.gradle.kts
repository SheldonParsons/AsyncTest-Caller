plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.intellij")
}

group = "com.sheldon" // 🔴 改成你自己的包名
version = properties["plugin_version"]!!

// 定义版本映射表
// ⚠️ 关键修改：JDK 21 对应的版本我改成了 2024.1.4
// 原因：原作者写的 2025.x 需要 Gradle Plugin 2.0 架构，而我们目前用的是 1.x 架构
// 2024.1.4 是旧架构能支持的最高版本，且能完美运行在 JDK 21 上
val intellijVersions = arrayOf(
    mapOf("jdk" to 21, "version" to "2024.1.4", "since" to "241"),
    mapOf("jdk" to 17, "version" to "2023.1.3", "since" to "231"),
    mapOf("jdk" to 11, "version" to "2021.2.1", "since" to "212")
)

// 获取当前 Gradle 运行环境的 Java 版本
val javaVersion = JavaVersion.current().majorVersion.toInt()

// 查找匹配的配置
val (targetIdeaVersion, targetSinceBuild) = intellijVersions
    .firstOrNull { javaVersion >= (it["jdk"] as Int) }
    ?.let { it["version"].toString() to it["since"].toString() }
// 兜底策略：如果都没匹配上（比如用了 JDK 8），默认用 2021.2.1
    ?: ("2021.2.1" to "212")

println("当前 JDK: $javaVersion, 将构建 IDEA 版本: $targetIdeaVersion (Since: $targetSinceBuild)")

repositories {
    mavenCentral()
}

dependencies {
    // 1. 引用兄弟模块
    implementation(project(":common-api")) {
        // 🟢 修正写法：显式指定 group 和 module 参数
        exclude(group = "org.apache.httpcomponents", module = "httpclient")
    }

    // 2. 引用 EasyApi 作者封装的各种支持库
    // 🟢 修正写法：将 excludeGroup 替换为 exclude(group = "...")
    implementation(libs.itangcent.commons) {
        exclude(group = "com.google.inject")
        exclude(group = "com.google.code.gson")
    }
    implementation(libs.itangcent.guice) {
        exclude(group = "com.google.inject")
        exclude(group = "com.google.code.gson")
    }
    implementation(libs.itangcent.jvm) {
        exclude(group = "com.google.inject")
        exclude(group = "com.google.code.gson")
    }
    implementation(libs.itangcent.idea) {
        exclude(group = "com.google.inject")
        exclude(group = "com.google.code.gson")
    }
    implementation(libs.itangcent.kotlin) {
        exclude(group = "com.google.inject")
        exclude(group = "com.google.code.gson")
    }
    implementation(libs.itangcent.groovy) {
        exclude(group = "com.google.inject")
        exclude(group = "com.google.code.gson")
    }

    // 3. 第三方库
    implementation(libs.guice) {
        // 🟢 修正写法
        exclude(group = "org.checkerframework", module = "checker-compat-qual")
        exclude(group = "com.google.guava", module = "guava")
    }

    // 以下保持不变
    implementation(kotlin("reflect"))
    implementation(libs.jackson.databind)
    implementation(libs.sqlite.jdbc)
    implementation(libs.okhttp)
    implementation(libs.openai.java)
    implementation(libs.openai.client)

    // 4. 测试库
    testImplementation(kotlin("test"))
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.mockito.inline)
}

// 配置 IntelliJ 插件环境
intellij {
    // 🟢 这里不再写死，而是使用上面算出来的变量
    version.set(targetIdeaVersion)
    type.set("IC")
    pluginName.set("AsyncTest Caller") // 你的插件名
    sandboxDir.set("idea-sandbox")     // 设置沙盒目录
    plugins.set(listOf("java", "maven", "gradle", "Groovy"))
}

tasks {
    patchPluginXml {
        // 设置兼容版本 (对应原项目的 since-build="241")
        sinceBuild.set("241")
        untilBuild.set("") // 不设上限

        // 暂时写死描述，先跑通再说
        pluginDescription.set("AsyncTest Caller Plugin")
        changeNotes.set("Initial migration.")
    }
}