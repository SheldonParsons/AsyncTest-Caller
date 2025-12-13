package com.sheldon.idea.plugin.api.utils

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.DumbService

/**
 * 包装类：作为所有上下文操作的接收者
 */
data class ContextExecutor(val project: Project)

fun Project.context() = ContextExecutor(this)

/**
 * 【Fire-and-Forget / 只执行】
 */
fun ContextExecutor.runBackgroundRead(
    backgroundTask: (Project) -> Unit
) {

    val application = ApplicationManager.getApplication()
    val projectRef = project

    if (DumbService.isDumb(projectRef)) {
        DumbService.getInstance(projectRef).runWhenSmart {
            runBackgroundRead(backgroundTask)
        }
        return // 退出当前执行
    }

    application.executeOnPooledThread {
        try {
            runReadAction { backgroundTask(projectRef) }
        } catch (e: Exception) {
            application.invokeLater {
                Notifier.notifyError(projectRef, "Error", "后台任务失败: ${e.message}")
            }
        }
    }

}


/**
 * 【Background-to-UI / 后台到前台】
 */
fun <T> ContextExecutor.runBackgroundReadUI(
    backgroundTask: (Project) -> T,
    uiUpdate: (T, Project) -> Unit
) {
    val application = ApplicationManager.getApplication()
    val projectRef = project // 捕获 Project 引用

    if (DumbService.isDumb(projectRef)) {
        DumbService.getInstance(projectRef).runWhenSmart {
            runBackgroundReadUI(backgroundTask, uiUpdate)
        }
        return
    }

    application.executeOnPooledThread {
        try {
            val result = runReadAction { backgroundTask(projectRef) }

            application.invokeLater {
                uiUpdate(result, projectRef)
            }
        } catch (e: Exception) {
            application.invokeLater {
                Notifier.notifyError(projectRef, "Error", "后台任务失败: ${e.message}")
            }
        }
    }
}
