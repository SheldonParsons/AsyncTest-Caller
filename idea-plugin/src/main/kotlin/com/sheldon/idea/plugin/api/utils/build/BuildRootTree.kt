package com.sheldon.idea.plugin.api.utils.build

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiManager
import com.sheldon.idea.plugin.api.model.ApiNode
import kotlin.collections.set

class BuildRootTree(private val project: Project) : TreeBuilder() {
    fun build(nextBuild: (PsiDirectory, ApiNode, String, Module) -> Unit): MutableMap<String, ApiNode> {
        return runReadAction {
            val resultRoots = mutableMapOf<String, ApiNode>()
            val modules = ModuleManager.getInstance(project).modules
            for (module in modules) {
                println("module:${module}")
                val moduleNode = buildModule(module, nextBuild)
                if (moduleNode.children.isNotEmpty()) {
                    resultRoots[module.name] = moduleNode
                    println("${module.name}.size:${moduleNode.children.size}:${moduleNode.children.isNotEmpty()}")
                }
            }
            return@runReadAction resultRoots
        }
    }

    fun buildModule(module: Module, nextBuild: (PsiDirectory, ApiNode, String, Module) -> Unit): ApiNode {
        val moduleNode = makeRootNode(module)
        val baseDir = getBaseDir(module)
        println("baseDir:${baseDir}")
        if (baseDir != null) {
            nextBuild(baseDir, moduleNode, moduleNode.tree_path, module)
        }
        return moduleNode
    }

    fun getBaseDir(module: Module): PsiDirectory? {
        var baseDir: PsiDirectory? = null
        val contentEntries = ModuleRootManager.getInstance(module).contentEntries.flatMap { it.sourceFolders.asList() }
        for (folder in contentEntries) {
            val url = folder.url
            if (!url.contains("/src/main/java")) continue
            val rootDir = PsiManager.getInstance(project).findDirectory(folder.file ?: continue) ?: continue
            baseDir = findBasePackageDirectory(rootDir)
            if (baseDir != null) {
                break
            } else {
                baseDir = rootDir
            }
        }
        return baseDir
    }
}