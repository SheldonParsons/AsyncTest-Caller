package com.sheldon.idea.plugin.api.utils.build.lifecycle
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.module.Module
import com.sheldon.idea.plugin.api.model.ApiRequest
import com.sheldon.idea.plugin.api.utils.GlobalObjectStorageService
import com.sheldon.idea.plugin.api.utils.MockGenerator
import com.sheldon.idea.plugin.api.utils.ProjectCacheService
class AfterBuildRequest(val request: ApiRequest) {
    fun execute(
        project: Project,
        module: Module,
        saveMock: Boolean = true,
        prefix: String = "",
        hasDocs: Boolean = false
    ): String {
        if (!hasDocs) {
            val cacheService = ProjectCacheService.getInstance(project = project)
            val requestKey =
                cacheService.saveOrUpdateSingleRequest(module.name, request)
            if (!requestKey.isNullOrEmpty()) {
                if (saveMock) {
                    val mockData = MockGenerator(module).generate(request, prefix)
                    cacheService.saveOrUpdateSingleRequestMock(
                        module.name,
                        requestKey,
                        mockData
                    )
                }
                cacheService.addReferToMethodPathPool(module.name, requestKey)
            }
            return requestKey ?: ""
        } else {
            val cacheService = ApplicationManager.getApplication().getService(GlobalObjectStorageService::class.java)
            val key = "${request.method!!.lowercase()}:${request.path}"
            cacheService.save(key, request)
            return ""
        }
    }
}