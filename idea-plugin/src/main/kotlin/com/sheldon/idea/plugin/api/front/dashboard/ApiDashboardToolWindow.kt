package com.sheldon.idea.plugin.api.front.dashboard

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.panel
import com.jetbrains.rd.util.first
import com.sheldon.idea.plugin.api.front.dashboard.panel.ApiTreePanel
import com.sheldon.idea.plugin.api.front.dashboard.utils.ApiTreeHelper
import com.sheldon.idea.plugin.api.front.dashboard.utils.TreeAction
import javax.swing.JTextArea

class ApiDashboardToolWindow : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val dashboardPanel = ApiDashboardPanel(project)
        val contentFactory = ContentFactory.getInstance()
        val content = contentFactory.createContent(dashboardPanel.getContent(), "接口", true)
        toolWindow.contentManager.addContent(content)
        TreeAction.reloadTree(project) { treeMap, _ ->
            val rootNode = treeMap.first().value
            dashboardPanel.treePanel.renderApiTree(rootNode)
        }
    }


    class ApiDashboardPanel(private val project: Project) {

        val treePanel = ApiTreePanel()

        // 创建一个文本区域，用来显示扫描日志
        private val logArea = JTextArea().apply {
            isEditable = false
            text = "准备就绪... 点击上方按钮开始扫描 Spring 接口"
        }

        fun getContent() = panel {
            // === 第一行：操作栏 ===
            row {
                button("重置树结构（全局）") {
                    TreeAction.reloadTree(project, force = true) { treeMap, _ ->
                        println("")
                        val rootNode = treeMap.first().value
                        treePanel.renderApiTree(rootNode)
                    }
                }
            }
            row {
                cell(treePanel).align(Align.FILL)
            }
        }.apply {
            // 给整个面板加点内边距，好看点
            border = javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10)
        }

    }
}