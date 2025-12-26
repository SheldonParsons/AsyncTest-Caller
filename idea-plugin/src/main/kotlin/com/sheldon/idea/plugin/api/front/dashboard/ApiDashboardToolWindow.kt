package com.sheldon.idea.plugin.api.front.dashboard

import com.google.gson.GsonBuilder
import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.JBSplitter
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.panel
import com.jetbrains.rd.util.first
import com.sheldon.idea.plugin.api.front.dashboard.component.ApiDebugPanel
import com.sheldon.idea.plugin.api.front.dashboard.component.ApiTreePanel
import com.sheldon.idea.plugin.api.front.dashboard.component.ModuleSelector
import com.sheldon.idea.plugin.api.front.dashboard.utils.TreeAction
import com.sheldon.idea.plugin.api.model.ApiMockRequest
import java.awt.BorderLayout
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel

class ApiDashboardToolWindow : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val dashboardPanel = ApiDashboardPanel(project)
        val contentFactory = ContentFactory.getInstance()
        val content = contentFactory.createContent(dashboardPanel.getContent(), "接口", true)
        toolWindow.contentManager.addContent(content)
        TreeAction.reloadTree(project) { treeMap, _ ->
//            val gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()
//            val jsonString = gson.toJson(treeMap)
//            println("jsonString: $jsonString")
            val rootNode = treeMap.first().value
            dashboardPanel.moduleSelector.updateDropdown(treeMap.keys)
            dashboardPanel.moduleSelector.isEditable = true
            dashboardPanel.treePanel.renderApiTree(rootNode)
        }
    }

    class ApiDashboardPanel(private val project: Project) {
        val treePanel = ApiTreePanel(project)
        val moduleSelector = ModuleSelector(project, treePanel)
        val debugPanel = ApiDebugPanel(project)
        fun getContent(): JComponent {
            // 1. 顶部工具栏 (保持原有的 layout 逻辑)
            val topBar = panel {
                row {
                    button("") { event ->
                        val btn = event.source as JButton
                        btn.icon = AnimatedIcon.Default()
                        btn.isEnabled = false
                        TreeAction.reloadTree(project, force = true) { treeMap, _ ->
                            if (treeMap.isNotEmpty()) {
                                val rootNode = treeMap.first().value
                                treePanel.renderApiTree(rootNode)
                                moduleSelector.updateDropdown(treeMap.keys)
                            }
                            btn.icon = AllIcons.Actions.Refresh
                            btn.isEnabled = true
                        }
                    }.applyToComponent {
                        icon = AllIcons.Actions.Refresh
                        toolTipText = "刷新所有服务"
                        isContentAreaFilled = false
                    }
                    cell(moduleSelector).align(Align.FILL)
                }
            }.apply {
                // 给顶部留一点边距
                border = javax.swing.BorderFactory.createEmptyBorder(5, 10, 5, 10)
            }

            // 2. 创建分割面板 (Splitter)
            // true 表示垂直分割 (上下结构)，0.6f 表示分割比例
            val splitter = JBSplitter(true, 0.6f)
            splitter.firstComponent = treePanel // 上面是树
            splitter.secondComponent = null // 下面是调试区

            treePanel.onNodeSelected = { mockRequest ->
                if (splitter.secondComponent == null) {
                    splitter.secondComponent = debugPanel
                }
                debugPanel.setData(mockRequest)
            }

            // 3. 组合最终面板
            val mainPanel = JPanel(BorderLayout())
            mainPanel.add(topBar, BorderLayout.NORTH)
            mainPanel.add(splitter, BorderLayout.CENTER)

            return mainPanel
        }
    }
}