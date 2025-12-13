package com.sheldon.idea.plugin.api.front.dashboard

import com.google.gson.GsonBuilder
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.panel
import com.sheldon.idea.plugin.api.model.GlobalSettings
import com.sheldon.idea.plugin.api.service.SpringWebScanner
import javax.swing.JTextArea
import com.sheldon.idea.plugin.api.utils.Notifier
import com.sheldon.idea.plugin.api.utils.ProjectCacheService
import com.sheldon.idea.plugin.api.utils.context
import com.sheldon.idea.plugin.api.utils.runBackgroundReadUI

class ApiDashboardToolWindow : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        // 1. 创建我们的 UI 面板实例
        val dashboardPanel = ApiDashboardPanel(project)

        // 2. 获取 ContentFactory (制造内容的工厂)
        val contentFactory = ContentFactory.getInstance()

        // 3. 将我们的 panel 包装成 Content 对象
        // 参数：component (UI组件), displayName (标签名), isLockable (是否可锁定)
        val content = contentFactory.createContent(dashboardPanel.getContent(), "接口", true)

        // 4. 把 Content 加到工具窗口里
        toolWindow.contentManager.addContent(content)
        println("loading....")

        project.context().runBackgroundReadUI(backgroundTask = { p ->
            val cacheService = ProjectCacheService.getInstance(p)
            val globalSettings: GlobalSettings = cacheService.getGlobalSettings()
            println("----cache----")
            println(globalSettings)
            println(globalSettings.publicServerUrl)
            val scanner = SpringWebScanner(p)
            val result = scanner.scanAndBuildTree()
            println(cacheService.getModuleRequests("spring-for-plugins-test"))
            result
        }, uiUpdate = { resultRoots, p ->
            val gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()

            // 2. 序列化并打印
            val jsonString = gson.toJson(resultRoots)

            println("====== API 树结构生成结果 ======")
            println(jsonString)
            println("===============================")
            Notifier.notifyInfo(p, "Success", "扫描完成，找到 ${resultRoots.size} 个模块！")
        })
    }


    class ApiDashboardPanel(private val project: Project) {

        // 创建一个文本区域，用来显示扫描日志
        private val logArea = JTextArea().apply {
            isEditable = false
            text = "准备就绪... 点击上方按钮开始扫描 Spring 接口"
        }

        fun getContent() = panel {
            // === 第一行：操作栏 ===
            row {
                button("扫描 Spring 接口") {
                    // 点击按钮后的回调逻辑
                    startScan()
                }
                button("清空日志") {
                    logArea.text = ""
                }
            }

            // === 第二行：分割线 ===
            group("扫描结果") {
                // === 第三行：主要内容区域 ===
                row {
                    // scrollCell 包裹住 JTextArea，让它支持滚动
                    // align(Align.FILL) 让它填满剩余空间
                    scrollCell(logArea).align(Align.Companion.FILL)
                }.resizableRow() // 这一行允许调整高度
            }
        }.apply {
            // 给整个面板加点内边距，好看点
            border = javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10)
        }

        private fun startScan() {
            logArea.append("\n正在扫描工程: ${project.name} ...\n")

            // TODO: 这里以后会放入真正的 PSI 解析逻辑
            // 模拟一下扫描过程
            logArea.append("找到 Controller: MethodTestController\n")
            logArea.append("${project.basePath}\n")
            logArea.append("  [GET] /api/v1/methods/simple\n")
            logArea.append("  [POST] /api/v1/methods/upload\n")
            logArea.append("扫描完成！\n")
        }
    }
}