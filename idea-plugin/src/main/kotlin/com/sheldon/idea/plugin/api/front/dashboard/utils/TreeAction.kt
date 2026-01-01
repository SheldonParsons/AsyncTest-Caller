package com.sheldon.idea.plugin.api.front.dashboard.utils
import com.intellij.openapi.project.Project
import com.sheldon.idea.plugin.api.constant.CommonConstant
import com.sheldon.idea.plugin.api.model.ApiNode
import com.sheldon.idea.plugin.api.model.GlobalSettings
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
                    val result = if (force) {
                        BuildController().reloadProjectForce(p)
                    } else {
                        BuildController().reloadProjectCheck(p)
                    }
                    result
                }, uiUpdate = { resultRoots, p ->
                    ui(resultRoots, p)
                    Notifier.notifyInfo(p, "Success", "扫描完成，找到 ${resultRoots.size} 个模块！")
                })
    }
}