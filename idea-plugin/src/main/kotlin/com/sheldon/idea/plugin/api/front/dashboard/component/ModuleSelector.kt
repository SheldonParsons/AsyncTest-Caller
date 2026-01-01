package com.sheldon.idea.plugin.api.front.dashboard.component
import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.SimpleListCellRenderer
import com.sheldon.idea.plugin.api.constant.CommonConstant
import com.sheldon.idea.plugin.api.utils.ProjectCacheService
import com.sheldon.idea.plugin.api.utils.context
import com.sheldon.idea.plugin.api.utils.runBackgroundReadUI
import java.awt.event.ItemEvent
import java.util.Vector
import javax.swing.DefaultComboBoxModel
import javax.swing.JList
class ModuleSelector(val project: Project, val treePanel: ApiTreePanel) : ComboBox<String>() {
    init {
        isSwingPopup = false
        setRenderer(object : SimpleListCellRenderer<String>() {
            override fun customize(
                list: JList<out String?>,
                value: String?,
                index: Int,
                selected: Boolean,
                hasFocus: Boolean
            ) {
                if (value.isNullOrEmpty()) {
                    text = "无模块"
                    return
                }
                text = value
                icon = AllIcons.Nodes.Module
                iconTextGap = 6
            }
        })
        isEditable = false
        this.addItemListener { e ->
            if (e.stateChange == ItemEvent.SELECTED) {
                val newValue = e.item as? String ?: return@addItemListener
                project.context().runBackgroundReadUI(
                    lockKey = CommonConstant.AST_CALLER_GLOBAL_ACTION,
                    backgroundTask = { p ->
                        val cacheService = ProjectCacheService.getInstance(p)
                        cacheService.getModuleTree(newValue)
                    },
                    uiUpdate = { newTree, _ ->
                        if (newTree != null) {
                            treePanel.reloadTreeData(newTree)
                        }
                    }
                )
            }
        }
    }
    val currentSelect: String?
        get() = this.item
    fun updateDropdown(items: MutableSet<String>) {
        val newModel = DefaultComboBoxModel(Vector(items))
        this.model = newModel
        if (items.isNotEmpty()) {
            this.selectedIndex = 0
        }
    }
}