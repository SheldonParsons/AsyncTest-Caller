package com.sheldon.idea.plugin.api.front.dashboard.component.child

import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.intellij.icons.AllIcons
import com.intellij.json.JsonFileType
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.components.JBPanel
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.datatransfer.StringSelection
import javax.swing.JPanel

/**
 * 一个带有工具栏的 JSON 编辑器组件
 */
class JsonEditorPanel(
    private val project: Project,
    parentDisposable: Disposable,
    private var mainTitle: String
) : JBPanel<JsonEditorPanel>(BorderLayout()), Disposable {

    private var editor: Editor? = null

    init {
        // 1. 创建 Document (文档内容)
        val editorFactory = EditorFactory.getInstance()
        val document = editorFactory.createDocument("")

        // 2. 创建 Editor (编辑器实例)
        // JsonFileType.INSTANCE 需要插件依赖 com.intellij.modules.json 或者 com.intellij.java
        editor = editorFactory.createEditor(document, project, JsonFileType.INSTANCE, false)

        // 3. 配置 Editor 设置
        editor?.settings?.apply {
            isLineNumbersShown = true       // 显示行号
            isFoldingOutlineShown = true    // 显示代码折叠
            isWhitespacesShown = false      // 不显示空格点
            isUseSoftWraps = true           // 自动换行
            isAdditionalPageAtBottom = false // 底部不留大片空白
        }

        // 4. 创建左侧/顶部的工具栏
        val actionGroup = DefaultActionGroup().apply {
            add(createFormatAction())
            add(createClearAction())
            add(Separator())
            add(createCopyAction())
            add(createExpandAction(mainTitle))
        }
        val actionToolbar = ActionManager.getInstance()
            .createActionToolbar("JsonEditorToolbar", actionGroup, true) // false表示垂直工具栏，true为水平
        actionToolbar.targetComponent = this

        // 5. 组装 UI
        val toolbarComponent = actionToolbar.component
        toolbarComponent.border =
            javax.swing.BorderFactory.createMatteBorder(0, 0, 1, 0, com.intellij.ui.JBColor.border())

        add(toolbarComponent, BorderLayout.NORTH)
        add(editor!!.component, BorderLayout.CENTER)

        val lineHeight = editor?.lineHeight ?: 20

        val toolbarHeight = toolbarComponent.preferredSize.height

        val targetHeight = toolbarHeight + (lineHeight * 2) + 4

        this.minimumSize = Dimension(0, 0)

        this.preferredSize = Dimension(100, targetHeight)
        Disposer.register(parentDisposable, this)
    }

    /**
     * 设置文本内容
     */
    fun setText(text: String?) {
        val safeText = text ?: ""
        // 修改 Document 必须在 WriteAction 中进行
        WriteCommandAction.runWriteCommandAction(project) {
            editor?.document?.setText(safeText)
        }
    }

    /**
     * 获取文本内容
     */
    fun getText(): String {
        return editor?.document?.text ?: ""
    }

    /**
     * 动作：格式化 JSON
     */
    private fun createFormatAction(): AnAction {
        return object : AnAction("格式化", "格式化 JSON 内容", AllIcons.Actions.PrettyPrint) {
            override fun actionPerformed(e: AnActionEvent) {
                val currentText = getText()
                if (currentText.isBlank()) return

                try {
                    // 使用 Gson 进行简单的格式化
                    val jsonElement = JsonParser.parseString(currentText)
                    val gson = GsonBuilder().setPrettyPrinting().create()
                    val prettyJson = gson.toJson(jsonElement)
                    setText(prettyJson)
                } catch (ex: Exception) {
                    // 解析失败不做处理，或者可以弹个通知
                }
            }
        }
    }

    /**
     * 动作：清空内容
     */
    private fun createClearAction(): AnAction {
        return object : AnAction("清空", "清空内容", AllIcons.Actions.GC) {
            override fun actionPerformed(e: AnActionEvent) {
                setText("")
            }
        }
    }

    /**
     * 动作：复制内容 (虽然 Editor 自带 Ctrl+C，但有个按钮更直观)
     */
    private fun createCopyAction(): AnAction {
        return object : AnAction("复制", "复制内容到剪贴板", AllIcons.Actions.Copy) {
            override fun actionPerformed(e: AnActionEvent) {
                val text = getText()
                if (text.isNotEmpty()) {
                    // 1. 创建字符串选区
                    val selection = StringSelection(text)
                    // 2. 使用 IDEA 的剪贴板管理器设置内容
                    CopyPasteManager.getInstance().setContents(selection)
                }
            }
        }
    }

    /**
     * 销毁资源，防止内存泄漏 (非常重要！)
     */
    override fun dispose() {
        editor?.let {
            if (!it.isDisposed) {
                EditorFactory.getInstance().releaseEditor(it)
            }
        }
        editor = null
    }

    private fun createExpandAction(mainTitle: String): AnAction {
        return object : AnAction("放大查看", "在弹窗中全屏编辑", AllIcons.General.ExpandComponent) {
            override fun actionPerformed(e: AnActionEvent) {
                // 1. 获取当前内容
                val currentText = getText()

                // 2. 创建弹窗
                val dialog = JsonExpandedDialog(project, mainTitle, currentText) { newText ->
                    // 3. 回调处理：当弹窗点击 OK 后，更新当前面板的内容
                    setText(newText)
                }

                // 4. 显示弹窗
                dialog.show()
            }
        }
    }
}