package com.sheldon.idea.plugin.api.utils.build.lifecycle
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.module.Module
import com.sheldon.idea.plugin.api.constant.CommonConstant
import com.sheldon.idea.plugin.api.model.DataStructure
import com.sheldon.idea.plugin.api.utils.GlobalObjectStorageService
import com.sheldon.idea.plugin.api.utils.ProjectCacheService
object AfterNode {
    fun execute(
        module: Module,
        dsTargetId: String,
        ds: DataStructure,
        hasDocs: Boolean = false
    ) {
        if (!hasDocs) {
            val cacheService = ProjectCacheService.getInstance(project = module.project)
            cacheService.saveOrUpdateSingleDataStructure(module.name, dsTargetId, ds)
            cacheService.addReferToDsPool(module.name, dsTargetId)
        } else {
            val cacheService = ApplicationManager.getApplication().getService(GlobalObjectStorageService::class.java)
            cacheService.save(dsTargetId, ds)
        }
    }
}