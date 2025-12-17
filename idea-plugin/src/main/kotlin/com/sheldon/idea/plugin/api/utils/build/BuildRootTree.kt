package com.sheldon.idea.plugin.api.utils.build

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiManager
import com.sheldon.idea.plugin.api.model.ApiNode


class BuildRootTree(private val project: Project) : TreeBuilder() {
    fun build(nextBuild: (PsiDirectory, ApiNode, String, Module) -> Unit): MutableMap<String, ApiNode> {
        return runReadAction {
            val resultRoots = mutableMapOf<String, ApiNode>()
            val modules = ModuleManager.getInstance(project).modules
            for (module in modules) {
                // 1. 创建 Module 节点
                val moduleNode = makeRootNode(module)
                val contentEntries = ModuleRootManager.getInstance(module).contentEntries
                    .flatMap { it.sourceFolders.asList() }
                for (folder in contentEntries) {
                    val url = folder.url
                    if (!url.contains("${module.name}/src/main/java")) continue
                    val rootDir = PsiManager.getInstance(project).findDirectory(folder.file ?: continue)
                        ?: continue
                    val baseDir = findBasePackageDirectory(rootDir) ?: rootDir
                    nextBuild(baseDir, moduleNode, moduleNode.treePath, module)
                }

                if (moduleNode.children.isNotEmpty()) {
                    resultRoots[module.name] = moduleNode
                    println("${module.name}.size:${moduleNode.children.size}:${moduleNode.children.isNotEmpty()}")
                }
            }
            return@runReadAction resultRoots
        }
    }
}