package com.sheldon.idea.plugin.api.front.dashboard.component.child

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.table.JBTable
import java.awt.BorderLayout
import javax.swing.JPanel
import javax.swing.table.DefaultTableModel

import com.sheldon.idea.plugin.api.front.dashboard.renderer.*

class KeyValueTablePanel : JPanel(BorderLayout()) {

    // 数据模型
    private val tableModel = object : DefaultTableModel(arrayOf("参数名", "参数值", ""), 0) {
        override fun isCellEditable(row: Int, column: Int): Boolean {
            return true // 所有单元格可编辑（删除按钮也是通过编辑器实现的）
        }
    }

    private val table = JBTable(tableModel)

    init {
        // 1. 配置表格
        table.apply {
            // 设置最后一列（删除列）的宽度固定
            columnModel.getColumn(2).apply {
                maxWidth = 30
                minWidth = 30
                headerValue = "" // 表头不显示文字
                cellRenderer = DeleteIconRenderer()
                cellEditor = DeleteButtonEditor { row ->
                    if (row >= 0 && row < tableModel.rowCount) {
                        tableModel.removeRow(row)
                    }
                }
            }
            // 设置行高
            rowHeight = 25
        }

        // 2. 使用 ToolbarDecorator 添加上方的 "+" 按钮
        val decorator = ToolbarDecorator.createDecorator(table)
            .setAddAction {
                tableModel.addRow(arrayOf("", "", ""))
            }
            .disableRemoveAction() // 我们用行内删除，禁用自带的 "-"
            .disableUpDownActions()

        add(decorator.createPanel(), BorderLayout.CENTER)
    }

    /**
     * 获取数据并转换为 JSON 字符串
     * 格式：{"key1": "value1", "key2": "value2"}
     */
    fun getData(): String {
        // 1. 停止当前的编辑状态
        // 如果用户正在输入最后一行还没按回车就点了保存，这行代码能确保正在输入的内容被保存
        if (table.isEditing) {
            table.cellEditor.stopCellEditing()
        }

        val map = mutableMapOf<String, String>()

        // 2. 遍历 Table Model
        for (i in 0 until tableModel.rowCount) {
            // 获取 Key 和 Value (注意处理 null)
            val key = tableModel.getValueAt(i, 0) as? String
            val value = tableModel.getValueAt(i, 1) as? String

            // 3. 过滤无效数据：Key 不能为空
            if (!key.isNullOrBlank()) {
                map[key] = value ?: ""
            }
        }

        // 4. 使用 Gson 转换为 JSON 字符串
        // 如果 Map 为空，返回 "{}"
        if (map.isEmpty()) {
            return "{}"
        }

        return try {
            GsonBuilder().setPrettyPrinting().create().toJson(map)
        } catch (e: Exception) {
            "{}"
        }
    }

    // 设置数据 (接收 JSON 字符串)
    fun setData(jsonString: String?) {
        // 1. 清空现有数据
        while (tableModel.rowCount > 0) tableModel.removeRow(0)

        if (jsonString.isNullOrBlank()) return

        try {
            // 2. 解析 JSON 为 Map<String, String>
            // 使用 TypeToken 处理泛型
            val type = object : TypeToken<Map<String, Any>>() {}.type
            val map: Map<String, Any> = Gson().fromJson(jsonString, type)

            // 3. 填充表格
            map.forEach { (key, value) ->
                // value 转为 String，防止部分非 String 值导致类型错误
                tableModel.addRow(arrayOf(key, value.toString(), ""))
            }
        } catch (e: Exception) {
            // 解析失败可以打印日志，或者仅仅是保持表格为空
            e.printStackTrace()
            // 如果你希望解析失败时显示一行错误提示，可以在这里处理
            // tableModel.addRow(arrayOf("Parse Error", e.message, ""))
        }
    }
}