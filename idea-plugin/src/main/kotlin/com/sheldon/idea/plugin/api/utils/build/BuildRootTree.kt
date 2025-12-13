package com.sheldon.idea.plugin.api.utils.build

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiManager
import com.sheldon.idea.plugin.api.model.ApiNode
import com.sheldon.idea.plugin.api.utils.ProjectCacheService

class BuildRootTree(private val project: Project) : TreeBuilder() {
    fun build(nextBuild: (PsiDirectory, ApiNode, String, Module) -> Unit): List<ApiNode> {
        return runReadAction {
            val resultRoots = mutableListOf<ApiNode>()
            val modules = ModuleManager.getInstance(project).modules
            val cacheService = ProjectCacheService.getInstance(project)
            for (module in modules) {
                // 1. 创建 Module 节点
                val moduleNode = makeRootNode(module, cacheService)
                val contentEntries = ModuleRootManager.getInstance(module).contentEntries
                    .flatMap { it.sourceFolders.asList() }
                for (folder in contentEntries) {
                    val url = folder.url
                    if (!url.contains("${module.name}/src/main/java")) continue
                    val rootDir = PsiManager.getInstance(project).findDirectory(folder.file ?: continue)
                        ?: continue
                    val baseDir = findBasePackageDirectory(rootDir) ?: rootDir
                    nextBuild(baseDir, moduleNode, module.name, module)
                }
                resultRoots.add(moduleNode)
            }
            return@runReadAction resultRoots
        }

    }
}