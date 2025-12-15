package com.sheldon.idea.plugin.api.utils

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import java.util.concurrent.ConcurrentHashMap

@Service(Service.Level.APP)
class GlobalObjectStorageService : Disposable {

    private val storage = ConcurrentHashMap<String, Any>()

    fun <T : Any> save(key: String, node: T) {
        storage[key] = node
    }

    fun getRaw(key: String): Any? {
        return storage[key]
    }

    inline fun <reified T> get(key: String): T? {
        return getRaw(key) as? T
    }

    fun contains(key: String): Boolean = storage.containsKey(key)

    fun clear() {
        storage.clear()
    }

    fun acquireLock(lockKey: String): Boolean {
        return storage.putIfAbsent(lockKey, true) == null
    }

    fun releaseLock(lockKey: String) {
        storage.remove(lockKey)
    }

    override fun dispose() {
        storage.clear()
        println("IDEA is closing, GlobalObjectStorageService disposed.")
    }
}






















