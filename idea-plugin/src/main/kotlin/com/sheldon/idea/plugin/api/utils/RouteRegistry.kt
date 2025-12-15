package com.sheldon.idea.plugin.api.utils

import com.intellij.psi.PsiClass
import com.sheldon.idea.plugin.api.model.ApiNode
import java.util.concurrent.ConcurrentHashMap

data class RouteKey(
    val method: String, // "GET", "POST"
    val fullUrl: String // "/api/v1/methods/simple"
)

class RouteRegistry {

    private val moduleRegistryContainer = ConcurrentHashMap<String, MutableMap<RouteKey, RegistryEntry>>()

    data class RegistryEntry(
        val node: ApiNode,
        val psiClass: PsiClass // 记录是谁定义了这个接口
    )

    /**
     * 尝试注册一个接口节点
     * @param module 模块名称 (作为隔离命名空间)
     */
    fun register(key: RouteKey, newNode: ApiNode, newClass: PsiClass, module: String) {

        val routeMap = moduleRegistryContainer.computeIfAbsent(module) { mutableMapOf() }

        synchronized(routeMap) {
            val existingEntry = routeMap[key]

            if (existingEntry == null) {
                routeMap[key] = RegistryEntry(newNode, newClass)
                return
            }
            val oldClass = existingEntry.psiClass
            if (newClass.isInheritor(oldClass, true)) {
                routeMap[key] = RegistryEntry(newNode, newClass)
                return
            }
            if (oldClass.isInheritor(newClass, true)) {
                return // 什么都不做，保留现有的(子类的)
            }
        }
    }

    /**
     * 【新增】通过 module, key, psiClass 获取 ApiNode
     *
     * 逻辑：
     * 1. 去路由表里找这个 Key 对应的“胜出者”。
     * 2. 如果找到了，且胜出者就是当前传入的 psiClass，则返回 Node。
     * 3. 如果胜出者是别人（比如子类），或者根本没这个 Key，则返回 null。
     */
    fun getApiNode(module: String, key: RouteKey, queryClass: PsiClass): ApiNode? {
        val routeMap = moduleRegistryContainer[module] ?: return null

        val entry = routeMap[key] ?: return null


        if (entry.psiClass == queryClass) {
            return entry.node
        }

        return null
    }

    /**
     * 清空指定模块的缓存
     */
    fun clearModule(module: String) {
        moduleRegistryContainer.remove(module)
    }
}