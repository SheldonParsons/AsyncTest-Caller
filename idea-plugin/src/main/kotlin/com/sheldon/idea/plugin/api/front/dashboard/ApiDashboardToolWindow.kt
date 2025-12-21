package com.sheldon.idea.plugin.api.front.dashboard

import com.google.gson.GsonBuilder
import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.application.invokeLater
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.panel

import com.jetbrains.rd.util.first
import com.sheldon.idea.plugin.api.front.dashboard.component.ApiTreePanel
import com.sheldon.idea.plugin.api.front.dashboard.component.ModuleSelector
import com.sheldon.idea.plugin.api.front.dashboard.utils.TreeAction
import javax.swing.JButton

class ApiDashboardToolWindow : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val dashboardPanel = ApiDashboardPanel(project)
        val contentFactory = ContentFactory.getInstance()
        val content = contentFactory.createContent(dashboardPanel.getContent(), "接口", true)
        toolWindow.contentManager.addContent(content)
        TreeAction.reloadTree(project) { treeMap, _ ->
            val gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()

            // 2. 序列化并打印
            val jsonString = gson.toJson(treeMap)
            println("jsonString: $jsonString")
            val rootNode = treeMap.first().value
            dashboardPanel.moduleSelector.updateDropdown(treeMap.keys)
            dashboardPanel.moduleSelector.isEditable = true
            dashboardPanel.treePanel.renderApiTree(rootNode)
        }
    }


    class ApiDashboardPanel(private val project: Project) {

        val treePanel = ApiTreePanel(project)

        val moduleSelector = ModuleSelector(project, treePanel)

        fun getContent() = panel {
            // === 第一行：操作栏 ===
            row {
                button("") { event ->
                    // 1. 获取按钮组件的引用
                    val btn = event.source as JButton

                    // 2. 切换为 Loading 状态 (IDEA 自带的旋转图标)
                    btn.icon = AnimatedIcon.Default()
                    btn.isEnabled = false // 建议加载时禁用按钮，防止重复点击

                    // 3. 执行你的业务逻辑
                    TreeAction.reloadTree(project, force = true) { treeMap, _ ->
                        // --- 业务逻辑 ---
                        val rootNode = treeMap.first().value
                        treePanel.renderApiTree(rootNode)
                        moduleSelector.updateDropdown(treeMap.keys)

                        // --- 4. 恢复按钮状态 ---
                        // 注意：如果 reloadTree 的回调是在后台线程执行的，
                        // 必须用 invokeLater 包裹以下代码来更新 UI。
                        // 如果你确定回调已经在 EDT (UI线程)，则不需要 invokeLater。
                        btn.icon = AllIcons.Actions.Refresh // 恢复原来的刷新图标
                        btn.isEnabled = true
                    }
                }.applyToComponent {
                    // 5. 初始化设置
                    icon = AllIcons.Actions.Refresh // 设置初始图标
                    toolTipText = "刷新所有服务"      // 只有图标时，Tooltip 很重要

                    // (可选) 如果你觉得默认的按钮方框太丑，想做成类似工具栏的透明按钮：
//                     isBorderPainted = false
                    isContentAreaFilled = false
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