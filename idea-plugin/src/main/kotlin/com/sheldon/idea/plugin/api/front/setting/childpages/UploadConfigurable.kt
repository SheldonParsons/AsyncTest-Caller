package com.sheldon.idea.plugin.api.front.setting.childpages

import com.google.gson.JsonParser
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.sheldon.idea.plugin.api.utils.ProjectCacheService

import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.*
import com.sheldon.idea.plugin.api.constant.AsyncTestConstant
import com.sheldon.idea.plugin.api.front.setting.component.ModuleMappingPanel
import com.sheldon.idea.plugin.api.model.ModuleSetting
import com.sheldon.idea.plugin.api.model.RemoteProject
import com.sheldon.idea.plugin.api.utils.HttpExecutor
import javax.swing.JLabel
import java.awt.Color

class UploadConfigurable(private val project: Project) : BoundConfigurable("上传AsyncTest") {

    private val cacheService = project.getService(ProjectCacheService::class.java)

    private val globalSettings = cacheService.getGlobalSettings()
    private val privateInfo = cacheService.getPrivateInfo()
    private val projectSettingsTable = cacheService.getModuleSetting(project.name)
    private var mappingPanelHelper: ModuleMappingPanel? = null
    private lateinit var urlInput: Cell<JBTextField>
    private lateinit var tokenInput: Cell<JBPasswordField>
    private var isModifiedFlag = false

    override fun apply() {
        super.apply()
        if (mappingPanelHelper == null) return

        val data: List<Triple<String, RemoteProject?, String>> = mappingPanelHelper!!.getData()
        cacheService.cleanModuleSetting(project.name)
        data.forEach { (first, second, third) ->
            if (
                first.isNotBlank() &&
                second != null &&
                third.isNotBlank()
            ) {
                cacheService.saveModuleSetting(
                    project.name,
                    ModuleSetting(first, second, third)
                )
            }
        }
        isModifiedFlag = false
    }

    override fun isModified(): Boolean {
        return super.isModified() || isModifiedFlag
    }

    override fun createPanel(): DialogPanel {
        mappingPanelHelper = ModuleMappingPanel(project, arrayListOf(), projectSettingsTable)

        mappingPanelHelper!!.onDataChanged = {
            isModifiedFlag = true
        }


        // 3. 【重点】调用 createPanel() 拿到那个带边框的 Group JPanel
        val groupUI = mappingPanelHelper!!.createPanel()
        val panel = panel {
            group("身份认证") {

                buttonsGroup {
                    row {
                        radioButton("公网 SaaS", value = true).actionListener { _, _ ->
                            urlInput.component.isEnabled = false
                        }
                        radioButton("指定服务器", value = false).actionListener { _, _ ->
                            urlInput.component.isEnabled = true
                        }
                    }
                }.bind(globalSettings::usingPublic)

                row("服务地址:") {
                    urlInput = textField().bindText(globalSettings::customerServerUrl) // 绑定到 customerServerUrl
                        .align(AlignX.FILL).comment("请输入私有化部署的服务器地址")
                    urlInput.component.isEnabled = !globalSettings.usingPublic
                }

                row("API Token:") {
                    tokenInput = passwordField().bindText(privateInfo::token).align(AlignX.FILL).comment("个人令牌")
                }

                row {
                    lateinit var statusLabel: Cell<JLabel>

                    button("测试令牌") {
                        val isSaaS = !urlInput.component.isEnabled

                        val targetUrl = if (isSaaS) {
                            globalSettings.publicServerUrl // 既然是 public，直接读取配置里的默认值
                        } else {
                            urlInput.component.text // 自定义模式，读取输入框当前填写的值
                        }
                        // 1. 切换到后台线程执行网络 IO
                        ApplicationManager.getApplication().executeOnPooledThread {
                            // 执行网络请求 (verifyToken 内部逻辑的一部分)
                            // 注意：verifyToken 里不要直接操作 label，改为返回 boolean
                            val success = verifyToken(targetUrl, String(tokenInput.component.password))
                            println("Success: $success")
                            // 2. 拿到结果后，切换回 UI 线程更新 Label
                            ApplicationManager.getApplication().invokeLater({
                                println("进入 invokeLater 了: $success")
                                if (success) {
                                    statusLabel.component.text = "验证成功"
                                    statusLabel.component.foreground = JBColor(Color(60, 179, 113), Color(98, 210, 145))
                                } else {
                                    statusLabel.component.text = "验证失败"
                                    statusLabel.component.foreground = JBColor.RED
                                }
                            }, ModalityState.any())
                        }
                    }

                    statusLabel = label("")
                }
            }
            row {
                // 【核心代码在这里】
                cell(groupUI).align(Align.FILL) // 让表格填充整个宽度和高度
            }.resizableRow()
        }
        mappingPanelHelper!!.tokenProvider = {
            String(tokenInput.component.password)
        }
        mappingPanelHelper!!.urlProvider = {
            urlInput.component.text
        }
        return panel
    }

    private fun verifyToken(url: String, token: String): Boolean {
        val httpExecutor = HttpExecutor()
        httpExecutor.setMethod("POST")
        httpExecutor.setUrl(url + AsyncTestConstant.PING_ROUTER)
        httpExecutor.setHeader("content-type", "application/json")
        httpExecutor.setBody(
            """
                    {
                        "token": "$token"
                    }
                """.trimIndent()
        )
        var isSuccess = true
        try {
            val element = JsonParser.parseString(httpExecutor.send())
            if (element.isJsonObject) {
                val resultElement = element.asJsonObject.get("result")

                val resultInt = resultElement?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isNumber }?.asInt

                if (resultInt == 0) {
                    isSuccess = false
                }
            }
        } catch (e: Exception) {
            println(e)
            isSuccess = false
        }
        return isSuccess
    }
}