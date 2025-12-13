import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.intellij) apply false
}

group = "com.sheldon"
version = libs.versions.plugin.version.get()

allprojects {
    // 统一配置 Maven 仓库
    repositories {
        mavenCentral()
    }

    // 强制 Java 编译版本为 17
    plugins.withType<JavaPlugin> {
        extensions.configure<JavaPluginExtension> {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }
    }

    // 强制 Kotlin 编译版本为 17
    tasks.withType<KotlinCompile> {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
}