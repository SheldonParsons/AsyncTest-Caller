package com.sheldon.idea.plugin.api.utils

import com.intellij.lang.properties.psi.PropertiesFile
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.YAMLMapping
import org.jetbrains.yaml.psi.YAMLScalar

object SpringConfigReader {

    private const val DEFAULT_PORT = "8080"
    private const val DEFAULT_HOST = "http://localhost"

    /**
     * 获取指定 Module 的 Spring Boot 基础 URL
     * 逻辑：合并 bootstrap 和 application 配置，后者优先级更高
     */
    fun getSpringBaseUrl(module: Module): String {
        // 读取并合并配置
        val config = readSpringConfig(module)

        val port = config["port"]?.takeIf { it.isNotBlank() } ?: DEFAULT_PORT
        var contextPath = config["context-path"] ?: ""

        // 规范化 contextPath (确保以 / 开头，不以 / 结尾)
        if (contextPath.isNotEmpty() && !contextPath.startsWith("/")) {
            contextPath = "/$contextPath"
        }
        if (contextPath == "/") {
            contextPath = ""
        }

        return "$DEFAULT_HOST:$port$contextPath"
    }

    /**
     * 核心读取逻辑：支持 bootstrap.yml/properties 和 application.yml/properties
     * 返回合并后的 Map: {"port": "...", "context-path": "..."}
     */
    private fun readSpringConfig(module: Module): Map<String, String> {
        val project = module.project
        val scope = GlobalSearchScope.moduleRuntimeScope(module, false)
        val finalConfig = mutableMapOf<String, String>()

        // 定义加载顺序：优先级低的先加载，优先级高的后加载（putAll 会覆盖旧值）
        // 顺序: bootstrap.properties -> bootstrap.yml -> application.properties -> application.yml
        val fileNames = listOf(
            "bootstrap.properties" to false, // false 表示不是 yaml
            "bootstrap.yml" to true,
            "application.properties" to false,
            "application.yml" to true
        )

        for ((fileName, isYaml) in fileNames) {
            val files = FilenameIndex.getVirtualFilesByName(fileName, scope)
            if (files.isNotEmpty()) {
                // 如果同一个名字有多个文件（极少见），取第一个
                val file = files.first()
                val configMap = if (isYaml) {
                    parseYaml(project, file)
                } else {
                    parseProperties(project, file)
                }
                // 存入总配置（后来的会覆盖先来的）
                finalConfig.putAll(configMap)
            }
        }

        return finalConfig
    }

    // --- 下面是解析逻辑，保持不变 ---

    private fun parseProperties(project: Project, file: VirtualFile): Map<String, String> {
        val psiFile = PsiManager.getInstance(project).findFile(file) as? PropertiesFile ?: return emptyMap()
        val result = mutableMapOf<String, String>()

        // port
        psiFile.findPropertyByKey("server.port")?.value?.let { result["port"] = it }

        // context-path
        val contextPath = psiFile.findPropertyByKey("server.servlet.context-path")?.value
            ?: psiFile.findPropertyByKey("server.context-path")?.value

        if (contextPath != null) result["context-path"] = contextPath
        return result
    }

    private fun parseYaml(project: Project, file: VirtualFile): Map<String, String> {
        val psiFile = PsiManager.getInstance(project).findFile(file) as? YAMLFile ?: return emptyMap()
        val result = mutableMapOf<String, String>()

        val documents = psiFile.documents
        if (documents.isEmpty()) return emptyMap()
        val topValue = documents[0].topLevelValue as? YAMLMapping ?: return emptyMap()

        // 递归查找 Yaml 值的辅助函数
        fun findValue(keys: List<String>, mapping: YAMLMapping): String? {
            val key = keys.first()
            val keyValue = mapping.getKeyValueByKey(key) ?: return null

            if (keys.size == 1) {
                return (keyValue.value as? YAMLScalar)?.textValue
            } else {
                val nextMapping = keyValue.value as? YAMLMapping ?: return null
                return findValue(keys.drop(1), nextMapping)
            }
        }

        // 查找 server.port
        findValue(listOf("server", "port"), topValue)?.let { result["port"] = it }

        // 查找 context-path (兼顾新旧版本)
        val contextPath = findValue(listOf("server", "servlet", "context-path"), topValue)
            ?: findValue(listOf("server", "context-path"), topValue)

        if (contextPath != null) result["context-path"] = contextPath

        return result
    }
}