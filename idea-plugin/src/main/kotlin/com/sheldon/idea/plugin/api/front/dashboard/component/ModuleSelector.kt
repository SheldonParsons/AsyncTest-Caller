package com.sheldon.idea.plugin.api.front.dashboard.component

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.sheldon.idea.plugin.api.constant.CommonConstant
import com.sheldon.idea.plugin.api.utils.ProjectCacheService
import com.sheldon.idea.plugin.api.utils.context
import com.sheldon.idea.plugin.api.utils.runBackgroundReadUI
import java.awt.event.ItemEvent
import java.util.Vector
import javax.swing.DefaultComboBoxModel

class ModuleSelector(val project: Project, val treePanel: ApiTreePanel) : ComboBox<String>() {

    init {
        isEditable = false
        this.addItemListener { e ->
            // 【重要】ItemListener 会触发两次：一次是取消选中旧值，一次是选中新值
            // 我们只关心 "选中新值" (SELECTED) 的时候
            if (e.stateChange == ItemEvent.SELECTED) {
                val newValue = e.item as? String ?: return@addItemListener
                project.context().runBackgroundReadUI(
                    lockKey = CommonConstant.AST_CALLER_GLOBAL_ACTION,
                    backgroundTask = { p ->
                        val cacheService = ProjectCacheService.getInstance(p)
                        cacheService.getModuleTree(newValue)
                    },
                    uiUpdate = { newTree,p ->
                        if (newTree != null) {
                            treePanel.reloadTreeData(newTree)
                        }
                    }
                )
                println(newValue)
            }
        }
    }

    val currentSelect: String?
        get() = this.item

    fun updateDropdown(items: MutableSet<String>) {

        // 【关键】Swing 更新数据的标准做法：替换 Model
        // ComboBox 需要 Vector 或者 Array
        val newModel = DefaultComboBoxModel(Vector(items))

        this.model = newModel

        // 默认选中第一个 (如果有数据的话)
        if (items.isNotEmpty()) {
            this.selectedIndex = 0
        }
    }
}