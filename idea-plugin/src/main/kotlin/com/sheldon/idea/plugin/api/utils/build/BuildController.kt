package com.sheldon.idea.plugin.api.utils.build
import com.intellij.openapi.project.Project
import com.sheldon.idea.plugin.api.model.ApiNode
import com.sheldon.idea.plugin.api.service.SpringWebScanner
import com.sheldon.idea.plugin.api.utils.ProjectCacheService
import com.sheldon.idea.plugin.api.utils.ScanSession
import com.sheldon.idea.plugin.api.utils.scanContext
class BuildController {
    fun reloadProjectForce(project: Project, saveMock: Boolean = true): MutableMap<String, ApiNode> {
        val cacheService = ProjectCacheService.getInstance(project = project)
        cacheService.cleanModuleTree()
        cacheService.cleanModuleRequests()
        cacheService.cleanModuleRequestMocks()
        cacheService.cleanModuleDs()
        cacheService.cleanModuleDsPool()
        cacheService.cleanModuleMethodPathPool()
        scanContext(ScanSession(saveMock = saveMock)) { session ->
            val projectTreeMap = SpringWebScanner(project, session).scanAndBuildTree()
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
            return projectTreeMap
        }
    }
}