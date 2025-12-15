package com.sheldon.idea.plugin.api.service

import com.intellij.openapi.project.Project
import com.sheldon.idea.plugin.api.model.ApiNode
import com.sheldon.idea.plugin.api.utils.ScanSession
import com.sheldon.idea.plugin.api.utils.build.BuildControllerNode
import com.sheldon.idea.plugin.api.utils.build.BuildDirectoryTree
import com.sheldon.idea.plugin.api.utils.build.BuildRootTree
import com.sheldon.idea.plugin.api.utils.build.MethodNodeBuilder

/**
 * 核心扫描服务：用于查找项目中的所有 Spring Web Controller
 */
class SpringWebScanner(private val project: Project, val session: ScanSession) {
    /**
     * 主入口：扫描所有模块并生成树
     */
    fun scanAndBuildTree(): MutableMap<String, ApiNode> {
        // 第一次遍历加载method
        println("--- First Scan ---")
        val routeRegistry = MethodNodeBuilder(project, session).scan()
        println("--- Second Scan ---")
        // 第二次遍历，挂在method
        return BuildRootTree(project).build { directory, parentNode, pathPrefix, module ->
            BuildDirectoryTree(module, project).build(
                directory,
                parentNode,
                pathPrefix,
            ) { psiClass, pathPrefix ->
                BuildControllerNode(module, project).build(psiClass, pathPrefix, routeRegistry)
            }
        }
    }
}