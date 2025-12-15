package com.sheldon.idea.plugin.api.utils.build.lifecycle

import com.intellij.openapi.project.Project
import com.intellij.openapi.module.Module
import com.sheldon.idea.plugin.api.model.ApiRequest
import com.sheldon.idea.plugin.api.utils.MockGenerator
import com.sheldon.idea.plugin.api.utils.ProjectCacheService

class AfterBuildRequest(val request: ApiRequest) {

    fun execute(project: Project, module: Module, saveMock: Boolean = true): String {
        val cacheService = ProjectCacheService.getInstance(project = project)
        // 存储请求
        val requestKey =
            cacheService.saveOrUpdateSingleRequest(module.name, request)

        if (!requestKey.isNullOrEmpty()) {
            val mockData = MockGenerator(module).generate(request)
            println("mockData:${mockData}")
            // 存储mock
            cacheService.saveOrUpdateSingleRequestMock(
                module.name,
                requestKey,
                mockData
            )
            // 存储method+path
            cacheService.addReferToMethodPathPool(module.name, requestKey)
        }
        return requestKey ?: ""
    }
}