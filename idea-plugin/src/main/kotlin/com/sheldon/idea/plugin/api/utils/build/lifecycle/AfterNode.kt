package com.sheldon.idea.plugin.api.utils.build.lifecycle

import com.intellij.openapi.module.Module
import com.sheldon.idea.plugin.api.model.DataStructure
import com.sheldon.idea.plugin.api.utils.ProjectCacheService

object AfterNode {
    fun execute(
        module: Module,
        dsTargetId: String,
        ds: DataStructure
    ) {
        val cacheService = ProjectCacheService.getInstance(project = module.project)
        cacheService.saveOrUpdateSingleDataStructure(module.name, dsTargetId, ds)
        cacheService.addReferToDsPool(module.name, dsTargetId)
    }
}