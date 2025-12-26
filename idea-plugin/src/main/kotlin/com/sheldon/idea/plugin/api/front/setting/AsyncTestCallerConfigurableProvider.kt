package com.sheldon.idea.plugin.api.front.setting

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurableProvider
import javax.swing.JComponent

class AsyncTestCallerConfigurableProvider : ConfigurableProvider() {
    override fun createConfigurable(): Configurable {
        return AsyncTestCallerConfigurable()
    }
}

class AsyncTestCallerConfigurable : Configurable {

    override fun getDisplayName(): String = "AsyncTest Caller"

    override fun createComponent(): JComponent? = null

    override fun isModified(): Boolean = false

    override fun apply() {}
}
