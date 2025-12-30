package com.sheldon.idea.plugin.api.front.dashboard

import com.google.gson.GsonBuilder
import com.intellij.icons.AllIcons
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.DumbService
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
import com.sheldon.idea.plugin.api.utils.Notifier
import java.awt.BorderLayout
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel

class ApiDashboardToolWindow : ToolWindowFactory, DumbAware {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val dashboardPanel = ApiDashboardPanel(project)
        val contentFactory = ContentFactory.getInstance()
        val content = contentFactory.createContent(dashboardPanel.getContent(), "接口", true)
        toolWindow.contentManager.addContent(content)
        DumbService.getInstance(project).runWhenSmart {
            dashboardPanel.loadData(isForceRefresh = false)
        }
    }

    class ApiDashboardPanel(private val project: Project) {
        val treePanel = ApiTreePanel(project)
        val moduleSelector = ModuleSelector(project, treePanel)
        val debugPanel = ApiDebugPanel(project)
        private var refreshButton: JButton? = null
        fun getContent(): JComponent {
            val topBar = panel {
                row {
                    button("") {
                        loadData(isForceRefresh = true)
                    }.applyToComponent {
                        refreshButton = this
                        icon = AllIcons.Actions.Refresh
                        toolTipText = "刷新所有服务"
                        isContentAreaFilled = false
                    }
                    cell(moduleSelector).align(Align.FILL)
                }
            }.apply {
                border = javax.swing.BorderFactory.createEmptyBorder(5, 10, 5, 10)
            }

            val splitter = JBSplitter(true, 0.6f)
            splitter.firstComponent = treePanel
            splitter.secondComponent = null

            debugPanel.onClose = {
                splitter.secondComponent = null
            }
            treePanel.onCloseMock = {
                splitter.secondComponent = null
            }

            treePanel.onNodeSelected = { mockRequest, moduleName ->
                if (splitter.secondComponent == null) {
                    splitter.secondComponent = debugPanel
                }
                debugPanel.setData(mockRequest, moduleName)
            }

            val mainPanel = JPanel(BorderLayout())
            mainPanel.add(topBar, BorderLayout.NORTH)
            mainPanel.add(splitter, BorderLayout.CENTER)

            return mainPanel
        }

        fun loadData(isForceRefresh: Boolean) {
            if (DumbService.isDumb(project)) {
                Notifier.notifyWarning(project, content = "索引正在构建中，请稍后刷新")
                return
            }

            refreshButton?.let {
                it.icon = AnimatedIcon.Default()
                it.isEnabled = false
            }

            TreeAction.reloadTree(project, force = isForceRefresh) { treeMap, _ ->
                try {
                    if (treeMap.isEmpty()) {
                        Notifier.notifyWarning(project, content = "未扫描到接口，请确认工程结构是否正确")
                    } else {
//                        val gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()
//                        val jsonString = gson.toJson(treeMap)
//                        println("jsonString: $jsonString")
                        val rootNode = treeMap.first().value
                        treePanel.renderApiTree(rootNode)
                        moduleSelector.updateDropdown(treeMap.keys)
                        moduleSelector.isEditable = true
                    }
                } finally {
                    refreshButton?.let {
                        it.icon = AllIcons.Actions.Refresh
                        it.isEnabled = true
                    }
                }
            }
        }
    }
}