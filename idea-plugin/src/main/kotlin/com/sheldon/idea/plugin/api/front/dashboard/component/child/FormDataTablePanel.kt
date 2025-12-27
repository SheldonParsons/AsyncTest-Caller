package com.sheldon.idea.plugin.api.front.dashboard.component.child

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
import com.sheldon.idea.plugin.api.front.dashboard.renderer.DeleteButtonEditor
import com.sheldon.idea.plugin.api.front.dashboard.renderer.DeleteIconRenderer
import java.awt.*
import java.io.File
import javax.swing.*
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellEditor
import javax.swing.table.TableCellRenderer

class FormDataTablePanel(private val project: Project) : JPanel(BorderLayout()) {

    private val COL_VALUE = 1
    private val COL_TYPE = 2
    private val COL_DELETE = 4

    // ★ 设定 File 类型的固定高度 (例如 120px，约等于显示按钮+3行文件)
    private val FILE_ROW_HEIGHT = 120

    private val tableModel = object : DefaultTableModel(
        arrayOf("参数名", "参数值", "参数类型", "Content-Type", ""), 0
    ) {
        override fun isCellEditable(row: Int, column: Int) = true
    }

    private val table = object : JBTable(tableModel) {
        // Renderer: 负责展示，逻辑与Editor完全一致
        override fun getCellRenderer(row: Int, column: Int): TableCellRenderer {
            if (column == COL_VALUE && isFileType(row)) {
                return FilePanelHandler(project, false)
            }
            return super.getCellRenderer(row, column)
        }

        // Editor: 负责交互
        override fun getCellEditor(row: Int, column: Int): TableCellEditor {
            if (column == COL_VALUE && isFileType(row)) {
                return FilePanelHandler(project, true)
            }
            return super.getCellEditor(row, column)
        }

        private fun isFileType(row: Int): Boolean {
            return (model.getValueAt(row, COL_TYPE) as? String) == "File"
        }
    }

    init {
        // 1. 类型列
        table.columnModel.getColumn(COL_TYPE).cellEditor = DefaultCellEditor(JComboBox(arrayOf("Text", "File")))

        // 2. 外部的大删除按钮（删除整个参数行）
        table.columnModel.getColumn(COL_DELETE).apply {
            maxWidth = 30
            minWidth = 30
            cellRenderer = DeleteIconRenderer()
            cellEditor = DeleteButtonEditor { if (it >= 0) tableModel.removeRow(it) }
        }

        // 3. 基础设置
        table.rowHeight = 30
        (table as JBTable).putClientProperty("JTable.autoStartsEdit", true) // 单击即进入编辑状态

        // ★★★ 核心修复1：点击其他行时，强制保存数据，而不是丢弃数据！ ★★★
        table.putClientProperty("terminateEditOnFocusLost", java.lang.Boolean.TRUE)

        // 4. 监听器：处理类型切换时的初始高度
        tableModel.addTableModelListener { e ->
            if (e.firstRow >= 0 && e.column == COL_TYPE) {
                val row = e.firstRow
                val type = tableModel.getValueAt(row, COL_TYPE)
                val currentValue = tableModel.getValueAt(row, COL_VALUE)

                if (type == "File") {
                    // ★ FIX: 只有当当前值不是 List 的时候才重置！
                    // 如果已经是 List 了，说明用户之前选过文件，千万别动它！
                    if (currentValue !is List<*>) {
                        tableModel.setValueAt(ArrayList<String>(), row, COL_VALUE)
                        updateRowHeightSafely(row, 0)
                    } else {
                        // 如果有数据，记得把高度恢复回来
                        updateRowHeightSafely(row, currentValue.size)
                    }
                } else {
                    // 切回 Text，清空数据
                    tableModel.setValueAt("", row, COL_VALUE)
                    table.setRowHeight(row, 30)
                }
            }
        }

        val decorator = ToolbarDecorator.createDecorator(table)
            .setAddAction { tableModel.addRow(arrayOf("", "", "Text", "", "")) }
            .disableRemoveAction()
            .disableUpDownActions()
        add(decorator.createPanel(), BorderLayout.CENTER)
    }

    // 安全更新行高：避免在渲染循环中无限调用
    private fun updateRowHeightSafely(row: Int, fileCount: Int) {
        // 核心公式：按钮高度(35) + 文件数 * 行高(30) + 底部缓冲(2)
        val targetHeight = 35 + (fileCount * 30) + 2

        // 只有当前高度不正确时，才去设置，防止死循环
        if (row >= 0 && row < table.rowCount && table.getRowHeight(row) != targetHeight) {
            SwingUtilities.invokeLater {
                if (row < table.rowCount) { // 二次检查防止越界
                    table.setRowHeight(row, targetHeight)
                }
            }
        }
    }

    /**
     * 核心处理器：既是 Renderer 又是 Editor
     * 严格遵守：第一行按钮，后面每行一个文件
     */
    inner class FilePanelHandler(
        private val project: Project,
        private val isEditor: Boolean
    ) : AbstractTableCellEditor(), TableCellRenderer {

        private var currentFiles = mutableListOf<String>()

        // 主面板：垂直布局 (Y_AXIS)
        private val panel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            isOpaque = true
        }

        private val scrollPane = JBScrollPane(panel).apply {
            border = JBUI.Borders.empty() // 去掉滚动条边框，融合进表格
            horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
            verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED
            isOpaque = true
            viewport.isOpaque = true
        }

        override fun getCellEditorValue(): Any = ArrayList(currentFiles)

        // ================= 渲染入口 =================
        override fun getTableCellRendererComponent(
            table: JTable, value: Any?, isSelected: Boolean, hasFocus: Boolean, row: Int, column: Int
        ): Component {
            // Renderer 只是展示，不开交互 (enableActions=false) 但按钮样子要有
            // 注意：虽然 Renderer 不开交互，但为了视觉一致，按钮要画出来
            buildPanel(table, value, isSelected, false, row)
            return scrollPane
        }

        // ================= 编辑入口 =================
        override fun getTableCellEditorComponent(
            table: JTable, value: Any?, isSelected: Boolean, row: Int, column: Int
        ): Component {
            buildPanel(table, value, isSelected, true, row)
            return scrollPane
        }

        /**
         * 构建 UI 的核心方法
         */
        private fun buildPanel(
            table: JTable, value: Any?, isSelected: Boolean, enableActions: Boolean, row: Int
        ) {
            panel.removeAll()
            // 背景色处理：滚动区域内部背景色
            val bgColor = if (isSelected) table.selectionBackground else table.background
            panel.background = bgColor
            scrollPane.background = bgColor
            scrollPane.viewport.background = bgColor

            @Suppress("UNCHECKED_CAST")
            val list = (value as? List<String>) ?: emptyList()
            currentFiles = ArrayList(list)

            // --- 第一行：固定“选择文件”按钮 ---
            val btnPanel = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0))
            btnPanel.isOpaque = false
            btnPanel.border = JBUI.Borders.empty(2, 0)
            // 强制限制按钮行高度，防止被挤压或过度拉伸
            btnPanel.maximumSize = Dimension(10000, 35)
            btnPanel.preferredSize = Dimension(100, 35)

            val selectBtn = JButton("Add")
            selectBtn.icon = AllIcons.General.Add
            selectBtn.margin = JBUI.insets(2, 5)

            // 只有编辑器模式下，按钮才真的能点
            if (enableActions) {
                selectBtn.addActionListener {
                    val descriptor = FileChooserDescriptor(true, false, false, false, false, true)
                    FileChooser.chooseFiles(descriptor, project, null) { vFiles ->
                        // 累加文件
                        val newPaths = vFiles.map { it.path }
                        currentFiles.addAll(newPaths)
                        commitDataAndResize(table, row) // 提交更新
                    }
                }
            }
            btnPanel.add(selectBtn)
            panel.add(btnPanel)

            // --- 第二行开始：循环显示文件列表 ---
            currentFiles.forEachIndexed { index, path ->
                // 每个文件占一行
                val fileRow = JPanel(BorderLayout())
                fileRow.isOpaque = false
                fileRow.border = JBUI.Borders.empty(1) // 微小间距
                fileRow.maximumSize = Dimension(10000, 30) // 固定高度 30px
                fileRow.preferredSize = Dimension(100, 30)

                // 1. 文件名 (Center) -> 自动截断
                val nameLabel = JBLabel(File(path).name)
                nameLabel.isOpaque = true
                nameLabel.background = JBColor.WHITE // ★ 你的要求：白色背景
                nameLabel.foreground = JBColor.BLACK
                nameLabel.border = JBUI.Borders.empty(0, 5)
                nameLabel.toolTipText = path // 鼠标悬停看全路径
                // 垂直居中
                nameLabel.verticalAlignment = SwingConstants.CENTER

                fileRow.add(nameLabel, BorderLayout.CENTER)

                // 2. 删除按钮 (East)
                val delBtn = JButton(AllIcons.Actions.Close)
                delBtn.preferredSize = Dimension(24, 24)
                delBtn.isBorderPainted = false
                delBtn.isContentAreaFilled = false
                delBtn.isFocusable = false

                if (enableActions) {
                    delBtn.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                    delBtn.addActionListener {
                        currentFiles.removeAt(index)
                        commitDataAndResize(table, row) // 提交更新
                    }
                }
                fileRow.add(delBtn, BorderLayout.EAST)

                panel.add(fileRow)
            }

            // 检查并修正行高 (如果是渲染阶段，依靠 safe check 防止死循环)
            updateRowHeightSafely(row, currentFiles.size)
        }

        /**
         * 提交数据变化并刷新高度
         */
        private fun commitDataAndResize(table: JTable, row: Int) {
            // 1. 停止编辑，保存数据
            stopCellEditing()
            // 2. 显式更新 Model (有时候 stopCellEditing 不会立即回写)
            table.model.setValueAt(ArrayList(currentFiles), row, COL_VALUE)
            // 3. 计算新高度
            updateRowHeightSafely(row, currentFiles.size)
        }
    }
}