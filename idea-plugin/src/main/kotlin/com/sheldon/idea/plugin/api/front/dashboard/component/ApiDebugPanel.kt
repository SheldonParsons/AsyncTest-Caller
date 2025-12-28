package com.sheldon.idea.plugin.api.front.dashboard.component

import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBTabbedPane
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBUI
import com.sheldon.idea.plugin.api.constant.CommonConstant
import com.sheldon.idea.plugin.api.front.dashboard.component.child.FormDataTablePanel
import com.sheldon.idea.plugin.api.front.dashboard.component.child.JsonEditorPanel
import com.sheldon.idea.plugin.api.front.dashboard.component.child.KeyValueTablePanel
import com.sheldon.idea.plugin.api.model.ApiMockRequest
import com.sheldon.idea.plugin.api.utils.Notifier
import com.sheldon.idea.plugin.api.utils.ProjectCacheService
import com.sheldon.idea.plugin.api.utils.context
import com.sheldon.idea.plugin.api.utils.runBackgroundReadUI
import java.awt.BorderLayout
import java.awt.Component
import javax.swing.JPanel
import javax.swing.JTextArea
import javax.swing.JTextField

class ApiDebugPanel(private val project: Project) : JPanel(BorderLayout()), Disposable {

    var onClose: (() -> Unit)? = null

    // --- 缓存 ---
    private var prefix: String = ""
    private var moduleName: String = ""


    // --- UI 组件定义 ---
    private val urlField = JTextField()
    private val methodComboBox = ComboBox(arrayOf("GET", "POST", "PUT", "DELETE"))

    // 请求部分的 Tabs
    private val requestTabPane = JBTabbedPane()
    private val headerArea = KeyValueTablePanel()
    private val paramsArea = KeyValueTablePanel()
    private val formArea = FormDataTablePanel(project)
    private val jsonArea = JsonEditorPanel(project, this, "请求Json详情")

    // 响应部分的 Tabs
    private val responseTabPane = JBTabbedPane()
    private val respBodyArea = JsonEditorPanel(project, this, "响应详情")
    private val respHeaderArea = JTextArea().apply { isEditable = false }

    init {
        initTabs()
        val mainPanel = buildUi()
        add(mainPanel, BorderLayout.CENTER)
    }

    override fun dispose() {
        // 这里留空即可
        // 因为 JsonEditorPanel 在初始化时通过 Disposer.register(this, jsonEditor) 绑定了
        // 当这个 ApiDebugPanel 被销毁时，IDEA 的 Disposer 机制会自动处理子组件
    }

    private fun initTabs() {
        // 初始化请求 Tab
        requestTabPane.addTab("Headers", createScrollArea(headerArea))
        requestTabPane.addTab("Params", createScrollArea(paramsArea))
        requestTabPane.addTab("Form", formArea)
        requestTabPane.addTab("Json", createScrollArea(jsonArea))

        // 初始化响应 Tab
        responseTabPane.addTab("Body", createScrollArea(respBodyArea))
        responseTabPane.addTab("Headers", createScrollArea(respHeaderArea))
    }

    // 辅助函数：给 TextArea 加上滚动条
    private fun createScrollArea(textArea: Component): javax.swing.JScrollPane {
        return javax.swing.JScrollPane(textArea).apply {
            border = JBUI.Borders.empty()
        }
    }

    private fun buildUi() = panel {
        // 1. URL 行
        row("") {
            cell(urlField).align(Align.FILL)
        }

        // 2. 方法与操作按钮行
        row {
            cell(methodComboBox).gap(com.intellij.ui.dsl.builder.RightGap.SMALL)

            button("发送") {
                // TODO: 执行发送逻辑，读取界面数据
//                val currentData = getData()
//                println("Sending: $currentData")
                // 模拟响应
                respBodyArea.setText("{\"status\": \"success\"}")
            }.applyToComponent { icon = AllIcons.Actions.Execute }

            button("保存") {
                val changeMockData = getData()
                project.context().runBackgroundReadUI(
                    lockKey = CommonConstant.AST_CALLER_GLOBAL_ACTION,
                    backgroundTask = { p ->
                        val cacheService = ProjectCacheService.getInstance(p)
                        cacheService.saveOrUpdateSingleRequestMock(
                            moduleName,
                            "${changeMockData.method!!.lowercase()}:${changeMockData.path}",
                            changeMockData
                        )
                    },
                    uiUpdate = { _, p ->
                        Notifier.notifyInfo(p, content = "保存成功")
                    }
                )
            }.applyToComponent { icon = AllIcons.Actions.MenuSaveall }

            button("关闭") {
                onClose?.invoke()
            }.applyToComponent { icon = AllIcons.Actions.Cancel }
        }.layout(RowLayout.INDEPENDENT)

        // 3. 请求参数 Tabs (设置为可伸缩)
        row {
            cell(requestTabPane).align(Align.FILL)
        }.resizableRow()

        // 4. 响应内容展示 (设置为可伸缩)
        row {
            cell(responseTabPane).align(Align.FILL)
        }.resizableRow()
    }

    /**
     * 将缓存中的数据回填到 UI
     */
    fun setData(data: ApiMockRequest?, treeModuleName: String?) {
        if (data == null) return
        if (treeModuleName == null) return
        prefix = data.prefix
        moduleName = treeModuleName
        urlField.text = "${data.prefix}${data.path}"
        methodComboBox.selectedItem = data.method
        headerArea.setData(data.headers ?: "")
        paramsArea.setData(data.query ?: "")
        formArea.setData(data.formData)
        jsonArea.setText(data.body)
        respBodyArea.setText(data.responseBody)
        respHeaderArea.text = data.responseHeaders
    }

    fun splitDomainAndPath(
        url: String,
        prefix: String
    ): Pair<String, String> {

        // 1️⃣ 优先按 prefix 拆
        if (prefix.isNotEmpty() && url.startsWith(prefix)) {
            val path = url.removePrefix(prefix)
            return prefix to (if (path.isEmpty()) "/" else path)
        }

        // 2️⃣ prefix 不匹配，尝试按 domain 拆
        return try {
            val uri = java.net.URI(url)
            val domain = "${uri.scheme}://${uri.authority}"
            val path = uri.rawPath ?: ""

            if (domain.isNotEmpty()) {
                domain to (if (path.isEmpty()) "/" else path)
            } else {
                "" to url
            }
        } catch (e: Exception) {
            // 3️⃣ domain 也拆不出来，兜底
            "" to url
        }
    }

    /**
     * 读取当前 UI 的内容
     */
    fun getData(): ApiMockRequest {
        val (domain, path) = splitDomainAndPath(urlField.text, prefix)
        return ApiMockRequest(
            path = path,
            prefix = domain,
            method = methodComboBox.selectedItem as String,
            headers = headerArea.getData(),
            query = paramsArea.getData(),
            formData = formArea.getData(),
            body = jsonArea.getText(),
            responseBody = respBodyArea.getText(),
            responseHeaders = respHeaderArea.text
        )
    }
}