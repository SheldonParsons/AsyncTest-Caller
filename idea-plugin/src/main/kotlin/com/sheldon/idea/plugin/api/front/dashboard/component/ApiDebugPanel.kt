package com.sheldon.idea.plugin.api.front.dashboard.component
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
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
import com.sheldon.idea.plugin.api.front.dashboard.utils.CachedResponse
import com.sheldon.idea.plugin.api.front.dashboard.utils.MockRequestHelper
import com.sheldon.idea.plugin.api.front.dashboard.utils.handleResponse
import com.sheldon.idea.plugin.api.model.ApiMockRequest
import com.sheldon.idea.plugin.api.utils.Notifier
import com.sheldon.idea.plugin.api.utils.ProjectCacheService
import com.sheldon.idea.plugin.api.utils.context
import com.sheldon.idea.plugin.api.utils.runBackgroundReadUI
import okhttp3.Response
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
    private val respHeaderArea = JsonEditorPanel(project, this, "响应头")
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
                prepareAndSend()
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
        respHeaderArea.setText(data.responseHeaders)
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
            responseHeaders = respHeaderArea.getText()
        )
    }
    private fun processResponse(response: CachedResponse) {
        handleResponse(project, response) { headersContent, bodyContent ->
            respBodyArea.setText(bodyContent)
            respHeaderArea.setText(headersContent)
        }
    }
    private fun prepareAndSend() {
        val requestData = getData() // 获取当前面板的所有数据
        println("requestData:${requestData}")
        println(isValidFormData(requestData.formData))
        val method = requestData.method?.uppercase() ?: "GET"
        // 最终决定使用的 Content-Type (null 表示不需要 body，或者默认)
        // 最终决定使用的 Body 内容 (Json 字符串 或 FormData 对象)
        var finalBodyType: String? = null
        var finalBodyContent: Any? = null
        // 2. 如果请求类型不是 POST (如 GET)，直接发送 (逻辑上通常不需要 Body)
        if (method != "POST") {
            println("Sending $method request without body check...")
            MockRequestHelper.send(project, requestData, urlField.text, "") { processResponse(it) }
            return
        }
        // --- POST 请求的处理逻辑 ---
        // 1. 获取上下文信息
        val headersStr = requestData.headers ?: "{}"
        val activeTabTitle = requestTabPane.getTitleAt(requestTabPane.selectedIndex)
        println("activeTabTitle:${activeTabTitle}")
        // 解析 Header 中的 Content-Type
        val contentTypeHeader = extractContentType(headersStr)
        println("contentTypeHeader:${contentTypeHeader}")
        // 标记 Header 中是否明确指定了类型
        val isJsonHeader = contentTypeHeader.contains("application/json", ignoreCase = true)
        val isFormHeader = contentTypeHeader.contains("multipart/form-data", ignoreCase = true)
        // 标记当前 Tab
        val isJsonTab = activeTabTitle == "Json"
        val isFormTab = activeTabTitle == "Form"
        // --- 核心判断流程 (基于你的5点需求) ---
        // 规则整合：
        // 1. Header 优先级最高 (需求 4 后半段, 需求 5 后半段)
        // 2. Tab 优先级次之 (需求 4 前半段)
        // 3. 内容嗅探优先级最低 (需求 5 前半段)
        if (isJsonHeader) {
            // Header 指定了 JSON -> 强制用 JSON
            finalBodyType = "application/json"
            finalBodyContent = requestData.body
        } else if (isFormHeader) {
            // Header 指定了 Form -> 强制用 Form
            finalBodyType = "multipart/form-data"
            finalBodyContent = requestData.formData
        } else {
            // Header 没有指定 (或指定了其他非 Body 相关类型)
            if (isJsonTab) {
                // 当前在 Json Tab -> 用 Json
                finalBodyType = "application/json"
                finalBodyContent = requestData.body
            } else if (isFormTab) {
                // 当前在 Form Tab -> 用 Form
                finalBodyType = "multipart/form-data"
                finalBodyContent = requestData.formData
            } else {
                // 当前既不在 Json 也不在 Form (例如在 Headers Tab) -> 检查内容 (Json 优先)
                val hasJsonContent = requestData.body.isNotBlank() && requestData.body != "{}"
                val hasFormContent = isValidFormData(requestData.formData)
                if (hasJsonContent) {
                    finalBodyType = "application/json"
                    finalBodyContent = requestData.body
                } else if (hasFormContent) {
                    finalBodyType = "multipart/form-data"
                    finalBodyContent = requestData.formData
                } else {
                    // 实在没东西，就不传 Body
                    finalBodyType = null
                }
            }
        }
        println("=== Send Request Preparation ===")
        println("Method: $method")
        println("Resolved Body Type: $finalBodyType")
        println("Body Content: $finalBodyContent")
        MockRequestHelper.send(project, requestData, urlField.text, finalBodyType) { processResponse(it) }
        // TODO: 在这里编写实际的发送请求代码 (OkHttp / HttpClient)
        // sendRequest(requestData, finalBodyType, finalBodyContent)
    }
    /**
     * 从 JSON 格式的 Header 字符串中提取 Content-Type 的值
     */
    private fun extractContentType(headersJson: String): String {
        try {
            if (headersJson.isBlank()) return ""
            val type = object : TypeToken<Map<String, String>>() {}.type
            val headersMap: Map<String, String> = Gson().fromJson(headersJson, type)
            // 忽略大小写查找 key
            return headersMap.entries.firstOrNull { it.key.equals("content-type", ignoreCase = true) }?.value ?: ""
        } catch (e: Exception) {
            return ""
        }
    }
    /**
     * 简单判断 FormData 是否有有效值
     * 根据你的 FormDataTablePanel 实现，formData 可能是 FormDataValue 对象或者 Map
     */
    private fun isValidFormData(formData: Any?): Boolean {
        if (formData == null) return false
        if (formData == "{}") return false
        return true
    }
}