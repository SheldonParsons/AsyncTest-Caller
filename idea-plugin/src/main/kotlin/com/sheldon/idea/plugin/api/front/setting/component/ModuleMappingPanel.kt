package com.sheldon.idea.plugin.api.front.setting.component

import com.google.gson.JsonParser
import com.intellij.icons.AllIcons
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.ItemRemovable
import com.sheldon.idea.plugin.api.constant.AsyncTestConstant
import com.sheldon.idea.plugin.api.constant.CommonConstant
import com.sheldon.idea.plugin.api.model.ModuleSetting
import com.sheldon.idea.plugin.api.model.RemoteProject
import com.sheldon.idea.plugin.api.utils.HttpExecutor
import com.sheldon.idea.plugin.api.utils.context
import com.sheldon.idea.plugin.api.utils.runBackgroundReadUI
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Cursor
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.event.PopupMenuEvent
import javax.swing.event.PopupMenuListener
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel

/**
 * 模块映射配置面板
 * @param project 当前 IDEA Project 对象
 * @param allModules IDEA 中的所有模块名列表 (Col 1 数据源)
 */
class ModuleMappingPanel(
    private val project: Project,
    private var allModules: ArrayList<String>,
    private val moduleSetting: ArrayList<ModuleSetting>
) {

    // 对应你的 RemoteProject 数据类，用于转换
    // 注意：UI上显示的是 String (Project Name)，保存时可能需要 ID
    // 这里为了演示方便，暂存 RemoteProject 的 Name
    private val remoteVersionNames = mutableListOf<String>()
    private val versionCache = mutableMapOf<String, List<String>>()
    private val projectList = ArrayList<RemoteProject>()

    // 表格模型
    private val tableModel = ModuleMappingTableModel()
    private val table = JBTable(tableModel)

    // 主面板
    val mainPanel: JPanel = JPanel(BorderLayout())
    var onDataChanged: (() -> Unit)? = null

    var tokenProvider: (() -> String)? = null
    var urlProvider: (() -> String)? = null

    init {
        initTableColumns()
        tableModel.addTableModelListener { e ->
            // 过滤掉一些不需要的事件（可选），通常直接通知即可
            onDataChanged?.invoke()
        }

        if (allModules.isEmpty()) {
            allModules = ArrayList(ModuleManager.getInstance(project).modules.map { it.name })
            updateColumnEditor(0, allModules)
        }
    }

    /**
     * 【核心方法】创建并返回在这个 Group 的完整 UI 面板
     * 包含了：标题边框、表格、加减号工具栏
     */
    fun createPanel(): JPanel {
        // 1. 创建 ToolbarDecorator (负责渲染 + 和 - 号)
        val decorator = ToolbarDecorator.createDecorator(table)
        // 设置 "+" 号点击事件
        decorator.setAddAction {
            val defaultModule = if (allModules.isNotEmpty()) allModules[0] else ""
            // 添加新行：默认模块, 空项目, 空版本
            tableModel.addRow(arrayOf(defaultModule, "", ""))
        }

        // 设置 "-" 号点击事件
        decorator.setRemoveAction {
            val selectedRow = table.selectedRow
            if (selectedRow >= 0) {
                tableModel.removeRow(selectedRow)
            }
        }

        // 【新】禁用侧边的移除按钮，因为我们要在行内删除了
        decorator.disableRemoveAction()

        val groupPanel = JPanel(BorderLayout())
        groupPanel.border = BorderFactory.createTitledBorder("代码模块与 AsyncTest 项目的对应关系")
        groupPanel.add(decorator.createPanel(), BorderLayout.CENTER)
        return groupPanel
    }

    // --- 下面是表格配置和数据更新逻辑 ---

    private fun initTableColumns() {
        tableModel.addColumn("代码模块")
        tableModel.addColumn("绑定项目")
        tableModel.addColumn("绑定版本")
        tableModel.addColumn("操作")

        // 1. 模块列
        val moduleComboBox = ComboBox(allModules.toTypedArray())
        table.columnModel.getColumn(0).cellEditor = DefaultCellEditor(moduleComboBox)
        val preData = mutableListOf<Triple<String, RemoteProject?, String>>()
        moduleSetting.forEach { moduleSetting ->
            preData.add(Triple(moduleSetting.moduleInfo, moduleSetting.bindProject, moduleSetting.bindVersion))
        }
        setData(preData)

        // 2. 项目列 & 版本列
        setupProjectColumn()

        // 3. 版本列 (Col 2) - 实现需求 2
        setupVersionColumn()

        // 【新】设置第4列（操作列）的渲染器和点击逻辑
        setupDeleteColumn()

        table.rowHeight = 25
        table.selectionModel.selectionMode = ListSelectionModel.SINGLE_SELECTION
    }

    /**
     * 需求1：在点下拉之前，判断有没有数据，没有则请求
     */
    private fun setupProjectColumn() {
        val comboBox = ComboBox<RemoteProject>()

        // 添加下拉监听器
        comboBox.addPopupMenuListener(object : PopupMenuListener {
            override fun popupMenuWillBecomeVisible(e: PopupMenuEvent?) {
                // 如果已有数据，不做处理 (或者你可以加个标志位强制刷新)
                if (projectList.isNotEmpty()) {
                    // 确保下拉框的数据是最新的
                    if (comboBox.itemCount == 0) {
                        comboBox.model = DefaultComboBoxModel(projectList.toTypedArray())
                    }
                    return
                }

                // 没有数据，发送请求
                fetchProjects(comboBox)
            }

            override fun popupMenuWillBecomeInvisible(e: PopupMenuEvent?) {}
            override fun popupMenuCanceled(e: PopupMenuEvent?) {}
        })

        table.columnModel.getColumn(1).cellEditor = DefaultCellEditor(comboBox)
    }

    /**
     * 需求2：在展开下拉之前，拿到第二列的值，通过它再去发送请求
     */
    private fun setupVersionColumn() {
        val comboBox = ComboBox<String>()

        comboBox.addPopupMenuListener(object : PopupMenuListener {
            override fun popupMenuWillBecomeVisible(e: PopupMenuEvent?) {
                // 1. 获取当前正在编辑的行
                // 注意：当下拉框展开时，表格处于 Editing 状态，table.editingRow 是可靠的
                val row = table.editingRow
                if (row == -1) return
                val result = ArrayList<String>()
                result.add("dev")
                comboBox.model = DefaultComboBoxModel(result.toTypedArray())
                if (comboBox.isShowing) {
                    comboBox.hidePopup()
                    comboBox.showPopup()
                }
////                versionCache[projectId] = result
//
//                // 2. 获取该行对应的 "绑定项目" (第2列，索引1)
//                // getValueAt 获取的是 Model 中的值
//                val selectedProject = table.getValueAt(row, 1) as? RemoteProject
//
//                if (selectedProject == null) {
//                    // 如果没选项目，清空下拉并提示（可选）
//                    comboBox.model = DefaultComboBoxModel(arrayOf("请先选择项目"))
//                    return
//                }
//
//                // 3. 检查缓存
//                val projectId = selectedProject.id
//                val cachedVersions = versionCache[projectId]
//
//                if (!cachedVersions.isNullOrEmpty()) {
//                    // 命中缓存，直接填充
//                    comboBox.model = DefaultComboBoxModel(cachedVersions.toTypedArray())
//                } else {
//                    // 4. 未命中缓存，发起请求
//                    // 先显示一个 Loading 占位
//                    comboBox.model = DefaultComboBoxModel(arrayOf("加载中..."))
//                    val result = ArrayList<String>()
//                    result.add("dev")
//                    comboBox.model = DefaultComboBoxModel(result.toTypedArray())
//                    if (comboBox.isShowing) {
//                        comboBox.hidePopup()
//                        comboBox.showPopup()
//                    }
//                    versionCache[projectId] = result
////                    fetchVersions(projectId, comboBox)
//                }
            }

            override fun popupMenuWillBecomeInvisible(e: PopupMenuEvent?) {}
            override fun popupMenuCanceled(e: PopupMenuEvent?) {}
        })

        table.columnModel.getColumn(2).cellEditor = DefaultCellEditor(comboBox)
    }
    // --- 网络请求逻辑封装 ---

    private fun fetchProjects(comboBox: ComboBox<RemoteProject>) {
        val token = tokenProvider?.invoke()
        val url = urlProvider?.invoke()
        if (token == null || url == null) return

        project.context().runBackgroundReadUI(
            lockKey = CommonConstant.AST_CALLER_GLOBAL_ACTION,
            requiresReadAction = false,
            backgroundTask = {
                val httpExecutor = HttpExecutor()
                httpExecutor.setMethod("GET")
                httpExecutor.setUrl(url + AsyncTestConstant.PROJECT_ROUTER)
                httpExecutor.setHeader("content-type", "application/json")
                httpExecutor.setHeader("Authorization", token)

                val list = ArrayList<RemoteProject>()
                try {
                    val element = JsonParser.parseString(httpExecutor.send())
                    if (element.isJsonArray) {
                        val projectElements = element.asJsonArray
                        for (projectElement in projectElements) {
                            val pObj = projectElement.asJsonObject.get("project").asJsonObject
                            val name = pObj.get("name").asString
                            val id = pObj.get("id").asString // 确保 ID 存在
                            list.add(RemoteProject(id, name))
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                println("list:${list}")
                list
            },
            uiUpdate = { result, _ ->
                println(result)
                projectList.clear()
                projectList.addAll(result)
                // 核心：请求回来后，动态更新当前正在显示的下拉框的数据源
                comboBox.model = DefaultComboBoxModel(result.toTypedArray())
                // 骚操作：如果数据回来了，尝试重新弹出下拉框让用户看到数据(可选)
                if (comboBox.isShowing) {
                    comboBox.hidePopup()
                    comboBox.showPopup()
                }
            }
        )
    }

    private fun fetchVersions(projectId: String, comboBox: ComboBox<String>) {
        val token = tokenProvider?.invoke()
        val url = urlProvider?.invoke() ?: return
        if (token == null) return
        project.context().runBackgroundReadUI(
            lockKey = CommonConstant.AST_CALLER_GLOBAL_ACTION,
            requiresReadAction = false,
            backgroundTask = {
                val httpExecutor = HttpExecutor()
                // 假设你的获取版本接口是这样的，根据实际情况修改
                httpExecutor.setMethod("GET")
                httpExecutor.setUrl("$url${AsyncTestConstant.PROJECT_ROUTER}/$projectId/versions")
                httpExecutor.setHeader("Authorization", token)

                val list = ArrayList<String>()
                try {
                    // 模拟请求逻辑...
                    val res = httpExecutor.send()
                    // 解析 json 放入 list ...
                    // 这里我先 mock 一下，你需要替换为真实的解析逻辑
                    // list.add("V1.0")
                    // list.add("V2.0")
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                list
            },
            uiUpdate = { result, _ ->
                // 写入缓存
                versionCache[projectId] = result
                // 更新 UI
                comboBox.model = DefaultComboBoxModel(result.toTypedArray())
                if (comboBox.isShowing) {
                    comboBox.hidePopup()
                    comboBox.showPopup()
                }
            }
        )
    }

    /**
     * 【新】设置删除列的样式和行为
     */
    private fun setupDeleteColumn() {
        val deleteColIndex = 3 // 第4列的索引是3
        val column = table.columnModel.getColumn(deleteColIndex)

        // 1. 设置渲染器：显示图标
        column.cellRenderer = object : DefaultTableCellRenderer() {
            override fun getTableCellRendererComponent(
                table: JTable?, value: Any?, isSelected: Boolean, hasFocus: Boolean, row: Int, col: Int
            ): Component {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col)
                // 使用 IDEA 自带的取消/删除图标
                icon = AllIcons.Actions.Cancel
                text = "" // 不显示文字
                horizontalAlignment = SwingConstants.CENTER // 居中
                cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) // 鼠标放上去变手型
                return this
            }
        }

        // 2. 设置列宽：固定一个小宽度，不要太宽
        column.maxWidth = 50
        column.minWidth = 50
        column.preferredWidth = 50

        // 3. 监听点击事件：实现删除逻辑
        table.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                val row = table.rowAtPoint(e.point)
                val col = table.columnAtPoint(e.point)

                // 如果点击的是删除列
                if (col == deleteColIndex && row >= 0 && row < tableModel.rowCount) {
                    // 可以在这里加一个确认弹窗，也可以直接删
                    // Stop editing first to avoid errors
                    if (table.isEditing) {
                        table.cellEditor.stopCellEditing()
                    }
                    tableModel.removeRow(row)
                }
            }
        })
    }


    // 辅助方法：更新某一列的下拉框选项
    private fun updateColumnEditor(colIndex: Int, items: List<String>) {
        val comboBox = ComboBox(items.toTypedArray())
        table.columnModel.getColumn(colIndex).cellEditor = DefaultCellEditor(comboBox)
    }

    /**
     * 更新绑定项目列表 (当网络请求回来后调用)
     */
    fun updateRemoteProjects(projects: ArrayList<RemoteProject>) {
        val comboBox = ComboBox(projects.toTypedArray())

        // 关键：虽然 toString() 能解决大部分显示问题，但 IntelliJ 的 ComboBox 最好显示设置 Renderer
        // 不过对于简单需求，重写 toString() 足够让 DefaultListCellRenderer 正常工作。

        table.columnModel.getColumn(1).cellEditor = DefaultCellEditor(comboBox)
    }

    /**
     * 更新绑定版本列表 (当网络请求回来后调用)
     */
    fun updateRemoteVersions(versions: ArrayList<String>) {
        this.remoteVersionNames.clear()
        this.remoteVersionNames.addAll(versions)

        // 重新设置第3列的编辑器
        val comboBox = ComboBox(versions.toTypedArray())
        table.columnModel.getColumn(2).cellEditor = DefaultCellEditor(comboBox)
    }

    /**
     * 获取当前表格中的数据 (用于保存到 Service)
     * 这里包含了去重逻辑校验
     */
    fun getData(): List<Triple<String, RemoteProject?, String>> {
        val list = mutableListOf<Triple<String, RemoteProject?, String>>()
        for (i in 0 until tableModel.rowCount) {
            val mod = tableModel.getValueAt(i, 0) as? String ?: ""
            val proj = tableModel.getValueAt(i, 1) as? RemoteProject
            val ver = tableModel.getValueAt(i, 2) as? String ?: ""

            // 【改】不需要读取第3列（操作列）的数据

            if (mod.isNotBlank()) {
                list.add(Triple(mod, proj, ver))
            }
        }
        return list
    }

    /**
     * 将数据回填到表格 (用于初始化显示)
     */
    fun setData(data: List<Triple<String, RemoteProject?, String>>) {
        while (tableModel.rowCount > 0) tableModel.removeRow(0)
        for (item in data) {
            // 【改】添加数据时，数组要加一个空位给操作列
            tableModel.addRow(arrayOf(item.first, item.second ?: "", item.third, ""))
        }
    }
}

/**
 * 自定义 TableModel，用于支持 removeRow 等操作
 */
class ModuleMappingTableModel : DefaultTableModel(), ItemRemovable {
    override fun isCellEditable(row: Int, column: Int): Boolean {
        // 所有单元格都可编辑（通过下拉框）
        return true
    }
}