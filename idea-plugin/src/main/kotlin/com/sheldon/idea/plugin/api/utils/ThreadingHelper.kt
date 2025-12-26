package com.sheldon.idea.plugin.api.utils

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.DumbService


data class ContextExecutor(val project: Project)

fun Project.context() = ContextExecutor(this)

fun ContextExecutor.runBackgroundRead(
    backgroundTask: (Project) -> Unit
) {
    val application = ApplicationManager.getApplication()
    val projectRef = project
    if (DumbService.isDumb(projectRef)) {
        DumbService.getInstance(projectRef).runWhenSmart {
            runBackgroundRead(backgroundTask)
        }
        return
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

fun <T> ContextExecutor.runBackgroundReadUI(
    lockKey: String? = null,
    requiresReadAction: Boolean = true,
    backgroundTask: (Project) -> T,
    uiUpdate: (T, Project) -> Unit
) {
    val application = ApplicationManager.getApplication()
    val projectRef = project
    val cacheService = application.getService(GlobalObjectStorageService::class.java)
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
    if (DumbService.isDumb(projectRef) && requiresReadAction) {
        safeReleaseLock()
        DumbService.getInstance(projectRef).runWhenSmart {
            runBackgroundReadUI(lockKey, requiresReadAction, backgroundTask, uiUpdate)
        }
        return
    }
    application.executeOnPooledThread {
        try {
            val result = if (requiresReadAction) {
                runReadAction { backgroundTask(projectRef) }
            } else {
                backgroundTask(projectRef)
            }
            application.invokeLater({
                try {
                    if (!projectRef.isDisposed) {
                        uiUpdate(result, projectRef)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Notifier.notifyError(projectRef, "UI Update Error", "${e.message}")
                } finally {
                    safeReleaseLock()
                }
            }, ModalityState.any())
        } catch (e: Exception) {
            application.invokeLater({
                try {
                    if (!projectRef.isDisposed) {
                        Notifier.notifyError(projectRef, "Task Error", "后台任务失败: ${e.message}")
                    }
                } finally {
                    safeReleaseLock()
                }
            }, ModalityState.any())
        }
    }
}
