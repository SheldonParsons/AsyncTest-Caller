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
        // 1. 启用 IDEA 原生的渲染风格
        // 这会让下拉框不再是操作系统的原生样式，而是 IDEA 的扁平化列表，且支持键盘搜索
        isSwingPopup = false

        // 2. 设置渲染器 (加图标、调样式)
        setRenderer(object : SimpleListCellRenderer<String>() {
            override fun customize(
                list: JList<out String?>,
                value: String?,
                index: Int,
                selected: Boolean,
                hasFocus: Boolean
            ) {
                // 处理空值情况
                if (value.isNullOrEmpty()) {
                    text = "无模块"
                    return
                }

                // 设置文字
                text = value

                // 设置图标 (这里用 IDEA 通用的 Module 图标，你也可以根据 value 动态判断)
                icon = AllIcons.Nodes.Module

                // (可选) 如果你想更高级，可以加一些间隔
                iconTextGap = 6
            }

        })

        isEditable = false

        // 3. 事件监听 (保持你原有的逻辑不变)
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