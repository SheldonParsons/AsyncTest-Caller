package com.sheldon.idea.plugin.api.utils
import com.intellij.notification.*
import com.intellij.openapi.project.Project
object Notifier {
    private const val NOTIFICATION_GROUP_ID = "AsyncTest Notifications"
    private val NOTIFICATION_GROUP by lazy {
        NotificationGroupManager.getInstance()
            .getNotificationGroup(NOTIFICATION_GROUP_ID)
    }
    fun notifyInfo(project: Project?, title: String = "AsyncTest Caller", content: String) {
        NOTIFICATION_GROUP
            .createNotification(title, content, NotificationType.INFORMATION)
            .notify(project)
    }
    fun notifyError(project: Project?, title: String = "AsyncTest Caller", content: String) {
        NOTIFICATION_GROUP
            .createNotification(title, content, NotificationType.ERROR)
            .notify(project)
    }
    fun notifyWarning(project: Project?, title: String = "AsyncTest Caller", content: String = "") {
        NOTIFICATION_GROUP
            .createNotification(title, content, NotificationType.WARNING)
            .notify(project)
    }
}