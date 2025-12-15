package com.sheldon.idea.plugin.api.front.dashboard.panel

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.treeStructure.Tree
import com.sheldon.idea.plugin.api.front.dashboard.component.ApiTreeCellRenderer
import com.sheldon.idea.plugin.api.front.dashboard.utils.toTreeNode
import com.sheldon.idea.plugin.api.model.ApiNode
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

//        // 2. 创建工具栏 (Toolbar)
//        val toolbar = createToolbar(data)
//        setToolbar(toolbar.component) // SimpleToolWindowPanel 自带的方法

        // 4. 设置内容
        setContent(JBScrollPane(tree))
    }

    /**
     * 创建顶部工具栏
     */
    private fun createToolbar(data: ApiNode): ActionToolbar {
        val actionGroup = DefaultActionGroup()

        // 添加一个刷新动作
        actionGroup.add(object : AnAction("刷新 API", "重新扫描项目接口", AllIcons.Actions.Refresh) {
            override fun actionPerformed(e: AnActionEvent) {
                reloadTreeData(data) // <--- 点击触发数据加载
            }
        })

        val toolbar = ActionManager.getInstance().createActionToolbar("ApiTreeToolbar", actionGroup, true)
        toolbar.targetComponent = this
        return toolbar
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
        com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
            val rootTreeNode = rootApiNode.toTreeNode()
            treeModel.setRoot(rootTreeNode)

            // 默认展开第一层
            // tree.expandRow(0)
        }
    }

    // 获取当前选中的 ApiNode (用于点击事件)
    fun getSelectedNode(): ApiNode? {
        val path = tree.selectionPath ?: return null
        val node = path.lastPathComponent as? DefaultMutableTreeNode ?: return null
        return node.userObject as? ApiNode
    }
}