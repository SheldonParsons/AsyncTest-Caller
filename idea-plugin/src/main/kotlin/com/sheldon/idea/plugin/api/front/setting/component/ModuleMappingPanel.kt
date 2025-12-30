package com.sheldon.idea.plugin.api.front.setting.component

import com.google.gson.JsonParser
import com.intellij.icons.AllIcons
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.ItemRemovable
import com.sheldon.idea.plugin.api.constant.AsyncTestConstant
import com.sheldon.idea.plugin.api.constant.CommonConstant
import com.sheldon.idea.plugin.api.model.ModuleSetting
import com.sheldon.idea.plugin.api.model.RemoteProject
import com.sheldon.idea.plugin.api.utils.HttpExecutor
import com.sheldon.idea.plugin.api.utils.SpringConfigReader
import com.sheldon.idea.plugin.api.utils.build.BuildRootTree
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
    private val remoteVersionNames = mutableListOf<String>()
    private val versionCache = mutableMapOf<String, List<String>>()
    private val projectList = ArrayList<RemoteProject>()
    private val tableModel = ModuleMappingTableModel()
    private val table = JBTable(tableModel)
    val mainPanel: JPanel = JPanel(BorderLayout())
    var onDataChanged: (() -> Unit)? = null
    var tokenProvider: (() -> String)? = null
    var urlProvider: (() -> String)? = null

    init {
        initTableColumns()
        tableModel.addTableModelListener { e ->
            onDataChanged?.invoke()
        }
        if (allModules.isEmpty()) {
            project.context().runBackgroundReadUI(
                lockKey = CommonConstant.AST_CALLER_GLOBAL_ACTION,
                backgroundTask = {
                    val result = ArrayList<String>()
                    val modules = ModuleManager.getInstance(project).modules
                    for (module in modules) {
                        val baseDir = BuildRootTree(module.project).getBaseDir(module)
                        if (baseDir != null) {
                            result.add(module.name)
                        }
                    }
                    result
                },
                uiUpdate = { result, _ ->
                    allModules = result
                    updateColumnEditor(0, allModules)
                }
            )
        }
    }

    /**
     * 【核心方法】创建并返回在这个 Group 的完整 UI 面板
     * 包含了：标题边框、表格、加减号工具栏
     */
    fun createPanel(): JPanel {
        val decorator = ToolbarDecorator.createDecorator(table)
        decorator.setAddAction {
            val defaultModule = if (allModules.isNotEmpty()) allModules[0] else ""
            tableModel.addRow(arrayOf(defaultModule, "", ""))
        }
        decorator.setRemoveAction {
            val selectedRow = table.selectedRow
            if (selectedRow >= 0) {
                tableModel.removeRow(selectedRow)
            }
        }
        decorator.disableRemoveAction()
        val groupPanel = JPanel(BorderLayout())
        groupPanel.border = BorderFactory.createTitledBorder("代码模块与 AsyncTest 项目的对应关系")
        groupPanel.add(decorator.createPanel(), BorderLayout.CENTER)
        return groupPanel
    }

    private fun initTableColumns() {
        tableModel.addColumn("代码模块")
        tableModel.addColumn("绑定项目")
        tableModel.addColumn("绑定版本")
        tableModel.addColumn("操作")
        val moduleComboBox = ComboBox(allModules.toTypedArray())
        table.columnModel.getColumn(0).cellEditor = DefaultCellEditor(moduleComboBox)
        val preData = mutableListOf<Triple<String, RemoteProject?, String>>()
        moduleSetting.forEach { moduleSetting ->
            preData.add(Triple(moduleSetting.moduleInfo, moduleSetting.bindProject, moduleSetting.bindVersion))
        }
        setData(preData)
        setupProjectColumn()
        setupVersionColumn()
        setupDeleteColumn()
        table.rowHeight = 25
        table.selectionModel.selectionMode = ListSelectionModel.SINGLE_SELECTION
    }

    /**
     * 需求1：在点下拉之前，判断有没有数据，没有则请求
     */
    private fun setupProjectColumn() {
        val comboBox = ComboBox<RemoteProject>()
        comboBox.addPopupMenuListener(object : PopupMenuListener {
            override fun popupMenuWillBecomeVisible(e: PopupMenuEvent?) {
                if (projectList.isNotEmpty()) {
                    if (comboBox.itemCount == 0) {
                        comboBox.model = DefaultComboBoxModel(projectList.toTypedArray())
                    }
                    return
                }
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
                val row = table.editingRow
                if (row == -1) return
                val result = ArrayList<String>()
                result.add("dev")
                comboBox.model = DefaultComboBoxModel(result.toTypedArray())
                if (comboBox.isShowing) {
                    comboBox.hidePopup()
                    comboBox.showPopup()
                }
            }

            override fun popupMenuWillBecomeInvisible(e: PopupMenuEvent?) {}
            override fun popupMenuCanceled(e: PopupMenuEvent?) {}
        })
        table.columnModel.getColumn(2).cellEditor = DefaultCellEditor(comboBox)
    }

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
                            val id = pObj.get("id").asString
                            list.add(RemoteProject(id, name))
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                list
            },
            uiUpdate = { result, _ ->
                projectList.clear()
                projectList.addAll(result)
                comboBox.model = DefaultComboBoxModel(result.toTypedArray())
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
                httpExecutor.setMethod("GET")
                httpExecutor.setUrl("$url${AsyncTestConstant.PROJECT_ROUTER}/$projectId/versions")
                httpExecutor.setHeader("Authorization", token)
                val list = ArrayList<String>()
                try {
                    val res = httpExecutor.send()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                list
            },
            uiUpdate = { result, _ ->
                versionCache[projectId] = result
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
        val deleteColIndex = 3
        val column = table.columnModel.getColumn(deleteColIndex)
        column.cellRenderer = object : DefaultTableCellRenderer() {
            override fun getTableCellRendererComponent(
                table: JTable?, value: Any?, isSelected: Boolean, hasFocus: Boolean, row: Int, col: Int
            ): Component {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col)
                icon = AllIcons.Actions.Cancel
                text = ""
                horizontalAlignment = SwingConstants.CENTER
                cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                return this
            }
        }
        column.maxWidth = 50
        column.minWidth = 50
        column.preferredWidth = 50
        table.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                val row = table.rowAtPoint(e.point)
                val col = table.columnAtPoint(e.point)
                if (col == deleteColIndex && row >= 0 && row < tableModel.rowCount) {
                    if (table.isEditing) {
                        table.cellEditor.stopCellEditing()
                    }
                    tableModel.removeRow(row)
                }
            }
        })
    }

    private fun updateColumnEditor(colIndex: Int, items: List<String>) {
        val comboBox = ComboBox(items.toTypedArray())
        table.columnModel.getColumn(colIndex).cellEditor = DefaultCellEditor(comboBox)
    }

    /**
     * 更新绑定项目列表 (当网络请求回来后调用)
     */
    fun updateRemoteProjects(projects: ArrayList<RemoteProject>) {
        val comboBox = ComboBox(projects.toTypedArray())
        table.columnModel.getColumn(1).cellEditor = DefaultCellEditor(comboBox)
    }

    /**
     * 更新绑定版本列表 (当网络请求回来后调用)
     */
    fun updateRemoteVersions(versions: ArrayList<String>) {
        this.remoteVersionNames.clear()
        this.remoteVersionNames.addAll(versions)
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
            tableModel.addRow(arrayOf(item.first, item.second ?: "", item.third, ""))
        }
    }
}

/**
 * 自定义 TableModel，用于支持 removeRow 等操作
 */
class ModuleMappingTableModel : DefaultTableModel(), ItemRemovable {
    override fun isCellEditable(row: Int, column: Int): Boolean {
        return true
    }
}