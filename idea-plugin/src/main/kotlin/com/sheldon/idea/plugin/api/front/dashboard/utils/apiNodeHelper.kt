package com.sheldon.idea.plugin.api.front.dashboard.utils

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.wm.ToolWindow
import com.sheldon.idea.plugin.api.front.dashboard.panel.ApiTreePanel
import com.sheldon.idea.plugin.api.model.ApiNode
import javax.swing.tree.DefaultMutableTreeNode

fun ApiNode.toTreeNode(): DefaultMutableTreeNode {
    val treeNode = DefaultMutableTreeNode(this)

    this.children?.forEach { childApiNode ->
        treeNode.add(childApiNode.toTreeNode())
    }

    return treeNode
}


object ApiTreeHelper {

    /**
     * 静态方法：从外部更新 API 树
     * @param project 当前项目
     * @param rootNode 你准备好的数据
     */
    fun refreshTree(toolWindow: ToolWindow, rootNode: ApiNode) {
        // 1. UI 操作必须在 EDT 线程执行
        ApplicationManager.getApplication().invokeLater {
            // 3. 防御性检查：窗口可能还没初始化（用户没点开过）
            if (toolWindow == null || !toolWindow.isAvailable) {
                return@invokeLater
            }

            // 4. 获取 Content (通常只有一个)
            val content = toolWindow.contentManager.getContent(0) ?: return@invokeLater

            // 5. 获取 Panel (这就是你的 ApiTreePanel 实例！)
            // content.component 返回的是 JComponent，我们需要强转回 ApiTreePanel
            val panel = content.component as? ApiTreePanel

            // 6. 调用方法
            if (panel != null) {
                panel.renderApiTree(rootNode)

                // 可选：如果窗口是隐藏的，自动让它显示出来
                // toolWindow.show(null)
            }
        }
    }
}