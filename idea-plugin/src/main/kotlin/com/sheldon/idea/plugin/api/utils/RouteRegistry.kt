package com.sheldon.idea.plugin.api.utils
import com.intellij.psi.PsiClass
import com.sheldon.idea.plugin.api.model.ApiNode
import java.util.concurrent.ConcurrentHashMap
data class RouteKey(
    val method: String,
    val fullUrl: String
)
class RouteRegistry {
    val moduleRegistryContainer = ConcurrentHashMap<String, MutableMap<RouteKey, RegistryEntry>>()
    data class RegistryEntry(
        val node: ApiNode,
        val psiClass: PsiClass
    )
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
                return
            }
        }
    }
    fun getApiNode(module: String, key: RouteKey, queryClass: PsiClass): ApiNode? {
        val routeMap = moduleRegistryContainer[module] ?: return null
        val entry = routeMap[key] ?: return null
        if (entry.psiClass == queryClass) {
            return entry.node
        }
        return null
    }
    fun clearModule(module: String) {
        moduleRegistryContainer.remove(module)
    }
}