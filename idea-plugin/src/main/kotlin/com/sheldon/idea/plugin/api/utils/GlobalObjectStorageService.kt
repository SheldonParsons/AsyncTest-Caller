package com.sheldon.idea.plugin.api.utils

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import java.util.concurrent.ConcurrentHashMap

@Service(Service.Level.APP)
class GlobalObjectStorageService : Disposable {

    // 使用并发集合，因为 Service 可能会在不同线程被访问
    private val storage = ConcurrentHashMap<String, Any>()

    // 存
    fun <T : Any> save(key: String, node: T) {
        storage[key] = node
    }

    fun getRaw(key: String): Any? {
        return storage[key]
    }

    // 取
    inline fun <reified T> get(key: String): T? {
        val value = getRaw(key)
        return value as? T
    }

    // 查
    fun contains(key: String): Boolean = storage.containsKey(key)

    // 清空 (例如用户点击了清除缓存按钮)
    fun clear() {
        storage.clear()
    }

    // IDEA 关闭时自动调用
    override fun dispose() {
        storage.clear()
        println("IDEA is closing, GlobalObjectStorageService disposed.")
    }
}

//import com.intellij.openapi.application.ApplicationManager
//
//val service = ApplicationManager.getApplication().getService(GlobalObjectStorageService::class.java)
//
//// --- 存 ---
//service.save("my.int.config", 100)
//service.save("my.user.node", AsyncTestVariableNode("user", "ds"))
//service.save("my.list.data", listOf("A", "B"))
//
//// --- 取 (自动推断类型) ---
//
//// 1. 显式指定类型
//val count: Int? = service.get<Int>("my.int.config")
//
//// 2. 根据变量类型自动推断
//val node: AsyncTestVariableNode? = service.get("my.user.node")
//
//// 3. 复杂类型 (List)
//val list: List<String>? = service.get("my.list.data")
//
//// --- 安全性测试 ---
//// 如果你尝试用错误的类型去取，它不会报错，而是返回 null
//val wrongType: String? = service.get<String>("my.int.config") // 结果是 null