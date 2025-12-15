package com.sheldon.idea.plugin.api.utils.build

import com.intellij.openapi.project.Project
import com.intellij.openapi.module.Module
import com.sheldon.idea.plugin.api.model.ApiNode
import com.sheldon.idea.plugin.api.service.SpringWebScanner
import com.sheldon.idea.plugin.api.utils.ProjectCacheService
import com.sheldon.idea.plugin.api.utils.ScanSession
import com.sheldon.idea.plugin.api.utils.scanContext

class BuildController {

    // 清除所有缓存数据（除了系统数据），重新创建树结构
    fun reloadProjectForce(project: Project, saveMock: Boolean = true): MutableMap<String, ApiNode> {
        val cacheService = ProjectCacheService.getInstance(project = project)
        // 清除所有tree
        cacheService.cleanModuleTree()
        // 清除所有request
        cacheService.cleanModuleRequests()
        // 清除所有所有mock
        cacheService.cleanModuleRequestMocks()
        // 清除所有DataStructure
        cacheService.cleanModuleDs()
        // 清除所有ds pool
        cacheService.cleanModuleDsPool()
        // 清除所有method+path pool
        cacheService.cleanModuleMethodPathPool()
        scanContext(ScanSession(saveMock = saveMock)) { session ->
            val projectTreeMap = SpringWebScanner(project, session).scanAndBuildTree()
            // TODO:需要将tree等信息放到GlobalObjectStorageService上下文中
            // 存树
            projectTreeMap.map { (moduleName, value) ->
                cacheService.saveModuleTree(moduleName, value)
            }

            return projectTreeMap
        }
    }

    fun reloadProjectCheck(project: Project, saveMock: Boolean = true): MutableMap<String, ApiNode> {
        val cacheService = ProjectCacheService.getInstance(project = project)
        val projectTreeMap = cacheService.getTreeMap()
        if (projectTreeMap.isEmpty()) {
            return reloadProjectForce(project, saveMock)
        } else {
            val newTreeMap = mutableMapOf<String, ApiNode>()
            projectTreeMap.map { (moduleName, value) ->
                ProjectCacheService().safeFromJson<ApiNode>(value)?.let { newTreeMap[moduleName] = it }
            }
            return newTreeMap
        }
    }

    fun reloadModule(module: Module) {

    }

    fun reloadNode(nodePath: String) {

    }
}