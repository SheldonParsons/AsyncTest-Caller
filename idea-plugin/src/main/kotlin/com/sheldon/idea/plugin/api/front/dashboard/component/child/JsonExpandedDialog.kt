package com.sheldon.idea.plugin.api.front.dashboard.component.child
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.util.ui.JBDimension
import javax.swing.JComponent
class JsonExpandedDialog(
    private val project: Project,
    private val mainTitle: String,
    private val initialText: String,
    private val onSave: (String) -> Unit
) : DialogWrapper(project) {
    private lateinit var editorPanel: JsonEditorPanel
    init {
        init()
        title = mainTitle
    }
    override fun createCenterPanel(): JComponent {
        // 创建一个新的 Editor，注意这里传入 'disposable' (DialogWrapper 自带的生命周期管理)
        editorPanel = JsonEditorPanel(project, disposable, mainTitle)
        editorPanel.setText(initialText)
        // 设置一个较大的首选尺寸，例如 800x600
        editorPanel.preferredSize = JBDimension(800, 600)
        return editorPanel
    }
    // 当用户点击 OK (确定) 按钮时触发
    override fun doOKAction() {
        onSave(editorPanel.getText())
        super.doOKAction()
    }
}