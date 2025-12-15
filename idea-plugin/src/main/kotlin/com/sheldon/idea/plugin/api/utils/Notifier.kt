package com.sheldon.idea.plugin.api.utils

import com.intellij.notification.*
import com.intellij.openapi.project.Project

/**
 * 插件通知管理器。
 * object 确保它是单例，全局可访问。
 */
object Notifier {

    // plugin.xml 中的 id 保持一致
    private const val NOTIFICATION_GROUP_ID = "AsyncTest Notifications"

    // 懒加载：当第一次使用时，从 IDEA 注册中心获取通知组
    private val NOTIFICATION_GROUP = NotificationGroupManager.getInstance()
        .getNotificationGroup(NOTIFICATION_GROUP_ID)

    /**
     * 发送气泡通知
     * @param project 当前项目 (可以为 null)
     * @param title 标题
     * @param content 内容
     */
    fun notifyInfo(project: Project?, title: String, content: String) {
        NOTIFICATION_GROUP
            .createNotification(title, content, NotificationType.INFORMATION)
            .notify(project)
    }

    /**
     * 发送错误 (红色) 气泡通知
     */
    fun notifyError(project: Project?, title: String, content: String) {
        NOTIFICATION_GROUP
            .createNotification(title, content, NotificationType.ERROR)
            .notify(project)
    }

    /**
     * 发送警告 (黄色) 气泡通知
     */
    fun notifyWarning(project: Project?, title: String, content: String) {
        NOTIFICATION_GROUP
            .createNotification(title, content, NotificationType.WARNING)
            .notify(project)
    }
}