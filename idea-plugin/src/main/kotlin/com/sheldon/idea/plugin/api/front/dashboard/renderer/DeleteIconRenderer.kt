package com.sheldon.idea.plugin.api.front.dashboard.renderer

import com.intellij.icons.AllIcons
import com.intellij.util.ui.AbstractTableCellEditor
import java.awt.Component
import java.awt.Cursor
import javax.swing.JButton
import javax.swing.JTable
import javax.swing.SwingConstants
import javax.swing.table.DefaultTableCellRenderer

// ==========================
// 1. 渲染器：只负责“看”
// ==========================
class DeleteIconRenderer : DefaultTableCellRenderer() {

    // ★ 在这里替换为你自己的图标，例如 IconLoader.getIcon("/icons/my_trash.svg")
    private val targetIcon = AllIcons.Actions.GC

    override fun getTableCellRendererComponent(
        table: JTable?, value: Any?, isSelected: Boolean, hasFocus: Boolean, row: Int, column: Int
    ): Component {
        // 让父类处理基础的背景色（比如选中变蓝）
        super.getTableCellRendererComponent(table, "", isSelected, hasFocus, row, column)

        this.icon = targetIcon
        this.horizontalAlignment = SwingConstants.CENTER
        this.text = "" // 不显示文字

        // ★ 关键修复：设置为透明，避免遮挡表格原本的选中高亮背景，解决“全黑”问题
        // 但如果该行被选中(isSelected)，DefaultTableCellRenderer 会自动处理背景，所以这里通常不需要强制设为透明
        // 如果你发现图标背景还是有色块，可以尝试解开下面这行的注释：
        // this.isOpaque = isSelected

        return this
    }
}

// ==========================
// 2. 编辑器：负责“点”
// ==========================
class DeleteButtonEditor(private val onDelete: (Int) -> Unit) : AbstractTableCellEditor() {

    // ★ 同样在这里替换图标
    private val targetIcon = AllIcons.Actions.GC

    // 初始化一个“幽灵按钮”
    private val button = JButton(targetIcon).apply {
        isBorderPainted = false      // 去掉边框
        isContentAreaFilled = false  // 去掉背景填充（关键！）
        isFocusPainted = false       // 去掉点击时的焦点框
        isOpaque = false             // 设置为透明
        cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) // 鼠标变成手型

        addActionListener {
            stopCellEditing() // 先停止编辑
            if (currentRow >= 0) {
                onDelete(currentRow)
            }
        }
    }

    private var currentRow: Int = -1

    override fun getCellEditorValue(): Any? = null

    override fun getTableCellEditorComponent(
        table: JTable?, value: Any?, isSelected: Boolean, row: Int, column: Int
    ): Component {
        currentRow = row
        return button
    }
}