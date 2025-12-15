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
    uiUpdate: (T, Project) -> Unit,
    lockKey: String? = null
) {
    val application = ApplicationManager.getApplication()
    val projectRef = project
    val cacheService = ApplicationManager.getApplication()
        .getService(GlobalObjectStorageService::class.java)

    if (lockKey != null) {
        val success = cacheService.acquireLock(lockKey)
        if (!success) {

            Notifier.notifyWarning(projectRef, "操作频繁", "任务正在进行中，请稍候...")
            return
        }
    }

    fun safeReleaseLock() {
        if (lockKey != null) {
            cacheService.releaseLock(lockKey)
        }
    }

    if (DumbService.isDumb(projectRef)) {

        safeReleaseLock()
        DumbService.getInstance(projectRef).runWhenSmart {
            runBackgroundReadUI(backgroundTask, uiUpdate, lockKey)
        }
        return
    }

    application.executeOnPooledThread {
        try {
            val result = runReadAction { backgroundTask(projectRef) }

            application.invokeLater {
                try {
                    uiUpdate(result, projectRef)
                } finally {

                    safeReleaseLock()
                }
            }
        } catch (e: Exception) {
            application.invokeLater {
                try {
                    Notifier.notifyError(projectRef, "Error", "后台任务失败: ${e.message}")
                } finally {

                    safeReleaseLock()
                }
            }
        }
    }
}
