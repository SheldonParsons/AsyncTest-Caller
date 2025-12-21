package com.sheldon.idea.plugin.api.utils

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
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
    lockKey: String? = null,
    // 【优化1】新增参数：控制是否需要读锁。默认为 false (适合网络请求)
    requiresReadAction: Boolean = true,
    backgroundTask: (Project) -> T,
    uiUpdate: (T, Project) -> Unit
) {
    val application = ApplicationManager.getApplication()
    val projectRef = project
    val cacheService = application.getService(GlobalObjectStorageService::class.java)

    // 1. 获取业务锁 (防止重复点击)
    if (lockKey != null) {
        val success = cacheService.acquireLock(lockKey)
        if (!success) {
            Notifier.notifyWarning(projectRef, "操作频繁", "任务正在进行中，请稍候...")
            return
        }
    }

    // 定义释放锁逻辑
    fun safeReleaseLock() {
        if (lockKey != null) {
            cacheService.releaseLock(lockKey)
        }
    }

    // 2. Dumb 模式检查
    // 如果需要 ReadAction (读取代码索引)，必须检查 Dumb 模式
    // 如果只是纯网络请求 (requiresReadAction=false)，其实可以跳过这个检查，但为了保险保留也可以
    if (DumbService.isDumb(projectRef) && requiresReadAction) {
        safeReleaseLock()
        DumbService.getInstance(projectRef).runWhenSmart {
            runBackgroundReadUI(lockKey, requiresReadAction, backgroundTask, uiUpdate)
        }
        return
    }

    // 3. 线程池执行
    application.executeOnPooledThread {
        try {
            // 【优化2】核心逻辑分离
            val result = if (requiresReadAction) {
                // 如果需要读取 PSI/代码结构，必须加锁
                runReadAction { backgroundTask(projectRef) }
            } else {
                // 如果是 OkHttp 网络请求，直接裸奔，绝对不要加锁！
                backgroundTask(projectRef)
            }

            // 4. 回调 UI 线程
            application.invokeLater({
                try {
                    // 【优化3】防崩检查：项目如果已经关闭，不要更新 UI
                    if (!projectRef.isDisposed) {
                        uiUpdate(result, projectRef)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Notifier.notifyError(projectRef, "UI Update Error", "${e.message}")
                } finally {
                    safeReleaseLock()
                }
            }, ModalityState.any()) // 【优化4】指定 ModalityState，确保在设置弹窗中也能回调

        } catch (e: Exception) {
            // 捕获 backgroundTask 中的网络异常或其他错误
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
