package com.sheldon.idea.plugin.api.front.dashboard.component

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.project.Project
import com.intellij.pom.Navigatable
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.treeStructure.Tree
import com.sheldon.idea.plugin.api.constant.AsyncTestConstant
import com.sheldon.idea.plugin.api.constant.CommonConstant
import com.sheldon.idea.plugin.api.front.dashboard.renderer.ApiTreeCellRenderer
import com.sheldon.idea.plugin.api.front.dashboard.utils.DependencyCollector
import com.sheldon.idea.plugin.api.front.dashboard.utils.toTreeNode
import com.sheldon.idea.plugin.api.method.ValidType
import com.sheldon.idea.plugin.api.model.ApiNode
import com.sheldon.idea.plugin.api.model.AstResponse
import com.sheldon.idea.plugin.api.model.AsyncTestDataResponse
import com.sheldon.idea.plugin.api.model.AsyncTestSyncTree
import com.sheldon.idea.plugin.api.model.AsyncTestUpdateDataRequest
import com.sheldon.idea.plugin.api.model.CollectNodeData
import com.sheldon.idea.plugin.api.model.ModuleSetting
import com.sheldon.idea.plugin.api.utils.HttpExecutor
import com.sheldon.idea.plugin.api.utils.Notifier
import com.sheldon.idea.plugin.api.utils.ProjectCacheService
import com.sheldon.idea.plugin.api.utils.ScanSession
import com.sheldon.idea.plugin.api.utils.build.BuildControllerNode
import com.sheldon.idea.plugin.api.utils.build.BuildDirectoryTree
import com.sheldon.idea.plugin.api.utils.build.BuildRootTree
import com.sheldon.idea.plugin.api.utils.build.MethodNodeBuilder
import com.sheldon.idea.plugin.api.utils.build.PsiPathResolver
import com.sheldon.idea.plugin.api.utils.context
import com.sheldon.idea.plugin.api.utils.runBackgroundReadUI
import com.sheldon.idea.plugin.api.utils.scanContext
import com.sheldon.idea.plugin.icons.AstCallerIcons
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath

class ApiTreePanel(private val project: Project) : SimpleToolWindowPanel(true, true) {

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

            override fun mouseClicked(e: MouseEvent) {
                // 左键 + 双击
                if (e.clickCount == 2 && e.button == MouseEvent.BUTTON1) {
                    handleDoubleClick(e)
                }
            }
        })

        // 4. 设置内容
        setContent(JBScrollPane(tree))
    }

    private fun handleDoubleClick(e: MouseEvent) {
        // 1. 获取点击的节点
        val path = tree.getPathForLocation(e.x, e.y) ?: return
        val node = path.lastPathComponent as? DefaultMutableTreeNode ?: return
        val apiNode = node.userObject as? ApiNode ?: return

        project.context()
            .runBackgroundReadUI(
                lockKey = CommonConstant.AST_CALLER_GLOBAL_ACTION, backgroundTask = { p ->
                    return@runBackgroundReadUI PsiPathResolver.resolve(p, apiNode.tree_path)
                },
                uiUpdate = { psiElement, p ->
                    if (psiElement is Navigatable && psiElement.canNavigate()) {
                        psiElement.navigate(true)
                    }
                }
            )
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
        val targetNode = selPath.lastPathComponent as? DefaultMutableTreeNode ?: return

        // 5. 创建并显示菜单
        createAndShowPopupMenu(e, targetNode)
    }

    /**
     * 使用 ActionManager 创建原生风格的右键菜单
     */
    private fun createAndShowPopupMenu(e: MouseEvent, targetNode: DefaultMutableTreeNode) {
        val actionManager = ActionManager.getInstance()
        val actionGroup = DefaultActionGroup()

        val apiNode = targetNode.userObject as ApiNode

        // --- 1. 添加带图标的动作 ---
        // 参数：文字，描述，图标
        actionGroup.add(object : AnAction("刷新节点", "Refresh current node", AllIcons.Actions.Refresh) {
            override fun actionPerformed(event: AnActionEvent) {
                tree.isEnabled = false
                project.context()
                    .runBackgroundReadUI(lockKey = CommonConstant.AST_CALLER_GLOBAL_ACTION, backgroundTask = { p ->
                        println("apiNode.treePath:${apiNode.tree_path}")
                        val resultElementType = PsiPathResolver.resolve(p, apiNode.tree_path)
                        var node: ApiNode? = null
                        val cacheService = ProjectCacheService.getInstance(p)
                        // TODO:清除节点中的request
                        when (resultElementType) {
                            is Module -> {
                                scanContext(ScanSession()) { session ->
                                    val routeRegistry =
                                        MethodNodeBuilder(project, session).scanModule(resultElementType)
                                    val newNode =
                                        BuildRootTree(project).buildModule(resultElementType) { directory, parentNode, pathPrefix, module ->
                                            BuildDirectoryTree(module, project).build(
                                                directory,
                                                parentNode,
                                                pathPrefix,
                                            ) { psiClass, pathPrefix ->
                                                BuildControllerNode(module, project).build(
                                                    psiClass, pathPrefix, routeRegistry
                                                )
                                            }
                                        }
                                    cacheService.saveModuleTree(resultElementType.name, newNode)
                                    node = newNode
                                }
                            }

                            is PsiDirectory -> {
                                val module = ModuleUtilCore.findModuleForPsiElement(resultElementType)
                                if (module != null) {
                                    val cacheResult = getCacheDataAndClean(cacheService, module, apiNode)
                                        ?: return@runBackgroundReadUI null
                                    val (moduleTree, _, oldParentNode) = cacheResult
                                    scanContext(ScanSession()) { session ->
                                        val routeRegistry =
                                            MethodNodeBuilder(project, session).scanDir(module, resultElementType)
                                        val newNode = BuildDirectoryTree(module, project).buildDir(
                                            resultElementType,
                                            oldParentNode.tree_path,
                                        ) { psiClass, pathPrefix ->
                                            BuildControllerNode(module, project).build(
                                                psiClass, pathPrefix, routeRegistry
                                            )
                                        }
                                        val replaceResult = moduleTree.replaceNodeByPath(apiNode.tree_path, newNode)
                                        if (replaceResult == 1) {
                                            cacheService.saveModuleTree(module.name, moduleTree)
                                            node = newNode
                                        }
                                    }
                                }

                            }

                            is PsiClass -> {
                                val module = ModuleUtilCore.findModuleForPsiElement(resultElementType)
                                if (module != null) {
                                    val cacheResult = getCacheDataAndClean(cacheService, module, apiNode)
                                        ?: return@runBackgroundReadUI null
                                    val (moduleTree, _, oldParentNode) = cacheResult
                                    scanContext(ScanSession()) { session ->
                                        val routeRegistry =
                                            MethodNodeBuilder(project, session).scanClass(module, resultElementType)
                                        val newNode = BuildControllerNode(module, project).build(
                                            resultElementType, oldParentNode.tree_path, routeRegistry
                                        ) ?: return@runBackgroundReadUI null
                                        val replaceResult = moduleTree.replaceNodeByPath(apiNode.tree_path, newNode)
                                        if (replaceResult == 1) {
                                            cacheService.saveModuleTree(module.name, moduleTree)
                                            node = newNode
                                        }
                                    }
                                }
                            }

                            is PsiMethod -> {
                                val module = ModuleUtilCore.findModuleForPsiElement(resultElementType)
                                if (module != null) {
                                    val cacheResult = getCacheDataAndClean(cacheService, module, apiNode)
                                        ?: return@runBackgroundReadUI null
                                    val (moduleTree, _, oldParentNode) = cacheResult
                                    scanContext(ScanSession()) { session ->
                                        val psiClass =
                                            resultElementType.containingClass ?: return@runBackgroundReadUI null
                                        val routeRegistry =
                                            MethodNodeBuilder(project, session).scanMethod(
                                                module,
                                                resultElementType,
                                                psiClass,
                                                oldParentNode
                                            )
                                        val newNode = BuildControllerNode(module, project).buildMethod(
                                            psiClass, oldParentNode.tree_path, routeRegistry
                                        ) ?: return@runBackgroundReadUI null
                                        val replaceResult = moduleTree.replaceNodeByPath(apiNode.tree_path, newNode)
                                        if (replaceResult == 1) {
                                            cacheService.saveModuleTree(module.name, moduleTree)
                                            node = newNode
                                        }
                                    }
                                }
                            }
                        }
                        node
                    }, uiUpdate = { node, p ->
                        if (node == null) {
                            Notifier.notifyWarning(p, "AsyncTest Caller", "树结构解析失败，请寻找父级节点进行更新")
                        } else {
                            refreshSpecificNode(targetNode, node)
                            Notifier.notifyInfo(p, "AsyncTest Caller", "刷新成功")
                        }
                        tree.isEnabled = true
                    })
            }
        })

        actionGroup.add(object : AnAction("上传 AsyncTest", "Upload to AsyncTest", AstCallerIcons.Logo) {
            override fun actionPerformed(event: AnActionEvent) {
                tree.isEnabled = false
                val cacheService = ProjectCacheService.getInstance(project)
                project.context()
                    .runBackgroundReadUI(lockKey = CommonConstant.AST_CALLER_GLOBAL_ACTION, backgroundTask = { p ->
                        println("apiNode.treePath:${apiNode.tree_path}")
                        var isModule = false
                        var hasMatch = false
                        var currentBindProject = ModuleSetting()
                        var currentUrl = ""
                        var currentToken = ""
                        val globalSetting = cacheService.getGlobalSettings()
                        if (globalSetting.usingPublic) {
                            currentUrl = globalSetting.publicServerUrl
                        } else {
                            currentUrl = globalSetting.customerServerUrl
                        }
                        if (currentUrl.isEmpty()) {
                            return@runBackgroundReadUI ValidType.NO_SET_URL
                        }
                        val privateInfo = cacheService.getPrivateInfo()
                        if (privateInfo.token.isEmpty()) {
                            return@runBackgroundReadUI ValidType.NO_TOKEN
                        } else {
                            currentToken = privateInfo.token
                        }
                        val moduleBindList: ArrayList<ModuleSetting> = cacheService.getModuleSetting(project.name)
                        val resultElementType = PsiPathResolver.resolve(p, apiNode.tree_path)
                        val module: Module
                        if (resultElementType is Module) {
                            isModule = true
                            module = resultElementType
                        } else {
                            module = ModuleUtilCore.findModuleForPsiElement(resultElementType as PsiElement)!!
                        }
                        moduleBindList.forEach { moduleSetting ->
                            if (moduleSetting.moduleInfo == module.name) {
                                currentBindProject = moduleSetting
                                hasMatch = true
                                return@forEach
                            }
                        }
                        if (!hasMatch) {
                            return@runBackgroundReadUI ValidType.NO_MATCH_MODULE_PROJECT
                        }
                        val collectDs = DependencyCollector(project, module.name).collect(apiNode)
                        return@runBackgroundReadUI CollectNodeData(
                            currentUrl,
                            currentToken,
                            currentBindProject,
                            collectDs,
                            apiNode,
                            isModule,
                            module.name
                        )
                    }, uiUpdate = { result, p ->
                        if (result == ValidType.NO_MATCH_MODULE_PROJECT) {
                            Notifier.notifyWarning(
                                project = project,
                                content = "没有找到该模块的绑定项目配置，请在Settings中进行配置。"
                            )
                            tree.isEnabled = true
                        } else if (result == ValidType.NO_SET_URL) {
                            Notifier.notifyWarning(
                                project = project,
                                content = "没有设置url，请在Settings中进行配置。"
                            )
                            tree.isEnabled = true
                        } else if (result == ValidType.NO_TOKEN) {
                            Notifier.notifyWarning(
                                project = project,
                                content = "没有设置token，请在Settings中进行配置。"
                            )
                            tree.isEnabled = true
                        } else {

                            project.context().runBackgroundReadUI(
                                lockKey = CommonConstant.AST_CALLER_GLOBAL_HTTP_ACTION,
                                requiresReadAction = false,
                                backgroundTask = { p ->
                                    val gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()
                                    val result: CollectNodeData = result as CollectNodeData
                                    // 2. 序列化并打印
                                    val requestBodyJson = gson.toJson(
                                        AsyncTestSyncTree(
                                            result.apiNode,
                                            result.dsMapping,
                                            result.projectInfo.bindProject.id,
                                            result.isModule
                                        )
                                    )
                                    val httpExecutor = HttpExecutor()
                                    httpExecutor.setMethod("POST")
                                    httpExecutor.setUrl(result.url + AsyncTestConstant.SYNC_TREE_NODE)
                                    httpExecutor.setHeader("content-type", "application/json")
                                    httpExecutor.setHeader("Authorization", result.token)
                                    httpExecutor.setBody(requestBodyJson)
                                    val response = httpExecutor.send()
                                    println("response:${response}")
                                    try {
                                        val element = JsonParser.parseString(response)
                                        val type = object : TypeToken<AstResponse<AsyncTestDataResponse>>() {}.type
                                        val responseObject: AstResponse<AsyncTestDataResponse> =
                                            Gson().fromJson(element, type)
                                        if (responseObject.result == 0) {
                                            return@runBackgroundReadUI responseObject.msg
                                        } else {
                                            val actionData = responseObject.msg.tree_action
                                            val dsChangeList = responseObject.msg.ds_change_list
                                            val updateRequest =
                                                AsyncTestUpdateDataRequest(
                                                    mutableMapOf(),
                                                    mutableMapOf(),
                                                    mutableListOf(),
                                                    result.projectInfo.bindProject.id,
                                                    result.isModule,
                                                )
                                            for ((key, value) in actionData.to_create) {
                                                val request = cacheService.getRequest(result.module, key)
                                                if (request != null) {
                                                    updateRequest.request_mapping[key] = request
                                                }
                                            }
                                            for ((key, value) in actionData.to_update) {
                                                val request = cacheService.getRequest(result.module, key)
                                                if (request != null) {
                                                    updateRequest.request_mapping[key] = request
                                                }
                                            }
                                            actionData.to_delete.forEach { content ->
                                                updateRequest.delete_request.add(content)
                                            }
                                            dsChangeList.forEach { content ->
                                                val ds = cacheService.getDataStructure(result.module, content)
                                                if (ds != null) {
                                                    updateRequest.ds_mapping[content] = ds
                                                }
                                            }
                                            val requestBodyJson = gson.toJson(updateRequest)
                                            println("requestBodyJson:${requestBodyJson}")
                                            val httpExecutor = HttpExecutor()
                                            httpExecutor.setMethod("POST")
                                            httpExecutor.setUrl(result.url + AsyncTestConstant.SYNC_TREE_DATA)
                                            httpExecutor.setHeader("content-type", "application/json")
                                            httpExecutor.setHeader("Authorization", result.token)
                                            httpExecutor.setBody(requestBodyJson)
                                            val response = httpExecutor.send()
                                            return@runBackgroundReadUI ValidType.SUCCESS
                                        }
                                    } catch (e: Exception) {
                                        return@runBackgroundReadUI ValidType.TO_JSON_FAILED
                                    }

                                },
                                uiUpdate = { result, p ->
                                    if (result == ValidType.TO_JSON_FAILED) {
                                        Notifier.notifyWarning(
                                            project = project,
                                            content = "AsyncTest服务错误，请联系作者<leesheldonparsons@gmail.com>。"
                                        )
                                    } else if (result == ValidType.SUCCESS) {
                                        Notifier.notifyInfo(
                                            project = project,
                                            content = "更新成功"
                                        )
                                    } else if (result is String) {
                                        Notifier.notifyWarning(
                                            project = project,
                                            content = result
                                        )
                                    }
                                }
                            )
                            tree.isEnabled = true
                        }


                    })
            }
        })

//        actionGroup.addSeparator()

        // --- 4. 【核心】创建并显示 Popup ---
        // "ApiTreePopup" 是一个 place 标识符，用于很多统计或内部逻辑，随便起个唯一的字符串即可
        val popupMenu = actionManager.createActionPopupMenu("ApiTreePopup", actionGroup)

        // 获取 component 并显示
        val component = popupMenu.component
        component.show(e.component, e.x, e.y)
    }

    fun getCacheDataAndClean(
        cacheService: ProjectCacheService,
        module: Module,
        apiNode: ApiNode
    ): Triple<ApiNode, ApiNode, ApiNode>? {
        val moduleTree =
            cacheService.getModuleTree(module.name) ?: return null
        val result = moduleTree.findNodeWithParent(apiNode.tree_path)
            ?: return null
        val (oldNode, oldParentNode) = result
        if (oldParentNode == null) return null
        oldNode.traverseFindRequest { requestString ->
            if (requestString != null) {
                cacheService.cleanRequest(module.name, requestString)
            }
        }
        return Triple(moduleTree, oldNode, oldParentNode)
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

    /**
     * 局部刷新：只更新指定节点及其子节点，不影响其他节点的展开状态
     * @param targetTreeNode 当前 UI 上选中的那个 DefaultMutableTreeNode
     * @param newApiNode 从后端拿回来的最新 ApiNode 数据
     */
    fun refreshSpecificNode(targetTreeNode: DefaultMutableTreeNode, newApiNode: ApiNode) {
        // 1. 记录当前节点是否是展开的 (reload 后可能会自动折叠，需要恢复)
        val treePath = TreePath(targetTreeNode.path)
        val isExpanded = tree.isExpanded(treePath)

        // 2. 更新当前节点的数据 (UserObject)
        targetTreeNode.userObject = newApiNode

        // 3. 【核心步骤】重构子节点结构
        // 先移除旧的子节点
        targetTreeNode.removeAllChildren()

        // 递归将新的 ApiNode 子节点转为 TreeNode 并添加进去
        // 假设你有一个方法叫 convertApiNodeToTreeNode(apiNode) -> DefaultMutableTreeNode
        // 或者手动循环：
        newApiNode.children.forEach { childApiNode ->
            val childTreeNode = childApiNode.toTreeNode() // 使用你之前的扩展方法
            targetTreeNode.add(childTreeNode)
        }

        // 4. 通知 Model 刷新该节点 (只会重绘 targetTreeNode 及其下方)
        // 这一步非常快，而且不会影响兄弟节点和父节点的展开状态
        treeModel.reload(targetTreeNode)

        // 5. 恢复之前的展开状态
        if (isExpanded) {
            tree.expandPath(treePath)
        }
    }
}