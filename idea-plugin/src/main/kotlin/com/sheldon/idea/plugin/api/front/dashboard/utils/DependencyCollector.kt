package com.sheldon.idea.plugin.api.front.dashboard.utils

import com.intellij.openapi.project.Project
import com.sheldon.idea.plugin.api.method.AsyncTestBodyType
import com.sheldon.idea.plugin.api.method.AsyncTestType
import com.sheldon.idea.plugin.api.method.AsyncTestVariableNode
import com.sheldon.idea.plugin.api.model.ApiNode
import com.sheldon.idea.plugin.api.model.DataStructure
// 假设你的 CacheService 所在的包
import com.sheldon.idea.plugin.api.utils.ProjectCacheService

class DependencyCollector(private val project: Project, private val moduleName: String) {

    // 获取 CacheService 实例
    private val cacheService = project.getService(ProjectCacheService::class.java)

    // 结果集：使用 Set 自动去重，存储找到的 dsTarget
    private val collectedDependencies = mutableMapOf<String, String>()

    // 防止数据结构循环引用导致的死循环 (比如 A -> B -> A)
    // 记录已经展开过的 dsTarget
    private val expandedDefinitions = mutableSetOf<String>()

    /**
     * 入口函数：传入根 ApiNode 或任意 ApiNode
     */
    fun collect(node: ApiNode): Map<String, String> {
        collectedDependencies.clear()
        expandedDefinitions.clear()
        traverseApiNode(node)
        return collectedDependencies
    }

    /**
     * 1. 递归遍历 ApiNode 树
     */
    private fun traverseApiNode(node: ApiNode) {
        // 步骤 1: 判断 code_type 是否为 3
        if (node.code_type == 3) {
            processInterfaceNode(node)
        }

        // 递归处理 ApiNode 的子节点
        node.children.forEach { child ->
            traverseApiNode(child)
        }
    }

    /**
     * 2-5. 处理单个接口节点
     */
    private fun processInterfaceNode(node: ApiNode) {
        val requestPath = node.request ?: return

        // 步骤 3: 调用 cacheService 获取 ApiRequest
        val apiRequest = cacheService.getRequest(moduleName, requestPath) ?: return

        // 步骤 5: 判断 bodyType 是否为 JSON
        if (apiRequest.bodyType == AsyncTestBodyType.JSON) {
            // 获取 Json 列表的第一个节点
            val rootVarNode = apiRequest.json.firstOrNull() ?: return

            // 开始处理变量节点
            processVariableNode(rootVarNode)
        }
    }

    /**
     * 6-7. 递归处理变量节点 (最关键的部分)
     */
    private fun processVariableNode(varNode: AsyncTestVariableNode) {
        // 步骤 6: 判断是否为 ds 类型
        if (varNode.type == AsyncTestType.DS) {
            val target = varNode.dsTarget
            if (!target.isNullOrBlank()) {
                // 修改点 2: 无论有没有 children，先尝试获取 DataStructure 对象
                // 因为我们需要它的 Hash
                val dataStructure = cacheService.getDataStructure(moduleName, target)

                if (dataStructure != null) {
                    // 记录 Hash (Map 会自动去重/更新)
                    collectedDependencies[target] = dataStructure.hash

                    // 步骤 7: 关键分叉点
                    if (varNode.children.isNotEmpty()) {
                        // 情况 A: 节点自身有 children (本地覆盖/快照)
                        // 我们不需要展开 dataStructure 的定义，而是继续遍历本地的 children
                        // 因为本地 children 里面可能还嵌套了其他的 ds
                        varNode.children.forEach { child ->
                            processVariableNode(child)
                        }
                    } else {
                        // 情况 B: 节点 children 为空，说明是纯引用
                        // 我们需要展开 dataStructure 的定义来寻找深层依赖
                        expandDataStructureDefinition(target, dataStructure)
                    }
                }
            }
        } else {
            // 普通节点 (object/array/string...)，继续递归寻找子节点
            varNode.children.forEach { child ->
                processVariableNode(child)
            }
        }
    }

    /**
     * 步骤 7 补充: 查找并展开 DataStructure
     */
    private fun expandDataStructureDefinition(target: String, dataStructure: DataStructure) {
        // 死循环防御：如果这个 DS 的定义已经被展开过，就不要再展开了
        if (expandedDefinitions.contains(target)) {
            return
        }
        expandedDefinitions.add(target)

        // 遍历 DataStructure 定义中的 data 列表
        dataStructure.data.forEach { dsChildNode ->
            processVariableNode(dsChildNode)
        }
    }
}