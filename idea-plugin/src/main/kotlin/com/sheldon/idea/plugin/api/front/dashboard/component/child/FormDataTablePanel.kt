package com.sheldon.idea.plugin.api.front.dashboard.component.child

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.intellij.icons.AllIcons
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.AbstractTableCellEditor
import com.intellij.util.ui.JBUI
import com.sheldon.idea.plugin.api.constant.CommonConstant.DEFAULT_FORM_DATA_FIELD_CONTENT_TYPE
import com.sheldon.idea.plugin.api.front.dashboard.renderer.DeleteButtonEditor
import com.sheldon.idea.plugin.api.front.dashboard.renderer.DeleteIconRenderer
import com.sheldon.idea.plugin.api.model.FormDataField
import java.awt.*
import java.io.File
import javax.swing.*
import javax.swing.event.CellEditorListener
import javax.swing.event.ChangeEvent
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellEditor
import javax.swing.table.TableCellRenderer

// 1. 定义复合值对象
data class FormDataValue(
    var text: String = "",
    var files: ArrayList<String> = ArrayList()
) {
    override fun toString(): String {
        return text
    }
}

class FormDataTablePanel(private val project: Project) : JPanel(BorderLayout()) {

    private val COL_KEY = 0
    private val COL_VALUE = 1
    private val COL_TYPE = 2
    private val COL_CONTENT_TYPE = 3
    private val COL_DELETE = 4

    // ★ 定义高度常量
    private val ROW_HEIGHT_NORMAL = 35
    private val ROW_HEIGHT_FILE_EXPANDED = 70

    private val gson = Gson()

    private val tableModel = object : DefaultTableModel(
        arrayOf("参数名", "参数值", "参数类型", "Content-Type", ""), 0
    ) {
        override fun isCellEditable(row: Int, column: Int) = true
    }

    private val table = object : JBTable(tableModel) {
        override fun getCellRenderer(row: Int, column: Int): TableCellRenderer {
            if (column == COL_VALUE) {
                return if (isFileType(row)) {
                    FilePanelHandler(project, false)
                } else {
                    TextValueRenderer()
                }
            }
            return super.getCellRenderer(row, column)
        }

        override fun getCellEditor(row: Int, column: Int): TableCellEditor {
            if (column == COL_VALUE) {
                return if (isFileType(row)) {
                    FilePanelHandler(project, true)
                } else {
                    TextValueEditor()
                }
            }
            return super.getCellEditor(row, column)
        }

        private fun isFileType(row: Int): Boolean {
            return (model.getValueAt(row, COL_TYPE) as? String) == "File"
        }

        // ★★★ 核心修复3：调整整体高度 ★★★
        override fun getPreferredScrollableViewportSize(): Dimension {
            val size = super.getPreferredScrollableViewportSize()
            // 将默认高度限制在 150px (大约 5 行普通数据的高度)，不再是巨大的 250px
            return Dimension(size.width, 150)
        }
    }

    init {
        // 1. 类型列编辑器
        val typeCombo = JComboBox(arrayOf("Text", "File"))
        val typeEditor = DefaultCellEditor(typeCombo)
        table.columnModel.getColumn(COL_TYPE).cellEditor = typeEditor

        // ★★★ 核心修复2：类型切换时，立即重新计算高度 ★★★
        typeEditor.addCellEditorListener(object : CellEditorListener {
            override fun editingStopped(e: ChangeEvent?) {
                val row = table.editingRow
                if (row >= 0) {
                    SwingUtilities.invokeLater {
                        updateRowHeight(row) // 切换 Text/File 后，根据内容决定高度
                        tableModel.fireTableRowsUpdated(row, row)
                    }
                }
            }

            override fun editingCanceled(e: ChangeEvent?) {}
        })

        // 2. 删除列
        table.columnModel.getColumn(COL_DELETE).apply {
            maxWidth = 30
            minWidth = 30
            cellRenderer = DeleteIconRenderer()
            cellEditor = DeleteButtonEditor { if (it >= 0) tableModel.removeRow(it) }
        }

        // 3. 基础设置
        table.rowHeight = ROW_HEIGHT_NORMAL
        (table as JBTable).putClientProperty("JTable.autoStartsEdit", true)
        table.putClientProperty("terminateEditOnFocusLost", java.lang.Boolean.TRUE)

        // 4. 监听器：兜底 FormDataValue 对象类型
        tableModel.addTableModelListener { e ->
            if (e.firstRow >= 0 && e.column == COL_VALUE) {
                val row = e.firstRow
                val value = tableModel.getValueAt(row, COL_VALUE)
                if (value != null && value !is FormDataValue) {
                    val newValue = FormDataValue(text = value.toString())
                    SwingUtilities.invokeLater {
                        tableModel.setValueAt(newValue, row, COL_VALUE)
                    }
                }
            }
        }

        val decorator = ToolbarDecorator.createDecorator(table)
            .setAddAction {
                tableModel.addRow(arrayOf("", FormDataValue(), "Text", DEFAULT_FORM_DATA_FIELD_CONTENT_TYPE, ""))
            }
            .disableRemoveAction()
            .disableUpDownActions()
        add(decorator.createPanel(), BorderLayout.CENTER)
    }

    // ★★★ 核心逻辑：统一计算行高的方法 ★★★
    private fun updateRowHeight(row: Int) {
        if (row < 0 || row >= table.rowCount) return

        val type = tableModel.getValueAt(row, COL_TYPE) as? String
        val value = tableModel.getValueAt(row, COL_VALUE) as? FormDataValue

        if (type == "File") {
            // 如果是 File 且 有文件 -> 60
            if (value != null && value.files.isNotEmpty()) {
                table.setRowHeight(row, ROW_HEIGHT_FILE_EXPANDED)
            } else {
                // 如果是 File 但没文件 -> 30
                table.setRowHeight(row, ROW_HEIGHT_NORMAL)
            }
        } else {
            // 如果是 Text -> 30
            table.setRowHeight(row, ROW_HEIGHT_NORMAL)
        }
    }

    fun getData(): String {
        if (table.isEditing) table.cellEditor.stopCellEditing()

        val dataMap = LinkedHashMap<String, FormDataField>()
        for (i in 0 until tableModel.rowCount) {
            val key = tableModel.getValueAt(i, COL_KEY) as? String ?: ""
            if (key.isBlank()) continue

            val uiType = tableModel.getValueAt(i, COL_TYPE) as? String ?: "Text"
            val compositeValue = tableModel.getValueAt(i, COL_VALUE) as? FormDataValue ?: FormDataValue()
            val contentType = tableModel.getValueAt(i, COL_CONTENT_TYPE) as? String ?: ""

            val field = FormDataField()
            field.name = key
            field.contentType = contentType
            field.type = uiType.lowercase()
            field.fileList = compositeValue.files
            field.value = compositeValue.text
            dataMap[key] = field
        }
        return gson.toJson(dataMap)
    }

    fun setData(jsonString: String?) {
        while (tableModel.rowCount > 0) tableModel.removeRow(0)
        if (jsonString.isNullOrBlank()) return

        try {
            val type = object : TypeToken<Map<String, FormDataField>>() {}.type
            val dataMap: Map<String, FormDataField> = gson.fromJson(jsonString, type)

            dataMap.forEach { (key, field) ->
                val uiType = if (field.type.equals("file", true)) "File" else "Text"
                val compositeValue = FormDataValue(
                    text = field.value ?: "",
                    files = field.fileList
                )

                tableModel.addRow(arrayOf(key, compositeValue, uiType, field.contentType, ""))

                // 数据加载后，应用高度规则
                val lastRowIndex = tableModel.rowCount - 1
                updateRowHeight(lastRowIndex)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ================== Renderers & Editors ==================

    class TextValueRenderer : DefaultTableCellRenderer() {
        override fun getTableCellRendererComponent(
            table: JTable?, value: Any?, isSelected: Boolean, hasFocus: Boolean, row: Int, column: Int
        ): Component {
            val text = (value as? FormDataValue)?.text ?: ""
            return super.getTableCellRendererComponent(table, text, isSelected, hasFocus, row, column)
        }
    }

    class TextValueEditor : AbstractTableCellEditor(), TableCellEditor {
        private val textField = JTextField()
        private var originalValue: FormDataValue = FormDataValue()

        override fun getTableCellEditorComponent(
            table: JTable?, value: Any?, isSelected: Boolean, row: Int, column: Int
        ): Component {
            originalValue = (value as? FormDataValue) ?: FormDataValue()
            textField.text = originalValue.text
            return textField
        }

        override fun getCellEditorValue(): Any {
            originalValue.text = textField.text
            return originalValue
        }
    }

    inner class FilePanelHandler(
        private val project: Project,
        private val isEditor: Boolean
    ) : AbstractTableCellEditor(), TableCellRenderer {

        private var currentFormData: FormDataValue = FormDataValue()
        private var currentFiles = mutableListOf<String>()

        private val panel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            isOpaque = true
        }

        private val scrollPane = JBScrollPane(panel).apply {
            border = JBUI.Borders.empty()
            horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
            verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED
            isOpaque = true
            viewport.isOpaque = true
            // 防止 ScrollPane 撑大
            preferredSize = Dimension(100, ROW_HEIGHT_FILE_EXPANDED)
        }

        override fun getCellEditorValue(): Any {
            currentFormData.files = ArrayList(currentFiles)
            return currentFormData
        }

        override fun getTableCellRendererComponent(
            table: JTable, value: Any?, isSelected: Boolean, hasFocus: Boolean, row: Int, column: Int
        ): Component {
            val formData = (value as? FormDataValue) ?: FormDataValue()
            // 渲染时也需要根据是否有文件来调整 Panel 的视觉
            buildPanel(table, formData, isSelected, false, row)
            return if (formData.files.isEmpty()) panel else scrollPane
        }

        override fun getTableCellEditorComponent(
            table: JTable, value: Any?, isSelected: Boolean, row: Int, column: Int
        ): Component {
            currentFormData = (value as? FormDataValue) ?: FormDataValue()
            buildPanel(table, currentFormData, isSelected, true, row)
            // 编辑时，如果有文件则显示滚动条，否则只显示 Panel (因为高度为30，塞不下滚动条)
            return if (currentFormData.files.isEmpty()) panel else scrollPane
        }

        private fun buildPanel(
            table: JTable, formData: FormDataValue, isSelected: Boolean, enableActions: Boolean, row: Int
        ) {
            panel.removeAll()
            val bgColor = if (isSelected) table.selectionBackground else table.background
            panel.background = bgColor
            scrollPane.background = bgColor
            scrollPane.viewport.background = bgColor

            currentFiles = ArrayList(formData.files)

            // Add 按钮
            val btnPanel = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0))
            btnPanel.isOpaque = false
            btnPanel.border = JBUI.Borders.empty(0, 0)
            btnPanel.maximumSize = Dimension(10000, 35) // 限制高度 24
            btnPanel.preferredSize = Dimension(100, 35)

            val selectBtn = JButton("Add")
            selectBtn.icon = com.intellij.util.IconUtil.scale(AllIcons.General.Add, null, 0.7f)

            // ★ 修改点2：极度压缩 Padding (Margin)
            // JBUI.insets(top, left, bottom, right)，上下设为 0，左右设为 4
            selectBtn.margin = JBUI.insets(0, 0)

            // ★ 修改点3：(可选) 稍微调小一点字体，配合小按钮更协调
            selectBtn.font = com.intellij.util.ui.UIUtil.getLabelFont(com.intellij.util.ui.UIUtil.FontSize.SMALL)

            if (enableActions) {
                selectBtn.addActionListener {
                    val descriptor = FileChooserDescriptor(true, false, false, false, false, true)
                    FileChooser.chooseFiles(descriptor, project, null) { vFiles ->
                        val newPaths = vFiles.map { it.path }
                        currentFiles.addAll(newPaths)
                        saveDataAndRefresh(table, row) // 传入 row
                    }
                }
            }
            btnPanel.add(selectBtn)
            panel.add(btnPanel)

//            // ★ 修改点2：增加按钮和下方文件列表之间的间距 (5px)
//            if (currentFiles.isNotEmpty()) {
//                panel.add(Box.createVerticalStrut(JBUI.scale(7)))
//            }

            // 文件列表
            currentFiles.forEachIndexed { index, path ->
                // ★ 修改点3：文件与文件之间增加一点间距 (3px)，第一行不需要
                if (index > 0) {
                    panel.add(Box.createVerticalStrut(JBUI.scale(3)))
                }
                val fileRow = JPanel(BorderLayout())
                fileRow.isOpaque = false
                fileRow.border = JBUI.Borders.empty(1)
                fileRow.maximumSize = Dimension(10000, 30)
                fileRow.preferredSize = Dimension(100, 30)

                val nameLabel = JBLabel(File(path).name)
                nameLabel.isOpaque = true
                nameLabel.background = JBColor.WHITE
                nameLabel.foreground = JBColor.BLACK
                nameLabel.border = JBUI.Borders.empty(0, 5)
                nameLabel.toolTipText = path
                nameLabel.verticalAlignment = SwingConstants.CENTER

                fileRow.add(nameLabel, BorderLayout.CENTER)

                val delBtn = JButton(AllIcons.Actions.Close)
                delBtn.preferredSize = Dimension(22, 22)
                delBtn.isBorderPainted = false
                delBtn.isContentAreaFilled = false
                delBtn.isFocusable = false

                if (enableActions) {
                    delBtn.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                    delBtn.addActionListener {
                        currentFiles.removeAt(index)
                        saveDataAndRefresh(table, row) // 传入 row
                    }
                }
                fileRow.add(delBtn, BorderLayout.EAST)
                panel.add(fileRow)
            }
        }

        private fun saveDataAndRefresh(table: JTable, row: Int) {
            stopCellEditing()
            // 提交数据后，立即判断并更新行高
            SwingUtilities.invokeLater {
                // 先刷新 View
                panel.revalidate()
                panel.repaint()
                // ★ 关键：根据文件数量是否 > 0 来决定高度是 30 还是 60
                updateRowHeight(row)
            }
        }
    }
}