package com.sheldon.idea.plugin.api.utils

import com.intellij.psi.PsiClass
import com.sheldon.idea.plugin.api.model.ApiNode
import java.util.concurrent.ConcurrentHashMap

data class RouteKey(
    val method: String, // "GET", "POST"
    val fullUrl: String // "/api/v1/methods/simple"
)

class RouteRegistry {

    // 缓冲池结构：Module -> (RouteKey -> RegistryEntry)
    // 使用 ConcurrentHashMap 保证模块级别的并发安全
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
        // computeIfAbsent 保证模块 Map 的初始化是线程安全的
        val routeMap = moduleRegistryContainer.computeIfAbsent(module) { mutableMapOf() }

        // 对该模块的路由表加锁，防止多线程扫描同一个模块时的竞争条件
        synchronized(routeMap) {
            val existingEntry = routeMap[key]

            // 1. 如果还没有这个路由 -> 直接注册
            if (existingEntry == null) {
                routeMap[key] = RegistryEntry(newNode, newClass)
                return
            }

            val oldClass = existingEntry.psiClass

            // 2. 发生路由冲突！开始“认亲” (判断继承关系)

            // 情况 A: 新来的类(newClass) 是 旧类(oldClass) 的子类
            // 意味着：子类重写了父类的方法 -> 【子类胜出】，覆盖旧的
            // 使用 checkDeep=true 进行深度继承检查
            if (newClass.isInheritor(oldClass, true)) {
                routeMap[key] = RegistryEntry(newNode, newClass)
                return
            }

            // 情况 B: 旧类(oldClass) 是 新类(newClass) 的子类
            // 意味着：我们先扫描到了子类，现在又扫描到了父类 -> 【子类依然胜出】，忽略新来的父类
            if (oldClass.isInheritor(newClass, true)) {
                return // 什么都不做，保留现有的(子类的)
            }

            // 情况 C: 两个类没有继承关系 (完全不同的类定义了相同的 URL)
            // 这是一个真正的路由冲突 (Ambiguous Mapping)。
            // 在这里你可以选择：
            // 1. 覆盖 (Last Win) -> routeMap[key] = RegistryEntry(newNode, newClass)
            // 2. 保留第一个 (First Win) -> return
            // 3. 记录错误 -> 将冲突信息存入 Node 用于 UI 展示
            // 目前保持“先入为主”策略，或者你可以根据需求解开注释覆盖
            // routeMap[key] = RegistryEntry(newNode, newClass)
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

        // 不需要加锁，读操作通常不需要严格锁，或者根据情况加锁
        val entry = routeMap[key] ?: return null

        // 核心判断：只有当注册表里的 owner 等于当前查询的 class 时，才算找到
        // 这意味着：如果父类被子类覆盖了，传入父类 class 查询时，这里会返回 null
        if (entry.psiClass == queryClass) {
            return entry.node
        }

        // 也就是：虽然这个 Key 存在，但它属于别人（子类），当前类(父类)的这个方法被隐藏了
        return null
    }

    /**
     * 清空指定模块的缓存 (通常在重新构建/清理时使用)
     */
    fun clearModule(module: String) {
        moduleRegistryContainer.remove(module)
    }
}