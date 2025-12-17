package com.sheldon.idea.plugin.api.front.dashboard.component

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.ui.JBPopupMenu
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.treeStructure.Tree
import com.sheldon.idea.plugin.api.front.dashboard.renderer.ApiTreeCellRenderer
import com.sheldon.idea.plugin.api.front.dashboard.utils.toTreeNode
import com.sheldon.idea.plugin.api.model.ApiNode
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JMenuItem
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel

class ApiTreePanel() : SimpleToolWindowPanel(true, true) {

    private val tree: Tree
    private val treeModel: DefaultTreeModel

    init {
        // 1. 初始化一个空的 Root 节点
        val root = DefaultMutableTreeNode("Loading...")
        treeModel = DefaultTreeModel(root)

        // 2. 创建 IDEA 风格的 Tree
        tree = Tree(treeModel).apply {
            isRootVisible = true // 通常隐藏最顶层的虚拟 Root
            cellRenderer = ApiTreeCellRenderer() // 挂载我们在第二步写的渲染器
        }

        // 3. 【核心修改】添加鼠标监听器处理右键菜单
        tree.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                handleContextMenu(e)
            }

            override fun mouseReleased(e: MouseEvent) {
                handleContextMenu(e)
            }
        })

        // 4. 设置内容
        setContent(JBScrollPane(tree))
    }

    /**
     * 处理右键菜单的核心逻辑
     */
    private fun handleContextMenu(e: MouseEvent) {
        // 1. 判断是否是触发菜单的事件 (不同操作系统右键触发时机不同，可能是 Pressed 也可能是 Released)
        if (!e.isPopupTrigger) return

        // 2. 获取鼠标点击位置下的 TreePath
        val selPath = tree.getPathForLocation(e.x, e.y) ?: return

        // 3. 【关键】Swing 右键默认不选中，需要手动强制选中该节点，否则用户会觉得点空了
        tree.selectionPath = selPath

        // 4. 获取节点数据
        val node = selPath.lastPathComponent as? DefaultMutableTreeNode ?: return
        val apiNode = node.userObject as? ApiNode ?: return

        // 5. 创建并显示菜单
        createAndShowPopupMenu(e, apiNode)
    }

    /**
     * 使用 ActionManager 创建原生风格的右键菜单
     */
    private fun createAndShowPopupMenu(e: MouseEvent, apiNode: ApiNode) {
        val actionManager = ActionManager.getInstance()
        val actionGroup = DefaultActionGroup()

        // --- 1. 添加带图标的动作 ---
        // 参数：文字，描述，图标
        actionGroup.add(object : AnAction("查看详情", "查看接口详细信息", AllIcons.Actions.Preview) {
            override fun actionPerformed(event: AnActionEvent) {
                println("查看详情: ${apiNode.name}")
            }
        })

        // --- 2. 动态逻辑 (例如：只有文件夹显示新增) ---
        if (apiNode.code_type == 0) {
            actionGroup.addSeparator() // 添加原生分割线

            actionGroup.add(object : AnAction("新增子接口", "在当前目录下创建", AllIcons.General.Add) {
                override fun actionPerformed(event: AnActionEvent) {
                    println("新增接口到: ${apiNode.name}")
                }
            })

            // 添加一个子菜单 (Submenu) 样式
            val subGroup = DefaultActionGroup("更多操作", true).apply {
                templatePresentation.icon = AllIcons.General.Settings
            }
            subGroup.add(object : AnAction("批量导入") {
                override fun actionPerformed(e: AnActionEvent) {}
            })
            actionGroup.add(subGroup)
        }

        // --- 3. 危险操作 (删除) ---
        actionGroup.addSeparator()
        actionGroup.add(object : AnAction("删除节点", "删除该接口", AllIcons.Actions.GC) {
            override fun actionPerformed(event: AnActionEvent) {
                println("删除: ${apiNode.name}")
            }
        })

        // --- 4. 【核心】创建并显示 Popup ---
        // "ApiTreePopup" 是一个 place 标识符，用于很多统计或内部逻辑，随便起个唯一的字符串即可
        val popupMenu = actionManager.createActionPopupMenu("ApiTreePopup", actionGroup)

        // 获取 component 并显示
        val component = popupMenu.component
        component.show(e.component, e.x, e.y)
    }

    fun reloadTreeData(rootNode: ApiNode) {
        renderApiTree(rootNode)
    }

    /**
     * 提供给 Controller 调用的刷新方法
     * 当你在后台解析完 ApiNode 后，调用这个方法
     */
    fun renderApiTree(rootApiNode: ApiNode) {
        // 必须在 EDT 线程执行
        val rootTreeNode = rootApiNode.toTreeNode()
        treeModel.setRoot(rootTreeNode)
        // 默认展开第一层
        // tree.expandRow(0)
    }

    // 获取当前选中的 ApiNode (用于点击事件)
    fun getSelectedNode(): ApiNode? {
        val path = tree.selectionPath ?: return null
        val node = path.lastPathComponent as? DefaultMutableTreeNode ?: return null
        return node.userObject as? ApiNode
    }
}