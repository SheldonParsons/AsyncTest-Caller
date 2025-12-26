package com.sheldon.idea.plugin.api.front.dashboard.component

import com.intellij.icons.AllIcons
import com.intellij.json.JsonLanguage
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.LanguageTextField
import com.intellij.ui.components.JBTabbedPane
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.panel
import com.sheldon.idea.plugin.api.front.dashboard.component.child.JsonEditorPanel
import com.sheldon.idea.plugin.api.model.ApiMockRequest
import java.awt.BorderLayout
import java.awt.Component
import javax.swing.JPanel
import javax.swing.JTextArea
import javax.swing.JTextField

class ApiDebugPanel(private val project: Project) : JPanel(BorderLayout()), Disposable {

    // --- UI 组件定义 ---
    private val urlField = JTextField()
    private val methodComboBox = ComboBox(arrayOf("GET", "POST", "PUT", "DELETE"))

    // 请求部分的 Tabs
    private val requestTabPane = JBTabbedPane()
    private val headerArea = JTextArea()
    private val paramsArea = JTextArea()
    private val formArea = JTextArea()
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
        requestTabPane.addTab("Form", createScrollArea(formArea))
        requestTabPane.addTab("Json", jsonArea)

        // 初始化响应 Tab
        responseTabPane.addTab("Body", respBodyArea)
        responseTabPane.addTab("Headers", createScrollArea(respHeaderArea))
    }

    // 辅助函数：给 TextArea 加上滚动条
    private fun createScrollArea(textArea: Component): javax.swing.JScrollPane {
        return javax.swing.JScrollPane(textArea).apply {
            border = javax.swing.BorderFactory.createEmptyBorder()
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

            button("刷新Mock") {
                // TODO: 从代码重新读取
            }.applyToComponent { icon = AllIcons.Actions.Refresh }

            button("保存") {
                // TODO: 保存回缓存
            }.applyToComponent { icon = AllIcons.Actions.MenuSaveall }
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
    fun setData(data: ApiMockRequest) {
        urlField.text = "${data.prefix}${data.path}"
        methodComboBox.selectedItem = data.method
        headerArea.text = data.headers
        paramsArea.text = data.query
        formArea.text = data.formData
        jsonArea.setText(data.body)

        // 如果有响应数据也可以回填
        respBodyArea.setText(data.responseBody)
        respHeaderArea.text = data.responseHeaders
    }

    /**
     * 读取当前 UI 的内容
     */
    fun getData(): ApiMockRequest {
        return ApiMockRequest(
            // TODO:url需要拆解
            path = urlField.text,
            method = methodComboBox.selectedItem as String,
            headers = headerArea.text,
            query = paramsArea.text,
            formData = formArea.text,
            body = jsonArea.getText(),
            responseBody = respBodyArea.getText(),
            responseHeaders = respHeaderArea.text
        )
    }
}