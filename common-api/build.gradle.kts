plugins {
    id("buildsrc.convention.kotlin-jvm")
}

dependencies {
    // 基础 Kotlin 依赖 (标准库反射库)
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))

    implementation(libs.gson)         // 用于 JSON 序列化/反序列化
}