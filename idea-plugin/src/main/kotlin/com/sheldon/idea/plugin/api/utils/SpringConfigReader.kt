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

    fun getSpringBaseUrl(module: Module): String {
        val config = readSpringConfig(module)
        val port = config["port"]?.takeIf { it.isNotBlank() } ?: DEFAULT_PORT
        var contextPath = config["context-path"] ?: ""
        if (contextPath.isNotEmpty() && !contextPath.startsWith("/")) {
            contextPath = "/$contextPath"
        }
        if (contextPath == "/") {
            contextPath = ""
        }
        return "$DEFAULT_HOST:$port$contextPath"
    }

    private fun readSpringConfig(module: Module): Map<String, String> {
        val project = module.project
        val scope = GlobalSearchScope.moduleRuntimeScope(module, false)
        val finalConfig = mutableMapOf<String, String>()
        val fileNames = listOf(
            "bootstrap.properties" to false,
            "bootstrap.yml" to true,
            "application.properties" to false,
            "application.yml" to true
        )
        for ((fileName, isYaml) in fileNames) {
            val files = FilenameIndex.getVirtualFilesByName(fileName, scope)
            if (files.isNotEmpty()) {
                val file = files.first()
                val configMap = if (isYaml) {
                    parseYaml(project, file)
                } else {
                    parseProperties(project, file)
                }
                finalConfig.putAll(configMap)
            }
        }
        return finalConfig
    }

    private fun parseProperties(project: Project, file: VirtualFile): Map<String, String> {
        val psiFile = PsiManager.getInstance(project).findFile(file) as? PropertiesFile ?: return emptyMap()
        val result = mutableMapOf<String, String>()
        psiFile.findPropertyByKey("server.port")?.value?.let { result["port"] = it }
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
        findValue(listOf("server", "port"), topValue)?.let { result["port"] = it }
        val contextPath = findValue(listOf("server", "servlet", "context-path"), topValue) ?: findValue(
            listOf(
                "server",
                "context-path"
            ), topValue
        )
        if (contextPath != null) result["context-path"] = contextPath
        return result
    }
}