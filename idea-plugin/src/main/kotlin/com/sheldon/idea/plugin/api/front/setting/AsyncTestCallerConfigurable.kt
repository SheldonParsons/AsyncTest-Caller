package com.sheldon.idea.plugin.api.front.setting

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.SearchableConfigurable
import javax.swing.JComponent

// 建议实现 SearchableConfigurable 以便能准确匹配 ID
class AsyncTestCallerConfigurable : SearchableConfigurable {

    // 这里的 ID 必须和 plugin.xml 中的 id 保持一致
    override fun getId(): String = "AsyncTest Caller Setting"

    override fun getDisplayName(): String = "AsyncTest Caller"

    // 作为父级目录，不需要 UI，返回 null 即可
    override fun createComponent(): JComponent? = null

    override fun isModified(): Boolean = false

    override fun apply() {}
}