package com.sheldon.idea.plugin.api.front.dashboard

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.panel
import com.jetbrains.rd.util.first
import com.sheldon.idea.plugin.api.front.dashboard.component.ApiTreePanel
import com.sheldon.idea.plugin.api.front.dashboard.component.ModuleSelector
import com.sheldon.idea.plugin.api.front.dashboard.utils.TreeAction

class ApiDashboardToolWindow : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val dashboardPanel = ApiDashboardPanel(project)
        val contentFactory = ContentFactory.getInstance()
        val content = contentFactory.createContent(dashboardPanel.getContent(), "接口", true)
        toolWindow.contentManager.addContent(content)
        TreeAction.reloadTree(project) { treeMap, _ ->
            val rootNode = treeMap.first().value
            dashboardPanel.moduleSelector.updateDropdown(treeMap.keys)
            dashboardPanel.moduleSelector.isEditable = true
            dashboardPanel.treePanel.renderApiTree(rootNode)
        }
    }


    class ApiDashboardPanel(private val project: Project) {

        val treePanel = ApiTreePanel()

        val moduleSelector = ModuleSelector(project, treePanel)

        fun getContent() = panel {
            // === 第一行：操作栏 ===
            row {
                button("刷新所有服务") {
                    TreeAction.reloadTree(project, force = true) { treeMap, _ ->
                        val rootNode = treeMap.first().value
                        treePanel.renderApiTree(rootNode)
                        moduleSelector.updateDropdown(treeMap.keys)
                    }
                }

                cell(moduleSelector).align(Align.FILL)
            }
            // 树结构
            row {
                cell(treePanel).align(Align.FILL)
            }
        }.apply {
            // 给整个面板加点内边距，好看点
            border = javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10)
        }

    }
}