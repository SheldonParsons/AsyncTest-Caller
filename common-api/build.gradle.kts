plugins {
    id("org.jetbrains.kotlin.jvm")
}

dependencies {
    // 基础 Kotlin 依赖 (标准库反射库现在通常自动包含，不写也没事，为了保险加上)
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))

    // EasyApi 核心工具包
    implementation(libs.itangcent.commons) {
        exclude(group = "com.google.inject")
        exclude(group = "com.google.code.gson")
    }

    // Gson (编译时需要，运行时由 IDE 提供，但为了保险用 compileOnly)
    compileOnly(libs.gson)

    // HTTP 组件 (原项目是 compileOnly，说明期望宿主环境提供)
    compileOnly(libs.httpmime)
    compileOnly(libs.httpclient)

    // --- 测试依赖
//    testImplementation(kotlin("test"))
//    testImplementation(libs.mockito.kotlin)
//    testImplementation(libs.mockito.inline)
}

tasks.withType<Test> {
    useJUnitPlatform()
}