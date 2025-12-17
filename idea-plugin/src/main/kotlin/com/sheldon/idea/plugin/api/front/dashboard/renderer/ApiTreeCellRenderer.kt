package com.sheldon.idea.plugin.api.front.dashboard.renderer

import com.intellij.icons.AllIcons
import com.intellij.ui.ColoredTreeCellRenderer
import com.intellij.ui.SimpleTextAttributes
import com.sheldon.idea.plugin.api.model.ApiNode
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode

class ApiTreeCellRenderer : ColoredTreeCellRenderer() {

    override fun customizeCellRenderer(
        tree: JTree,
        value: Any?,
        selected: Boolean,
        expanded: Boolean,
        leaf: Boolean,
        row: Int,
        hasFocus: Boolean
    ) {
        // 1. 安全类型转换
        if (value !is DefaultMutableTreeNode) return
        val apiNode = value.userObject as? ApiNode ?: return

        // 2. 设置图标 (根据你的 type/code_type 逻辑来定)
        // 这里的逻辑你需要根据你的业务微调，我先举个例子
        icon = when (apiNode.code_type) {
            0 -> AllIcons.Nodes.Module
            1 -> AllIcons.Nodes.Module // 模块/文件夹
            2 -> AllIcons.Nodes.Class  // 类
            3 -> AllIcons.Nodes.Method
            else -> AllIcons.General.Warning
        }

        // 3. 拼接文本 (关键！)

        // A. 如果有 HTTP 方法，先显示方法名 (加粗 + 颜色)
        if (apiNode.code_type == 3) {
            val methodAttr = SimpleTextAttributes(
                SimpleTextAttributes.STYLE_BOLD,
                getMethodColor(apiNode.method) // 自定义颜色函数
            )
            append("[${apiNode.method}] ", methodAttr)
        }
        // B. 显示名称

        append(apiNode.name, SimpleTextAttributes.REGULAR_ATTRIBUTES)
        // C. 显示描述 (灰色 + 小字)
        if (apiNode.code_type == 3) {
            append("  (${apiNode.path ?: ""})", SimpleTextAttributes.GRAYED_ATTRIBUTES)
        } else {
            val comment = apiNode.alias ?: apiNode.desc
            if (!comment.isNullOrEmpty()) {
                append("  ($comment)", SimpleTextAttributes.GRAYED_ATTRIBUTES)
            }
        }
    }

    // 辅助：给 HTTP 方法一点颜色
    private fun getMethodColor(method: String?): java.awt.Color? {
        return when (method?.uppercase()) {
            "GET" -> java.awt.Color(73, 156, 84) // 绿色
            "POST" -> java.awt.Color(73, 126, 235) // 蓝色
            "DELETE" -> java.awt.Color(200, 80, 80) // 红色
            "PUT" -> java.awt.Color(200, 150, 50) // 橙色
            else -> null // 默认色
        }
    }
}