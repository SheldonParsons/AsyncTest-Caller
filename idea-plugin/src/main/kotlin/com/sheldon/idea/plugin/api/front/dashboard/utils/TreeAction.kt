package com.sheldon.idea.plugin.api.front.dashboard.utils

import com.google.gson.GsonBuilder
import com.intellij.openapi.project.Project
import com.sheldon.idea.plugin.api.constant.CommonConstant
import com.sheldon.idea.plugin.api.model.ApiNode
import com.sheldon.idea.plugin.api.model.GlobalSettings
import com.sheldon.idea.plugin.api.model.ModuleRequestMapping
import com.sheldon.idea.plugin.api.utils.Notifier
import com.sheldon.idea.plugin.api.utils.ProjectCacheService
import com.sheldon.idea.plugin.api.utils.build.BuildController
import com.sheldon.idea.plugin.api.utils.context
import com.sheldon.idea.plugin.api.utils.runBackgroundReadUI


object TreeAction {

    fun reloadTree(project: Project, force: Boolean = false, ui: (MutableMap<String, ApiNode>, Project) -> Unit) {
        project.context()
            .runBackgroundReadUI(
                lockKey = CommonConstant.AST_CALLER_GLOBAL_ACTION,
                backgroundTask = { p ->
                    val cacheService = ProjectCacheService.getInstance(p)
                    val globalSettings: GlobalSettings = cacheService.getGlobalSettings()
                    println("----cache----")
                    println(globalSettings)
                    println(globalSettings.publicServerUrl)
                    val result =
                        if (force) BuildController().reloadProjectForce(p) else BuildController().reloadProjectCheck(p)
                    println(cacheService.getModuleRequests("spring-for-plugins-test"))
                    result
                }, uiUpdate = { resultRoots, p ->
                    ui(resultRoots, p)
                    val gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()

                    // 2. 序列化并打印
                    val jsonString = gson.toJson(resultRoots)

                    println("====== API 树结构生成结果 ======")
                    println(jsonString)
                    println("===============================")
                    Notifier.notifyInfo(p, "Success", "扫描完成，找到 ${resultRoots.size} 个模块！")
                })
    }
}