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
        if (value !is DefaultMutableTreeNode) return
        val apiNode = value.userObject as? ApiNode ?: return
        icon = when (apiNode.code_type) {
            0 -> AllIcons.Nodes.Module
            1 -> AllIcons.Nodes.Module
            2 -> AllIcons.Nodes.Class
            3 -> AllIcons.Nodes.Method
            else -> AllIcons.General.Warning
        }
        if (apiNode.code_type == 3) {
            val methodAttr = SimpleTextAttributes(
                SimpleTextAttributes.STYLE_BOLD,
                getMethodColor(apiNode.method)
            )
            append("[${apiNode.method}] ", methodAttr)
        }
        append(apiNode.name, SimpleTextAttributes.REGULAR_ATTRIBUTES)
        if (apiNode.code_type == 3) {
            append("  (${apiNode.path ?: ""})", SimpleTextAttributes.GRAYED_ATTRIBUTES)
        } else {
            val comment = apiNode.alias ?: apiNode.desc
            if (!comment.isNullOrEmpty()) {
                append("  ($comment)", SimpleTextAttributes.GRAYED_ATTRIBUTES)
            }
        }
    }
    private fun getMethodColor(method: String?): java.awt.Color? {
        return when (method?.uppercase()) {
            "GET" -> java.awt.Color(73, 156, 84)
            "POST" -> java.awt.Color(73, 126, 235)
            "DELETE" -> java.awt.Color(200, 80, 80)
            "PUT" -> java.awt.Color(200, 150, 50)
            else -> null
        }
    }
}